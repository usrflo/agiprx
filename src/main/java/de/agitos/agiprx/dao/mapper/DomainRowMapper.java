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
import de.agitos.agiprx.model.Domain;

public class DomainRowMapper extends AbstractRowMapper implements RowMapper<Domain> {

	public Domain mapRow(ResultSet rs, int line) throws SQLException {

		Domain model = new Domain();
		model.setId(getLong(rs, "id"));
		model.setVersion(getInteger(rs, "version"));

		model.setDomain(rs.getString("domain"));
		model.setBackendId(rs.getLong("backend_id"));
		model.setCertProvided(rs.getBoolean("certprovided"));
		model.setLetsEncrypt(rs.getBoolean("letsencrypt"));
		model.setRedirectToUrl(rs.getString("redirect_to_url"));

		return model;
	}
}
