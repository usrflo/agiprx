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

// non persistent model
public class CombinedCertificate {

	private String domain;

	private String privateKey;

	private String certificate;

	private String intermediateCertificates;

	public CombinedCertificate(String domain) {
		this.domain = domain;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	public String getIntermediateCertificates() {
		return intermediateCertificates;
	}

	public void setIntermediateCertificates(String intermediateCertificates) {
		this.intermediateCertificates = intermediateCertificates;
	}

	public String getCombinedCertificateFilename() {
		return "ext-" + domain + ".pem";
	}

	public String getCombinedCertificate() {
		StringBuilder buf = new StringBuilder();
		buf.append(certificate.trim()).append("\n");
		if (intermediateCertificates != null) {
			buf.append(intermediateCertificates.trim()).append("\n");
		}
		buf.append(privateKey.trim()).append("\n");
		return buf.toString();
	}

}
