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
import de.agitos.agiprx.dto.ContainerDto;
import de.agitos.agiprx.executor.NonInteractiveContainerExecutor;
import de.agitos.agiprx.util.UserContext;
import io.helidon.common.http.Http;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.Handler;
import io.helidon.webserver.HttpException;
import io.helidon.webserver.Routing.Rules;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class ContainerService extends AbstractService {

	private static final Logger LOG = Logger.getLogger(ContainerService.class.getName());

	NonInteractiveContainerExecutor nonInteractiveContainerExecutor;

	ProxySyncProcessor proxySyncProcessor;

	UserContext userContext;

	public ContainerService(boolean isMaster) {
		super(isMaster);
		userContext = UserContext.getBean();
		nonInteractiveContainerExecutor = NonInteractiveContainerExecutor.getBean();
		proxySyncProcessor = ProxySyncProcessor.getBean();
	}

	@Override
	public void update(Rules rules) {
		rules.put("/{+projectLabel}", WebSecurity.authenticate(), Handler.create(ContainerDto.class,
				(req, res, containerDto) -> res.send(putContainer(req, res, containerDto))));
	}

	// Sample PUT-Request:
	// {
	// "label": "clabel1",
	// "fullname": "cfullname1",
	// "ipv6": "fd42:d555:aca:2e34::1",
	// "hostname": "host48.agitos.de",
	// "containerPermissions": [{
	// "userId": "1",
	// "permission": "root"
	// }]
	// }
	private ServiceResult putContainer(ServerRequest serverRequest, ServerResponse serverResponse,
			ContainerDto containerDto) {

		if (!validateMasterInstance(serverResponse)) {
			return null;
		}

		try {
			String projectLabel = serverRequest.path().param("projectLabel");

			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));

			Long containerId = nonInteractiveContainerExecutor.createOrUpdateContainer(projectLabel, containerDto);

			List<String> warningMessages = new ArrayList<>();

			return ServiceResult.create(new SimpleImmutableEntry<String, Long>("containerId", containerId),
					warningMessages);

		} catch (Exception e) {

			LOG.log(Level.SEVERE, "Unable to process Container create/update", e);

			throw new HttpException("Unable to process Container create/update: " + e.getMessage(),
					Http.Status.INTERNAL_SERVER_ERROR_500, e);

		} finally {
			userContext.unregister();
		}
	}

}
