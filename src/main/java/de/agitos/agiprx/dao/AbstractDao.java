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

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.db.DataSourceUtils;
import de.agitos.agiprx.db.JdbcTemplate;
import de.agitos.agiprx.db.NamedParameterJdbcTemplate;
import de.agitos.agiprx.db.exception.OptimisticLockingFailureException;

public class AbstractDao implements DependencyInjector {

	protected DataSourceUtils dataSourceUtils;
	protected JdbcTemplate jdbcTemplate;
	protected NamedParameterJdbcTemplate namedParamsJdbcTemplate;

	@Override
	public void postConstruct() {
		dataSourceUtils = DataSourceUtils.getBean();
		jdbcTemplate = new JdbcTemplate(dataSourceUtils.getDataSource());
		namedParamsJdbcTemplate = new NamedParameterJdbcTemplate(dataSourceUtils.getDataSource());
	}

	protected void handleInsertionError(Object data, Exception e) {
		if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		}
		throw new RuntimeException("Error on insertion of " + data, e);
	}

	protected void handleUpdateError(Object data, Exception e) {
		if (e.getCause() != null && e.getCause() instanceof java.sql.SQLException
				&& "Optimistic Lock".equals(e.getCause().getMessage())) {
			throw new OptimisticLockingFailureException("Version conflict, canceled change on " + data, e);
		}
		if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		}
		throw new RuntimeException(e);
	}

	protected void checkDeletionError(Object data, int rowsAffected) {
		if (rowsAffected == 0) {
			throw new OptimisticLockingFailureException("Version conflict, canceled deletion of " + data);
		}
	}

	protected void handleDeletionError(Object data, Exception e) {
		if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		}
		throw new RuntimeException("Error on deletion of " + data);
	}

}
