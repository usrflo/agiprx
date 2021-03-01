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

public class Domain extends AbstractModel {

	private String domain;

	private Long backendId;

	private Backend backend;

	private Boolean certProvided;

	private Boolean letsEncrypt;

	private String redirectToUrl;

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Long getBackendId() {
		return backendId;
	}

	public void setBackendId(Long backendId) {
		this.backendId = backendId;
	}

	public Backend getBackend() {
		return backend;
	}

	public void setBackend(Backend backend) {
		this.backend = backend;
		this.backendId = backend == null ? null : backend.getId();
	}

	public Boolean getCertProvided() {
		return certProvided;
	}

	public void setCertProvided(Boolean certProvided) {
		this.certProvided = certProvided;
	}

	public Boolean getLetsEncrypt() {
		return letsEncrypt;
	}

	public void setLetsEncrypt(Boolean letsEncrypt) {
		this.letsEncrypt = letsEncrypt;
	}

	public String getRedirectToUrl() {
		return redirectToUrl;
	}

	public void setRedirectToUrl(String redirectToUrl) {
		this.redirectToUrl = redirectToUrl;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Domain other = (Domain) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Domain [id=" + id + ", domain=" + domain + ", backend=" + backend.getLabel() + ", certProvided="
				+ certProvided + ", letsEncrypt=" + letsEncrypt + ", redirectToUrl=" + redirectToUrl + "]";
	}

}
