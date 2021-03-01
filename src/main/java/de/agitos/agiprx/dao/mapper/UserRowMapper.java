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
package de.agitos.agiprx.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.agitos.agiprx.db.RowMapper;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.model.UserRoleType;

public class UserRowMapper extends AbstractRowMapper implements RowMapper<User> {

	public User mapRow(ResultSet rs, int line) throws SQLException {

		User model = new User();
		model.setId(getLong(rs, "id"));
		model.setVersion(getInteger(rs, "version"));

		model.setFullname(rs.getString("fullname"));
		model.setEmail(rs.getString("email"));
		model.setSshPublicKey(rs.getString("ssh_public_key"));
		model.setRole(nullSafeEnumValue(UserRoleType.class, rs.getString("role")));
		model.setDefaultPermission(getStringArray(rs, "default_permission", ","));
		model.setAgiPrxPermission(getStringArray(rs, "agiprx_permission", ","));

		return model;
	}
}
