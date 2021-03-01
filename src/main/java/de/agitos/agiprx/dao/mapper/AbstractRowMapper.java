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
import java.sql.Timestamp;
import java.util.Date;

public class AbstractRowMapper {

	protected <E extends Enum<E>> E nullSafeEnumValue(Class<E> c, String s) {
		if (s == null) {
			return null;
		}
		return (E) Enum.valueOf(c, s);
	}

	protected Integer getInteger(ResultSet rs, String columnName) throws SQLException {
		int value = rs.getInt(columnName);
		return rs.wasNull() ? null : value;
	}

	protected Integer getInteger(ResultSet rs, int columnPos) throws SQLException {
		int value = rs.getInt(columnPos);
		return rs.wasNull() ? null : value;
	}

	protected Long getLong(ResultSet rs, String columnName) throws SQLException {
		long value = rs.getLong(columnName);
		return rs.wasNull() ? null : value;
	}

	protected Long getLong(ResultSet rs, int columnPos) throws SQLException {
		long value = rs.getLong(columnPos);
		return rs.wasNull() ? null : value;
	}

	protected Date getDate(ResultSet rs, String columnName) throws SQLException {

		Timestamp timestamp = rs.getTimestamp(columnName);
		if (timestamp == null) {
			return null;
		}
		return new java.util.Date(timestamp.getTime());
	}

	protected String[] getStringArray(ResultSet rs, String columnName, String splitPattern) throws SQLException {
		String value = rs.getString(columnName);
		if (value == null) {
			return new String[] {};
		}

		return value.split(splitPattern);
	}
}
