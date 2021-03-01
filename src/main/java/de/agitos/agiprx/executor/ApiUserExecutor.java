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

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.bean.processor.AgiPrxSshAuthProcessor;
import de.agitos.agiprx.dao.ApiUserDao;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.ApiUser;
import de.agitos.agiprx.util.Assert;

public class ApiUserExecutor extends AbstractExecutor {

	private static ApiUserExecutor BEAN;

	private AgiPrxSshAuthProcessor agiPrxSshAuthProcessor;

	private ApiUserDao apiUserDao;

	public ApiUserExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();
		agiPrxSshAuthProcessor = AgiPrxSshAuthProcessor.getBean();
		apiUserDao = ApiUserDao.getBean();
	}

	public static ApiUserExecutor getBean() {
		return BEAN;
	}

	public void run() throws Exception {

		list();

		help();

		setCommandCompletion();

		String out;

		while (true) {
			out = console.readLine("API-USER");

			try {
				handleExitOrAbort(out);
			} catch (AbortionException e) {
				return;
			}

			if (isCommand(CMD_HELP, out)) {
				help();
			} else if (isCommand(CMD_LS, out)) {
				list();
			} else if (isCommand(CMD_ADD, out)) {
				insert(new ApiUser());
			} else if (isCommandWithParam(CMD_EDIT, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					update(apiUserDao.find(id));
				}
			} else if (isCommandWithParam(CMD_DEL, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					delete(apiUserDao.find(id));
				}
			} else {
				console.printlnfError("Incorrect command %s", out);
				help();
			}
		}
	}

	protected void list() {
		console.printlnfStress("Available API-users");
		for (ApiUser user : apiUserDao.findAll()) {
			console.printlnf("\t%s", user);
		}
	}

	private void help() {
		printHelp(CMD_LS, "list users");
		printHelp(CMD_ADD, "add new user");
		printHelp(CMD_EDIT + " <id>", "edit listed user");
		printHelp(CMD_DEL + " <id>", "delete listed user");
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_ADD, CMD_EDIT, CMD_DEL);
	}

	private void editHelper(ApiUser model) throws Exception {

		String out;

		// username
		while (true) {
			out = console.readLine("Username", model.getUsername());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if (out.length() > 50) {
				console.printlnfError("Invalid username, please limit to max 50 characters");
			} else if (validator.isUsername(out)) {
				model.setUsername(out);
				break;
			}
			console.printlnfError("Invalid username, try again");
		}

		// password
		while (true) {
			out = console.readLine("Password", model.getPassword());

			handleExitOrAbort(out);
			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if (out.length() < 6 || out.length() > 20) {
				console.printlnfError("Invalid password, please set to min 6 and max 20 characters");
			} else {
				model.setPassword(out);
				break;
			}

			console.printlnfError("Invalid password, try again");
		}

		// email
		while (true) {
			out = console.readLine("email", model.getEmail());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if (validator.isEmail(out)) {
				model.setEmail(out);
				break;
			}
			console.printlnfError("Invalid email, try again");
		}

		// agiprx_permissions
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

	private void insert(ApiUser model) throws Exception {

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
				apiUserDao.create(model);
				console.printlnfStress("Inserted new user with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void update(ApiUser model) throws Exception {

		if (model == null) {
			console.printlnfError("invalid user, try again");
			list();
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
				apiUserDao.update(model);
				console.printlnfStress("Updated user with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void delete(ApiUser model) throws Exception {

		if (model == null) {
			console.printlnfError("invalid user, try again");
			list();
			return;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {
			apiUserDao.delete(model);
			console.printlnfStress("Deleted user with id %d", model.getId());
		} else {
			console.printlnf("Canceled deletion");
		}
	}

	private void writeSshAuthConfiguration() throws IOException, InterruptedException, AbortionException {
		agiPrxSshAuthProcessor.manageConfiguration(false);
		console.printlnfStress("Updated AgiPrx SSH authentication configuration");
	}

}
