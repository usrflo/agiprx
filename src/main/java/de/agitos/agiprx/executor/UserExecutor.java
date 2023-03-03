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

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.bean.processor.AgiPrxSshAuthProcessor;
import de.agitos.agiprx.bean.processor.SshProxyProcessor;
import de.agitos.agiprx.dao.ContainerPermissionDao;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dao.RelationType;
import de.agitos.agiprx.dao.UserDao;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.model.UserRoleType;
import de.agitos.agiprx.output.table.ConsoleTableBuffer;
import de.agitos.agiprx.output.table.LongColumn;
import de.agitos.agiprx.output.table.Row;
import de.agitos.agiprx.output.table.StringColumn;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.SshKeyConverter;

public class UserExecutor extends AbstractExecutor {

	private static UserExecutor BEAN;

	private UserDao userDao;

	private ProjectDao projectDao;

	private ContainerPermissionDao permissionDao;

	private SshProxyProcessor sshProxyProcessor;

	private AgiPrxSshAuthProcessor agiPrxSshAuthProcessor;

	public UserExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		userDao = UserDao.getBean();
		projectDao = ProjectDao.getBean();
		permissionDao = ContainerPermissionDao.getBean();
		sshProxyProcessor = SshProxyProcessor.getBean();
		agiPrxSshAuthProcessor = AgiPrxSshAuthProcessor.getBean();
	}

	public static UserExecutor getBean() {
		return BEAN;
	}

	public void run() throws Exception {

		help();

		setCommandCompletion();

		String out;

		while (true) {
			out = console.readLine("USER");

			try {
				handleExitOrAbort(out);
			} catch (AbortionException e) {
				return;
			}

			if (isCommand(CMD_HELP, out)) {
				help();
			} else if (isCommandWithParam(CMD_LS, out)) {
				list(getStringParam(out));
			} else if (isCommand(CMD_LS, out)) {
				list(null);
			} else if (isCommand(CMD_ADD, out)) {
				insert(new User());
			} else if (isCommandWithParam(CMD_EDIT, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					update(userDao.find(id));
				}
			} else if (isCommandWithParam(CMD_DEL, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					delete(userDao.find(id));
				}
			} else if (isCommandWithParam(CMD_PERMISSIONS, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					permissionList(userDao.find(id));
				}
			} else if (isCommand(CMD_WRITESSHPRX, out)) {
				writeSshProxyConfiguration();
			} else {
				console.printlnfError("Incorrect command %s", out);
				help();
			}
		}
	}

	protected void list(String filter) {
		console.printlnfStress("Available users");

		List<User> result;
		if (filter == null) {
			result = userDao.findAll();
		} else {
			String sqlFilter = filter.replaceAll("\\*", "%");
			result = userDao.findAllWithFilter(sqlFilter);
		}

		ConsoleTableBuffer tableBuf = new ConsoleTableBuffer(console.getTerminalColumns());
		tableBuf.addColumn(new LongColumn("id", 2));
		tableBuf.addColumn(new StringColumn("fullname", 20).setMaxLength(35));
		tableBuf.addColumn(new StringColumn("email", 20).setMaxLength(45));
		tableBuf.addColumn(new StringColumn("role", 6));
		tableBuf.addColumn(new StringColumn("defaultPerm", 15));
		tableBuf.addColumn(new StringColumn("agiPrxPerm", 15));
		// tableBuf.addColumn(new StringColumn("sshPublicKey",
		// 15).setMinorImportance(true));

		for (User model : result) {
			// console.printlnf("\t%s", user);
			Row row = new Row(model.getId(), model.getFullname(), model.getEmail(), model.getRole().toString(),
					Arrays.toString(model.getDefaultPermission()),
					(UserRoleType.ADMIN.equals(model.getRole()) ? "<admin-global>"
							: Arrays.toString(model.getAgiPrxPermission())));
			tableBuf.addRow(row);
		}

		tableBuf.printTable(console, "\t");
	}

	private void help() {
		printHelp(CMD_LS + " [*name*]", "list users, optionally filter by name, *-wildcard supported");
		printHelp(CMD_ADD, "add new user");
		printHelp(CMD_EDIT + " <id>", "edit listed user");
		printHelp(CMD_DEL + " <id>", "delete listed user");
		printHelp(CMD_PERMISSIONS + " <id>", "container permissions of listed user");
		printHelp(CMD_WRITESSHPRX, "update all SSH proxy configurations");
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_ADD, CMD_EDIT, CMD_DEL, CMD_PERMISSIONS, CMD_WRITESSHPRX);
	}

	private void editHelper(User model) throws Exception {

		String out;

		// fullname
		while (true) {
			out = console.readLine("Fullname", model.getFullname());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				if (model.getFullname() != null) {
					break;
				}
				console.printlnfError("A fullname needs to be set");
				continue;
			} else if (out.length() > 50) {
				console.printlnfError("Invalid full name, please limit to max 50 characters");
			} else {
				model.setFullname(out);
				break;
			}
		}

		// email
		while (true) {
			out = console.readLine("email", model.getEmail());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				if (model.getEmail() != null) {
					break;
				}
			} else if (validator.isEmail(out)) {
				model.setEmail(out);
				break;
			}
			console.printlnfError("Invalid email, try again");
		}

		// sshPublicKey
		while (true) {
			out = console.readLine("ssh public key",
					SshKeyConverter.getNormalizedSSHKeyShort(model.getSshPublicKey(), new StringBuilder(), 40));

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				if (model.getSshPublicKey() != null) {
					break;
				}
				console.printlnfError("Ssh public key is required, try again");
				continue;
			}

			StringBuilder errorInfo = new StringBuilder();
			String sshPublicKey = SshKeyConverter.getNormalizedSSHKey(out, errorInfo);
			if (sshPublicKey != null) {
				model.setSshPublicKey(sshPublicKey);
				break;
			}
			console.printlnfError("Invalid ssh public key %s, try again", errorInfo.toString());
		}

		if (model.getId() == User.SUPERUSER_ID) {

			console.printlnfStress("role: ADMIN by default with SUPERUSER ID " + User.SUPERUSER_ID);
			model.setRole(UserRoleType.ADMIN);

		} else {

			printHelp("ADMIN", "Full access to AgiPrx");
			printHelp("USER", "Administration of own projects in AgiPrx without user data");
			printHelp("CONTACT", "No access to AgiPrx; user data is used to grant SSH access to containers only");

			// role
			while (true) {
				out = console.readLine("role (ADMIN or USER or CONTACT)",
						model.getRole() == null ? "CONTACT" : model.getRole());

				handleExitOrAbort(out);

				if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
					if (model.getRole() == null) {
						model.setRole(UserRoleType.CONTACT);
					}
					break;
				}

				try {
					model.setRole(UserRoleType.valueOf(out));
					break;
				} catch (IllegalArgumentException e) {
					console.printlnfError("Invalid role %s, try again", out);
				}
			}
		}

		// default_permissions
		while (true) {

			String defaultPermissionSerialized = model.getDefaultPermission() == null ? ""
					: String.join(",", model.getDefaultPermission());

			out = console.readLine("default permissions (e.g. www-data,root); enter '-' to reset default",
					defaultPermissionSerialized);

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if ("-".equals(out)) {
				model.setDefaultPermission(null);
				break;
			}

			String[] splitPermissions = out.split(",");
			String[] newPermissions = new String[splitPermissions.length];
			for (int i = 0; i < splitPermissions.length; i++) {
				newPermissions[i] = splitPermissions[i].trim();
			}
			model.setDefaultPermission(newPermissions);
			break;
		}

		// agiprx_permissions
		if (UserRoleType.ADMIN.equals(model.getRole())) {
			// admins have all permissions by default
			model.setAgiPrxPermission(null);
		} else {
			while (true) {

				String agiPrxPermissionSerialized = model.getAgiPrxPermission() == null ? ""
						: String.join(",", model.getAgiPrxPermission());

				out = console.readLine(
						"AgiPrx project permissions, filtered by label (e.g. company.*,xproj); enter '-' to reset default",
						agiPrxPermissionSerialized);

				handleExitOrAbort(out);

				if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
					break;
				} else if ("-".equals(out)) {
					model.setAgiPrxPermission(null);
					break;
				}

				String[] splitPermissions = out.split(",");
				String[] newPermissions = new String[splitPermissions.length];
				for (int i = 0; i < splitPermissions.length; i++) {
					newPermissions[i] = splitPermissions[i].trim();
				}
				model.setAgiPrxPermission(newPermissions);
				break;
			}
		}
	}

	private void insert(User model) throws Exception {

		try {
			editHelper(model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("insert? y/n/e", "y");

		if (isEdit(out)) {
			insert(model);
		} else if (isNo(out)) {
			console.printlnf("Canceled insertion");
			return;
		} else {
			try {
				userDao.create(model);
				console.printlnfStress("Inserted new user with id %d", model.getId());

				if (model.getId() == User.SUPERUSER_ID && !UserRoleType.ADMIN.equals(model.getRole())) {
					// force super user to have ADMIN permission
					model.setRole(UserRoleType.ADMIN);
					update(model);
					console.printlnfStress("Granted role ADMIN to superuser with ID %d", model.getId());
				}

				applyDefaultPermissions(model);
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void update(User model) throws Exception {

		if (!isValid(model)) {
			return;
		}

		try {
			editHelper(model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("update? y/n/e", "y");

		if (isEdit(out)) {
			update(model);
		} else if (isNo(out)) {
			console.printlnf("Canceled update");
			return;
		} else {
			try {
				userDao.update(model);
				console.printlnfStress("Updated user with id %d", model.getId());

				applyDefaultPermissions(model);
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void delete(User model) throws Exception {

		if (!isValid(model)) {
			return;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {
			for (ContainerPermission cp : permissionDao.findAllByUser(model.getId())) {
				permissionDao.delete(cp);
			}
			userDao.delete(model);
			console.printlnfStress("Deleted user with id %d", model.getId());
		} else {
			console.printlnf("Canceled deletion");
		}
	}

	private void writeSshProxyConfiguration() throws IOException, InterruptedException, AbortionException {
		sshProxyProcessor.manageConfiguration(false);
		agiPrxSshAuthProcessor.manageConfiguration(false);
		console.printlnfStress("Updated SSH proxy configuration on all projects");
	}

	private void applyDefaultPermissions(User user) throws Exception {

		if (user.getDefaultPermission() == null || user.getDefaultPermission().length == 0) {
			return;
		}

		String out = console.readLine(
				"Apply additional default permissions of user " + user.getFullname() + " to containers? y/n", "n");

		if (!isYes(out)) {
			return;
		}

		boolean permissionsAdded = false;

		for (String techUserPermission : user.getDefaultPermission()) {
			if (StringUtils.isEmptyOrWhitespaceOnly(techUserPermission)) {
				continue;
			}
			for (Project project : projectDao.findAllAsUser(EnumSet.of(RelationType.CONTAINER))) {
				for (Container container : project.getContainers()) {
					if (permissionDao.findByUniqueIds(container.getId(), user.getId(), techUserPermission) != null) {
						continue;
					}
					ContainerPermission model = new ContainerPermission();
					model.setContainer(container);
					model.setPermission(techUserPermission);
					model.setUser(user);

					permissionDao.create(model);

					permissionsAdded = true;

					console.printlnf("Prepared granting user to %s", model.getSshProxyUsername());
				}
			}
		}

		if (permissionsAdded) {
			out = console.readLine("Rewrite SSH proxy configuration on all projects? y/n", "n");
			if (isYes(out)) {
				writeSshProxyConfiguration();
			}
		} else {
			console.printlnf("No changes applied.");
		}
	}

	private void permissionList(User user) {

		if (!isValid(user)) {
			return;
		}

		console.printlnfStress("Technical user permissions of user " + user.getFullname() + ":");

		for (Project project : projectDao.findAllAsAdmin(EnumSet.of(RelationType.CONTAINER, RelationType.PERMISSION))) {
			for (Container container : project.getContainers()) {
				// set back-reference
				container.setProject(project);
				for (ContainerPermission cp : container.getContainerPermissions()) {
					// set back-reference
					cp.setContainer(container);
					if (cp.getUserId().equals(user.getId())) {
						console.printlnf("\t%s", cp.getSshProxyUsername());
					}
				}
			}
		}
	}

	private boolean isValid(User model) {

		if (model == null || !userDao.findAllIds().contains(model.getId())) {
			console.printlnfError("invalid user, try again");
			return false;
		}

		return true;
	}
}
