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

import javax.json.bind.annotation.JsonbTransient;

import de.agitos.agiprx.model.BackendContainer;

public class BackendContainerDto {

	@JsonbTransient
	private BackendContainer backendContainer;

	public BackendContainerDto() {
		this.backendContainer = new BackendContainer();
	}

	public BackendContainerDto(BackendContainer backendContainer) {
		this.backendContainer = backendContainer;
	}

	public Long getContainerId() {
		return backendContainer.getContainerId();
	}

	public void setContainerId(Long containerId) {
		this.backendContainer.setContainerId(containerId);
	}

	public Long getBackendId() {
		return backendContainer.getBackendId();
	}

	public void setBackendId(Long backendId) {
		this.backendContainer.setBackendId(backendId);
	}

	public String getParams() {
		return backendContainer.getParams();
	}

	public void setParams(String params) {
		this.backendContainer.setParams(params);
	}

	public Integer getVersion() {
		return backendContainer.getVersion();
	}

	public BackendContainer getBackendContainer() {
		return this.backendContainer;
	}
}
