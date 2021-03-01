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
package de.agitos.agiprx.executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.agitos.agiprx.bean.processor.LesslCertProcessor;
import de.agitos.agiprx.dto.DomainDto;

public abstract class AbstractCertificateRelatedExecutor extends AbstractExecutor {

	protected LesslCertProcessor lesslCertProcessor;

	@Override
	public void postConstruct() {
		super.postConstruct();
		lesslCertProcessor = LesslCertProcessor.getBean();
	}

	protected void cleanupCertificates(List<DomainDto> removedDomains) throws IOException {

		if (removedDomains != null) {
			List<String> warningMessages = new ArrayList<String>();
			lesslCertProcessor.cleanupLesslCerts(removedDomains, warningMessages);
			warningMessages.forEach(x -> console.printlnfError(x));
		}
	}

	protected void cleanupCertificates(List<DomainDto> removedDomains, List<String> warningMessages)
			throws IOException {

		if (removedDomains != null) {
			lesslCertProcessor.cleanupLesslCerts(removedDomains, warningMessages);
		}
	}
}
