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

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.dao.BackendContainerDao;
import de.agitos.agiprx.dao.ContainerDao;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.BackendContainer;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.util.Assert;

public class BackendContainerExecutor extends AbstractExecutor {

	private static BackendContainerExecutor BEAN;

	private ContainerExecutor containerExecutor;

	private ContainerDao containerDao;

	private BackendContainerDao backendContainerDao;

	public BackendContainerExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();
		containerExecutor = ContainerExecutor.getBean();
		containerDao = ContainerDao.getBean();
		backendContainerDao = BackendContainerDao.getBean();
	}

	public static BackendContainerExecutor getBean() {
		return BEAN;
	}

	public void run(Backend backend) throws Exception {

		list(backend);

		help();

		setCommandCompletion();

		String out;

		while (true) {
			out = console
					.readLine("PROJ " + backend.getProject().getLabel() + " BCKE " + backend.getLabel() + " CNTRREF");

			try {
				handleExitOrAbort(out);
			} catch (AbortionException e) {
				return;
			}

			if (isCommand(CMD_HELP, out)) {
				help();
			} else if (isCommand(CMD_LS, out)) {
				list(backend);
			} else if (isCommand(CMD_ADD, out)) {
				insert(backend, new BackendContainer());
			} else if (isCommandWithParam(CMD_EDIT, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					update(backend, backendContainerDao.find(id));
				}
			} else if (isCommandWithParam(CMD_DEL, out)) {
				Long id = getIdParam(out);
				if (id != null) {
					delete(backend, backendContainerDao.find(id));
				}
			} else {
				console.printlnfError("Incorrect command %s", out);
				help();
			}
		}
	}

	private void list(Backend backend) {
		console.printlnfStress("Backend Container References");
		for (BackendContainer model : backendContainerDao.findAllByBackend(backend.getId())) {
			model.setBackend(backend); // set back-reference
			console.printlnf("\t%s", model);
		}
	}

	private void help() {
		printHelp(CMD_LS, "list project backend container references");
		printHelp(CMD_ADD, "add new backend container reference");
		printHelp(CMD_EDIT + " <id>", "edit listed backend container reference");
		printHelp(CMD_DEL + " <id>", "delete listed backend container reference");
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_ADD, CMD_EDIT, CMD_DEL);
	}

	private void editHelper(BackendContainer model) throws Exception {

		containerExecutor.list(model.getBackend().getProject());

		String out;

		// label
		while (true) {
			out = console.readLine("Assign container id to backend",
					model.getContainer() == null ? null : model.getContainer().getId());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out) && model.getContainer() != null) {
				break;
			}

			Long id = getId(out);
			Container container = null;
			if (id != null) {
				container = containerDao.find(id);
			}

			if (id == null || container == null) {
				console.printlnfError("Invalid container, please enter id");
			} else {
				model.setContainer(container);
				break;
			}
		}

		// params
		if (StringUtils.isEmptyOrWhitespaceOnly(model.getParams())) {
			while (true) {
				out = console.readLine("HAProxy params");

				handleExitOrAbort(out);

				model.setParams(out);
				break;
			}
		} else {
			while (true) {
				out = console.readLine("HAProxy params; enter '-' to reset default", model.getParams());

				handleExitOrAbort(out);

				if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
					break;
				} else if ("-".equals(out)) {
					out = null;
				}

				model.setParams(out);
				break;
			}
		}
	}

	public void insert(Backend backend, BackendContainer model) throws Exception {

		model.setBackend(backend);

		try {
			editHelper(model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("insert? y/n/e", "y");

		if (isEdit(out)) {
			insert(backend, model);
		} else if (isNo(out)) {
			console.printlnf("Canceled insertion");
			return;
		} else {
			try {
				backendContainerDao.create(model);
				backend.getBackendContainers().add(model);
				console.printlnfStress("Inserted new backend container reference with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void update(Backend backend, BackendContainer model) throws Exception {

		if (!isValid(backend, model)) {
			list(backend);
			return;
		}

		// set back-reference
		model.setBackend(backend);

		try {
			editHelper(model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("update? y/n/e", "y");

		if (isEdit(out)) {
			update(backend, model);
		} else if (isNo(out)) {
			console.printlnf("Canceled update");
			return;
		} else {
			try {
				backendContainerDao.update(model);
				console.printlnfStress("Updated backend container reference with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void delete(Backend backend, BackendContainer model) throws Exception {

		if (!isValid(backend, model)) {
			list(backend);
			return;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {
			backendContainerDao.delete(model);
			backend.getBackendContainers().remove(model);
			console.printlnfStress("Deleted backend container reference with id %d", model.getId());
		} else {
			console.printlnf("Canceled deletion");
		}
	}

	private boolean isValid(Backend backend, BackendContainer model) {

		if (model == null || !backendContainerDao.findAllIdsByBackend(backend.getId()).contains(model.getId())) {
			console.printlnfError("invalid backend container reference, try again");
			return false;
		}

		return true;
	}
}
