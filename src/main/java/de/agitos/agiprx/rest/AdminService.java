/*******************************************************************************
 * Copyright (C) 2023 Florian Sager, www.agitos.de
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
package de.agitos.agiprx.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.bean.processor.ProxySyncProcessor;
import de.agitos.agiprx.bean.processor.SshProxyProcessor;
import de.agitos.agiprx.util.UserContext;
import io.helidon.common.http.Http;
import io.helidon.common.http.Http.ResponseStatus;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.HttpException;
import io.helidon.webserver.Routing.Rules;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class AdminService extends AbstractService {

	private static final Logger LOG = Logger.getLogger(BackendService.class.getName());

	ProxySyncProcessor proxySyncProcessor;

	HAProxyProcessor haProxyProcessor;

	SshProxyProcessor sshProxyProcessor;

	UserContext userContext;

	private static final String ADMIN_THREAD_NAME = "MainAdminJob";

	private static boolean adminJobRunning = false;

	public AdminService(boolean isMaster) {
		super(isMaster);
		userContext = UserContext.getBean();
		proxySyncProcessor = ProxySyncProcessor.getBean();
		haProxyProcessor = HAProxyProcessor.getBean();
		sshProxyProcessor = SshProxyProcessor.getBean();
	}

	@Override
	public void update(Rules rules) {
		rules.get("/gensynchaprx", WebSecurity.authenticate(), this::generateConfigAndSyncHAProxy);
		rules.get("/writesshprx", WebSecurity.authenticate(), this::writeSshProxyConfiguration);
	}

	// GET: /gensynchaprx/
	private void generateConfigAndSyncHAProxy(ServerRequest serverRequest, ServerResponse serverResponse) {

		if (!validateMasterInstance(serverResponse)) {
			return;
		}

		if (adminJobRunning) {
			serverResponse.status(ResponseStatus.create(423 /* Locked */, "Job is already running")).send();
			return;
		}

		try {
			adminJobRunning = true;

			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));

			LOG.log(Level.INFO, "Started reload and sync of HAProxy");

			List<String> warningMessages = new ArrayList<>();

			// roll out changed configuration
			haProxyProcessor.manageConfiguration(false, true);

			if (proxySyncProcessor.isSyncRequired()) {
				proxySyncProcessor.syncToSlaveInstances(false, warningMessages);
			}

			LOG.log(Level.INFO, "Finished reload and sync of HAProxy");

			serverResponse.send(warningMessages);

		} catch (Exception e) {

			LOG.log(Level.SEVERE, "Unable to reload and sync of HAProxy", e);

			throw new HttpException("Unable to reload and sync of HAProxy: " + e.getMessage(),
					Http.Status.INTERNAL_SERVER_ERROR_500, e);

		} finally {
			userContext.unregister();

			adminJobRunning = false;
		}
	}

	// GET: /writesshprx/
	private void writeSshProxyConfiguration(ServerRequest serverRequest, ServerResponse serverResponse) {

		if (!validateMasterInstance(serverResponse)) {
			return;
		}

		if (adminJobRunning) {
			serverResponse.status(ResponseStatus.create(423 /* Locked */, "Job is already running")).send();
			return;
		}

		Runnable maintenanceJob = () -> {

			try {
				adminJobRunning = true;

				userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));

				LOG.log(Level.INFO, "Started writing SSH proxy configuration");

				List<String> warningMessages = new ArrayList<>();

				sshProxyProcessor.manageConfiguration(false);

				LOG.log(Level.INFO, "Finished writing SSH proxy configuration");

				serverResponse.send(warningMessages);

			} catch (Exception e) {

				LOG.log(Level.SEVERE, "Unable to write SSH proxy configuration", e);

			} finally {
				userContext.unregister();

				adminJobRunning = false;
			}

		};
		new Thread(maintenanceJob, ADMIN_THREAD_NAME).start();

		serverResponse.status(ResponseStatus.create(200, "Job started")).send();
	}
}
