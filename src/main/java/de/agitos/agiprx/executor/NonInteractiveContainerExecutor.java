/*******************************************************************************
 * Copyright (C) 2023 Florian Sager, www.agitos.de
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
import de.agitos.agiprx.dao.ContainerDao;
import de.agitos.agiprx.dao.ContainerPermissionDao;
import de.agitos.agiprx.dao.HostDao;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dto.ContainerDto;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.Host;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.UserContext;
import de.agitos.agiprx.util.Validator;

public class NonInteractiveContainerExecutor extends AbstractExecutor {

	private static NonInteractiveContainerExecutor BEAN;

	private static final Logger LOG = Logger.getLogger(NonInteractiveContainerExecutor.class.getName());

	private ProjectDao projectDao;

	private ContainerDao containerDao;

	private HostDao hostDao;

	private ContainerPermissionDao containerPermissionDao;

	private UserContext userContext;

	private ConsoleWrapper console;

	public static NonInteractiveContainerExecutor getBean() {
		return BEAN;
	}

	public NonInteractiveContainerExecutor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		projectDao = ProjectDao.getBean();
		containerDao = ContainerDao.getBean();
		hostDao = HostDao.getBean();
		containerPermissionDao = ContainerPermissionDao.getBean();
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

	private Container checkAndFindContainer(Project project, String containerLabel) {

		Container container = findContainer(project, containerLabel);

		if (container != null) {
			return container;
		}

		throw new RuntimeException("Invalid container: label " + containerLabel + " not existing or access denied");
	}

	private Container findContainer(Project project, String containerLabel) {

		for (Container container : project.getContainers()) {
			if (container.getLabel().equals(containerLabel)) {
				return container;
			}
		}

		return null;
	}

	private Host checkAndFindHost(ContainerDto containerDto, boolean force) {

		String hostname = containerDto.getHostname();
		if (containerDto.getHost() != null) {
			hostname = containerDto.getHost().getHostname();
		}

		if (hostname != null) {
			return checkAndFindHost(hostname);
		}

		if (containerDto.getHostId() != null) {
			return checkAndFindHost(containerDto.getHostId());
		}

		if (force) {
			throw new RuntimeException("Invalid host: no hostname or host id available in input");
		}

		return null;
	}

	private Host checkAndFindHost(String hostname) {
		Host host = hostDao.find(hostname);
		if (host == null) {
			throw new RuntimeException("Invalid host: name " + hostname + " not existing or access denied");
		}
		return host;
	}

	private Host checkAndFindHost(Long id) {
		Host host = hostDao.find(id);
		if (host == null) {
			throw new RuntimeException("Invalid host: id " + id + " not existing or access denied");
		}
		return host;
	}

	private void checkPermissionsFromInput(List<ContainerPermission> containerPermissions) {

		throw new RuntimeException("Changing container permissions is currently disabled for security reasons");

		// for (ContainerPermission containerPermission : containerPermissions) {
		// TODO
		// }
	}

	/**
	 * Create or update container
	 * 
	 * @param projectLabel
	 * @param containerLabel : container to be created
	 * @param port           : container port
	 * 
	 * @return
	 * @throws IOException
	 * @throws AbortionException
	 * @throws InterruptedException
	 */
	public Long createOrUpdateContainer(String projectLabel, ContainerDto containerDto)
			throws IOException, InterruptedException, AbortionException {

		LOG.info("Start to create or update container " + containerDto.getLabel() + " in project " + projectLabel);

		Project project = checkAndFindProject(projectLabel);

		Container existingContainer = findContainer(project, containerDto.getLabel());

		Container container = containerDto.getContainer();

		if (existingContainer == null) {

			// create new container
			container.setProjectId(project.getId());

			container.setHost(checkAndFindHost(containerDto, true));

			containerDao.create(container);

			if (container.getContainerPermissions() != null) {

				checkPermissionsFromInput(container.getContainerPermissions());

				for (ContainerPermission containerPermission : container.getContainerPermissions()) {
					containerPermission.setContainer(container);
					containerPermissionDao.create(containerPermission);
				}
			}

		} else {

			// update existing container
			if (container.getFullname() != null) {
				existingContainer.setFullname(container.getFullname());
			}

			if (container.getLabel() != null) {
				existingContainer.setLabel(container.getLabel());
			}

			if (container.getIpv6() != null) {
				existingContainer.setIpv6(container.getIpv6());
			}

			Host host = checkAndFindHost(containerDto, false);
			if (host != null && host.getId() != existingContainer.getHostId()) {
				existingContainer.setHost(host);
			}

			if (container.getContainerPermissions() != null) {

				checkPermissionsFromInput(container.getContainerPermissions());

				for (ContainerPermission containerPermission : existingContainer.getContainerPermissions()) {
					containerPermissionDao.delete(containerPermission);
				}

				for (ContainerPermission containerPermission : container.getContainerPermissions()) {
					containerPermission.setContainer(existingContainer);
					containerPermissionDao.create(containerPermission);
				}
			}

			containerDao.update(existingContainer);

			container.setId(existingContainer.getId());
		}

		// LOG.info("Removed " + containerRef.toString());

		// roll out changed configuration
		// haProxyProcessor.manageConfiguration(false, true);

		LOG.info("Finished create/update of container");

		return container.getId();
	}

	@Override
	protected void setCommandCompletion() {
		console.setCommandCompletion();
	}
}
