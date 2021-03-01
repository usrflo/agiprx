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
import java.util.List;

import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.dao.mapper.ApiUserRowMapper;
import de.agitos.agiprx.db.GeneratedKeyHolder;
import de.agitos.agiprx.db.KeyHolder;
import de.agitos.agiprx.db.MapSqlParameterSource;
import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.model.ApiUser;
import de.agitos.agiprx.util.Assert;

public class ApiUserDao extends AbstractDao implements DependencyInjector /* implements UserDetailsService */ {

	private static ApiUserDao BEAN;

	// @formatter:off
	private static final String SELECT_ALL_STMT =
			"SELECT "
			+ "`id`,"
			+ "`version`,"
			+ "`username`,"
			+ "`password`,"
			+ "`email`,"
			+ "`agiprx_permission`"
			+ " FROM `api_user`";

	private static final String INSERT_STMT =
			"INSERT INTO `api_user` ("
			+ "`version`,"
			+ "`username`,"
			+ "`password`,"
			+ "`email`,"
			+ "`agiprx_permission`"
			+ ") VALUES (0, :username, :password, :email, :agiprx_permission)";
	
	private static final String UPDATE_STMT =
			"UPDATE `api_user` SET "
			+ "`version` = :version,"
			+ "`username` = :username,"
			+ "`password` = :password,"
			+ "`email` = :email,"
			+ "`agiprx_permission` = :agiprx_permission"
			+ " WHERE `id` = :id";
	
	private static final String DELETE_STMT = "DELETE FROM `api_user` WHERE `id` = ? AND `version` = ?";
	// @formatter:on

	private static final String fixedSecret = "2DfdEUPsDN";

	private PBEStringEncryptor stringEncryptor;

	public ApiUserDao() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		stringEncryptor = new StandardPBEStringEncryptor();
		stringEncryptor.setPassword(Config.getBean().getString("application.secret") + fixedSecret);
	}

	public static ApiUserDao getBean() {
		return BEAN;
	}

	// @Transactional
	public void create(ApiUser model) {

		try {
			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("username", model.getUsername());
			parameters.addValue("password",
					model.getPassword() == null ? null : stringEncryptor.encrypt(model.getPassword()));
			parameters.addValue("email", model.getEmail());
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

	public ApiUser find(Long id) {
		try {
			ApiUser cp = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE id = ?", new Object[] { id },
					new int[] { Types.NUMERIC }, new ApiUserRowMapper(stringEncryptor));
			initRelations(cp);
			return cp;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public ApiUser findByUsername(String username) {
		try {
			ApiUser cp = jdbcTemplate.queryForObject(SELECT_ALL_STMT + " WHERE username = ?", new Object[] { username },
					new int[] { Types.VARCHAR }, new ApiUserRowMapper(stringEncryptor));
			initRelations(cp);
			return cp;
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public List<ApiUser> findAll() {
		List<ApiUser> result = jdbcTemplate.query(SELECT_ALL_STMT, new ApiUserRowMapper(stringEncryptor));
		for (ApiUser cp : result) {
			initRelations(cp);
		}
		return result;
	}

	private void initRelations(ApiUser containerPermission) {
	}

	// @Transactional
	public void update(ApiUser model) {

		try {

			dataSourceUtils.startTransaction();

			MapSqlParameterSource parameters = new MapSqlParameterSource();
			parameters.addValue("id", model.getId());
			parameters.addValue("version", model.getVersion());
			parameters.addValue("username", model.getUsername());
			parameters.addValue("password",
					model.getPassword() == null ? null : stringEncryptor.encrypt(model.getPassword()));
			parameters.addValue("email", model.getEmail());
			parameters.addValue("agiprx_permission",
					model.getAgiPrxPermission() == null ? null : String.join(",", model.getAgiPrxPermission()));

			namedParamsJdbcTemplate.update(UPDATE_STMT, parameters);

			model.incrementVersion();

		} catch (Exception e) {
			handleUpdateError(model, e);
		}
	}

	// @Transactional
	public void delete(ApiUser model) {
		try {

			dataSourceUtils.startTransaction();

			int rowsAffected = jdbcTemplate.update(DELETE_STMT, new Object[] { model.getId(), model.getVersion() });
			checkDeletionError(model, rowsAffected);

		} catch (Exception e) {
			handleDeletionError(model, e);
		}
	}

	/*
	 * @Override public UserDetails loadUserByUsername(String username) throws
	 * UsernameNotFoundException {
	 * 
	 * ApiUser apiUser = findByUsername(username); if (apiUser == null) { throw new
	 * UsernameNotFoundException("API user '" + username + "' does not exist."); }
	 * 
	 * return new ApiUserDetails(apiUser); }
	 * 
	 * private class ApiUserDetails implements UserDetails {
	 * 
	 * private static final long serialVersionUID = 1L;
	 * 
	 * private final ApiUser apiUser;
	 * 
	 * public ApiUserDetails(ApiUser apiUser) { this.apiUser = apiUser; }
	 * 
	 * @Override public Collection<? extends GrantedAuthority> getAuthorities() {
	 * List<GrantedAuthority> authorities = new ArrayList<>(); authorities.add(new
	 * SimpleGrantedAuthority(UserRoleType.USER.name())); return authorities; }
	 * 
	 * @Override public String getPassword() { return this.apiUser.getPassword(); }
	 * 
	 * @Override public String getUsername() { return this.apiUser.getUsername(); }
	 * 
	 * @Override public boolean isAccountNonExpired() { return true; }
	 * 
	 * @Override public boolean isAccountNonLocked() { return true; }
	 * 
	 * @Override public boolean isCredentialsNonExpired() { return true; }
	 * 
	 * @Override public boolean isEnabled() { return true; }
	 * 
	 * }
	 */
}
