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
import de.agitos.agiprx.dao.mapper.BackendContainerRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.model.BackendContainer;
import de.agitos.agiprx.util.Assert;

public class BackendContainerDao extends AbstractDao implements DependencyInjector {

	private static BackendContainerDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`version`,"
			+ "`backend_id`,"
			+ "`container_id`,"
			+ "`params`"
			+ " FROM `backend_container`";

	private static final String INSERT_STMT =
			"INSERT INTO `backend_container` ("
			+ "`version`,"
			+ "`backend_id`,"
			+ "`container_id`,"
			+ "`params`"					
			+ ") VALUES (0, :backend_id, :container_id, :params)";
	
	private static final String UPDATE_STMT =
			"UPDATE `backend_container` SET "
			+ "`version` = :version,"
			+ "`backend_id` = :backend_id,"
			+ "`container_id` = :container_id,"
			+ "`params` = :params"
			+ " WHERE `id` = :id";

	private static final String DELETE_STMT = "DELETE FROM `backend_container` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	private BackendDao backendDao;

	private ContainerDao containerDao;

	public BackendContainerDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		this.backendDao = BackendDao.getBean();
		this.containerDao = ContainerDao.getBean();
	}

	public static BackendContainerDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(BackendContainer model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("backend_id", model.getBackendId());
			parameters.addValue("container_id", model.getContainerId());
			parameters.addValue("params", model.getParams());

			KeyHolder keyHolder = new GeneratedKeyHolder();

			namedParamsJdbcTemplate.update(INSERT_STMT, parameters, keyHolder);

			model.setId(keyHolder.getKey().longValue());

			model.setVersion(0);

		} catch (Exception e) {
			handleInsertionError(model, e);
		}
	}

	public BackendContainer find(Long id) {
		try {
			BackendContainer cp = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new BackendContainerRowMapper());
			initRelations(cp, EnumSet.of(RelationType.CONTAINER));
			return cp;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<BackendContainer> findAll() {
		List<BackendContainer> result = jdbcTemplate.query(SELECT_ALL_STMT, new BackendContainerRowMapper());
		for (BackendContainer cp : result) {
			initRelations(cp, EnumSet.of(RelationType.CONTAINER));
		}
		return result;
	}

	public List<BackendContainer> findAllByContainer(Long containerId) {
		List<BackendContainer> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE container_id = ?",
				new Object[] { containerId }, new int[] { Types.NUMERIC }, new BackendContainerRowMapper());
		for (BackendContainer cp : result) {
			initRelations(cp, EnumSet.of(RelationType.BACKEND));
		}
		return result;
	}

	public List<BackendContainer> findAllByBackend(Long backendId) {
		List<BackendContainer> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE backend_id = ?",
				new Object[] { backendId }, new int[] { Types.NUMERIC }, new BackendContainerRowMapper());
		for (BackendContainer cp : result) {
			initRelations(cp, EnumSet.of(RelationType.CONTAINER));
		}
		return result;
	}

	public Set<Long> findAllIdsByBackend(Long backendId) {
		Set<Long> result = new HashSet<Long>();
		List<BackendContainer> backendContainer = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE backend_id = ?",
				new Object[] { backendId }, new int[] { Types.NUMERIC }, new BackendContainerRowMapper());
		for (BackendContainer cp : backendContainer) {
			result.add(cp.getId());
		}
		return result;
	}

	private void initRelations(BackendContainer backendContainer, EnumSet<RelationType> relationTypes) {

		if (relationTypes == null) {
			return;
		}

		boolean updateAll = relationTypes.contains(RelationType.ALL);

		if ((updateAll || relationTypes.contains(RelationType.BACKEND)) && backendContainer.getBackend() == null) {
			backendContainer.setBackend(backendDao.find(backendContainer.getBackendId()));
		}

		if ((updateAll || relationTypes.contains(RelationType.CONTAINER)) && backendContainer.getContainer() == null) {
			backendContainer.setContainer(containerDao.find(backendContainer.getContainerId()));
		}
	}

	// @Transactional
	public void update(BackendContainer model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("backend_id", model.getBackendId());
			parameters.addValue("container_id", model.getContainerId());
			parameters.addValue("params", model.getParams());

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {
			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(BackendContainer model) {

		try {

			dataSourceUtils.startTransaction();

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}
	}
}
