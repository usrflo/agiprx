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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.dao.mapper.UserRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.model.UserRoleType;
import de.agitos.agiprx.util.Assert;

public class UserDao extends AbstractDao implements DependencyInjector {

	private static UserDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`version`,"
			+ "`fullname`,"
			+ "`email`,"
			+ "`ssh_public_key`,"
			+ "`role`,"
			+ "`default_permission`,"
			+ "`agiprx_permission`"
			+ " FROM `user`";
	
	private static final String INSERT_STMT =
			"INSERT INTO `user` ("
			+ "`version`,"
			+ "`fullname`,"
			+ "`email`,"
			+ "`ssh_public_key`,"
			+ "`role`,"
			+ "`default_permission`,"
			+ "`agiprx_permission`"
			+ ") VALUES (0, :fullname, :email, :ssh_public_key, :role, :default_permission, :agiprx_permission)";

	private static final String UPDATE_STMT =
			"UPDATE `user` SET "
			+ "`version` = :version,"
			+ "`fullname` = :fullname,"
			+ "`email` = :email,"
			+ "`ssh_public_key` = :ssh_public_key,"
			+ "`role` = :role,"
			+ "`default_permission` = :default_permission,"
			+ "`agiprx_permission` = :agiprx_permission"
			+ " WHERE `id` = :id";

	private static final String DELETE_STMT = "DELETE FROM `user` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	public UserDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	public static UserDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(User model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("fullname", model.getFullname());
			parameters.addValue("email", model.getEmail());
			parameters.addValue("ssh_public_key", model.getSshPublicKey());
			parameters.addValue("role", model.getRole() == null ? UserRoleType.USER : model.getRole().name());
			parameters.addValue("default_permission",
					model.getDefaultPermission() == null ? "" : String.join(",", model.getDefaultPermission()));
			parameters.addValue("agiprx_permission",
					model.getAgiPrxPermission() == null ? null : String.join(",", model.getAgiPrxPermission()));

			KeyHolder keyHolder = new GeneratedKeyHolder();

			namedParamsJdbcTemplate.update(INSERT_STMT, parameters, keyHolder);

			model.setId(keyHolder.getKey().longValue());
			model.setVersion(0);

		} catch (Exception e) {
			handleInsertionError(model, e);
		}
	}

	public User find(Long id) {
		try {
			return jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new UserRowMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<User> findAll() {
		return jdbcTemplate.query(SELECT_ALL_STMT, new UserRowMapper());
	}

	public Set<Long> findAllIds() {
		Set<Long> result = new HashSet<Long>();
		for (User user : findAll()) {
			result.add(user.getId());
		}
		return result;
	}

	public List<User> findAllWithFilterDefaultPermission(String permission) {

		List<User> result = findAll();

		Iterator<User> iter = result.iterator();
		while (iter.hasNext()) {
			User user = iter.next();
			boolean found = false;
			for (String defaultPermission : user.getDefaultPermission()) {
				if (permission.equals(defaultPermission)) {
					found = true;
					break;
				}
			}
			if (!found) {
				iter.remove();
			}
		}

		return result;
	}

	public List<User> findAllWithFilter(String filter) {
		return jdbcTemplate.query(SELECT_ALL_STMT + " WHERE fullname LIKE ? OR email LIKE ?",
				new Object[] { filter, filter }, new int[] { Types.VARCHAR, Types.VARCHAR }, new UserRowMapper());
	}

	// @Transactional
	public void update(User model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("fullname", model.getFullname());
			parameters.addValue("email", model.getEmail());
			parameters.addValue("ssh_public_key", model.getSshPublicKey());
			parameters.addValue("role", model.getRole().name());
			parameters.addValue("default_permission",
					model.getDefaultPermission() == null ? "" : String.join(",", model.getDefaultPermission()));
			parameters.addValue("agiprx_permission",
					model.getAgiPrxPermission() == null ? null : String.join(",", model.getAgiPrxPermission()));

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {
			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(User model) {

		try {
			dataSourceUtils.startTransaction();

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}
	}
}
