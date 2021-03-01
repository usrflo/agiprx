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
import de.agitos.agiprx.dao.mapper.DomainRowMapper;
import de.agitos.agiprx.dao.mapper.StringRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.util.Assert;

public class DomainDao extends AbstractDao implements DependencyInjector {

	private static DomainDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`version`,"
			+ "`domain`,"
			+ "`backend_id`,"
			+ "`certprovided`,"
			+ "`letsencrypt`,"
			+ "`redirect_to_url`"
			+ " FROM `domain`";

	private static final String INSERT_STMT =
			"INSERT INTO `domain` ("
			+ "`version`,"
			+ "`domain`,"
			+ "`backend_id`,"
			+ "`certprovided`,"
			+ "`letsencrypt`,"
			+ "`redirect_to_url`"
			+ ") VALUES (0, :domain, :backend_id, :certprovided, :letsencrypt, :redirect_to_url)";

	private static final String UPDATE_STMT =
			"UPDATE `domain` SET "
			+ "`version` = :version,"
			+ "`domain` = :domain,"
			+ "`backend_id` = :backend_id,"
			+ "`certprovided` = :certprovided,"
			+ "`letsencrypt` = :letsencrypt,"
			+ "`redirect_to_url` = :redirect_to_url"
			+ " WHERE `id` = :id";

	private static final String DELETE_STMT = "DELETE FROM `domain` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	private BackendDao backendDao;

	public DomainDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		super.postConstruct();

		this.backendDao = BackendDao.getBean();
	}

	public static DomainDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(Domain model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("domain", model.getDomain());
			parameters.addValue("backend_id", model.getBackendId());
			parameters.addValue("certprovided", model.getCertProvided());
			parameters.addValue("letsencrypt", model.getLetsEncrypt());
			parameters.addValue("redirect_to_url", model.getRedirectToUrl());

			KeyHolder keyHolder = new GeneratedKeyHolder();

			namedParamsJdbcTemplate.update(INSERT_STMT, parameters, keyHolder);

			model.setId(keyHolder.getKey().longValue());

			model.setVersion(0);

		} catch (Exception e) {
			handleInsertionError(model, e);
		}
	}

	public Domain find(Long id) {
		try {
			return jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new DomainRowMapper());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Domain find(String domainName) {
		return find(domainName, true);
	}

	public Domain find(String domainName, boolean initBackRelations) {
		try {
			Domain domain = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE domain = ?",
					new Object[] { domainName }, new int[] { Types.VARCHAR }, new DomainRowMapper());

			if (domain != null && initBackRelations) {
				initBackRelations(domain);
			}

			return domain;

		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<Domain> findAll() {
		return jdbcTemplate.query(SELECT_ALL_STMT, new DomainRowMapper());
	}

	public List<String> findAllLesslDomains() {
		return jdbcTemplate.query("SELECT `domain` FROM `domain` WHERE `letsencrypt` = 1",
				new StringRowMapper("domain"));
	}

	public List<Domain> findAllByBackend(Long backendId, String filter) {
		if (filter == null) {
			return jdbcTemplate.query(SELECT_ALL_STMT + " WHERE backend_id = ?", new Object[] { backendId },
					new int[] { Types.NUMERIC }, new DomainRowMapper());
		} else {
			return jdbcTemplate.query(SELECT_ALL_STMT + " WHERE backend_id = ? AND domain LIKE ?",
					new Object[] { backendId, filter }, new int[] { Types.NUMERIC, Types.VARCHAR },
					new DomainRowMapper());
		}
	}

	public Set<Long> findAllIdsByBackend(Long backendId) {
		Set<Long> result = new HashSet<Long>();
		for (Domain domain : findAllByBackend(backendId, null)) {
			result.add(domain.getId());
		}
		return result;
	}

	public List<Domain> findAllWithFilterDomain(String filter) {
		List<Domain> result = jdbcTemplate.query(SELECT_ALL_STMT + " WHERE domain LIKE ?", new Object[] { filter },
				new int[] { Types.VARCHAR }, new DomainRowMapper());
		for (Domain domain : result) {
			initBackRelations(domain);
		}
		return result;
	}

	private void initBackRelations(Domain domain) {
		domain.setBackend(backendDao.find(domain.getBackendId()));
		backendDao.initBackRelations(domain.getBackend());
	}

	// @Transactional
	public void update(Domain model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("domain", model.getDomain());
			parameters.addValue("backend_id", model.getBackendId());
			parameters.addValue("certprovided", model.getCertProvided());
			parameters.addValue("letsencrypt", model.getLetsEncrypt());
			parameters.addValue("redirect_to_url", model.getRedirectToUrl());

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {
			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(Domain model, List<DomainDto> removedDomains) {

		try {
			dataSourceUtils.startTransaction();

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}

		if (removedDomains != null) {
			// collect removed domains for post-processing, e.g. certificate removal
			removedDomains.add(new DomainDto(model));
		}
	}
}
