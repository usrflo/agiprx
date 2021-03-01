/*******************************************************************************
 * Copyright (C) 2021 Florian Sager, www.agitos.de
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.agitos.agiprx.bean.processor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dao.RelationType;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.UserContext;
import de.agitos.agiprx.util.Validator;

public class SshProxyProcessor extends AbstractProcessor implements DependencyInjector {

	private static SshProxyProcessor BEAN;

	// private final static String HOME_ROOT_DIR = "/home/";
	protected String homeRootDirectory;

	// @Value("${agiprx.defaultSshKey:/opt/agiprx/etc/prx_rsa}")
	private String defaultSshKeyFullpath;

	protected ConsoleWrapper console;

	private ProjectDao projectDao;

	protected Validator validator;

	private UserContext userContext;

	private ProxySyncProcessor proxySyncProcessor;

	public SshProxyProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		homeRootDirectory = Config.getBean().getString("agiprx.homeDirectory", "/home/");
		defaultSshKeyFullpath = Config.getBean().getString("agiprx.defaultSshKey", "/opt/agiprx/etc/prx_rsa");
	}

	@Override
	public void postConstruct() {
		console = ConsoleWrapper.getBean();
		projectDao = ProjectDao.getBean();
		validator = Validator.getBean();
		userContext = UserContext.getBean();
		proxySyncProcessor = ProxySyncProcessor.getBean();
	}

	public static SshProxyProcessor getBean() {
		return BEAN;
	}

	public void manageConfiguration(boolean verbose) throws IOException, InterruptedException, AbortionException {

		Set<String> processedUserAccounts = new HashSet<String>();

		// HINT: all projects are processed; only user's projects are shown in verbose
		// mode
		for (Project project : projectDao.findAllAsAdmin(EnumSet.of(RelationType.CONTAINER))) {

			boolean projectVerbose = verbose && userContext.isUserAllowed(project.getLabel());

			if (projectVerbose) {
				console.printlnfStress("Project %s (%s)", project.getLabel(), project.getFullname());
			}
			processedUserAccounts.addAll(manageConfiguration(project, projectVerbose));
			if (projectVerbose) {
				console.printlnf("\n");
			}
		}

		// CLEANUP: remove all accounts in /home that were not processed
		cleanupProxyUsers(processedUserAccounts);
	}

	protected void cleanupProxyUsers(Set<String> keepUsernames)
			throws IOException, InterruptedException, AbortionException {

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(homeRootDirectory))) {
			for (Path path : directoryStream) {

				if (!Files.isDirectory(path)) {
					continue;
				}

				String username = path.getFileName().toString();

				// skip all non proxy accounts
				if (!validator.isSshProxyUsername(username)) {
					continue;
				}

				// skip / keep all processed usernames
				if (keepUsernames.contains(username)) {
					continue;
				}

				// remove account if not processed anymore
				if (userExists(username)) {
					removeUser(username);
				}
			}
		} catch (IOException ex) {
			throw ex;
		}
	}

	public Set<String> manageConfiguration(Project project, boolean verbose)
			throws IOException, InterruptedException, AbortionException {

		Set<String> processedUserAccounts = new HashSet<String>();

		for (Container container : project.getContainers()) {

			// set back-reference
			container.setProject(project);

			if (verbose) {
				console.printlnfStress("  Container %s (%s)", container.getLabel(), container.getIpv6());
			}

			processedUserAccounts.addAll(manageConfiguration(container, verbose));
		}

		return processedUserAccounts;
	}

	public Set<String> manageConfiguration(Container container, boolean verbose)
			throws IOException, InterruptedException, AbortionException {

		Set<String> processedUserAccounts = new HashSet<String>();

		Map<String, SshProxyUser> prxUsers = new HashMap<>();

		for (ContainerPermission permission : container.getContainerPermissions()) {

			// set back-reference
			permission.setContainer(container);

			String sshProxyUsername = permission.getSshProxyUsername();

			if (!prxUsers.containsKey(sshProxyUsername)) {
				prxUsers.put(sshProxyUsername, new SshProxyUser(sshProxyUsername, permission.getPassword(),
						permission.getPermission(), container));
			}

			prxUsers.get(sshProxyUsername).addAllowedUser(permission.getUser());
		}

		// process SshProxyUsers
		for (SshProxyUser prxUser : prxUsers.values()) {

			if (verbose) {
				console.printlnf("    # %s%s", prxUser.getProxyUsername(),
						prxUser.getProxyPassword() != null ? " with password" : "");
			}

			// check if user exists, opt. creation
			if (!userExists(prxUser.getProxyUsername())) {
				createUser(prxUser);
			}

			if (prxUser.getProxyPassword() != null) {
				// set password if available
				if (execWithInput("chpasswd", prxUser.getProxyUsername() + ":" + prxUser.getProxyPassword()) != 0) {
					throw new AbortionException("Setting password for user " + prxUser.getProxyUsername() + " failed");
				}
			} else {
				// reset password if not available
				exec(0, "passwd", "-d", prxUser.getProxyUsername());
			}

			// re-generate login shell script
			generateLoginShellScript(prxUser);

			// re-generate authorized_keys file
			generateAuthorizedKeysFile(prxUser, verbose);

			if (proxySyncProcessor.isMasterInstance()) {
				// write ProxyUsername to container for scpwd.sh
				writeProxyUsernameToContainer(prxUser, verbose);
			}

			// add username to list of processed accounts
			processedUserAccounts.add(prxUser.getProxyUsername());
		}

		return processedUserAccounts;
	}

	private void writeProxyUsernameToContainer(SshProxyUser prxUser, boolean verbose) {

		StringBuilder out = new StringBuilder();
		StringBuilder errOut = new StringBuilder();
		try {
			// exec(out, errOut, "ssh", "-p22", "-6", "-i" + defaultSshKeyFullpath,
			// "-oStrictHostKeyChecking=no",
			// prxUser.getContainerUsername() + "@" + prxUser.getContainer().getIpv6(),
			// "\"/bin/echo 'LOGIN="
			// + prxUser.getProxyUsername() + "@proxy.agitos.de' >
			// /root/tools/scpwd.conf\"");

			exec(out, errOut, "ssh", "-p22", "-6", "-i" + defaultSshKeyFullpath, "-oStrictHostKeyChecking=no",
					prxUser.getContainerUsername() + "@" + prxUser.getContainer().getIpv6(),
					"/bin/bash -c \"/bin/echo 'LOGIN=" + prxUser.getProxyUsername() + "@proxy.agitos.de' > ~/.scpwd\"");

		} catch (Exception e) {
			if (verbose) {
				console.printlnfError("Error writing .scpwd for %s on container %s: %s", prxUser.getContainerUsername(),
						prxUser.getContainer().getFQLabel(), e.getMessage());
			}
		}
		if (verbose) {
			if (out.length() > 0) {
				console.printlnf(out.toString());
			}
			if (errOut.length() > 0) {
				console.printlnfError(errOut.toString());
			}
		}
	}

	protected boolean userExists(String username) throws IOException, InterruptedException {
		// id -u <username>: status code == 0 --> exists
		return exec("id", "-u", username) == 0;
	}

	protected void createUser(SshProxyUser prxUser) throws IOException, InterruptedException, AbortionException {
		exec(0, "useradd", "-d", prxUser.getHomeDirectory(), "-s", prxUser.getLoginShellScriptFile(),
				prxUser.getProxyUsername());
		exec(0, "mkdir", "-p", prxUser.getSshDirectory());
		exec(0, "cp", defaultSshKeyFullpath, prxUser.getSshPrivateKeyFile());
		// turn off the login banner
		exec(0, "touch", prxUser.getHushLoginFile());
		exec(0, "chown", "-R", prxUser.getProxyUsername() + "." + prxUser.getProxyUsername(),
				prxUser.getHomeDirectory());
	}

	private void removeUser(String username) throws IOException, InterruptedException, AbortionException {
		exec(0, "userdel", "-rf", username);
	}

	private void generateLoginShellScript(SshProxyUser prxUser)
			throws IOException, InterruptedException, AbortionException {
		List<String> lines = new ArrayList<>();

		lines.add("#!/bin/bash");
		lines.add("export SSH_ORIGINAL_COMMAND=${*:2}");

		StringBuilder buf = new StringBuilder();
		buf.append("ssh -t -p22 -6 -oLogLevel=QUIET -oSendEnv=SSH_LOGIN_USER -oStrictHostKeyChecking=no -i ");
		buf.append(prxUser.getSshPrivateKeyFile()).append(" ");
		buf.append(prxUser.getContainerUsername()).append("@").append(prxUser.getContainer().getIpv6());
		buf.append(" $SSH_ORIGINAL_COMMAND");
		lines.add(buf.toString());

		Path file = Paths.get(prxUser.getLoginShellScriptFile());
		Files.write(file, lines, Charset.forName("latin1"));

		exec(0, "chmod", "755", prxUser.getLoginShellScriptFile());
	}

	private void generateAuthorizedKeysFile(SshProxyUser prxUser, boolean verbose)
			throws IOException, InterruptedException, AbortionException {
		List<String> lines = new ArrayList<>();

		for (User allowedUser : prxUser.getAllowedUsers()) {

			if (verbose) {
				console.printlnf("      %s (%s)", allowedUser.getFullname(), allowedUser.getEmail());
			}

			lines.add(generateAuthorizedKeysLine(prxUser, allowedUser));
		}

		Path file = Paths.get(prxUser.getAuthorizedKeysFile());
		Files.write(file, lines, Charset.forName("latin1"));

		exec(0, "chown", prxUser.getProxyUsername() + "." + prxUser.getProxyUsername(),
				prxUser.getAuthorizedKeysFile());
	}

	private String generateAuthorizedKeysLine(SshProxyUser prxUser, User allowedUser) {

		StringBuilder buf = new StringBuilder();

		// environment="SSH_USER=sager@agitos.de"

		buf.append("environment=\"SSH_LOGIN_USER=");
		buf.append(prxUser.getProxyUsername());
		buf.append("\" ");

		// ssh-rsa
		// AAAAB3NzaC1yc2EAAAADAQABAAABAQDRA8T6Td/OGEyoxr0IK42K3hq6jcx8kYg9eJoa72lTcazOI7o4gTW1LRpdRzmc4VfdTbWtii8rIHtQG8AGFZHnlcCRqxG36QmWq8/RwexdbC3fLgSPJXfEyOSg5I99Os1ixjaqWomXaDf+YpFDM+oBIC0WfBedmZ44Ef95Nvo9HefotjBc+PwqX0vyn2wYczdJd7n9JeHi9HCbWcxtoxAgafWx9o77fUdDE6lfPaCV7NgjDaVkj/CLkKl3ICJ4R9j3SrCmdfmbzwm3i6n+v6zYCEzQhDYkBDaYdvKfEwuVdSvxWcjP3StwCdHSxonuIFVeTjWsXfT2DrZDZeztq5Ct
		// sager@agitos.de

		buf.append(allowedUser.getSshPublicKey());

		return buf.toString();
	}

	protected class SshProxyUser {

		private final String proxyUsername;

		private final String proxyPassword;

		private final String containerUsername;

		private final Container container;

		private List<User> allowedUsers;

		public SshProxyUser(String proxyUsername, String proxyPassword, String containerUsername, Container container) {
			this.proxyUsername = proxyUsername;
			this.proxyPassword = proxyPassword;
			this.containerUsername = containerUsername;
			this.container = container;
			this.allowedUsers = new ArrayList<>();
		}

		public String getProxyUsername() {
			return proxyUsername;
		}

		public String getProxyPassword() {
			return proxyPassword;
		}

		public String getContainerUsername() {
			return containerUsername;
		}

		public String getHomeDirectory() {
			return homeRootDirectory + proxyUsername + "/";
		}

		public String getSshDirectory() {
			return homeRootDirectory + proxyUsername + "/.ssh/";
		}

		public String getSshPrivateKeyFile() {
			return homeRootDirectory + proxyUsername + "/.ssh/prx_rsa";
		}

		public String getAuthorizedKeysFile() {
			return homeRootDirectory + proxyUsername + "/.ssh/authorized_keys";
		}

		public String getHushLoginFile() {
			return homeRootDirectory + proxyUsername + "/.hushlogin";
		}

		public String getLoginShellScriptFile() {
			return homeRootDirectory + proxyUsername + "/sshprx.sh";
		}

		public Container getContainer() {
			return container;
		}

		public List<User> getAllowedUsers() {
			return allowedUsers;
		}

		public void addAllowedUser(User u) {
			this.allowedUsers.add(u);
		}
	}
}
