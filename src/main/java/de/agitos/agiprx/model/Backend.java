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
import de.agitos.agiprx.output.HAProxyBackendFormatter;
import de.agitos.agiprx.output.table.DomainTableFormatter;

public class Backend extends AbstractModel {

	public static final String DEFAULT_LABEL = "prod";

	// show empty page
	public static final String NO_CONTENT_LABEL = "nocontent";

	// temporary redirect to specific domain
	public static final String TEMPORARY_REDIRECT_LABEL = "tempredirect";

	// permanent redirect to specific domain
	public static final String PERMANENT_REDIRECT_LABEL = "permredirect";

	public static final Integer DEFAULT_PORT = 80;

	private String label;

	private String fullname;

	private Long projectId;

	private Project project;

	private Integer port;

	private String params;

	// Transient --- START

	private List<Domain> domainForwardings;

	// if multiple containers: load-balancing
	private List<BackendContainer> backendContainers;

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

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public List<Domain> getDomainForwardings() {
		return domainForwardings;
	}

	public void setDomainForwardings(List<Domain> domainForwardings) {
		this.domainForwardings = domainForwardings;
	}

	public List<BackendContainer> getBackendContainers() {
		return backendContainers;
	}

	public void setBackendContainers(List<BackendContainer> backendContainers) {
		this.backendContainers = backendContainers;
	}

	public String getFQLabel() {
		if (this.project == null) {
			throw new RuntimeException("Backend id " + id + ": project is not initialized");
		}
		return this.project.getLabel() + "_" + this.id + "_" + this.label;
	}

	public boolean isGlobalBackend() {
		return (Backend.NO_CONTENT_LABEL.equals(this.getLabel())
				|| Backend.PERMANENT_REDIRECT_LABEL.equals(this.getLabel())
				|| Backend.TEMPORARY_REDIRECT_LABEL.equals(this.getLabel()));
	}

	public String toStringRecursive(String linePrefix, int maxTotalWidth) {
		StringBuilder buf = new StringBuilder();

		int countDomains = this.getDomainForwardings().size();
		int showMaxDomains = 6;

		buf.append(linePrefix).append(this.toString()).append("\n");

		// output of haProxy backend config
		buf.append(ConsoleWrapper.ANSI_RESET);

		if (this.isGlobalBackend() /* e.g. redirection backend */) {
			// skip 'nocontent' or domain redirect backend
		} else if (this.getBackendContainers().size() == 0) {
			buf.append(String.format(linePrefix + "Warning: no containers assigned, backend is non-functional."));
		} else {
			HAProxyBackendFormatter backendFormatter = new HAProxyBackendFormatter();
			backendFormatter.formatBackend(this, buf, linePrefix);
		}

		// add domain info
		buf.append(ConsoleWrapper.ANSI_GREEN);

		buf.append(linePrefix).append("# ").append(countDomains).append(" Domains\n");

		int cnt = 1;
		buf.append(ConsoleWrapper.ANSI_RESET);

		DomainTableFormatter tableFormatter = new DomainTableFormatter(maxTotalWidth);

		String overrideFooter = null;
		for (Domain domain : this.getDomainForwardings()) {
			// set back-reference
			domain.setBackend(this);

			tableFormatter.addDomain(domain);

			// limit output of domains if too many to show directly
			cnt++;
			if (cnt > showMaxDomains && countDomains - showMaxDomains != 1) {
				overrideFooter = "and " + (countDomains - showMaxDomains) + " more ...";
				break;
			}
		}

		tableFormatter.printTable(buf, linePrefix, overrideFooter);

		buf.append(ConsoleWrapper.ANSI_GREEN);

		return buf.toString();
	}

	@Override
	public String toString() {
		return "Backend [id=" + id + ", label=" + label + (fullname == null ? "" : " (" + fullname + ")")
				+ ", #backendContainers=" + (backendContainers == null ? 0 : backendContainers.size()) + ", port="
				+ port + ", #domainForwardings=" + domainForwardings.size() + "]";
	}
}
