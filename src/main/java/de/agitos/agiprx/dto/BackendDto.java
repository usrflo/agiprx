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
package de.agitos.agiprx.dto;

import java.util.ArrayList;
import java.util.List;

import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.BackendContainer;
import de.agitos.agiprx.model.Domain;

public class BackendDto {

	private Backend backend;

	public BackendDto(Backend backend) {
		this.backend = backend;
	}

	public String getLabel() {
		return backend.getLabel();
	}

	public String getFullname() {
		return backend.getFullname();
	}

	public Long getProjectId() {
		return backend.getProjectId();
	}

	public Integer getPort() {
		return backend.getPort();
	}

	public String getParams() {
		return backend.getParams();
	}

	public List<DomainDto> getDomainForwardings() {
		List<DomainDto> result = new ArrayList<>();
		if (backend.getDomainForwardings() == null) {
			return result;
		}
		for (Domain domain : backend.getDomainForwardings()) {
			result.add(new DomainDto(domain));
		}
		return result;
	}

	public List<BackendContainerDto> getBackendContainers() {
		List<BackendContainerDto> result = new ArrayList<>();
		if (backend.getBackendContainers() == null) {
			return result;
		}
		for (BackendContainer backendContainer : backend.getBackendContainers()) {
			result.add(new BackendContainerDto(backendContainer));
		}
		return result;
	}

	public String getFQLabel() {
		return backend.getFQLabel();
	}

	public boolean isGlobalBackend() {
		return backend.isGlobalBackend();
	}

	public Integer getVersion() {
		return backend.getVersion();
	}
}
