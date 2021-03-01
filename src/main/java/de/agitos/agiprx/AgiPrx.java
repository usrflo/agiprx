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
package de.agitos.agiprx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.bean.SearchBean;
import de.agitos.agiprx.bean.maintenance.CertChecker;
import de.agitos.agiprx.bean.maintenance.MainMaintenanceBean;
import de.agitos.agiprx.bean.processor.AgiPrxSshAuthProcessor;
import de.agitos.agiprx.bean.processor.DatabaseBackupProcessor;
import de.agitos.agiprx.bean.processor.HAProxyLesslBackendProcessor;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.bean.processor.LesslCertProcessor;
import de.agitos.agiprx.bean.processor.LxdProcessor;
import de.agitos.agiprx.bean.processor.ProxySyncProcessor;
import de.agitos.agiprx.bean.processor.SshProxyProcessor;
import de.agitos.agiprx.bean.processor.SslCertProcessor;
import de.agitos.agiprx.dao.ApiUserDao;
import de.agitos.agiprx.dao.BackendContainerDao;
import de.agitos.agiprx.dao.BackendDao;
import de.agitos.agiprx.dao.ContainerDao;
import de.agitos.agiprx.dao.ContainerPermissionDao;
import de.agitos.agiprx.dao.DomainDao;
import de.agitos.agiprx.dao.HostDao;
import de.agitos.agiprx.dao.ProjectDao;
import de.agitos.agiprx.dao.UserDao;
import de.agitos.agiprx.db.DataSourceUtils;
import de.agitos.agiprx.dns.DomainIpChecker;
import de.agitos.agiprx.executor.ApiUserExecutor;
import de.agitos.agiprx.executor.BackendContainerExecutor;
import de.agitos.agiprx.executor.BackendExecutor;
import de.agitos.agiprx.executor.ContainerExecutor;
import de.agitos.agiprx.executor.ContainerPermissionExecutor;
import de.agitos.agiprx.executor.DomainExecutor;
import de.agitos.agiprx.executor.HostExecutor;
import de.agitos.agiprx.executor.MainExecutor;
import de.agitos.agiprx.executor.NonInteractiveDomainExecutor;
import de.agitos.agiprx.executor.NonInteractiveProjectExecutor;
import de.agitos.agiprx.executor.ProjectExecutor;
import de.agitos.agiprx.executor.UserExecutor;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.EmailSender;
import de.agitos.agiprx.util.UserContext;
import de.agitos.agiprx.util.Validator;

public class AgiPrx {

	public static boolean PRODUCTION = true;

	public static String agiPrxRootDirectory;

	public static String version;

	public static void main(String[] args) throws IOException {

		/*
		 * String helidonStartupTest = System.getProperty("exit.on.started"); if
		 * (helidonStartupTest != null) { System.exit(0); }
		 */
		agiPrxRootDirectory = System.getProperty("agiprx.root");
		Assert.notNull(agiPrxRootDirectory,
				"The environment variable 'agiprx.root' needs to be set to the base directory");

		if (args.length > 0 && "test".equals(args[0])) {
			AgiPrx.PRODUCTION = false;
		}

		// force execution as root in production
		if (AgiPrx.PRODUCTION) {
			String execUser = System.getProperty("user.name");
			if (!"root".equals(execUser)) {
				throw new RuntimeException("execution with user " + execUser + " instead of user root, terminated");
			}
		}

		// set logging properties
		// String path =
		// AgiPrx.class.getClassLoader().getResource("jul-log.properties").getFile();
		Path logPropertiesFile = Paths.get(AgiPrx.agiPrxRootDirectory, "etc", "jul-log.properties");
		Assert.isTrue(Files.exists(logPropertiesFile),
				"Logging properties file '" + logPropertiesFile + "' needs to exist");
		System.setProperty("java.util.logging.config.file", logPropertiesFile.toString());

		// set version from manifest
		Package mainPackage = AgiPrx.class.getPackage();
		version = mainPackage.getImplementationVersion();

		new AgiPrx();
	}

	private AgiPrx() throws IOException {

		// bean registration
		List<DependencyInjector> diList = new ArrayList<DependencyInjector>();
		diList.add(new Config());
		diList.add(new ConsoleWrapper());
		diList.add(new Validator());
		diList.add(new UserContext());
		diList.add(new DataSourceUtils());

		diList.add(new ProxySyncProcessor());
		diList.add(new AgiPrxSshAuthProcessor());
		diList.add(new ApiUserDao());
		diList.add(new BackendContainerDao());
		diList.add(new BackendDao());
		diList.add(new CertChecker());
		diList.add(new ContainerDao());
		diList.add(new ContainerPermissionDao());
		diList.add(new DatabaseBackupProcessor());
		diList.add(new DomainDao());
		diList.add(new DomainIpChecker());
		diList.add(new EmailSender());
		diList.add(new HAProxyLesslBackendProcessor());
		diList.add(new HAProxyProcessor());
		diList.add(new HostDao());
		diList.add(new LesslCertProcessor());
		diList.add(new LxdProcessor());
		diList.add(new MainMaintenanceBean());
		diList.add(new ProjectDao());
		diList.add(new RestAPI());
		diList.add(new SshProxyProcessor());
		diList.add(new SslCertProcessor());
		diList.add(new UserDao());

		diList.add(new ApiUserExecutor());
		diList.add(new BackendContainerExecutor());
		diList.add(new BackendExecutor());
		diList.add(new ContainerExecutor());
		diList.add(new ContainerPermissionExecutor());
		diList.add(new DomainExecutor());
		diList.add(new HostExecutor());
		diList.add(new MainExecutor());
		diList.add(new NonInteractiveProjectExecutor());
		diList.add(new NonInteractiveDomainExecutor());
		diList.add(new ProjectExecutor());
		diList.add(new SearchBean());
		diList.add(new UserExecutor());

		// dependency injection
		for (DependencyInjector di : diList) {
			di.postConstruct();
		}

		if (ProxySyncProcessor.getBean().isMasterInstance()) {

			// start to read the domain to cert map
			HAProxyProcessor.getBean().generateDomainToCertFilesDetached();

		} else {

			// if slave instance, reload HAProxy and write ssh proxy configuration
			try {
				// re-generate slave haproxy configuration and reload HAProxy
				HAProxyProcessor.getBean().manageConfiguration(false, false);
				// re-generate all SSH proxy accounts by database configuration
				SshProxyProcessor.getBean().manageConfiguration(false);
			} catch (Exception e) {
				throw new RuntimeException("Slave instance startup failed", e);
			}
		}

		// start ssh daemon
		SshConsoleServer.getBean().runServer();

		// start webserver for REST API
		RestAPI.getBean().runServer();

		// force jlink to add according module
		new java.awt.datatransfer.UnsupportedFlavorException(null);
	}
}
