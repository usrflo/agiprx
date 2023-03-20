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

import java.util.List;
import java.util.Map;

import de.agitos.agiprx.bean.processor.ProxySyncProcessor;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.dto.MassDomainUpdateDto;
import de.agitos.agiprx.executor.NonInteractiveDomainExecutor;
import de.agitos.agiprx.util.UserContext;
import io.helidon.common.http.Http;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.Handler;
import io.helidon.webserver.HttpException;
import io.helidon.webserver.Routing.Rules;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class DomainService extends AbstractService {

	NonInteractiveDomainExecutor nonInteractiveDomainExecutor;

	ProxySyncProcessor proxySyncProcessor;

	UserContext userContext;

	public DomainService(boolean isMaster) {
		super(isMaster);
		userContext = UserContext.getBean();
		nonInteractiveDomainExecutor = NonInteractiveDomainExecutor.getBean();
		proxySyncProcessor = ProxySyncProcessor.getBean();
	}

	@Override
	public void update(Rules rules) {
		rules.post("/massupdate", WebSecurity.authenticate(), Handler.create(MassDomainUpdateDto.class,
				(req, res, massDomainUpdateDto) -> res.send(massUpdate(req, res, massDomainUpdateDto))));
		rules.get("/{+projectLabel}/{+backendLabel}", WebSecurity.authenticate(), this::findBackendDomains);
		rules.get("/{+projectLabel}", WebSecurity.authenticate(), this::findProjectDomains);
	}

	// POST: /domains/massupdate
	private List<String> massUpdate(ServerRequest serverRequest, ServerResponse serverResponse,
			MassDomainUpdateDto massDomainUpdate) {

		if (!validateMasterInstance(serverResponse)) {
			return null;
		}

		try {
			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));
			List<String> warningMessages = nonInteractiveDomainExecutor.massDomainUpdate(massDomainUpdate);
			// if (proxySyncProcessor.isSyncRequired()) {
			// proxySyncProcessor.syncToSlaveInstances(false, warningMessages);
			// }
			return warningMessages;

		} catch (Exception e) {
			// TODO: status(...create(500, "Unable to process mass update")).send() ??
			throw new HttpException("Unable to process mass update", Http.Status.INTERNAL_SERVER_ERROR_500, e);
		} finally {
			userContext.unregister();
		}
	}

	// GET: /domains/{projectLabel}/{backendLabel}
	private void findBackendDomains(ServerRequest serverRequest, ServerResponse serverResponse) {
		try {
			String projectLabel = serverRequest.path().param("projectLabel");
			String backendLabel = serverRequest.path().param("backendLabel");
			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));
			Map<String, DomainDto> result = nonInteractiveDomainExecutor.findBackendDomains(projectLabel, backendLabel);

			serverResponse.send(result);

		} finally {
			userContext.unregister();
		}
	}

	// GET: /domains/{projectLabel}
	private void findProjectDomains(ServerRequest serverRequest, ServerResponse serverResponse) {
		try {
			String projectLabel = serverRequest.path().param("projectLabel");
			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));
			Map<String, DomainDto> result = nonInteractiveDomainExecutor.findProjectDomains(projectLabel);

			serverResponse.send(result);

		} finally {
			userContext.unregister();
		}
	}
}
