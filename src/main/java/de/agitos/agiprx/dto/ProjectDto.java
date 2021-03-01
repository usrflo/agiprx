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
import de.agitos.agiprx.model.Container;
import de.agitos.agiprx.model.Project;

public class ProjectDto {

	private Project project;

	public ProjectDto(Project project) {
		this.project = project;
	}

	public String getLabel() {
		return project.getLabel();
	}

	public String getFullname() {
		return project.getFullname();
	}

	public List<ContainerDto> getContainers() {
		List<ContainerDto> result = new ArrayList<>();
		if (project.getContainers() == null) {
			return result;
		}
		for (Container container : project.getContainers()) {
			result.add(new ContainerDto(container));
		}
		return result;
	}

	public List<BackendDto> getBackends() {
		List<BackendDto> result = new ArrayList<>();
		if (project.getBackends() == null) {
			return result;
		}
		for (Backend backend : project.getBackends()) {
			result.add(new BackendDto(backend));
		}
		return result;
	}

	public Integer getVersion() {
		return project.getVersion();
	}
}
