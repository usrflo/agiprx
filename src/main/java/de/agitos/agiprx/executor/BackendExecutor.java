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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.dao.BackendDao;
import de.agitos.agiprx.dao.RelationType;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.BackendContainer;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.output.table.ConsoleTableBuffer;
import de.agitos.agiprx.output.table.IntegerColumn;
import de.agitos.agiprx.output.table.LongColumn;
import de.agitos.agiprx.output.table.Row;
import de.agitos.agiprx.output.table.StringColumn;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.Validator;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

public class BackendExecutor extends AbstractCertificateRelatedExecutor {

	private static BackendExecutor BEAN;

	private DomainExecutor domainExecutor;

	private BackendContainerExecutor backendContainerExecutor;

	private BackendDao backendDao;

	public BackendExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		domainExecutor = DomainExecutor.getBean();
		backendDao = BackendDao.getBean();
		backendContainerExecutor = BackendContainerExecutor.getBean();
	}

	public static BackendExecutor getBean() {
		return BEAN;
	}

	public void run(Project project) throws Exception {

		list(project, null);

		help(null);

		String out;

		Backend backend = fetchIfSingle(project);

		if (backend != null) {
			console.printlnfStress("Auto-selected single backend " + backend.getLabel());
			backend.setProject(project);
			help(backend);
		}

		while (true) {

			if (backend == null) {

				setCommandCompletion();

				out = console.readLine("PROJ " + project.getLabel() + " BCKE");

				try {
					handleExitOrAbort(out);
				} catch (AbortionException e) {
					return;
				}

				if (isCommand(CMD_HELP, out)) {
					help(backend);
				} else if (isCommandWithParam(CMD_LS, out)) {
					list(project, getStringParam(out));
				} else if (isCommand(CMD_LS, out)) {
					list(project, null);
				} else if (isCommand(CMD_ADD, out)) {
					insert(project, new Backend());
				} else if (isCommandWithParam(CMD_USE, out)) {
					String stringParam = getStringParam(out);
					Long id = getIdOrNull(stringParam);
					if (id != null) {
						backend = backendDao.find(id);
					} else {
						backend = backendDao.find(project, stringParam, EnumSet.of(RelationType.ALL));
					}
					if (isValid(project, backend)) {
						// init back-reference
						backend.setProject(project);
						// print project details
						show(backend);
						help(backend);
					}
					continue;
				} else {
					console.printlnfError("Incorrect command %s", out);
					help(backend);
				}

			} else {

				setCommandCompletionOnEdit();

				out = console.readLine("PROJ " + project.getLabel() + " BCKE " + backend.getLabel());

				try {
					handleExitOrAbort(out);
				} catch (AbortionException e) {
					backend = null;
					continue;
				}

				if (isCommand(CMD_HELP, out)) {
					help(backend);
				} else if (isCommand(CMD_SHOW, out)) {
					show(backend);
				} else if (isCommand(CMD_EDIT, out)) {
					update(project, backend);
				} else if (isCommand(CMD_DEL, out)) {
					if (delete(project, backend)) {
						backend = null;
					}
				} else if (isCommand(CMD_CONTAINERS, out)) {
					if (project.getContainers().size() == 0) {
						console.printlnfError("Assign containers to project first");
					} else {
						backendContainerExecutor.run(backend);
					}
				} else if (isCommand(CMD_DOMAINS, out)) {
					domainExecutor.run(backend);
				} else {
					console.printlnfError("Incorrect command %s", out);
					help(backend);
				}
			}
		}
	}

	private void list(Project project, String filter) {
		console.printlnfStress("Project backends");

		if (filter != null) {
			filter = filter.replaceAll("\\?", ".?").replaceAll("\\*", ".*?");
		}

		ConsoleTableBuffer tableBuf = new ConsoleTableBuffer(console.getTerminalColumns());
		tableBuf.addColumn(new LongColumn("id", 4));
		tableBuf.addColumn(new StringColumn("label", 20));
		tableBuf.addColumn(new IntegerColumn("port", 4));
		tableBuf.addColumn(new IntegerColumn("#cntrref", 8));
		tableBuf.addColumn(new IntegerColumn("#domains", 8));

		for (Backend model : backendDao.findAllByProject(project.getId(), EnumSet.of(RelationType.ALL))) {
			model.setProject(project); // set back-reference
			if (filter != null) {
				try {
					if (!model.getLabel().matches(filter)) {
						continue;
					}
				} catch (PatternSyntaxException pse) {
					// ignore
				}
			}

			// console.printlnf("\t%s", model);

			Row row = new Row(model.getId(),
					model.getLabel() + (model.getFullname() == null ? "" : " (" + model.getFullname() + ")"),
					model.getPort(), model.getBackendContainers() == null ? 0 : model.getBackendContainers().size(),
					model.getDomainForwardings().size());

			tableBuf.addRow(row);
		}

		tableBuf.printTable(console, "\t");
	}

	private Backend fetchIfSingle(Project project) {
		List<Backend> list = backendDao.findAllByProject(project.getId(), EnumSet.of(RelationType.ALL));
		if (list == null || list.size() != 1) {
			return null;
		}
		return list.get(0);
	}

	private void help(Backend backend) {
		if (backend == null) {
			printHelp(CMD_LS + " [*label*]", "list project backends, optionally filter by label, *-wildcard supported");
			printHelp(CMD_ADD, "add new backend");
			printHelp(CMD_USE + " <id>|<label>", "switch to backend");
		} else {
			printHelp(CMD_EDIT, "edit backend");
			printHelp(CMD_DEL, "delete backend");
			printHelp(CMD_CONTAINERS, "edit assigned containers");
			printHelp(CMD_DOMAINS, "edit assigned domain names");
		}
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_ADD, CMD_USE);
	}

	protected void setCommandCompletionOnEdit() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_EDIT,
				CMD_DEL, CMD_CONTAINERS, CMD_DOMAINS);
	}

	private void editHelper(Backend model) throws Exception {

		String out;

		// label
		while (true) {
			out = console.readLine("Label, e.g. prod, test, qsu, " + Backend.PERMANENT_REDIRECT_LABEL + ", "
					+ Backend.TEMPORARY_REDIRECT_LABEL + ", " + Backend.NO_CONTENT_LABEL, model.getLabel());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				if (model.getLabel() == null) {
					console.printlnfError("A label needs to be set");
				} else {
					break;
				}
			} else if (out.length() > 16) {
				console.printlnfError("Invalid label, please limit to max 16 characters");
			} else if (!validator.isLabel(out)) {
				console.printlnfError(Validator.LABEL_ERRORMSG);
			} else {
				model.setLabel(out);
				break;
			}
		}

		// skip specific settings for global backends
		if (model.isGlobalBackend()) {

			console.printlnfStress("Selected global backend " + model.getLabel());

			model.setPort(-1);
			model.setParams(null);
			return;
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

		// port
		while (true) {
			out = console.readLine("port", model.getPort());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			}

			try {
				Integer i = Integer.parseInt(out);
				model.setPort(i);
				break;
			} catch (NumberFormatException nfe) {
			}

			console.printlnfError("Invalid port, try again");
		}

		out = console.readLine("Help on HAProxy params? y/n", "n");
		if (isYes(out)) {
			console.printlnf("\t" + HAProxyProcessor.MAINTENANCE
					+ " : return HTTP 503 with maintenance hint; this overwrites default backend configuration");
			console.printlnf("\t" + HAProxyProcessor.USERAUTH
					+ " <userlist> : add HTTP basic authentication against userlist; the named userlist needs to be defined in haproxy-footer.cfg");
			console.printlnf(
					"\ttimeout server <time_with_unit> : set special server timeout, e.g. for long running requests");
		}

		StringsCompleter timeoutCommandCompleter = new StringsCompleter(HAProxyProcessor.MAINTENANCE,
				HAProxyProcessor.USERAUTH + " <userlistname>", "timeout server <time_with_unit>");
//		StringsCompleter timeoutServerCmdArgsCompleter = new StringsCompleter("server");
//		StringsCompleter timeoutCmdArgsCompleter = new StringsCompleter("5s", "10s", "20s", "40s", "80s", "160s");
//		ArgumentCompleter combinedTimeoutArgsCommandCompleter = new ArgumentCompleter(timeoutServerCmdArgsCompleter,
//				timeoutCmdArgsCompleter);
//		ArgumentCompleter combinedTimeoutCommandCompleter = new ArgumentCompleter(timeoutCommandCompleter,
//				combinedTimeoutArgsCommandCompleter);

		// combine completers for commands
		List<Completer> completers = new ArrayList<>();
		completers.add(timeoutCommandCompleter);
		// completers.add(cmd2ArgumentCompleter);

		AggregateCompleter aggregateCompleter = new AggregateCompleter(completers) {
			@Override
			public boolean equals(Object obj) {
				return true;
			}
		};

		Collection<Completer> previousCompleters = console.setCommandCompletion(aggregateCompleter);

		// params
		if (StringUtils.isEmptyOrWhitespaceOnly(model.getParams())) {
			while (true) {
				out = console.readLine("HAProxy params", null, true);

				handleExitOrAbort(out);

				model.setParams(out);
				break;
			}
		} else {
			while (true) {
				out = console.readLine("HAProxy params; enter '-' to reset default", model.getParams(), true);

				handleExitOrAbort(out);

				if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
					break;
				} else if (out.startsWith("-")) {
					out = null;
				}

				model.setParams(out);
				break;
			}
		}

		console.setCommandCompletion(
				previousCompleters.isEmpty() ? ConsoleWrapper.defaultCompleter : previousCompleters.iterator().next());
	}

	private void show(Backend model) {
		console.printlnfStress("\n%s", model.toStringRecursive("", console.getTerminalColumns()));
	}

	public void insert(Project project, Backend model) throws Exception {

		model.setProject(project);

		model.setLabel(Backend.DEFAULT_LABEL);
		model.setPort(Backend.DEFAULT_PORT);

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
				backendDao.create(model);
				project.getBackends().add(model);
				console.printlnfStress("Inserted new backend with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
				return;
			}

			// init relations
			model = backendDao.find(model.getId());
			model.setProject(project);

			// optional default configuration after backend creation
			while (true) {
				out = console.readLine("Add domain configuration? y/n", "y");

				if (isNo(out)) {
					break;
				}

				domainExecutor.insert(model, new Domain());
			}

			if (model.isGlobalBackend()) {
				return;
			}

			out = console.readLine("Connect container to backend? y/n", "y");

			if (isNo(out)) {
				return;
			}

			backendContainerExecutor.insert(model, new BackendContainer());
		}
	}

	private void update(Project project, Backend model) throws Exception {

		if (!isValid(project, model)) {
			list(project, null);
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
				backendDao.update(model);
				console.printlnfStress("Updated backend with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}

			// init relations
			// model = backendDao.find(model.getId());
			// model.setProject(project);
		}
	}

	private boolean delete(Project project, Backend model) throws Exception {

		if (!isValid(project, model)) {
			list(project, null);
			return false;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {
			List<DomainDto> removedDomains = new ArrayList<>();
			backendDao.delete(model, removedDomains);
			project.getBackends().remove(model);
			cleanupCertificates(removedDomains);
			console.printlnfStress("Deleted backend with id %d", model.getId());
			return true;
		} else {
			console.printlnf("Canceled deletion");
		}
		return false;
	}

	private boolean isValid(Project project, Backend model) {

		if (model == null || !backendDao.findAllIdsByProject(project.getId()).contains(model.getId())) {
			console.printlnfError("invalid backend, try again");
			return false;
		}

		return true;
	}
}
