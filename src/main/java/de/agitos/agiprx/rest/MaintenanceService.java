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
package de.agitos.agiprx.rest;

import de.agitos.agiprx.bean.maintenance.MainMaintenanceBean;
import io.helidon.common.http.Http.ResponseStatus;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.Routing.Rules;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class MaintenanceService extends AbstractService {

	private static final String MAINTENANCE_THREAD_NAME = "MainMaintenanceJob";

	private static boolean maintenanceRunning = false;

	public MaintenanceService(boolean isMaster) {
		super(isMaster);
	}

	@Override
	public void update(Rules rules) {
		rules.get("/start", WebSecurity.authenticate(), this::startMaintenance);
	}

	// GET: /maintenance/start
	// @formatter:off
	// Test with e.g.
	// curl -s -o /dev/null -w "%{http_code}" http://test:testit@agiprxtest:8002/maintenance/start
	// @formatter:on
	private void startMaintenance(ServerRequest serverRequest, ServerResponse serverResponse) {

		if (!validateMasterInstance(serverResponse)) {
			return;
		}

		if (maintenanceRunning) {
			serverResponse.status(ResponseStatus.create(423 /* Locked */, "Job is already running")).send();
			return;
		}

		Runnable maintenanceJob = () -> {

			try {
				maintenanceRunning = true;

				MainMaintenanceBean.getBean().runScheduled();

			} finally {
				maintenanceRunning = false;
			}
		};
		new Thread(maintenanceJob, MAINTENANCE_THREAD_NAME).start();

		serverResponse.status(ResponseStatus.create(200, "Job started")).send();
	}
}
