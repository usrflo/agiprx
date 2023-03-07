package de.agitos.agiprx.rest;

import io.helidon.common.http.Http.ResponseStatus;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

public abstract class AbstractService implements Service {

	protected final boolean isMaster;

	protected AbstractService(boolean isMaster) {
		this.isMaster = isMaster;
	}

	protected boolean validateMasterInstance(ServerResponse serverResponse) {
		if (!isMaster) {
			serverResponse.status(ResponseStatus.create(405 /* Method not allowed */,
					"Job needs to be started on master instance (this is a slave instance)")).send();
			return false;
		}
		return true;
	}
}
