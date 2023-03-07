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

import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.Host;

public class ContainerDto {

	private Container container;

	public ContainerDto() {
		this.container = new Container();
	}

	public ContainerDto(Container container) {
		this.container = container;
	}

	public String getLabel() {
		return container.getLabel();
	}

	public void setLabel(String label) {
		container.setLabel(label);
	}

	public String getFullname() {
		return container.getFullname();
	}

	public void setFullname(String fullname) {
		container.setFullname(fullname);
	}

	public Long getProjectId() {
		return container.getProjectId();
	}

	public Long getHostId() {
		return container.getHostId();
	}

	public HostDto getHost() {
		return new HostDto(container.getHost());
	}

	public void setHost(Host host) {
		container.setHost(host);
	}

	public void setHostname(String hostname) {
		if (container.getHost() == null) {
			container.setHost(new Host());
		}
		container.getHost().setHostname(hostname);
	}

	public String getHostname() {
		return container.getHost().getHostname();
	}

	public String getIpv6() {
		return container.getIpv6();
	}

	public void setIpv6(String ipv6) {
		container.setIpv6(ipv6);
	}

	public List<ContainerPermissionDto> getContainerPermissions() {
		List<ContainerPermissionDto> result = new ArrayList<>();
		if (container.getContainerPermissions() == null) {
			return result;
		}
		for (ContainerPermission containerPermission : container.getContainerPermissions()) {
			result.add(new ContainerPermissionDto(containerPermission));
		}
		return result;
	}

	public void setContainerPermissions(ArrayList<ContainerPermissionDto> containerPermissionsDto) {
		List<ContainerPermission> containerPermissions = new ArrayList<>();

		for (ContainerPermissionDto containerPermissionDto : containerPermissionsDto) {
			containerPermissions.add(containerPermissionDto.getContainerPermission());
		}

		container.setContainerPermissions(containerPermissions);
	}

	public String getFQLabel() {
		return container.getFQLabel();
	}

	public Integer getVersion() {
		return container.getVersion();
	}

	public Container getContainer() {
		return container;
	}
}
