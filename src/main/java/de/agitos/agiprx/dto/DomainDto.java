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

import de.agitos.agiprx.model.Domain;

public class DomainDto {

	@JsonbTransient
	private Domain domain;

	public DomainDto() {
		this.domain = new Domain();
		this.domain.setLetsEncrypt(false);
		this.domain.setCertProvided(false);
	}

	public DomainDto(Domain domain) {
		this.domain = domain;
	}

	public String getDomainName() {
		return domain.getDomain();
	}

	public void setDomainName(String domainName) {
		this.domain.setDomain(domainName);
	}

	public Integer getVersion() {
		return domain.getVersion();
	}

	public String getBackendLabel() {
		return domain.getBackend().getLabel();
	}

	public Boolean getLetsEncrypt() {
		return domain.getLetsEncrypt();
	}

	public void setLetsEncrypt(Boolean letsEncrypt) {
		this.domain.setLetsEncrypt(letsEncrypt);
	}

	public Boolean getCertProvided() {
		return domain.getCertProvided();
	}

	public void setCertProvided(Boolean certProvided) {
		this.domain.setCertProvided(certProvided);
	}

	public String getRedirectToUrl() {
		return domain.getRedirectToUrl();
	}

	public void setRedirectToUrl(String redirectToUrl) {
		this.domain.setRedirectToUrl(redirectToUrl);
	}

	public Domain getDomain() {
		return this.domain;
	}
}
