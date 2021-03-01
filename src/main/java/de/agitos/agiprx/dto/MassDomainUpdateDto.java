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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MassDomainUpdateDto {

	private boolean fullSync = false;

	private List<DomainOperationDto> domainOperations;

	public boolean isFullSync() {
		return fullSync;
	}

	public void setFullSync(boolean fullSync) {
		this.fullSync = fullSync;
	}

	public List<DomainOperationDto> getDomainOperations() {
		return domainOperations;
	}

	public void setDomainOperations(List<DomainOperationDto> domainOperations) {
		this.domainOperations = domainOperations;
	}

	public Set<String> verifyUniq() throws Exception {

		String projectLabel = null;
		Set<String> containedDomains = new HashSet<>();

		for (DomainOperationDto domainOp : this.domainOperations) {

			if (containedDomains.contains(domainOp.getDomainName())) {
				throw new RuntimeException(domainOp.getDomainName() + ": disallowed duplicate update operation");
			}

			if (this.fullSync) {

				if (OperationType.DELETE.equals(domainOp.getOperation())) {
					throw new RuntimeException(domainOp.getDomainName()
							+ ": full synchronizations allow 'create' and 'update' records only.");
				}

				if (projectLabel == null) {
					projectLabel = domainOp.getProjectLabel();
				} else if (!projectLabel.equals(domainOp.getProjectLabel())) {
					throw new RuntimeException(
							"Full synchronizations need to refer to a single existing project, containing all active domains in the project.");
				}
			}

			containedDomains.add(domainOp.getDomainName());
		}

		return containedDomains;
	}
}
