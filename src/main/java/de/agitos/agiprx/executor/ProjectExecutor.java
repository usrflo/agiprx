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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.bean.processor.SshProxyProcessor;
import de.agitos.agiprx.dao.DomainDao;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dao.RelationType;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.output.table.ConsoleTableBuffer;
import de.agitos.agiprx.output.table.IntegerColumn;
import de.agitos.agiprx.output.table.LongColumn;
import de.agitos.agiprx.output.table.Row;
import de.agitos.agiprx.output.table.StringColumn;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.Validator;

public class ProjectExecutor extends AbstractCertificateRelatedExecutor {

	private static ProjectExecutor BEAN;

	private ContainerExecutor containerExecutor;

	private BackendExecutor backendExecutor;

	private SshProxyProcessor sshProxyProcessor;

	private ProjectDao projectDao;

	private DomainDao domainDao;

	public ProjectExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		containerExecutor = ContainerExecutor.getBean();
		backendExecutor = BackendExecutor.getBean();
		sshProxyProcessor = SshProxyProcessor.getBean();
		projectDao = ProjectDao.getBean();
		domainDao = DomainDao.getBean();
	}

	public static ProjectExecutor getBean() {
		return BEAN;
	}

	public void run() throws Exception {

		help(null);

		String out;

		Project project = null;

		while (true) {

			if (project == null) {

				setCommandCompletion();

				out = console.readLine("PROJ");

				try {
					handleExitOrAbort(out);
				} catch (AbortionException e) {
					return;
				}

				if (isCommand(CMD_HELP, out)) {
					help(project);
				} else if (isCommandWithParam(CMD_LS, out)) {
					list(getStringParam(out));
				} else if (isCommand(CMD_LS, out)) {
					list(null);
				} else if (isCommandWithParam(CMD_FIND, out)) {
					find(getStringParam(out));
				} else if (isCommand(CMD_ADD, out)) {
					insert(new Project());
				} else if (isCommandWithParam(CMD_USE, out)) {
					String stringParam = getStringParam(out);
					Long id = getIdOrNull(stringParam);
					if (id != null) {
						project = projectDao.find(id, EnumSet.of(RelationType.ALL));
					} else {
						project = projectDao.find(stringParam);
					}
					if (isValid(project)) {
						// print project details
						show(project);
						help(project);
					}
					continue;
				} else {
					console.printlnfError("Incorrect command %s", out);
					help(project);
				}

			} else {

				setCommandCompletionOnEdit();

				out = console.readLine("PROJ " + project.getLabel());

				try {
					handleExitOrAbort(out);
				} catch (AbortionException e) {
					project = null;
					continue;
				}

				if (isCommand(CMD_HELP, out)) {
					help(project);
				} else if (isCommand(CMD_SHOW, out)) {
					show(project);
				} else if (isCommand(CMD_EDIT, out)) {
					update(project);
				} else if (isCommandWithParam(CMD_DEL, out)) {
					String label = getStringParam(out);
					if (label != null && label.equals(project.getLabel())) {
						if (delete(project)) {
							project = null;
						}
					} else {
						console.printlnfError("Invalid label, deletion aborted; please try again");
					}
				} else if (isCommand(CMD_CONTAINERS, out)) {
					containerExecutor.run(project);
					// reload containers
					project.setContainers(null);
					projectDao.initRelations(project, EnumSet.of(RelationType.CONTAINER));
				} else if (isCommand(CMD_BACKENDS, out)) {
					backendExecutor.run(project);
					// reload backends
					project.setBackends(null);
					projectDao.initRelations(project, EnumSet.of(RelationType.BACKEND));
				} else if (isCommand(CMD_WRITESSHPRX, out)) {
					sshProxyProcessor.manageConfiguration(project, true);
					console.printlnfStress("Updated SSH user configuration on project " + project.getLabel());
				} else {
					console.printlnfError("Incorrect command %s", out);
					help(project);
				}
			}
		}
	}

	private void list(String filter) {
		console.printlnfStress("Available projects");

		List<Project> result;
		if (filter == null) {
			result = projectDao.findAllAsUser(EnumSet.of(RelationType.CONTAINER, RelationType.BACKEND));
		} else {
			String sqlFilter = filter.replaceAll("\\*", "%");
			result = projectDao.findAllWithFilterLabel(sqlFilter,
					EnumSet.of(RelationType.CONTAINER, RelationType.BACKEND));
		}

		Set<Long> allAllowedProjectIds = projectDao.findAllAllowedProjectIds();

		ConsoleTableBuffer tableBuf = new ConsoleTableBuffer(console.getTerminalColumns());
		tableBuf.addColumn(new LongColumn("id", 5));
		tableBuf.addColumn(new StringColumn("label", 15));
		tableBuf.addColumn(new StringColumn("fullname", 25).setMinorImportance(true));
		tableBuf.addColumn(new IntegerColumn("#containers", 5));
		tableBuf.addColumn(new IntegerColumn("#backends", 5));

		for (Project model : result) {

			if (allAllowedProjectIds.contains(model.getId())) {
				// console.printlnf("\t%s", model);
				Row row = new Row(model.getId(), model.getLabel(), model.getFullname(), model.getContainers().size(),
						model.getBackends().size());
				tableBuf.addRow(row);
			}
		}

		tableBuf.printTable(console, "\t");
	}

	private void find(String domainFilter) {

		if (domainFilter == null) {
			return;
		}

		Set<Long> allAllowedProjectIds = projectDao.findAllAllowedProjectIds();

		String sqlFilter = domainFilter.replaceAll("\\*", "%");
		List<Domain> result = domainDao.findAllWithFilterDomain(sqlFilter);

		if (result.size() > 0) {
			console.printlnfStress("Found " + domainFilter + " in projects/backends");

			for (Domain model : result) {
				if (allAllowedProjectIds.contains(model.getBackend().getProject().getId())) {
					console.printlnf("\tPROJ %d: %s BCKE %d: %s DOMAIN %d: %s", model.getBackend().getProject().getId(),
							model.getBackend().getProject().getLabel(), model.getBackend().getId(),
							model.getBackend().getLabel(), model.getId(), model.getDomain());
				}
			}
		} else {
			console.printlnfError("No matching domain.");
		}
	}

	private void help(Project project) {
		if (project == null) {
			printHelp(CMD_LS + " [*label*]", "list projects, optionally filter by label, *-wildcard supported");
			printHelp(CMD_FIND + " *domain*",
					"list projects/backends that refer to a given domain, *-wildcard supported");
			printHelp(CMD_ADD, "add new project");
			printHelp(CMD_USE + " <id>|<label>", "switch to project");
		} else {
			printHelp(CMD_SHOW, "show project overview");
			printHelp(CMD_EDIT, "edit project");
			printHelp(CMD_DEL + " <label>",
					"delete project, enter label '" + project.getLabel() + "' as a deletion protection");
			printHelp(CMD_CONTAINERS, "edit containers of project");
			printHelp(CMD_BACKENDS, "edit proxy backends on project");
			printHelp(CMD_WRITESSHPRX, "update ssh proxy configuration on project");
		}
	}

	protected void setCommandCompletion() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_LS,
				CMD_FIND, CMD_ADD, CMD_USE);
	}

	protected void setCommandCompletionOnEdit() {
		console.setCommandCompletion(CMD_HELP, CMD_ABORT, CMD_CANCEL, CMD_CDUP, CMD_TOP, CMD_EXIT, CMD_QUIT, CMD_SHOW,
				CMD_EDIT, CMD_DEL, CMD_CONTAINERS, CMD_BACKENDS, CMD_WRITESSHPRX);
	}

	private void editHelper(Project model) throws Exception {

		String out;

		// label
		while (true) {
			out = console.readLine("Label", model.getLabel());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				if (model.getLabel() == null) {
					console.printlnfError("A label needs to be set");
				} else {
					break;
				}
			} else if (out.length() > 13) {
				console.printlnfError("Invalid label, please limit to max 13 characters");
			} else if (!validator.isLabel(out)) {
				console.printlnfError(Validator.LABEL_ERRORMSG);
			} else {
				model.setLabel(out);
				break;
			}
		}

		// fullname
		while (true) {
			out = console.readLine("Fullname", model.getFullname());

			handleExitOrAbort(out);

			if (StringUtils.isEmptyOrWhitespaceOnly(out)) {
				break;
			} else if (out.length() > 60) {
				console.printlnfError("Invalid full name, please limit to max 60 characters");
			} else {
				model.setFullname(out);
				break;
			}
		}
	}

	private void show(Project model) {
		console.printlnf("\n%s", model.toStringRecursive(console.getTerminalColumns()));
	}

	private void insert(Project model) throws Exception {

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
				projectDao.create(model);
				console.printlnfStress("Inserted new project with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
				return;
			}
		}

		// optional default configuration after project creation
		out = console.readLine("Create initial container, backend and domain configuration? y/n", "y");

		if (isNo(out)) {
			return;
		}

		// init relations
		model = projectDao.find(model.getId(), EnumSet.of(RelationType.ALL));

		// optional default configuration after backend creation
		console.printlnfStress("Container:");
		containerExecutor.insert(model, new Container());

		console.printlnfStress("Backend:");
		backendExecutor.insert(model, new Backend());

		// re-init relations
		model = projectDao.find(model.getId(), EnumSet.of(RelationType.ALL));

		show(model);
	}

	private void update(Project model) throws Exception {

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
				projectDao.update(model);
				console.printlnfStress("Updated project with id %d", model.getId());
			} catch (DuplicateKeyException e) {
				handleCaughtException(e);
			}
		}
	}

	private boolean delete(Project model) throws Exception {

		if (!isValid(model)) {
			return false;
		}

		String out = console.readLine("Please confirm deletion, y/n", "n");

		if (isYes(out)) {
			List<DomainDto> removedDomains = new ArrayList<>();
			projectDao.delete(model, removedDomains);
			cleanupCertificates(removedDomains);
			console.printlnfStress("Deleted project with id %d", model.getId());
			return true;
		} else {
			console.printlnf("Canceled deletion");
		}

		return false;
	}

	private boolean isValid(Project model) {

		if (model == null || !projectDao.findAllAllowedProjectIds().contains(model.getId())) {
			console.printlnfError("invalid project, try again");
			return false;
		}

		return true;
	}
}
