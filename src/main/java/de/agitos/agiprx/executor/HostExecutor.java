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

import java.util.EnumSet;
import java.util.List;

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.dao.ContainerDao;
import de.agitos.agiprx.dao.HostDao;
import de.agitos.agiprx.dao.RelationType;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.Host;
import de.agitos.agiprx.output.table.ConsoleTableBuffer;
import de.agitos.agiprx.output.table.LongColumn;
import de.agitos.agiprx.output.table.Row;
import de.agitos.agiprx.output.table.StringColumn;
import de.agitos.agiprx.util.Assert;

public class HostExecutor extends AbstractExecutor {

	private static HostExecutor BEAN;

	private HostDao hostDao;

	private ContainerDao containerDao;

	public HostExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		hostDao = HostDao.getBean();
		containerDao = ContainerDao.getBean();
	}

	public static HostExecutor getBean() {
		return BEAN;
	}

	public void run() throws Exception {

		list();

		help(null);

		String out;

		Host host = null;

		while (true) {

			if (host == null) {

				setCommandCompletion();

				out = console.readLine("HOST");

				try {
					handleExitOrAbort(out);
				} catch (AbortionException e) {
					return;
				}

				if (isCommand(CMD_HELP, out)) {
					help(null);
				} else if (isCommand(CMD_LS, out)) {
					list();
				} else if (isCommand(CMD_ADD, out)) {
					insert(new Host());
				} else if (isCommandWithParam(CMD_USE, out)) {
					String stringParam = getStringParam(out);
					Long id = getIdOrNull(stringParam);
					if (id != null) {
						host = hostDao.find(id);
					} else {
						host = hostDao.find(stringParam);
					}
					if (isValid(host)) {
						// print project details
						show(host);
						help(host);
					}
					continue;
				} else {
					console.printlnfError("Incorrect command %s", out);
					help(null);
				}

			} else {

				setCommandCompletionOnEdit();

				out = console.readLine("HOST " + host.getHostname());

				try {
					handleExitOrAbort(out);
				} catch (AbortionException e) {
					host = null;
					continue;
				}

				if (isCommand(CMD_HELP, out)) {
					help(host);
				} else if (isCommand(CMD_SHOW, out)) {
					show(host);
				} else if (isCommand(CMD_EDIT, out)) {
					update(host);
				} else if (isCommand(CMD_DEL, out)) {
					if (delete(host)) {
						host = null;
					}
				}
			}
		}
	}

	public void list() {
		console.printlnfStress("Available hosts");

		ConsoleTableBuffer tableBuf = new ConsoleTableBuffer(console.getTerminalColumns());
		tableBuf.addColumn(new LongColumn("id", 4));
		tableBuf.addColumn(new StringColumn("hostname", 20));
		tableBuf.addColumn(new StringColumn("ipv6", 25));

		for (Host host : hostDao.findAll()) {
			// console.printlnf("\t%s", host);

			Row row = new Row(host.getId(), host.getHostname(), host.getIpv6());

			tableBuf.addRow(row);
		}

		tableBuf.printTable(console, "\t");
	}

	private void help(Host host) {
		if (host == null) {
			printHelp(CMD_LS, "list hosts");
			printHelp(CMD_ADD, "add new host");
			printHelp(CMD_USE + " <id>|<hostname>", "switch to host");
		} else {
			printHelp(CMD_SHOW, "show host overview");
			printHelp(CMD_EDIT, "edit host");
			printHelp(CMD_DEL, "delete host");
		}
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_ADD, CMD_USE);
	}

	protected void setCommandCompletionOnEdit() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_EDIT, CMD_DEL, CMD_SHOW);
	}

	private void editHelper(Host model) throws Exception {

		String out;

		// hostname
		while (true) {
			out = console.readLine("hostname", model.getHostname());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if (validator.isDomainName(out)) {
				model.setHostname(out);
				break;
			}
			console.printlnfError("Invalid hostname, try again");
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

		// adminPassword (not in use)
		/*
		 * while (true) { out = console.readLine("admin password",
		 * model.getAdminPassword());
		 * 
		 * handleExitOrAbort(out);
		 * 
		 * if (StringUtils.isEmptyOrWhitespaceOnly(out)) { break; } else if
		 * (out.length() > 20) {
		 * console.printlnfError("Invalid password, please limit to max 20 characters");
		 * } else { model.setAdminPassword(out); break; } }
		 */
	}

	private void show(Host model) {

		console.printlnfStress("Containers on " + model.getHostname() + " / " + model.getIpv6());

		ConsoleTableBuffer tableBuf = new ConsoleTableBuffer(console.getTerminalColumns());

		tableBuf.addColumn(new StringColumn("project", 15));
		tableBuf.addColumn(new StringColumn("container", 20));
		tableBuf.addColumn(new StringColumn("fullname", 25).setMinorImportance(true));
		tableBuf.addColumn(new StringColumn("ipv6", 40));

		for (Container container : containerDao.findAllByHost(model, EnumSet.of(RelationType.PROJECT))) {
			// console.printlnf("\t%s", host);

			Row row = new Row(container.getProject().getLabel(), container.getLabel(), container.getFullname(),
					container.getIpv6());

			tableBuf.addRow(row);
		}

		tableBuf.printTable(console, "\t");
	}

	private void insert(Host model) throws Exception {

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
				hostDao.create(model);
				console.printlnfStress("Inserted new host with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private void update(Host model) throws Exception {

		if (!isValid(model)) {
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
				hostDao.update(model);
				console.printlnfStress("Updated host with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private boolean delete(Host model) throws Exception {

		if (!isValid(model)) {
			list();
			return false;
		}

		List<Container> containers = containerDao.findAllByHost(model, EnumSet.of(RelationType.PROJECT));
		if (!containers.isEmpty()) {
			console.printlnfError(
					"Cannot delete host, it is used in the following container(s); remove those containers first:");
			for (Container container : containers) {
				console.printlnfError(container.toString());
			}
			return false;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {
			hostDao.delete(model);
			console.printlnfStress("Deleted host with id %d", model.getId());
			return true;
		} else {
			console.printlnf("Canceled deletion");
			return false;
		}
	}

	private boolean isValid(Host model) {

		if (model == null || !hostDao.findAllIds().contains(model.getId())) {
			console.printlnfError("invalid host, try again");
			return false;
		}

		return true;
	}
}
