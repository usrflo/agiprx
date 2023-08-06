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
package de.agitos.agiprx.executor;

import de.agitos.agiprx.AgiPrx;
import de.agitos.agiprx.bean.SearchBean;
import de.agitos.agiprx.bean.processor.AgiPrxSshAuthProcessor;
import de.agitos.agiprx.bean.processor.DatabaseBackupProcessor;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.bean.processor.ProxySyncProcessor;
import de.agitos.agiprx.bean.processor.SshProxyProcessor;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.exception.ExitException;
import de.agitos.agiprx.exception.GotoTopException;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.UserContext;

public class MainExecutor extends AbstractExecutor {

	private static MainExecutor BEAN;

	private HostExecutor hostExecutor;

	private UserExecutor userExecutor;

	private ApiUserExecutor apiUserExecutor;

	private ProjectExecutor projectExecutor;

	private HAProxyProcessor haProxyProcessor;

	private DatabaseBackupProcessor databaseBackupProcessor;

	private SshProxyProcessor sshProxyProcessor;

	private AgiPrxSshAuthProcessor agiPrxSshAuthProcessor;

	private ProxySyncProcessor proxySyncProcessor;

	private SearchBean searchBean;

	private UserContext userContext;

	public MainExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		hostExecutor = HostExecutor.getBean();
		userExecutor = UserExecutor.getBean();
		apiUserExecutor = ApiUserExecutor.getBean();
		projectExecutor = ProjectExecutor.getBean();
		haProxyProcessor = HAProxyProcessor.getBean();
		databaseBackupProcessor = DatabaseBackupProcessor.getBean();
		sshProxyProcessor = SshProxyProcessor.getBean();
		agiPrxSshAuthProcessor = AgiPrxSshAuthProcessor.getBean();
		proxySyncProcessor = ProxySyncProcessor.getBean();
		searchBean = SearchBean.getBean();
		userContext = UserContext.getBean();
	}

	public static MainExecutor getBean() {
		return BEAN;
	}

	public void run() throws Exception {

		// the configuration is managed by the master instance only, so exit on slave
		// instances
		if (!proxySyncProcessor.isMasterInstance()) {

			if (proxySyncProcessor.getMasterIp() == null) {
				console.printlnfError(
						"This AgiPrx instance is running as a slave but without any master ip setup; please fix your configuration and connect to the master instance only for configuration.");
			} else {
				console.printlnfError(
						"This AgiPrx instance is running as a slave, please connect to the master instance at %s for configuration",
						proxySyncProcessor.getMasterIp());
			}
			return;
		}

		console.printlnf("Version %s\n", AgiPrx.version);

		boolean isAdmin = userContext.isAdmin();

		console.printlnfStress("===============================");
		console.printlnfStress("AgiPrx CONFIG ENTITIES OVERVIEW");
		console.printlnfStress("===============================\n");

		if (isAdmin) {
			console.printlnfStress("Users: user contact data with SSH keys that get granted to access containers");
			console.printlnfStress("API-Users: technical user data to access the AgiPrx REST API");
			console.printlnfStress("Hosts: hosts that serve containers");
		}
		console.printlnfStress(
				"Projects: customer projects comprise project containers, user permissions, backends and customer domain names");
		console.printlnfStress("  > Containers: project containers");
		console.printlnfStress("    > Permissions: user-references and permission levels to access containers");
		console.printlnfStress("  > Backends: http-port-references to single or multiple containers");
		console.printlnfStress("    > Domains: backend assigned domain names with SSL certificate configuration");

		console.printlnf("");

		if (AgiPrx.PRODUCTION) {
			console.printf("Starting initial DB backup ... ");
			console.flush();
			databaseBackupProcessor.createBackup("pre");
			console.printlnf("finished.\n\n");
		}

		console.printlnfStress("==========================");
		console.printlnfStress("COMMON NAVIGATION OVERVIEW");
		console.printlnfStress("==========================\n");

		printHelp(CMD_HELP, "show a list of available commands");
		printHelp(CMD_ABORT + " or " + CMD_CANCEL, "cancel the current operation");
		printHelp(CMD_DIRUP + " or " + CMD_CDUP, "return to the parent navigation level");
		printHelp(CMD_TOP, "return to the main menu");
		printHelp(CMD_EXIT + " or " + CMD_QUIT, "exit the tool");

		console.printlnf("");

		console.printlnfStress("=========");
		console.printlnfStress("MAIN MENU");
		console.printlnfStress("=========\n");

		help();

		setCommandCompletion();

		try {

			String out;
			while (true) {
				out = console.readLine();

				try {
					handleExitOrAbort(out);

					if (isCommand(CMD_HELP, out) || isCommand("ls", out)) {
						help();
					} else if (isAdmin && isCommand(CMD_HOSTS, out)) {
						hostExecutor.run();
					} else if (isAdmin && isCommand(CMD_USERS, out)) {
						userExecutor.run();
					} else if (isAdmin && isCommand(CMD_APIUSERS, out)) {
						apiUserExecutor.run();
					} else if (isCommand(CMD_PROJECTS, out)) {
						projectExecutor.run();
					} else if (isCommandWithParam(CMD_FIND, out)) {
						projectExecutor.find(getStringParam(out));
					} else if (isAdmin && isCommandWithParam(CMD_JUMP, out)) {
						searchBean.jump(getStringParam(out));
					} else if (isCommandWithParam(CMD_CERTS, out)) {
						haProxyProcessor.listCerts(getStringParam(out));
					} else if (isCommand(CMD_CERTS, out)) {
						haProxyProcessor.listCerts(null);
					} else if (isCommand(CMD_GENHAPRX, out)) {
						haProxyProcessor.manageConfiguration(true, true);
					} else if (isCommand(CMD_WRITESSHPRX, out)) {
						sshProxyProcessor.manageConfiguration(true);
						agiPrxSshAuthProcessor.manageConfiguration(true);
						console.printlnfStress("Updated SSH user configuration.");
					} else if (proxySyncProcessor.isSyncRequired() && isCommand(CMD_SYNCSLAVES, out)) {
						proxySyncProcessor.syncToSlaveInstances(true, null);
					} else {
						console.printlnfError("Incorrect command %s", out);
						help();
					}
				} catch (GotoTopException e) {
					// do nothing, just continue in top menu
				}

			}
		} catch (AbortionException | ExitException e) {
			// do nothing
			if (e.getMessage() != null) {
				console.printlnfError(e.getMessage());
			}
		} finally {

			if (AgiPrx.PRODUCTION) {
				console.printf("Starting final DB backup ... ");
				console.flush();
				databaseBackupProcessor.createBackup("post");
				console.printlnf("finished.\n");
			}
		}
	}

	private void help() {
		printHelp(CMD_PROJECTS, "list, add, edit and delete projects");
		printHelp(CMD_FIND + " *domain*",
				"list project/backend/container that refer to a given domain, *-wildcard supported");

		if (userContext.isAdmin()) {
			printHelp(CMD_JUMP + " [p|r|c|b|n] <*query*>",
					"jump to element, the first matching project, user, container, backend, domain or IPv6 is opened");

			printHelp(CMD_USERS, "list, add, edit and delete users");
			printHelp(CMD_APIUSERS, "list, add, edit and delete API users");
			printHelp(CMD_HOSTS, "list, add, edit and delete hosts");
		}

		printHelp(CMD_CERTS + " [*domain*]",
				"list configured and currently valid SSL certificates, optionally filter by domain, *-wildcard supported");
		printHelp(CMD_GENHAPRX, "generate HAProxy config and reload proxy");
		printHelp(CMD_WRITESSHPRX, "update ssh proxy configuration");
		if (proxySyncProcessor.isSyncRequired()) {
			printHelp(CMD_SYNCSLAVES, "sync configuration to slave instance(s)");
		}
		printHelp(CMD_EXIT + " or " + CMD_QUIT, "exit from config tool");
	}

	protected void setCommandCompletion() {

		if (proxySyncProcessor.isSyncRequired()) {
			console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT,
					CMD_PROJECTS, CMD_FIND, CMD_JUMP, CMD_USERS, CMD_APIUSERS, CMD_HOSTS, CMD_CERTS, CMD_GENHAPRX,
					CMD_WRITESSHPRX, CMD_SYNCSLAVES);

		} else {
			console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT,
					CMD_PROJECTS, CMD_FIND, CMD_JUMP, CMD_USERS, CMD_APIUSERS, CMD_HOSTS, CMD_CERTS, CMD_GENHAPRX,
					CMD_WRITESSHPRX);
		}
	}

}
