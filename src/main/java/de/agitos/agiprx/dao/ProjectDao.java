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
package de.agitos.agiprx.dao;

import java.sql.Types;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.dao.mapper.ProjectRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.db.exception.PermissionDeniedDataAccessException;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.UserContext;

public class ProjectDao extends AbstractDao implements DependencyInjector {

	private static ProjectDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`version`,"
			+ "`label`,"
			+ "`fullname`"
			+ " FROM `project`";

	private static final String INSERT_STMT =
			"INSERT INTO `project` ("
			+ "`version`,"
			+ "`label`,"
			+ "`fullname`"					
			+ ") VALUES (0, :label, :fullname)";
	
	private static final String UPDATE_STMT =
			"UPDATE `project` SET "
			+ "`version` = :version,"
			+ "`label` = :label,"
			+ "`fullname` = :fullname"
			+ " WHERE `id` = :id";

	private static final String DELETE_STMT = "DELETE FROM `project` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	private BackendDao backendDao;

	private ContainerDao containerDao;

	private UserContext userContext;

	public ProjectDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		backendDao = BackendDao.getBean();
		containerDao = ContainerDao.getBean();
		userContext = UserContext.getBean();
	}

	public static ProjectDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(Project model) {

		if (!userContext.isUserAllowed(model.getLabel())) {
			throw new PermissionDeniedDataAccessException("User is not allowed to work on project " + model.getLabel(),
					null);
		}

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("label", model.getLabel());
			parameters.addValue("fullname", model.getFullname());

			KeyHolder keyHolder = new GeneratedKeyHolder();

			namedParamsJdbcTemplate.update(INSERT_STMT, parameters, keyHolder);

			model.setId(keyHolder.getKey().longValue());

			model.setVersion(0);

		} catch (Exception e) {
			handleInsertionError(model, e);
		}
	}

	public Project find(Long id, EnumSet<RelationType> relationTypes) {
		try {
			Project project = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new ProjectRowMapper());
			if (!userContext.isUserAllowed(project.getLabel())) {
				return null;
			}
			initRelations(project, relationTypes);
			return project;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Project find(String label) {

		if (!userContext.isUserAllowed(label)) {
			return null;
		}

		try {
			Project project = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE label = ?", new Object[] { label },
					new int[] { Types.VARCHAR }, new ProjectRowMapper());
			initRelations(project, EnumSet.of(RelationType.ALL));
			return project;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<Project> findAllAsUser(EnumSet<RelationType> relationTypes) {
		return findAll(false, relationTypes);
	}

	public List<Project> findAllAsAdmin(EnumSet<RelationType> relationTypes) {
		return findAll(true, relationTypes);
	}

	public List<Project> findAll(boolean asAdmin, EnumSet<RelationType> relationTypes) {
		List<Project> result = jdbcTemplate.query(SELECT_ALL_STMT, new ProjectRowMapper());
		Iterator<Project> iter = result.iterator();
		while (iter.hasNext()) {
			Project project = iter.next();
			if (!asAdmin && !userContext.isUserAllowed(project.getLabel())) {
				iter.remove();
			} else {
				initRelations(project, relationTypes);
			}
		}
		return result;
	}

	public List<Project> findAllWithFilterLabel(String filter, EnumSet<RelationType> relationTypes) {
		List<Project> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE label LIKE ? OR fullname LIKE ?",
				new Object[] { filter, filter }, new int[] { Types.VARCHAR, Types.VARCHAR }, new ProjectRowMapper());
		Iterator<Project> iter = result.iterator();
		while (iter.hasNext()) {
			Project project = iter.next();
			if (!userContext.isUserAllowed(project.getLabel())) {
				iter.remove();
			} else {
				initRelations(project, relationTypes);
			}
		}
		return result;
	}

	public Set<Long> findAllAllowedProjectIds() {
		Set<Long> allowedProjectIds = new HashSet<Long>();

		boolean asAdmin = userContext.isAdmin();

		List<Project> projects = jdbcTemplate.query(SELECT_ALL_STMT, new ProjectRowMapper());
		Iterator<Project> iter = projects.iterator();
		while (iter.hasNext()) {
			Project project = iter.next();
			if (asAdmin || userContext.isUserAllowed(project.getLabel())) {
				allowedProjectIds.add(project.getId());
			}
		}
		return allowedProjectIds;
	}

	public void initRelations(Project project, EnumSet<RelationType> relationTypes) {

		if (relationTypes == null) {
			return;
		}

		boolean updateAll = relationTypes.contains(RelationType.ALL);

		if ((updateAll || relationTypes.contains(RelationType.CONTAINER)) && project.getContainers() == null) {
			project.setContainers(containerDao.findAllByProject(project));
		}

		if ((updateAll || relationTypes.contains(RelationType.BACKEND)) && project.getBackends() == null) {
			project.setBackends(backendDao.findAllByProject(project.getId(), relationTypes));
		}
	}

	// @Transactional
	public void update(Project model) {

		if (!userContext.isUserAllowed(model.getLabel())) {
			throw new PermissionDeniedDataAccessException("User is not allowed to work on project " + model.getLabel(),
					null);
		}

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("label", model.getLabel());
			parameters.addValue("fullname", model.getFullname());

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {
			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(Project model, List<DomainDto> removedDomains) {

		if (!userContext.isUserAllowed(model.getLabel())) {
			throw new PermissionDeniedDataAccessException("User is not allowed to work on project " + model.getLabel(),
					null);
		}

		try {
			dataSourceUtils.startTransaction();

			initRelations(model, EnumSet.of(RelationType.ALL));

			for (Container container : model.getContainers()) {
				containerDao.delete(container);
			}

			for (Backend backend : model.getBackends()) {
				backendDao.delete(backend, removedDomains);
			}

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}
	}
}
