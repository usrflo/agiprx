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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.dao.mapper.ContainerPermissionRowMapper;
import de.agitos.agiprx.dao.mapper.StringRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.util.Assert;

public class ContainerPermissionDao extends AbstractDao implements DependencyInjector {

	private static ContainerPermissionDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`version`,"
			+ "`container_id`,"
			+ "`user_id`,"
			+ "`permission`,"
			+ "`password`"
			+ " FROM `container_permission`";

	private static final String INSERT_STMT =
			"INSERT INTO `container_permission` ("
			+ "`version`,"
			+ "`container_id`,"
			+ "`user_id`,"
			+ "`permission`,"
			+ "`password`"
			+ ") VALUES (0, :container_id, :user_id, :permission, :password)";
	
	private static final String UPDATE_STMT =
			"UPDATE `container_permission` SET "
			+ "`version` = :version,"
			+ "`container_id` = :container_id,"
			+ "`user_id` = :user_id,"
			+ "`permission` = :permission,"
			+ "`password` = :password"
			+ " WHERE `id` = :id";

	private static final String DELETE_STMT = "DELETE FROM `container_permission` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	private UserDao userDao;

	private static final String FIXED_SECRET = "O70bL4cvMG";

	private PBEStringEncryptor stringEncryptor;

	public ContainerPermissionDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		this.stringEncryptor = new StandardPBEStringEncryptor();
		this.stringEncryptor.setPassword(Config.getBean().getString("application.secret") + FIXED_SECRET);
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		this.userDao = UserDao.getBean();
	}

	public static ContainerPermissionDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(ContainerPermission model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("container_id", model.getContainerId());
			parameters.addValue("user_id", model.getUserId());
			parameters.addValue("permission", model.getPermission());
			parameters.addValue("password",
					model.getPassword() == null ? null : stringEncryptor.encrypt(model.getPassword()));

			KeyHolder keyHolder = new GeneratedKeyHolder();

			namedParamsJdbcTemplate.update(INSERT_STMT, parameters, keyHolder);

			model.setId(keyHolder.getKey().longValue());

			model.setVersion(0);

		} catch (Exception e) {
			handleInsertionError(model, e);
		}
	}

	public ContainerPermission find(Long id) {
		try {
			ContainerPermission cp = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new ContainerPermissionRowMapper(stringEncryptor));
			initRelations(cp);
			return cp;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public ContainerPermission findByUniqueIds(Long containerId, Long userId, String permission) {
		try {
			ContainerPermission cp = jdbcTemplate.queryForObject(
					SELECT_ALL_STMT + " WHERE container_id = ? AND user_id = ? AND permission = ?",
					new Object[] { containerId, userId, permission },
					new int[] { Types.NUMERIC, Types.NUMERIC, Types.VARCHAR },
					new ContainerPermissionRowMapper(stringEncryptor));
			initRelations(cp);
			return cp;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<ContainerPermission> findAll() {
		List<ContainerPermission> result = jdbcTemplate.query(SELECT_ALL_STMT,
				new ContainerPermissionRowMapper(stringEncryptor));
		for (ContainerPermission cp : result) {
			initRelations(cp);
		}
		return result;
	}

	public List<ContainerPermission> findAllByContainer(Long containerId) {
		List<ContainerPermission> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE container_id = ?",
				new Object[] { containerId }, new int[] { Types.NUMERIC },
				new ContainerPermissionRowMapper(stringEncryptor));
		for (ContainerPermission cp : result) {
			initRelations(cp);
		}
		return result;
	}

	public Set<Long> findAllIdsByContainer(Long containerId) {
		Set<Long> result = new HashSet<Long>();
		List<ContainerPermission> cpList = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE container_id = ?",
				new Object[] { containerId }, new int[] { Types.NUMERIC },
				new ContainerPermissionRowMapper(stringEncryptor));
		for (ContainerPermission cp : cpList) {
			result.add(cp.getId());
		}
		return result;
	}

	public List<ContainerPermission> findAllByUser(Long userId) {
		return jdbcTemplate.query(SELECT_ALL_STMT + " WHERE user_id = ?", new Object[] { userId },
				new int[] { Types.NUMERIC }, new ContainerPermissionRowMapper(stringEncryptor));
	}

	public ContainerPermission findPasswordOfSamePermission(Long containerId, String permission) {
		try {
			// return first non null password record
			return jdbcTemplate.queryForObject(
					SELECT_ALL_STMT + " WHERE container_id = ? AND permission = ? AND password is not NULL LIMIT 1",
					new Object[] { containerId, permission }, new int[] { Types.NUMERIC, Types.VARCHAR },
					new ContainerPermissionRowMapper(stringEncryptor));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	// @Transactional
	public List<Long> updatePasswordsToSamePermission(ContainerPermission containerPermission) {

		List<Long> changedPermissionIds = new ArrayList<Long>();

		try {
			dataSourceUtils.startTransaction();

			String encryptedPassword = jdbcTemplate.queryForObject(
					"SELECT `password` FROM `container_permission` WHERE id = ?",
					new Object[] { containerPermission.getId() }, new int[] { Types.NUMERIC },
					new StringRowMapper("password"));

			// HINT: a reset to NULL is processed as well as a password propagation

			MapSqlParameterSource updateParameters = new MapSqlParameterSource();
			updateParameters.addValue("encryptedPassword", encryptedPassword);
			updateParameters.addValue("containerId", containerPermission.getContainerId());
			updateParameters.addValue("permission", containerPermission.getPermission());
			updateParameters.addValue("id", containerPermission.getId());

			int changedRows = namedParamsJdbcTemplate.update(
					"UPDATE `container_permission` SET " + "`password` = :encryptedPassword"
							+ " WHERE `container_id` = :containerId AND `permission` = :permission AND `id` != :id",
					updateParameters);

			if (changedRows > 0) {

				MapSqlParameterSource selectParameters = new MapSqlParameterSource();
				selectParameters.addValue("containerId", containerPermission.getContainerId());
				selectParameters.addValue("permission", containerPermission.getPermission());
				selectParameters.addValue("id", containerPermission.getId());

				List<Map<String, Object>> queryResult = namedParamsJdbcTemplate.queryForList(
						"SELECT `id` FROM `container_permission`"
								+ " WHERE `container_id` = :containerId AND `permission` = :permission AND `id` != :id",
						selectParameters);

				for (Map<String, Object> row : queryResult) {
					changedPermissionIds.add((Long) row.get("id"));
				}
			}

		} catch (Exception e) {
			handleUpdateError(containerPermission, e);
		}

		return changedPermissionIds;
	}

	private void initRelations(ContainerPermission containerPermission) {
		// containerPermission.setContainer(containerDao.find(containerPermission.getContainerId()));
		containerPermission.setUser(userDao.find(containerPermission.getUserId()));
	}

	// @Transactional
	public void update(ContainerPermission model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("container_id", model.getContainerId());
			parameters.addValue("user_id", model.getUserId());
			parameters.addValue("permission", model.getPermission());
			parameters.addValue("password",
					model.getPassword() == null ? null : stringEncryptor.encrypt(model.getPassword()));

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {
			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(ContainerPermission model) {

		try {
			dataSourceUtils.startTransaction();

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}
	}
}
