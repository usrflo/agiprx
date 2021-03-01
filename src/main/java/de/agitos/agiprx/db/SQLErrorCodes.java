/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Arrays;

/**
 * JavaBean for holding JDBC error codes for a particular database.
 * Instances of this class are normally loaded through a bean factory.
 *
 * <p>Used by Spring's {@link SQLErrorCodeSQLExceptionTranslator}.
 * The file "sql-error-codes.xml" in this package contains default
 * {@code SQLErrorCodes} instances for various databases.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SQLErrorCodesFactory
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class SQLErrorCodes {

	
	private String[] databaseProductNames;

	private boolean useSqlStateForTranslation = false;

	private String[] badSqlGrammarCodes = new String[0];

	private String[] invalidResultSetAccessCodes = new String[0];

	private String[] duplicateKeyCodes = new String[0];

	private String[] dataIntegrityViolationCodes = new String[0];

	private String[] permissionDeniedCodes = new String[0];

	private String[] dataAccessResourceFailureCodes = new String[0];

	private String[] transientDataAccessResourceCodes = new String[0];

	private String[] cannotAcquireLockCodes = new String[0];

	private String[] deadlockLoserCodes = new String[0];

	private String[] cannotSerializeTransactionCodes = new String[0];

	/**
	 * Set this property if the database name contains spaces,
	 * in which case we can not use the bean name for lookup.
	 */
	public void setDatabaseProductName( String databaseProductName) {
		this.databaseProductNames = new String[] {databaseProductName};
	}

	
	public String getDatabaseProductName() {
		return (this.databaseProductNames != null && this.databaseProductNames.length > 0 ?
				this.databaseProductNames[0] : null);
	}

	/**
	 * Set this property to specify multiple database names that contains spaces,
	 * in which case we can not use bean names for lookup.
	 */
	public void setDatabaseProductNames( String... databaseProductNames) {
		this.databaseProductNames = databaseProductNames;
	}

	
	public String[] getDatabaseProductNames() {
		return this.databaseProductNames;
	}

	/**
	 * Set this property to true for databases that do not provide an error code
	 * but that do provide SQL State (this includes PostgreSQL).
	 */
	public void setUseSqlStateForTranslation(boolean useStateCodeForTranslation) {
		this.useSqlStateForTranslation = useStateCodeForTranslation;
	}

	public boolean isUseSqlStateForTranslation() {
		return this.useSqlStateForTranslation;
	}

	public void setBadSqlGrammarCodes(String... badSqlGrammarCodes) {
		this.badSqlGrammarCodes = sortStringArray(badSqlGrammarCodes);
	}

	public String[] getBadSqlGrammarCodes() {
		return this.badSqlGrammarCodes;
	}

	public void setInvalidResultSetAccessCodes(String... invalidResultSetAccessCodes) {
		this.invalidResultSetAccessCodes = sortStringArray(invalidResultSetAccessCodes);
	}

	public String[] getInvalidResultSetAccessCodes() {
		return this.invalidResultSetAccessCodes;
	}

	public String[] getDuplicateKeyCodes() {
		return this.duplicateKeyCodes;
	}

	public void setDuplicateKeyCodes(String... duplicateKeyCodes) {
		this.duplicateKeyCodes = duplicateKeyCodes;
	}

	public void setDataIntegrityViolationCodes(String... dataIntegrityViolationCodes) {
		this.dataIntegrityViolationCodes = sortStringArray(dataIntegrityViolationCodes);
	}

	public String[] getDataIntegrityViolationCodes() {
		return this.dataIntegrityViolationCodes;
	}

	public void setPermissionDeniedCodes(String... permissionDeniedCodes) {
		this.permissionDeniedCodes = sortStringArray(permissionDeniedCodes);
	}

	public String[] getPermissionDeniedCodes() {
		return this.permissionDeniedCodes;
	}

	public void setDataAccessResourceFailureCodes(String... dataAccessResourceFailureCodes) {
		this.dataAccessResourceFailureCodes = sortStringArray(dataAccessResourceFailureCodes);
	}

	public String[] getDataAccessResourceFailureCodes() {
		return this.dataAccessResourceFailureCodes;
	}

	public void setTransientDataAccessResourceCodes(String... transientDataAccessResourceCodes) {
		this.transientDataAccessResourceCodes = sortStringArray(transientDataAccessResourceCodes);
	}

	public String[] getTransientDataAccessResourceCodes() {
		return this.transientDataAccessResourceCodes;
	}

	public void setCannotAcquireLockCodes(String... cannotAcquireLockCodes) {
		this.cannotAcquireLockCodes = sortStringArray(cannotAcquireLockCodes);
	}

	public String[] getCannotAcquireLockCodes() {
		return this.cannotAcquireLockCodes;
	}

	public void setDeadlockLoserCodes(String... deadlockLoserCodes) {
		this.deadlockLoserCodes = sortStringArray(deadlockLoserCodes);
	}

	public String[] getDeadlockLoserCodes() {
		return this.deadlockLoserCodes;
	}

	public void setCannotSerializeTransactionCodes(String... cannotSerializeTransactionCodes) {
		this.cannotSerializeTransactionCodes = sortStringArray(cannotSerializeTransactionCodes);
	}

	public String[] getCannotSerializeTransactionCodes() {
		return this.cannotSerializeTransactionCodes;
	}

	private String[] sortStringArray(String[] array) {
		if (array!=null && array.length!=0) {
			Arrays.sort(array);
		}
		return array;
	}
}
