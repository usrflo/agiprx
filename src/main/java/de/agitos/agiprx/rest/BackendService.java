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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.agitos.agiprx.bean.processor.ProxySyncProcessor;
import de.agitos.agiprx.dto.BackendDto;
import de.agitos.agiprx.executor.NonInteractiveBackendExecutor;
import de.agitos.agiprx.util.UserContext;
import io.helidon.common.http.Http;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.Handler;
import io.helidon.webserver.HttpException;
import io.helidon.webserver.Routing.Rules;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class BackendService extends AbstractService {

	private static final Logger LOG = Logger.getLogger(BackendService.class.getName());

	NonInteractiveBackendExecutor nonInteractiveBackendExecutor;

	ProxySyncProcessor proxySyncProcessor;

	UserContext userContext;

	public BackendService(boolean isMaster) {
		super(isMaster);
		userContext = UserContext.getBean();
		nonInteractiveBackendExecutor = NonInteractiveBackendExecutor.getBean();
		proxySyncProcessor = ProxySyncProcessor.getBean();
	}

	@Override
	public void update(Rules rules) {
		rules.post("/{+projectLabel}", WebSecurity.authenticate(),
				Handler.create(BackendDto.class, (req, res, backendDto) -> res.send(putBackend(req, res, backendDto))));
		rules.patch("/{+projectLabel}/{+backendLabel}/setContainersOf/{+targetBackendLabel}",
				WebSecurity.authenticate(), this::setContainersOfTargetBackend);
	}

	// Sample POST-Request:
	// {
	// "label": "label1",
	// "fullname": "fullname1",
	// "port": "3306",
	// "params": null,
	// "domainForwardings": [{
	// "domain": "foo1.agitos.de",
	// "certProvided": true
	// }],
	// "backendContainers": [{
	// "containerId": 1
	// }]
	// }
	private ServiceResult putBackend(ServerRequest serverRequest, ServerResponse serverResponse, BackendDto backend) {

		if (!validateMasterInstance(serverResponse)) {
			return null;
		}

		try {
			String projectLabel = serverRequest.path().param("projectLabel");

			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));

			Long backendId = nonInteractiveBackendExecutor.createOrUpdateBackend(projectLabel, backend);

			List<String> warningMessages = new ArrayList<>();

			return ServiceResult.create(new SimpleImmutableEntry<String, Long>("backendId", backendId),
					warningMessages);

		} catch (Exception e) {

			LOG.log(Level.SEVERE, "Unable to process backend create/update", e);

			throw new HttpException("Unable to process backend create/update: " + e.getMessage(),
					Http.Status.INTERNAL_SERVER_ERROR_500, e);
		} finally {
			userContext.unregister();
		}
	}

	// PATCH:
	// /backends/{projectLabel}/{backendLabel}/setContainersOf/{targetBackendLabel}
	private void setContainersOfTargetBackend(ServerRequest serverRequest, ServerResponse serverResponse) {

		if (!validateMasterInstance(serverResponse)) {
			return;
		}

		try {
			String projectLabel = serverRequest.path().param("projectLabel");
			String backendLabel = serverRequest.path().param("backendLabel");
			String targetBackendLabel = serverRequest.path().param("targetBackendLabel");

			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));

			boolean result = nonInteractiveBackendExecutor.setContainersOfTargetBackend(projectLabel, backendLabel,
					targetBackendLabel);

			List<String> warningMessages = new ArrayList<>();

			serverResponse.send(ServiceResult.create(result, "Switched containers to backend " + targetBackendLabel,
					warningMessages));

		} catch (Exception e) {

			LOG.log(Level.SEVERE, "Unable to process backend container update", e);

			throw new HttpException("Unable to process backend container update: " + e.getMessage(),
					Http.Status.INTERNAL_SERVER_ERROR_500, e);
		} finally {
			userContext.unregister();
		}
	}

}
