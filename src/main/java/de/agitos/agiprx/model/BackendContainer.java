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
package de.agitos.agiprx.model;

public class BackendContainer extends AbstractModel {

	private Long backendId;

	private Backend backend;

	private Long containerId;

	private Container container;

	// HAProxy parameter
	private String params;

	public Long getContainerId() {
		return containerId;
	}

	public void setContainerId(Long containerId) {
		this.containerId = containerId;
	}

	public Container getContainer() {
		return container;
	}

	public void setContainer(Container container) {
		this.container = container;
		this.containerId = container == null ? null : container.getId();
	}

	public Long getBackendId() {
		return backendId;
	}

	public void setBackendId(Long backendId) {
		this.backendId = backendId;
	}

	public Backend getBackend() {
		return backend;
	}

	public void setBackend(Backend backend) {
		this.backend = backend;
		this.backendId = backend == null ? null : backend.getId();
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	@Override
	public String toString() {
		return "BackendContainer [id=" + id + ", container=" + (container == null ? "?" : container.getLabel())
				+ ", backend=" + (backend == null ? "?" : backend.getLabel()) + ", params=" + params + "]";
	}
}
