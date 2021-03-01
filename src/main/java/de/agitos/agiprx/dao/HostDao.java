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
import java.util.List;
import java.util.Set;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.dao.mapper.HostRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.model.Host;
import de.agitos.agiprx.util.Assert;

public class HostDao extends AbstractDao implements DependencyInjector {

	private static HostDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`hostname`,"
			+ "`version`,"
			+ "`ipv6`,"
			+ "`admin_password`"
			+ " FROM `host`";

	private static final String INSERT_STMT =
			"INSERT INTO `host` ("
			+ "`hostname`,"
			+ "`version`,"
			+ "`ipv6`,"
			+ "`admin_password`"
			+ ") VALUES (:hostname, 0, :ipv6, :admin_password)";

	private static final String UPDATE_STMT =
			"UPDATE `host` SET "
			+ "`hostname` = :hostname,"
			+ "`version` = :version,"
			+ "`ipv6` = :ipv6,"
			+ "`admin_password` = :admin_password"
			+ " WHERE `id` = :id";

	private static final String DELETE_STMT = "DELETE FROM `host` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	public HostDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	public static HostDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(Host model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("hostname", model.getHostname());
			parameters.addValue("ipv6", model.getIpv6());
			parameters.addValue("admin_password", model.getAdminPassword());

			KeyHolder keyHolder = new GeneratedKeyHolder();

			namedParamsJdbcTemplate.update(INSERT_STMT, parameters, keyHolder);

			model.setId(keyHolder.getKey().longValue());

			model.setVersion(0);

		} catch (Exception e) {
			handleInsertionError(model, e);
		}
	}

	public Host find(Long id) {
		try {
			return jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new HostRowMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Host find(String label) {

		try {
			return jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE hostname = ?", new Object[] { label },
					new int[] { Types.VARCHAR }, new HostRowMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<Host> findAll() {
		return jdbcTemplate.query(SELECT_ALL_STMT, new HostRowMapper());
	}

	public Set<Long> findAllIds() {
		Set<Long> result = new HashSet<Long>();
		for (Host host : jdbcTemplate.query(SELECT_ALL_STMT, new HostRowMapper())) {
			result.add(host.getId());
		}
		return result;
	}

	// @Transactional
	public void update(Host model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("hostname", model.getHostname());
			parameters.addValue("ipv6", model.getIpv6());
			parameters.addValue("admin_password", model.getAdminPassword());

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {
			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(Host model) {

		try {
			dataSourceUtils.startTransaction();

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}
	}
}
