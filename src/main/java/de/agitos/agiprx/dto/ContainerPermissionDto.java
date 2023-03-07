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

import de.agitos.agiprx.model.ContainerPermission;
import de.agitos.agiprx.model.User;

public class ContainerPermissionDto {

	@JsonbTransient
	private ContainerPermission containerPermission;

	public ContainerPermissionDto() {
		this.containerPermission = new ContainerPermission();
	}

	public ContainerPermissionDto(ContainerPermission containerPermission) {
		this.containerPermission = containerPermission;
	}

	public Long getContainerId() {
		return containerPermission.getContainerId();
	}

	public Long getUserId() {
		return containerPermission.getUserId();
	}

	public void setUserId(Long userId) {
		containerPermission.setUserId(userId);
	}

	public UserDto getUser() {
		return new UserDto(containerPermission.getUser());
	}

	public void setUser(User user) {
		containerPermission.setUser(user);
	}

	public String getPermission() {
		return containerPermission.getPermission();
	}

	public void setPermission(String permission) {
		containerPermission.setPermission(permission);
	}

	public String getSshProxyUsername() {
		return containerPermission.getSshProxyUsername();
	}

	public Integer getVersion() {
		return containerPermission.getVersion();
	}

	public ContainerPermission getContainerPermission() {
		return containerPermission;
	}
}
