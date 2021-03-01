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
package de.agitos.agiprx.bean;

import java.util.EnumSet;
import java.util.List;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.dao.ContainerDao;
import de.agitos.agiprx.dao.DomainDao;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dao.RelationType;
import de.agitos.agiprx.dao.UserDao;
import de.agitos.agiprx.executor.AbstractExecutor;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.UserContext;

public class SearchBean extends AbstractExecutor implements DependencyInjector {

	private static SearchBean BEAN;

	private UserContext userContext;

	private DomainDao domainDao;

	private ProjectDao projectDao;

	private ContainerDao containerDao;

	private UserDao userDao;

	public static SearchBean getBean() {
		return BEAN;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		userContext = UserContext.getBean();
		domainDao = DomainDao.getBean();
		projectDao = ProjectDao.getBean();
		containerDao = ContainerDao.getBean();
		userDao = UserDao.getBean();
	}

	public SearchBean() {
		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	public void jump(String out) {

		if (!userContext.isAdmin()) {
			// check if this can be removed: 'isValid' in *Executors may block
			// non-legitimate access on user level
			throw new RuntimeException("This is for admins only!");
		}

		String filter = null;
		String level = null;
		if (isCommandWithParam(CMD_PROJECT, out)) {
			level = CMD_PROJECT;
			filter = getStringParam(out);
		} else if (isCommandWithParam(CMD_BACKEND, out)) {
			level = CMD_BACKEND;
			filter = getStringParam(out);
		} else if (isCommandWithParam(CMD_CONTAINER, out)) {
			level = CMD_CONTAINER;
			filter = getStringParam(out);
		} else if (isCommandWithParam(CMD_DOMAINS, out)) {
			level = CMD_DOMAINS;
			filter = getStringParam(out);
		} else if (isCommandWithParam(CMD_USER, out)) {
			level = CMD_USER;
			filter = getStringParam(out);
		} else {
			filter = out;
		}

		// transform to SQL wildcard
		filter = filter.replaceAll("\\*", "%");

		try {

			// search domain by query
			List<Domain> domains = domainDao.findAllWithFilterDomain(filter);
			if (!domains.isEmpty()) {
				Domain domain = domains.get(0);

				addCommandToBuffer(CMD_PROJECTS, null);
				addCommandToBuffer(CMD_USE, domain.getBackend().getProjectId());
				if (CMD_PROJECT.equals(level)) {
					return;
				}
				if (CMD_CONTAINER.equals(level)) {
					addCommandToBuffer(CMD_CONTAINERS, null);
					addCommandToBuffer(CMD_USE, domain.getBackend().getProject().getContainers().get(0).getId());
					return;
				}
				if (CMD_USER.equals(level)) {
					addCommandToBuffer(CMD_CONTAINERS, null);
					addCommandToBuffer(CMD_USE, domain.getBackend().getProject().getContainers().get(0).getId());
					addCommandToBuffer(CMD_PERMISSIONS, null);
					return;
				}

				addCommandToBuffer(CMD_BACKENDS, null);
				addCommandToBuffer(CMD_USE, domain.getBackendId());
				if (CMD_BACKEND.equals(level)) {
					return;
				}

				// domain editing is default on domain search
				addCommandToBuffer(CMD_DOMAINS, null);
				addCommandToBuffer(CMD_EDIT, domain.getId());

				return;
			}

			// if no match search container by query
			// TODO ?

			// if no match search backend by query
			// TODO ?

			// if no match search container by IPv6
			List<Container> containers = containerDao.findAllWithFilterIPv6(filter);
			if (!containers.isEmpty()) {
				Container container = containers.get(0);

				addCommandToBuffer(CMD_PROJECTS, null);
				addCommandToBuffer(CMD_USE, container.getProjectId());

				if (CMD_PROJECT.equals(level)) {
					return;
				}
				if (CMD_USER.equals(level)) {
					addCommandToBuffer(CMD_CONTAINERS, null);
					addCommandToBuffer(CMD_USE, container.getId());
					addCommandToBuffer(CMD_PERMISSIONS, null);
					return;
				}
				if (CMD_BACKEND.equals(level)) {
					addCommandToBuffer(CMD_BACKENDS, null);
					addCommandToBuffer(CMD_USE, container.getProject().getBackends().get(0));
					return;
				}
				if (CMD_DOMAIN.equals(level)) {
					addCommandToBuffer(CMD_BACKENDS, null);
					addCommandToBuffer(CMD_USE, container.getProject().getBackends().get(0).getId());
					addCommandToBuffer(CMD_DOMAINS, null);
				}

				// container selection is default on ipv6 search
				addCommandToBuffer(CMD_CONTAINERS, null);
				addCommandToBuffer(CMD_USE, container.getId());

				return;
			}

			// if no match search project by query
			List<Project> projects = projectDao.findAllWithFilterLabel(filter, EnumSet.of(RelationType.ALL));
			if (!projects.isEmpty()) {

				Project project = projects.get(0);
				addCommandToBuffer(CMD_PROJECTS, null);
				addCommandToBuffer(CMD_USE, project.getId());

				if (CMD_PROJECT.equals(level)) {
					return;
				}

				if (CMD_CONTAINER.equals(level)) {
					addCommandToBuffer(CMD_CONTAINERS, null);
					addCommandToBuffer(CMD_USE, project.getContainers().get(0).getId());
					return;
				}
				if (CMD_BACKEND.equals(level)) {
					addCommandToBuffer(CMD_BACKENDS, null);
					addCommandToBuffer(CMD_USE, project.getBackends().get(0).getId());
					return;
				}
				if (CMD_DOMAIN.equals(level)) {
					addCommandToBuffer(CMD_BACKENDS, null);
					addCommandToBuffer(CMD_USE, project.getBackends().get(0).getId());
					addCommandToBuffer(CMD_DOMAINS, null);
					return;
				}
				if (CMD_USER.equals(level)) {
					addCommandToBuffer(CMD_CONTAINERS, null);
					addCommandToBuffer(CMD_USE, project.getContainers().get(0).getId());
					addCommandToBuffer(CMD_PERMISSIONS, null);
					return;
				}

				// project selection is default
				return;
			}
		} catch (NullPointerException | IndexOutOfBoundsException e) {
			// present best match
			return;
		}

		// if no match search user by query
		if (userContext.isAdmin()) {
			List<User> users = userDao.findAllWithFilter(filter);
			if (!users.isEmpty()) {
				User user = users.get(0);
				addCommandToBuffer(CMD_USERS, null);
				addCommandToBuffer(CMD_EDIT, user.getId());
			}
		}

		console.printlnfError("No matches found");

	}

	private void addCommandToBuffer(String cmd, Object argument) {
		if (argument == null) {
			console.addBufferedInput(cmd.replaceFirst("_", ""));
		} else {
			console.addBufferedInput(cmd.replaceFirst("_", "") + " " + argument);
		}
	}

	@Override
	protected void setCommandCompletion() {
		// nothing to do
	}
}
