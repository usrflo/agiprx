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
import java.util.Map;

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.bean.processor.LxdProcessor;
import de.agitos.agiprx.dao.BackendContainerDao;
import de.agitos.agiprx.dao.ContainerDao;
import de.agitos.agiprx.dao.HostDao;
import de.agitos.agiprx.dao.UserDao;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.BackendContainer;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.Host;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.output.table.ConsoleTableBuffer;
import de.agitos.agiprx.output.table.LongColumn;
import de.agitos.agiprx.output.table.Row;
import de.agitos.agiprx.output.table.StringColumn;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.ListUtils;
import de.agitos.agiprx.util.Validator;

public class ContainerExecutor extends AbstractExecutor {

	private static ContainerExecutor BEAN;

	private ContainerPermissionExecutor containerPermissionExecutor;

	private HostExecutor hostExecutor;

	private ContainerDao containerDao;

	private BackendContainerDao backendContainerDao;

	private HostDao hostDao;

	private UserDao userDao;

	private LxdProcessor lxdProcessor;

	public ContainerExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		containerPermissionExecutor = ContainerPermissionExecutor.getBean();
		hostExecutor = HostExecutor.getBean();
		containerDao = ContainerDao.getBean();
		backendContainerDao = BackendContainerDao.getBean();
		hostDao = HostDao.getBean();
		userDao = UserDao.getBean();
		lxdProcessor = LxdProcessor.getBean();
	}

	public static ContainerExecutor getBean() {
		return BEAN;
	}

	public void run(Project project) throws Exception {

		list(project);

		help(null);

		String out;

		Container container = fetchIfSingle(project);

		if (container != null) {
			console.printlnfStress("Auto-selected single container " + container.getLabel());
			container.setProject(project);
			help(container);
		}

		while (true) {

			if (container == null) {

				setCommandCompletion();

				out = console.readLine("PROJ " + project.getLabel() + " CNTR");

				try {
					handleExitOrAbort(out);
				} catch (AbortionException e) {
					return;
				}

				if (isCommand(CMD_HELP, out)) {
					help(container);
				} else if (isCommand(CMD_LS, out)) {
					list(project);
				} else if (isCommand(CMD_ADD, out)) {
					insert(project, new Container());
				} else if (isCommandWithParam(CMD_USE, out)) {
					String stringParam = getStringParam(out);
					Long id = getIdOrNull(stringParam);
					if (id != null) {
						container = containerDao.find(id);
					} else {
						container = containerDao.find(project, stringParam);
					}
					if (isValid(project, container)) {
						// init back-reference
						container.setProject(project);
						// print project details
						show(container);
						help(container);
					}
					continue;
				} else {
					console.printlnfError("Incorrect command %s", out);
					help(container);
				}

			} else {

				setCommandCompletionOnEdit();

				out = console.readLine("PROJ " + project.getLabel() + " CNTR " + container.getLabel());

				try {
					handleExitOrAbort(out);
				} catch (AbortionException e) {
					container = null;
					continue;
				}

				if (isCommand(CMD_HELP, out)) {
					help(container);
				} else if (isCommand(CMD_SHOW, out)) {
					show(container);
				} else if (isCommand(CMD_EDIT, out)) {
					update(project, container);
				} else if (isCommandWithParam(CMD_DEL, out)) {
					if (delete(project, container)) {
						container = null;
					}
				} else if (isCommand(CMD_PERMISSIONS, out)) {
					if (userDao.findAll().size() == 0) {
						console.printlnfError("Assign users first: enter 'top', then 'user'");
					} else {
						containerPermissionExecutor.run(container);
					}
				} else {
					console.printlnfError("Incorrect command %s", out);
					help(container);
				}
			}
		}
	}

	public void list(Project project) {
		console.printlnfStress("Project containers");

		ConsoleTableBuffer tableBuf = new ConsoleTableBuffer(console.getTerminalColumns());
		tableBuf.addColumn(new LongColumn("id", 4));
		tableBuf.addColumn(new StringColumn("label", 20));
		tableBuf.addColumn(new StringColumn("host", 20));
		tableBuf.addColumn(new StringColumn("ipv6", 40));

		for (Container model : containerDao.findAllByProject(project)) {
			// console.printlnf("\t%s", model);

			Row row = new Row(model.getId(),
					model.getLabel() + (model.getFullname() == null ? "" : " (" + model.getFullname() + ")"),
					model.getHost() == null ? "" : model.getHost().getHostname(), model.getIpv6());

			tableBuf.addRow(row);
		}

		tableBuf.printTable(console, "\t");
	}

	private Container fetchIfSingle(Project project) {
		List<Container> list = containerDao.findAllByProject(project);
		if (list == null || list.size() != 1) {
			return null;
		}
		return list.get(0);
	}

	private void help(Container container) {
		if (container == null) {
			printHelp(CMD_LS, "list project containers");
			printHelp(CMD_ADD, "add new container");
			printHelp(CMD_USE + " <id>|<label>", "switch to container");
		} else {
			printHelp(CMD_SHOW, "show container overview");
			printHelp(CMD_EDIT, "edit container");
			printHelp(CMD_DEL + " <label>",
					"delete container, enter label '" + container.getLabel() + "' as a deletion protection");
			printHelp(CMD_PERMISSIONS, "edit user permissions on this container");
		}
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_ADD, CMD_USE);
	}

	protected void setCommandCompletionOnEdit() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_SHOW,
				CMD_EDIT, CMD_DEL, CMD_PERMISSIONS);
	}

	private void editHelper(Container model) throws Exception {

		String out;

		// list available container labels
		Map<String, String> lxdContainers = lxdProcessor.getAvailableContainers();
		if (lxdContainers.size() != 0) {
			console.printlnfStress("containers by lxd");
			for (String containerLabel : lxdContainers.keySet()) {
				console.printlnf("\t%s : %s", containerLabel, lxdContainers.get(containerLabel));
			}
		}

		// label
		while (true) {
			out = console.readLine("Label, e.g. prod, test, qsu", model.getLabel());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				if (model.getLabel() == null) {
					console.printlnfError("A label needs to be set");
				} else {
					break;
				}
			} else if (out.length() > 5) {
				console.printlnfError("Invalid label, please limit to max 5 characters");
			} else if (!validator.isLabel(out)) {
				console.printlnfError(Validator.LABEL_ERRORMSG);
			} else {
				model.setLabel(out);
				break;
			}
		}

		// fullname
		while (true) {
			out = console.readLine(
					"opt. Fullname" + (model.getFullname() != null ? "; enter '-' to reset default" : ""),
					model.getFullname());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if ("-".equals(out)) {
				model.setFullname(null);
				break;
			} else if (out.length() > 60) {
				console.printlnfError("Invalid full name, please limit to max 60 characters");
			} else {
				model.setFullname(out);
				break;
			}
		}

		// ipv6
		while (true) {
			out = console.readLine("ipv6", model.getIpv6());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if (validator.isIPv6(out)) {
				model.setIpv6(out);
				break;
			}
			console.printlnfError("Invalid IPv6, try again");
		}

		hostExecutor.list();

		// host
		while (true) {
			out = console.readLine("Assign host", model.getHost() == null ? null : model.getHost().getId());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out) && model.getHost() != null) {
				break;
			}

			Long id = getId(out);
			Host host = null;
			if (id != null) {
				host = hostDao.find(id);
			}

			if (id == null || host == null) {
				console.printlnfError("Invalid host, please enter id");
			} else {
				model.setHost(host);
				break;
			}
		}
	}

	private void show(Container model) {
		console.printlnfStress("\n%s", model.toStringRecursive(""));
	}

	public void insert(Project project, Container model) throws Exception {

		model.setProject(project);

		model.setLabel(Container.DEFAULT_LABEL);

		try {
			editHelper(model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("insert? y/n/e", "y");

		if (isEdit(out)) {
			insert(project, model);
		} else if (isNo(out)) {
			console.printlnf("Canceled insertion");
			return;
		} else {
			try {
				containerDao.create(model);
				project.getContainers().add(model);
				console.printlnfStress("Inserted new container with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
				return;
			}

			// init relations
			model = containerDao.find(model.getId());
			model.setProject(project);

			// optional default configuration after container creation
			while (true) {
				out = console.readLine("Create permission configuration? y/n", "y");

				if (isNo(out)) {
					break;
				}

				containerPermissionExecutor.insert(model, new ContainerPermission());
			}
		}
	}

	private void update(Project project, Container model) throws Exception {

		if (!isValid(project, model)) {
			list(project);
			return;
		}

		try {
			editHelper(model);
		} catch (AbortionException e) {
			return;
		}

		String out = console.readLine("update? y/n/e", "y");

		if (isEdit(out)) {
			update(project, model);
		} else if (isNo(out)) {
			console.printlnf("Canceled update");
			return;
		} else {
			try {
				containerDao.update(model);
				ListUtils.replace(project.getContainers(), model);
				console.printlnfStress("Updated container with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private boolean delete(Project project, Container model) throws Exception {

		if (!isValid(project, model)) {
			list(project);
			return false;
		}

		List<BackendContainer> backendContainers = backendContainerDao.findAllByContainer(model.getId());
		if (!backendContainers.isEmpty()) {
			console.printlnfError(
					"Cannot delete container, it is used in the following backend(s); remove those backends first:");
			for (BackendContainer backendContainer : backendContainers) {
				console.printlnfError(backendContainer.getBackend().toString());
			}
			return false;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {
			containerDao.delete(model);
			project.getContainers().remove(model);
			console.printlnfStress("Deleted container with id %d", model.getId());
			return true;
		} else {
			console.printlnf("Canceled deletion");
		}
		return false;
	}

	private boolean isValid(Project project, Container model) {

		if (model == null || !containerDao.findAllIdsByProject(project.getId()).contains(model.getId())) {
			console.printlnfError("invalid container, try again");
			return false;
		}

		return true;
	}
}
