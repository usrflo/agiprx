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
import java.util.List;
import java.util.logging.Logger;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.dao.BackendContainerDao;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.BackendContainer;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.UserContext;
import de.agitos.agiprx.util.Validator;

public class NonInteractiveBackendExecutor extends AbstractExecutor {

	private static NonInteractiveBackendExecutor BEAN;

	private static final Logger LOG = Logger.getLogger(NonInteractiveBackendExecutor.class.getName());

	private ProjectDao projectDao;

	private BackendContainerDao backendContainerDao;

	private HAProxyProcessor haProxyProcessor;

	private UserContext userContext;

	private ConsoleWrapper console;

	public static NonInteractiveBackendExecutor getBean() {
		return BEAN;
	}

	public NonInteractiveBackendExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		projectDao = ProjectDao.getBean();
		backendContainerDao = BackendContainerDao.getBean();
		haProxyProcessor = HAProxyProcessor.getBean();
		userContext = UserContext.getBean();
		console = ConsoleWrapper.getBean();
		validator = Validator.getBean();
	}

	private Project checkAndFindProject(String projectLabel) {
		Project project = projectDao.find(projectLabel);
		if (project == null) {
			throw new RuntimeException("Invalid project: label " + projectLabel + " not existing or access denied");
		}
		return project;
	}

	private Backend checkAndFindBackend(Project project, String backendLabel) {

		for (Backend backend : project.getBackends()) {
			if (backend.getLabel().equals(backendLabel)) {
				return backend;
			}
		}

		throw new RuntimeException("Invalid backend: label " + backendLabel + " not existing or access denied");
	}

	/**
	 * Set backend containers of target backend to given backend
	 * 
	 * @param projectLabel
	 * @param backendLabel       : backend to be updated (existing backends are
	 *                           removed)
	 * @param targetBackendLabel : backend in the same project whose backend
	 *                           containers should be set
	 * @return
	 * @throws IOException
	 * @throws AbortionException
	 * @throws InterruptedException
	 */
	public boolean setContainersOfTargetBackend(String projectLabel, String backendLabel, String targetBackendLabel)
			throws IOException, InterruptedException, AbortionException {

		LOG.info("Start to set backend containers from project " + projectLabel + " backend " + targetBackendLabel
				+ " to " + backendLabel);

		Project project = checkAndFindProject(projectLabel);

		Backend backend = checkAndFindBackend(project, backendLabel);

		Backend targetBackend = checkAndFindBackend(project, targetBackendLabel);

		List<BackendContainer> targetContainerRefs = targetBackend.getBackendContainers();

		if (targetContainerRefs == null || targetContainerRefs.isEmpty()) {
			throw new RuntimeException(
					"Target backend with label " + targetBackendLabel + " does not contain any container references");
		}

		for (BackendContainer containerRef : backend.getBackendContainers()) {
			backendContainerDao.delete(containerRef);
			LOG.info("Removed " + containerRef.toString());
		}

		for (BackendContainer containerRef : targetContainerRefs) {
			containerRef.setId(null);
			containerRef.setVersion(0);
			containerRef.setBackend(backend);
			backendContainerDao.create(containerRef);
			LOG.info("Added " + containerRef.toString());
		}

		// roll out changed configuration
		haProxyProcessor.manageConfiguration(false, true);

		LOG.info("Finished change of backend container configuration");

		return true;
	}

	@Override
	protected void setCommandCompletion() {
		console.setCommandCompletion();
	}
}
