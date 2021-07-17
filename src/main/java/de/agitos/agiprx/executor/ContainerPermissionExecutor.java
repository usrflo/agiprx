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

import java.util.List;

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.bean.processor.SshProxyProcessor;
import de.agitos.agiprx.dao.ContainerPermissionDao;
import de.agitos.agiprx.dao.UserDao;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.output.table.ConsoleTableBuffer;
import de.agitos.agiprx.output.table.LongColumn;
import de.agitos.agiprx.output.table.Row;
import de.agitos.agiprx.output.table.StringColumn;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.EmailSender;
import de.agitos.agiprx.util.ListUtils;
import de.agitos.agiprx.util.PasswordGenerator;
import de.agitos.agiprx.util.SshKeyConverter;

public class ContainerPermissionExecutor extends AbstractExecutor {

	private static ContainerPermissionExecutor BEAN;

	private UserExecutor userExecutor;

	private UserDao userDao;

	private ContainerPermissionDao permissionDao;

	private EmailSender emailSender;

	private SshProxyProcessor sshProxyProcessor;

	// @Value("${proxy.domainname}")
	private String proxyDomainname;

	// @Value("${proxy.port}")
	private Integer proxyPort;

	// TODO : make configurable
	private Integer passwordEmailDelaySeconds = 30;

	public ContainerPermissionExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		proxyDomainname = Config.getBean().getString("proxy.domainname");
		proxyPort = Config.getBean().getInteger("proxy.port");
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		userExecutor = UserExecutor.getBean();
		userDao = UserDao.getBean();
		permissionDao = ContainerPermissionDao.getBean();
		emailSender = EmailSender.getBean();
		sshProxyProcessor = SshProxyProcessor.getBean();
	}

	public static ContainerPermissionExecutor getBean() {
		return BEAN;
	}

	public void run(Container container) throws Exception {

		list(container);

		help();

		setCommandCompletion();

		String out;

		while (true) {
			out = console
					.readLine("PROJ " + container.getProject().getLabel() + " CNTR " + container.getLabel() + " PERM");

			try {
				handleExitOrAbort(out);
			} catch (AbortionException e) {
				return;
			}

			if (isCommand(CMD_HELP, out)) {
				help();
			} else if (isCommand(CMD_LS, out)) {
				list(container);
			} else if (isCommand(CMD_GROUPADD, out)) {
				groupAdd(container);
			} else if (isCommand(CMD_ADD, out)) {
				insert(container, new ContainerPermission());
			} else if (isCommandWithParam(CMD_EDIT, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					update(container, permissionDao.find(id));
				}
			} else if (isCommand(CMD_INFORMALL, out)) {
				informAll(container);
			} else if (isCommandWithParam(CMD_INFORM, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					inform(container, permissionDao.find(id), true);
				}
			} else if (isCommandWithParam(CMD_DEL, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					delete(container, permissionDao.find(id));
				}
			} else if (isCommand(CMD_WRITESSHPRX, out)) {
				sshProxyProcessor.manageConfiguration(container, true);
				console.printlnfStress("Updated SSH user configuration on this container.");
			} else {
				console.printlnfError("Incorrect command %s", out);
				help();
			}
		}
	}

	private void list(Container container) {

		console.printlnfStress("Container permissions");

		ConsoleTableBuffer tableBuf = new ConsoleTableBuffer(console.getTerminalColumns());
		tableBuf.addColumn(new LongColumn("id", 4));
		tableBuf.addColumn(new StringColumn("user", 20).setMaxLength(30));
		tableBuf.addColumn(new StringColumn("permission", 10));
		tableBuf.addColumn(new StringColumn("sshProxyUsername", 25));
		tableBuf.addColumn(new StringColumn("password", 8));

		for (ContainerPermission model : permissionDao.findAllByContainer(container.getId())) {
			model.setContainer(container); // set back-reference
			// console.printlnf("\t%s", model);

			Row row = new Row(model.getId(), model.getUser().getFullname(), model.getPermission(),
					container == null ? "?" : model.getSshProxyUsername(),
					model.getPassword() == null ? "<none>" : "<set>");

			tableBuf.addRow(row);
		}

		tableBuf.printTable(console, "\t");
	}

	private void help() {
		printHelp(CMD_LS, "list container user permissions");
		printHelp(CMD_GROUPADD, "add new user permissions by default assignments of technical users");
		printHelp(CMD_ADD, "add new user permission to single user");
		printHelp(CMD_EDIT + " <id>", "edit listed user permission");
		printHelp(CMD_DEL + " <id>", "delete listed user permission");
		printHelp(CMD_INFORM + " <id>", "send account summary to specific user");
		printHelp(CMD_INFORMALL, "send account summary to all authorized users by email");
		printHelp(CMD_WRITESSHPRX, "update ssh proxy configuration on this container");
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_GROUPADD, CMD_ADD, CMD_EDIT, CMD_DEL, CMD_INFORM, CMD_INFORMALL, CMD_WRITESSHPRX);
	}

	private void editHelper(ContainerPermission model) throws Exception {

		String out;

		// list users
		while (true) {

			out = console.readLine("List user? ls [*name*]/n", "n");

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out) || isNo(out)) {
				break;
			} else if (isCommandWithParam(CMD_LS, out)) {
				userExecutor.list(getStringParam(out));
			} else if (isCommand(CMD_LS, out)) {
				userExecutor.list(null);
			}
		}

		// label
		while (true) {
			out = console.readLine("Assign permission to user id",
					model.getUser() == null ? null : model.getUser().getId());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out) && model.getUser() != null) {
				break;
			}

			Long id = getId(out);
			User user = null;
			if (id != null) {
				user = userDao.find(id);
			}

			if (id == null || user == null) {
				console.printlnfError("Invalid user, please enter id");
			} else {
				model.setUser(user);
				break;
			}
		}

		// technical user (permission string)
		while (true) {
			out = console.readLine("Technical username in target environment", model.getPermission());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out) && model.getPermission() != null) {
				break;
			} else if (out.length() > 12) {
				console.printlnfError("Invalid technical username, please limit to max 12 characters");
			} else if (validator.isUsername(out)) {
				model.setPermission(out);
				break;
			}
			console.printlnfError("Invalid username, try again");
		}

		// search for existing password of same <project>-<backend>_<techuser>
		if (StringUtils.isEmptyOrWhitespaceOnly(model.getPassword())) {
			ContainerPermission cp = permissionDao.findPasswordOfSamePermission(model.getContainerId(),
					model.getPermission());
			if (cp != null) {
				model.setPassword(cp.getPassword());
			}
		}

		// optional password (alternative to SSH auth)
		if (StringUtils.isEmptyOrWhitespaceOnly(model.getPassword())) {
			while (true) {
				out = console.readLine("Optional password, generate with '+'");

				handleExitOrAbort(out);
				if ("-".equals(out) || "".equals(out)) {
					out = null;
					model.setPassword(out);
					break;
				} else if ("+".equals(out)) {
					model.setPassword(PasswordGenerator.getPassword());
					console.printlnfStress(model.getPassword());
					break;
				} else if (out.length() < 6 || out.length() > 20) {
					console.printlnfError("Invalid password, please set to min 6 and max 20 characters");
				} else {
					model.setPassword(out);
					break;
				}

				console.printlnfError("Invalid password, try again");
			}
		} else {
			while (true) {
				out = console.readLine("Optional password; delete with '-', generate with '+'", model.getPassword());

				handleExitOrAbort(out);

				if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
					break;
				} else if ("-".equals(out)) {
					out = null;
					model.setPassword(out);
					break;
				} else if ("+".equals(out)) {
					model.setPassword(PasswordGenerator.getPassword());
					console.printlnfStress(model.getPassword());
					break;
				} else if (out.length() < 6 || out.length() > 20) {
					console.printlnfError("Invalid password, please set to min 6 and max 20 characters");
				} else {
					model.setPassword(out);
					break;
				}

				console.printlnfError("Invalid password, try again");
			}
		}
	}

	private void groupAdd(Container container) throws Exception {

		String out, techUserPermission = null;

		try {

			// technical user (permission string)
			while (true) {
				out = console.readLine("Technical user access");

				handleExitOrAbort(out);

				if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
					break;
				} else if (out.length() > 12) {
					console.printlnfError("Invalid username, please limit to max 12 characters");
				} else if (validator.isUsername(out)) {
					techUserPermission = out;
					break;
				}
				console.printlnfError("Invalid username, try again");
			}

		} catch (AbortionException e) {
			return;
		}

		List<User> filteredUsers = userDao.findAllWithFilterDefaultPermission(techUserPermission);
		if (filteredUsers.size() == 0) {
			console.printlnfError("No matching default permissions on existing users found");
			return;
		}
		console.printlnfStress("Users with permission default on " + techUserPermission + ":");
		for (User user : filteredUsers) {
			console.printlnf("\t%s", user);
		}

		out = console.readLine("add permission to listed users? y/n", "y");

		if (isNo(out)) {
			console.printlnf("Canceled insertion");
			return;
		}

		for (User user : filteredUsers) {

			if (permissionDao.findByUniqueIds(container.getId(), user.getId(), techUserPermission) != null) {
				continue;
			}

			ContainerPermission model = new ContainerPermission();
			model.setContainer(container);
			model.setPermission(techUserPermission);
			model.setUser(user);

			permissionDao.create(model);
			container.getContainerPermissions().add(model);
		}
		console.printlnfStress("Inserted new permission on " + techUserPermission + " to listed users");
	}

	public void insert(Container container, ContainerPermission model) throws Exception {

		model.setContainer(container);

		try {
			editHelper(model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("insert? y/n/e", "y");

		if (isEdit(out)) {
			insert(container, model);
		} else if (isNo(out)) {
			console.printlnf("Canceled insertion");
			return;
		} else {
			try {
				permissionDao.create(model);
				container.getContainerPermissions().add(model);
				updatePasswordsToSamePermission(container.getContainerPermissions(), model);
				console.printlnfStress("Inserted new permission with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void update(Container container, ContainerPermission model) throws Exception {

		if (!isValid(container, model)) {
			list(container);
			return;
		}

		try {
			editHelper(model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("update? y/n/e", "y");

		if (isEdit(out)) {
			update(container, model);
		} else if (isNo(out)) {
			console.printlnf("Canceled update");
			return;
		} else {
			try {
				permissionDao.update(model);
				ListUtils.replace(container.getContainerPermissions(), model);
				updatePasswordsToSamePermission(container.getContainerPermissions(), model);
				console.printlnfStress("Updated permission with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void delete(Container container, ContainerPermission model) throws Exception {

		if (!isValid(container, model)) {
			list(container);
			return;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {
			permissionDao.delete(model);
			container.getContainerPermissions().remove(model);
			console.printlnfStress("Deleted permission with id %d", model.getId());
		} else {
			console.printlnf("Canceled deletion");
		}
	}

	private void updatePasswordsToSamePermission(List<ContainerPermission> containerPermissions,
			ContainerPermission model) {
		List<Long> changedPermissionIds = permissionDao.updatePasswordsToSamePermission(model);
		for (long changedPermissionId : changedPermissionIds) {
			ContainerPermission changedModel = permissionDao.find(changedPermissionId);
			ListUtils.replace(containerPermissions, changedModel);
		}
	}

	private void informAll(Container container) throws Exception {
		List<ContainerPermission> allAuthorizedUsers = permissionDao.findAllByContainer(container.getId());
		for (ContainerPermission p : allAuthorizedUsers) {
			inform(container, p, false);
		}
	}

	private void inform(Container container, ContainerPermission model, boolean validityCheck) throws Exception {

		if (validityCheck && !isValid(container, model)) {
			return;
		}

		// set back-reference
		model.setContainer(container);

		String subject = container.getProject().getFullname() + ", access data: " + model.getSshProxyUsername();

		StringBuilder buf = new StringBuilder();
		buf.append("Dear ").append(model.getUser().getFullname()).append(",\n\n");
		buf.append("this is an access data summary:\n\n");

		buf.append("Project:                       ").append(container.getProject().getFullname()).append("\n");
		buf.append("Containername:                 ").append(container.getFQLabel()).append("\n");
		buf.append("Technical user in container:   ").append(model.getPermission()).append("\n\n");

		buf.append("SSH-Proxy-Server:              ").append(proxyDomainname).append("\n");
		buf.append("SSH-Proxy-Port:                ").append(proxyPort).append("\n");
		buf.append("SSH-Proxy-Username:            ").append(model.getSshProxyUsername()).append("\n");

		String keyShort = SshKeyConverter.getNormalizedSSHKeyShort(model.getUser().getSshPublicKey(),
				new StringBuilder(), 40);
		if (keyShort == null) {
			keyShort = model.getUser().getSshPublicKey().substring(0, 35) + "...";
		}
		buf.append("Authorized SSH-PubKey:         ").append(keyShort).append("\n");

		buf.append("Additional SSH-Proxy-Password: ")
				.append(model.getPassword() == null ? "none" : "set and will be sent by separate email").append("\n\n");

		buf.append("SSH-Sample-Command:            ").append("ssh -p").append(proxyPort).append(" ")
				.append(model.getSshProxyUsername()).append("@").append(proxyDomainname).append("\n\n");

		buf.append(emailSender.getEmailFooter()).append("\n\n");

		console.printlnf(buf.toString());

		if (model.getPassword() != null) {
			console.printlnfStress("Secret Info: SSH-User-Password: " + model.getPassword());
		}

		String out = console.readLine("Send access data summary to " + model.getUser().getEmail() + " ? y/n", "n");

		if (isYes(out)) {

			emailSender.sendMailToUser(model.getUser().getEmail(), model.getUser().getFullname(), subject,
					buf.toString());

			console.printlnfStress("Sent out to %s:", model.getUser().getEmail());
			console.printlnfStress("Subject: %s", subject);

			if (model.getPassword() != null) {

				console.printlnfStress("A separate email with SSH-Proxy-Password will be sent out in %d seconds",
						passwordEmailDelaySeconds);

				// send out password with time lag
				new java.util.Timer().schedule(new java.util.TimerTask() {
					@Override
					public void run() {

						StringBuilder buf = new StringBuilder();
						buf.append("Dear ").append(model.getUser().getFullname()).append(",\n\n");
						buf.append("like mentioned in the last email: ").append(model.getPassword()).append("\n\n");
						buf.append("Please remove this email as soon as possible from your email inbox.")
								.append("\n\n");
						buf.append(emailSender.getEmailFooter()).append("\n\n");

						emailSender.sendMailToUser(model.getUser().getEmail(), model.getUser().getFullname(),
								"follow-up to last email", buf.toString());
					}
				}, passwordEmailDelaySeconds * 1000 // send after 30 seconds
				);
			}
		}
	}

	private boolean isValid(Container container, ContainerPermission model) {

		if (model == null || !permissionDao.findAllIdsByContainer(container.getId()).contains(model.getId())) {
			console.printlnfError("invalid container permission, try again");
			return false;
		}

		return true;
	}
}
