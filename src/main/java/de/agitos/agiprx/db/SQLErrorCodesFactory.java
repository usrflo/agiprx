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

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import de.agitos.agiprx.db.exception.MetaDataAccessException;
import de.agitos.agiprx.util.Assert;

/**
 * Factory for creating {@link SQLErrorCodes} based on the
 * "databaseProductName" taken from the {@link java.sql.DatabaseMetaData}.
 *
 * <p>Returns {@code SQLErrorCodes} populated with vendor codes
 * defined in a configuration file named "sql-error-codes.xml".
 * Reads the default file in this package if not overridden by a file in
 * the root of the class path (for example in the "/WEB-INF/classes" directory).
 *
 * @author Thomas Risberg
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
 */
public class SQLErrorCodesFactory {

	/**
	 * The name of custom SQL error codes file, loading from the root
	 * of the class path (e.g. from the "/WEB-INF/classes" directory).
	 */
	public static final String SQL_ERROR_CODE_OVERRIDE_PATH = "sql-error-codes.xml";

	/**
	 * The name of default SQL error code files, loading from the class path.
	 */
	public static final String SQL_ERROR_CODE_DEFAULT_PATH = "de/agitos/agiprx/db/sql-error-codes.xml";

	
	private static final Logger LOG = Logger.getLogger(SQLErrorCodesFactory.class.getName());


	/**
	 * Keep track of a single instance so we can return it to classes that request it.
	 */
	private static final SQLErrorCodesFactory instance = new SQLErrorCodesFactory();


	/**
	 * Return the singleton instance.
	 */
	public static SQLErrorCodesFactory getInstance() {
		return instance;
	}


	/**
	 * Map to hold error codes for all databases defined in the config file.
	 * Key is the database product name, value is the SQLErrorCodes instance.
	 */
	private final Map<String, SQLErrorCodes> errorCodesMap = new HashMap<>(6);

	/**
	 * Map to cache the SQLErrorCodes instance per DataSource.
	 */
	private final Map<DataSource, SQLErrorCodes> dataSourceCache = new HashMap<>(2);


	/**
	 * Create a new instance of the {@link SQLErrorCodesFactory} class.
	 * <p>Not public to enforce Singleton design pattern. Would be private
	 * except to allow testing via overriding the
	 * {@link #loadResource(String)} method.
	 * <p><b>Do not subclass in application code.</b>
	 * @see #loadResource(String)
	 */
	protected SQLErrorCodesFactory() {

		/*
		<bean id="MySQL" class="org.springframework.jdbc.support.SQLErrorCodes">
			<property name="databaseProductNames">
				<list>
					<value>MySQL</value>
					<value>MariaDB</value>
				</list>
			</property>
			<property name="badSqlGrammarCodes">
				<value>1054,1064,1146</value>
			</property>
			<property name="duplicateKeyCodes">
				<value>1062</value>
			</property>
			<property name="dataIntegrityViolationCodes">
				<value>630,839,840,893,1169,1215,1216,1217,1364,1451,1452,1557</value>
			</property>
			<property name="dataAccessResourceFailureCodes">
				<value>1</value>
			</property>
			<property name="cannotAcquireLockCodes">
				<value>1205,3572</value>
			</property>
			<property name="deadlockLoserCodes">
				<value>1213</value>
			</property>
		</bean>
		*/

		SQLErrorCodes mySQLErrorCodes = new SQLErrorCodes();
		mySQLErrorCodes.setDatabaseProductNames("MySQL", "MariaDB");
		mySQLErrorCodes.setBadSqlGrammarCodes("1054","1064","1146");
		mySQLErrorCodes.setDuplicateKeyCodes("1062");
		mySQLErrorCodes.setDataIntegrityViolationCodes("630","839","840","893","1169","1215","1216","1217","1364","1451","1452","1557");
		mySQLErrorCodes.setDataAccessResourceFailureCodes("1");
		mySQLErrorCodes.setCannotAcquireLockCodes("1205", "3572");
		mySQLErrorCodes.setDeadlockLoserCodes("1213");

		this.errorCodesMap.put("MySQL", mySQLErrorCodes);
		this.errorCodesMap.put("MariaDB", mySQLErrorCodes);
	}

	/**
	 * Return the {@link SQLErrorCodes} instance for the given database.
	 * <p>No need for a database meta-data lookup.
	 * @param databaseName the database name (must not be {@code null})
	 * @return the {@code SQLErrorCodes} instance for the given database
	 * (never {@code null}; potentially empty)
	 * @throws IllegalArgumentException if the supplied database name is {@code null}
	 */
	public SQLErrorCodes getErrorCodes(String databaseName) {
		Assert.notNull(databaseName, "Database product name must not be null");

		SQLErrorCodes sec = this.errorCodesMap.get(databaseName);
		if (sec == null) {
			for (SQLErrorCodes candidate : this.errorCodesMap.values()) {
				for (String candidateName : candidate.getDatabaseProductNames()) {
					if (databaseName.equals(candidateName)) {
						sec = candidate;
						break;
					}
				}
				
				if (sec != null) {
					break;
				}
			}
		}
		if (sec != null) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.log(Level.FINE, "SQL error codes for '" + databaseName + "' found");
			}
			return sec;
		}

		// Could not find the database among the defined ones.
		if (LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "SQL error codes for '" + databaseName + "' not found");
		}
		return new SQLErrorCodes();
	}

	/**
	 * Return {@link SQLErrorCodes} for the given {@link DataSource},
	 * evaluating "databaseProductName" from the
	 * {@link java.sql.DatabaseMetaData}, or an empty error codes
	 * instance if no {@code SQLErrorCodes} were found.
	 * @param dataSource the {@code DataSource} identifying the database
	 * @return the corresponding {@code SQLErrorCodes} object
	 * (never {@code null}; potentially empty)
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public SQLErrorCodes getErrorCodes(DataSource dataSource) {
		SQLErrorCodes sec = resolveErrorCodes(dataSource);
		return (sec != null ? sec : new SQLErrorCodes());
	}

	/**
	 * Return {@link SQLErrorCodes} for the given {@link DataSource},
	 * evaluating "databaseProductName" from the
	 * {@link java.sql.DatabaseMetaData}, or {@code null} if case
	 * of a JDBC meta-data access problem.
	 * @param dataSource the {@code DataSource} identifying the database
	 * @return the corresponding {@code SQLErrorCodes} object,
	 * or {@code null} in case of a JDBC meta-data access problem
	 * @since 5.2.9
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	
	public SQLErrorCodes resolveErrorCodes(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		if (LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "Looking up default SQLErrorCodes for DataSource [" + identify(dataSource) + "]");
		}

		// Try efficient lock-free access for existing cache entry
		SQLErrorCodes sec = this.dataSourceCache.get(dataSource);
		if (sec == null) {
			synchronized (this.dataSourceCache) {
				// Double-check within full dataSourceCache lock
				sec = this.dataSourceCache.get(dataSource);
				if (sec == null) {
					// We could not find it - got to look it up.
					try {
						String name = JdbcUtils.extractDatabaseMetaData(dataSource,
								DatabaseMetaData::getDatabaseProductName);
						if (name!=null && name.length()>0) {
							return registerDatabase(dataSource, name);
						}
					}
					catch (MetaDataAccessException ex) {
						LOG.log(Level.WARNING, "Error while extracting database name", ex);
					}
					return null;
				}
			}
		}

		if (LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "SQLErrorCodes found in cache for DataSource [" + identify(dataSource) + "]");
		}

		return sec;
	}

	/**
	 * Associate the specified database name with the given {@link DataSource}.
	 * @param dataSource the {@code DataSource} identifying the database
	 * @param databaseName the corresponding database name as stated in the error codes
	 * definition file (must not be {@code null})
	 * @return the corresponding {@code SQLErrorCodes} object (never {@code null})
	 * @see #unregisterDatabase(DataSource)
	 */
	public SQLErrorCodes registerDatabase(DataSource dataSource, String databaseName) {
		SQLErrorCodes sec = getErrorCodes(databaseName);
		if (LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "Caching SQL error codes for DataSource [" + identify(dataSource) +
					"]: database product name is '" + databaseName + "'");
		}
		this.dataSourceCache.put(dataSource, sec);
		return sec;
	}

	/**
	 * Clear the cache for the specified {@link DataSource}, if registered.
	 * @param dataSource the {@code DataSource} identifying the database
	 * @return the corresponding {@code SQLErrorCodes} object that got removed,
	 * or {@code null} if not registered
	 * @since 4.3.5
	 * @see #registerDatabase(DataSource, String)
	 */
	
	public SQLErrorCodes unregisterDatabase(DataSource dataSource) {
		return this.dataSourceCache.remove(dataSource);
	}

	/**
	 * Build an identification String for the given {@link DataSource},
	 * primarily for logging purposes.
	 * @param dataSource the {@code DataSource} to introspect
	 * @return the identification String
	 */
	private String identify(DataSource dataSource) {
		return dataSource.getClass().getName() + '@' + Integer.toHexString(dataSource.hashCode());
	}

}
