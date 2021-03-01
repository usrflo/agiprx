/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.agitos.agiprx.db;

import javax.sql.DataSource;

import de.agitos.agiprx.util.Assert;

/**
 * Base class for {@link org.springframework.jdbc.core.JdbcTemplate} and
 * other JDBC-accessing DAO helpers, defining common properties such as
 * DataSource and exception translator.
 *
 * <p>Not intended to be used directly.
 * See {@link org.springframework.jdbc.core.JdbcTemplate}.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 28.11.2003
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public abstract class JdbcAccessor {

	private DataSource dataSource;

	private volatile SQLExceptionTranslator exceptionTranslator;

	/**
	 * Set the JDBC DataSource to obtain connections from.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Return the DataSource used by this template.
	 */
	
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * Obtain the DataSource for actual use.
	 * @return the DataSource (never {@code null})
	 * @throws IllegalStateException in case of no DataSource set
	 * @since 5.0
	 */
	protected DataSource obtainDataSource() {
		DataSource dataSource = getDataSource();
		Assert.isTrue(dataSource != null, "No DataSource set");
		return dataSource;
	}

	/**
	 * Return the exception translator for this instance.
	 * <p>Creates a default {@link SQLErrorCodeSQLExceptionTranslator}
	 * for the specified DataSource if none set, or a
	 * {@link SQLStateSQLExceptionTranslator} in case of no DataSource.
	 * @see #getDataSource()
	 */
	public SQLExceptionTranslator getExceptionTranslator() {
		
		if (exceptionTranslator==null) {
			if (dataSource != null) {
				exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
			} else {
				exceptionTranslator = new SQLStateSQLExceptionTranslator();
			}
		}
		
		return exceptionTranslator;
	}

	/**
	 * Eagerly initialize the exception translator, if demanded,
	 * creating a default one for the specified DataSource if none set.
	 */
	public void afterPropertiesSet() {
		if (getDataSource() == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
		getExceptionTranslator();
	}

}
