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

import org.jasypt.encryption.pbe.PBEStringEncryptor;

import de.agitos.agiprx.db.RowMapper;
import de.agitos.agiprx.model.ContainerPermission;

public class ContainerPermissionRowMapper extends AbstractRowMapper implements RowMapper<ContainerPermission> {

	private PBEStringEncryptor stringEncryptor;

	public ContainerPermissionRowMapper(PBEStringEncryptor stringEncryptor) {
		this.stringEncryptor = stringEncryptor;
	}

	public ContainerPermission mapRow(ResultSet rs, int line) throws SQLException {

		ContainerPermission model = new ContainerPermission();
		model.setId(getLong(rs, "id"));
		model.setVersion(getInteger(rs, "version"));

		model.setContainerId(rs.getLong("container_id"));
		model.setUserId(rs.getLong("user_id"));
		model.setPermission(rs.getString("permission"));

		String encryptedPassword = rs.getString("password");
		if (encryptedPassword == null) {
			model.setPassword(null);
		} else {
			model.setPassword(this.stringEncryptor.decrypt(encryptedPassword));
		}

		return model;
	}
}
