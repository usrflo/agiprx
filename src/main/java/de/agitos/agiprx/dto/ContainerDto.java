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

public class ContainerDto {

	private Container container;

	public ContainerDto(Container container) {
		this.container = container;
	}

	public String getLabel() {
		return container.getLabel();
	}

	public String getFullname() {
		return container.getFullname();
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

	public String getIpv6() {
		return container.getIpv6();
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

	public String getFQLabel() {
		return container.getFQLabel();
	}

	public Integer getVersion() {
		return container.getVersion();
	}
}
