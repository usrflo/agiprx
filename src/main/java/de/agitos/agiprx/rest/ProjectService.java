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

import de.agitos.agiprx.dto.ProjectDto;
import de.agitos.agiprx.executor.NonInteractiveProjectExecutor;
import de.agitos.agiprx.util.UserContext;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.webserver.Routing.Rules;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

public class ProjectService extends AbstractService {

	NonInteractiveProjectExecutor nonInteractiveProjectExecutor;

	UserContext userContext;

	public ProjectService(boolean isMaster) {
		super(isMaster);
		userContext = UserContext.getBean();
		nonInteractiveProjectExecutor = NonInteractiveProjectExecutor.getBean();
	}

	@Override
	public void update(Rules rules) {
		rules.get("/{+projectLabel}", WebSecurity.authenticate(), this::findProjectDetails);
		rules.get("/", WebSecurity.authenticate(), this::findProjectList);
	}

	// GET: /projects/
	private void findProjectList(ServerRequest serverRequest, ServerResponse serverResponse) {
		try {
			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));
			List<String> result = nonInteractiveProjectExecutor.findProjectList();

			serverResponse.send(result);

		} finally {
			userContext.unregister();
		}
	}

	// GET: /projects/{projectLabel}
	private void findProjectDetails(ServerRequest serverRequest, ServerResponse serverResponse) {
		try {
			String projectLabel = serverRequest.path().param("projectLabel");
			userContext.registerApiUser(RestServiceUtil.getUsername(serverRequest));
			ProjectDto result = nonInteractiveProjectExecutor.findProjectDetails(projectLabel);

			serverResponse.send(result);

		} finally {
			userContext.unregister();
		}
	}
}
