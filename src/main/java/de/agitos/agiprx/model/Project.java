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

public class Project extends AbstractModel {

	private String label;

	// readable name, can contain spaces (just informative)
	private String fullname;

	// Transient --- START

	private List<Container> containers;

	private List<Backend> backends;

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

	public List<Container> getContainers() {
		return containers;
	}

	public void setContainers(List<Container> containers) {
		this.containers = containers;
	}

	public List<Backend> getBackends() {
		return backends;
	}

	public void setBackends(List<Backend> backends) {
		this.backends = backends;
	}

	public String toStringRecursive(int maxTotalWidth) {
		StringBuilder buf = new StringBuilder();
		buf.append(ConsoleWrapper.ANSI_GREEN);
		buf.append(this.toString()).append("\n");
		buf.append("# Containers\n");
		for (Container container : this.containers) {
			container.setProject(this);
			buf.append(container.toStringRecursive("\t"));
		}
		buf.append("# Backends\n");
		for (Backend backend : this.backends) {
			backend.setProject(this);
			buf.append(backend.toStringRecursive("\t", maxTotalWidth));
		}
		buf.append(ConsoleWrapper.ANSI_RESET);
		return buf.toString();
	}

	@Override
	public String toString() {
		return "Project [id=" + id + ", label=" + label + ", fullname=" + fullname + ", #containers="
				+ containers.size() + ", #backends=" + backends.size() + "]";
	}
}
