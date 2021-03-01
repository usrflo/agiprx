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
import java.util.List;
import java.util.Set;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.dao.mapper.BackendRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.BackendContainer;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.model.Project;
import de.agitos.agiprx.util.Assert;

public class BackendDao extends AbstractDao implements DependencyInjector {

	private static BackendDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`version`,"
			+ "`label`,"
			+ "`fullname`,"
			+ "`project_id`,"
			+ "`port`,"
			+ "`params`"
			+ " FROM `backend`";

	private static final String INSERT_STMT =
			"INSERT INTO `backend` ("
			+ "`version`,"
			+ "`label`,"
			+ "`fullname`,"
			+ "`project_id`,"
			+ "`port`,"
			+ "`params`"					
			+ ") VALUES (0, :label, :fullname, :project_id, :port, :params)";
	
	private static final String UPDATE_STMT =
			"UPDATE `backend` SET "
			+ "`version` = :version,"
			+ "`label` = :label,"
			+ "`fullname` = :fullname,"
			+ "`project_id` = :project_id,"
			+ "`port` = :port,"
			+ "`params` = :params"
			+ " WHERE `id` = :id";

	private static final String DELETE_STMT = "DELETE FROM `backend` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	private ProjectDao projectDao;

	private DomainDao domainDao;

	private BackendContainerDao backendContainerDao;

	public BackendDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		this.projectDao = ProjectDao.getBean();
		this.domainDao = DomainDao.getBean();
		this.backendContainerDao = BackendContainerDao.getBean();
	}

	public static BackendDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(Backend model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("label", model.getLabel());
			parameters.addValue("fullname", model.getFullname());
			parameters.addValue("project_id", model.getProjectId());
			parameters.addValue("port", model.getPort());
			parameters.addValue("params", model.getParams());

			KeyHolder keyHolder = new GeneratedKeyHolder();

			namedParamsJdbcTemplate.update(INSERT_STMT, parameters, keyHolder);

			model.setId(keyHolder.getKey().longValue());

			model.setVersion(0);

		} catch (Exception e) {
			handleInsertionError(model, e);
		}
	}

	public Backend find(Long id) {
		try {
			Backend backend = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new BackendRowMapper());
			initRelations(backend, EnumSet.of(RelationType.ALL));
			return backend;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Backend find(Project project, String label, EnumSet<RelationType> relationTypes) {
		try {
			Backend backend = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE project_id = ? AND label = ?",
					new Object[] { project.getId(), label }, new int[] { Types.NUMERIC, Types.VARCHAR },
					new BackendRowMapper());
			initRelations(backend, relationTypes);
			return backend;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<Backend> findAll(EnumSet<RelationType> relationTypes) {
		List<Backend> result = jdbcTemplate.query(SELECT_ALL_STMT, new BackendRowMapper());
		for (Backend backend : result) {
			initRelations(backend, relationTypes);
		}
		return result;
	}

	public List<Backend> findAllByProject(Long projectId, EnumSet<RelationType> relationTypes) {
		List<Backend> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE project_id = ?", new Object[] { projectId },
				new int[] { Types.NUMERIC }, new BackendRowMapper());
		for (Backend backend : result) {
			initRelations(backend, relationTypes);
		}
		return result;
	}

	public Set<Long> findAllIdsByProject(Long projectId) {
		Set<Long> result = new HashSet<Long>();
		List<Backend> backends = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE project_id = ?",
				new Object[] { projectId }, new int[] { Types.NUMERIC }, new BackendRowMapper());
		for (Backend backend : backends) {
			result.add(backend.getId());
		}
		return result;
	}

	private void initRelations(Backend backend, EnumSet<RelationType> relationTypes) {

		if (relationTypes == null) {
			return;
		}

		boolean updateAll = relationTypes.contains(RelationType.ALL);

		if ((updateAll || relationTypes.contains(RelationType.DOMAIN)) && backend.getDomainForwardings() == null) {
			backend.setDomainForwardings(domainDao.findAllByBackend(backend.getId(), null));
		}

		if ((updateAll || relationTypes.contains(RelationType.CONTAINER)) && backend.getBackendContainers() == null) {
			backend.setBackendContainers(backendContainerDao.findAllByBackend(backend.getId()));
		}
	}

	public void initBackRelations(Backend backend) {
		backend.setProject(projectDao.find(backend.getProjectId(), EnumSet.of(RelationType.ALL)));
	}

	// @Transactional
	public void update(Backend model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("label", model.getLabel());
			parameters.addValue("fullname", model.getFullname());
			parameters.addValue("project_id", model.getProjectId());
			parameters.addValue("port", model.getPort());
			parameters.addValue("params", model.getParams());

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {
			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(Backend model, List<DomainDto> removedDomains) {

		try {
			dataSourceUtils.startTransaction();

			initRelations(model, EnumSet.of(RelationType.ALL));

			for (BackendContainer backendContainer : model.getBackendContainers()) {
				backendContainerDao.delete(backendContainer);
			}

			for (Domain domain : model.getDomainForwardings()) {
				domainDao.delete(domain, removedDomains);
			}

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}
	}
}
