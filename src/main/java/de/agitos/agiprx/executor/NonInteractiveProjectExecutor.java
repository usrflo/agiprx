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
import java.util.List;
import java.util.logging.Logger;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dto.ProjectDto;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.Validator;

public class NonInteractiveProjectExecutor extends AbstractExecutor {

	private static NonInteractiveProjectExecutor BEAN;

	private static final Logger LOG = Logger.getLogger(NonInteractiveProjectExecutor.class.getName());

	private ProjectDao projectDao;

	private ConsoleWrapper console;

	public static NonInteractiveProjectExecutor getBean() {
		return BEAN;
	}

	public NonInteractiveProjectExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		projectDao = ProjectDao.getBean();
		console = ConsoleWrapper.getBean();
		validator = Validator.getBean();
	}

	public List<String> findProjectList() {

		List<String> result = new ArrayList<>();

		List<Project> projects = projectDao.findAllAsUser(null);

		for (Project project : projects) {
			result.add(project.getLabel());
		}

		return result;
	}

	private Project checkAndFindProject(String projectLabel) {
		Project project = projectDao.find(projectLabel);
		if (project == null) {
			throw new RuntimeException("Invalid project: label " + projectLabel + " not existing or access denied");
		}
		return project;
	}

	public ProjectDto findProjectDetails(String projectLabel) {

		Project project = checkAndFindProject(projectLabel);

		// set back-references for backends
		for (Backend backend : project.getBackends()) {
			backend.setProject(project);
			for (Domain domain : backend.getDomainForwardings()) {
				domain.setBackend(backend);
			}
		}

		// set back-references for containers
		for (Container container : project.getContainers()) {
			container.setProject(project);
			for (ContainerPermission containerPermission : container.getContainerPermissions()) {
				containerPermission.setContainer(container);
			}
		}

		return new ProjectDto(project);
	}

	@Override
	protected void setCommandCompletion() {
		console.setCommandCompletion();
	}
}
