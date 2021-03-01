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
package de.agitos.agiprx.bean.processor;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.util.Assert;

public class DatabaseBackupProcessor extends AbstractProcessor implements DependencyInjector {

	private static DatabaseBackupProcessor BEAN;

	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

	private final static String ARCHIVE_PATH = "/etc/haproxy/archive/";

	protected ConsoleWrapper console;

	// @Value("${db.database}")
	private String dbName;

	public DatabaseBackupProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		dbName = Config.getBean().getString("db.database");
	}

	@Override
	public void postConstruct() {
		console = ConsoleWrapper.getBean();
	}

	public static DatabaseBackupProcessor getBean() {
		return BEAN;
	}

	public void createBackup(String suffix) throws IOException, InterruptedException, AbortionException {

		// save previous cfg/maps to YYYYMMDD-HHMMSS-<suffix>-<filename>
		String backupPrefix = DATE_FORMAT.format(new Date()) + "-" + suffix + "-";

		exec(0, new String[] {}, "/usr/bin/mysqldump", "--defaults-file=/etc/mysql/debian.cnf", "--opt",
				"--single-transaction", "--order-by-primary", "--flush-logs", "--events", "--routines", dbName,
				"--result-file=" + ARCHIVE_PATH + backupPrefix + dbName + ".sql");
	}

}
