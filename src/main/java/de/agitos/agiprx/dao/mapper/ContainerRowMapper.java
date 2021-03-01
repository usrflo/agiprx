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
import de.agitos.agiprx.model.Container;

public class ContainerRowMapper extends AbstractRowMapper implements RowMapper<Container> {

	public Container mapRow(ResultSet rs, int line) throws SQLException {

		Container model = new Container();
		model.setId(getLong(rs, "id"));
		model.setVersion(getInteger(rs, "version"));

		model.setLabel(rs.getString("label"));
		model.setFullname(rs.getString("fullname"));
		model.setHostId(rs.getLong("host_id"));
		model.setProjectId(rs.getLong("project_id"));
		model.setIpv6(rs.getString("ipv6"));

		return model;
	}
}
