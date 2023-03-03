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

import java.util.List;

import de.agitos.agiprx.ConsoleWrapper;

public class Container extends AbstractModel {

	public static final String DEFAULT_LABEL = "prod";

	private String label;

	private String fullname;

	private Long projectId;

	private Project project;

	private Long hostId;

	private Host host;

	private String ipv6;

	// Transient --- START

	private List<ContainerPermission> containerPermissions;

	// Transient --- END

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
		this.projectId = project == null ? null : project.getId();
	}

	public Long getHostId() {
		return hostId;
	}

	public void setHostId(Long hostId) {
		this.hostId = hostId;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
		this.hostId = host == null ? null : host.getId();
	}

	public String getIpv6() {
		return ipv6;
	}

	public void setIpv6(String ipv6) {
		this.ipv6 = ipv6;
	}

	public List<ContainerPermission> getContainerPermissions() {
		return containerPermissions;
	}

	public void setContainerPermissions(List<ContainerPermission> containerPermissions) {
		this.containerPermissions = containerPermissions;
	}

	public String getFQLabel() {
		if (this.project == null) {
			throw new RuntimeException("Container id " + id + ": project is not initialized");
		}
		return this.project.getLabel() + "-" + this.label;
	}

	public String toStringRecursive(String linePrefix) {
		StringBuilder buf = new StringBuilder();

		buf.append(linePrefix).append(this.toString()).append("\n");
		if (this.getContainerPermissions() != null) {
			if (this.getContainerPermissions().isEmpty()) {
				buf.append(linePrefix).append("# No permissions defined\n");
			} else {
				buf.append(linePrefix).append("# Permissions\n");
				buf.append(ConsoleWrapper.ANSI_RESET);
				for (ContainerPermission permission : this.getContainerPermissions()) {
					permission.setContainer(this);
					buf.append(linePrefix).append(permission.getUser().getFullname()).append(": ")
							.append(permission.getSshProxyUsername()).append(" -> ").append(permission.getPermission())
							.append("\n");
				}
			}
		}
		buf.append(ConsoleWrapper.ANSI_GREEN);
		return buf.toString();
	}

	@Override
	public String toString() {
		return "Container [id=" + id + ", label=" + label + (fullname == null ? "" : " (" + fullname + ")") + ", host="
				+ (host == null ? "" : host.getHostname()) + ", ipv6=" + ipv6 + "]";
	}
}
