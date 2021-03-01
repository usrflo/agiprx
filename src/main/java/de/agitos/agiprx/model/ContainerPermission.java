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

public class ContainerPermission extends AbstractModel {

	private Long containerId;

	private Container container;

	private Long userId;

	private User user;

	// technical username
	private String permission;

	private String password;

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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
		this.userId = user == null ? null : user.getId();
	}

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSshProxyUsername() {
		if (this.container == null) {
			throw new RuntimeException("Permission id " + id + ": container is not initialized");
		}
		// TOWATCH: max user name length : 32 chars
		return this.container.getFQLabel() + "_" + this.permission;
	}

	@Override
	public String toString() {
		return "ContainerPermission [id=" + id + ", container=" + (container == null ? "?" : container.getLabel())
				+ ", user=" + user.getFullname() + ", permission=" + permission + ", password="
				+ (password == null ? "<none>" : "<set>") + ", sshProxyUsername="
				+ (container == null ? "?" : this.getSshProxyUsername()) + " ]";
	}
}
