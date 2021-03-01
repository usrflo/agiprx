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
package de.agitos.agiprx.bean.processor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.model.CombinedCertificate;
import de.agitos.agiprx.util.Assert;

public class SslCertProcessor extends AbstractProcessor implements DependencyInjector {

	private static SslCertProcessor BEAN;

	private ConsoleWrapper console;

	private HAProxyProcessor haProxyProcessor;

	public SslCertProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		console = ConsoleWrapper.getBean();
		haProxyProcessor = HAProxyProcessor.getBean();
	}

	public static SslCertProcessor getBean() {
		return BEAN;
	}

	public void setPrivateKeyByFilename(CombinedCertificate cCert, String filename) {
		if (checkPrivKeyFile(filename)) {
			try {
				cCert.setPrivateKey(getFileContent(filename));
			} catch (IOException e) {
				console.printlnfError("Error reading file %s: key file not readable?", filename);
			}
		} else {
			console.printlnfError("Error reading file %s: invalid key?", filename);
		}
	}

	public void setPrivateKey(CombinedCertificate cCert, String pemString) {
		try {
			if (checkPrivKeyString(pemString)) {
				cCert.setPrivateKey(pemString);
			}
		} catch (IOException e) {
			console.printlnfError("Error setting key: invalid key format?");
		}
	}

	public void setCertificate(CombinedCertificate cCert, String pemString) {
		cCert.setCertificate(pemString);
	}

	public void setCertificateByFilename(CombinedCertificate cCert, String filename) {
		try {
			cCert.setCertificate(getFileContent(filename));
		} catch (IOException e) {
			console.printlnfError("Error reading file %s: certificate file not readable?", filename);
		}
	}

	public void setIntermediateCertificates(CombinedCertificate cCert, String pemString) {
		cCert.setIntermediateCertificates(pemString);
	}

	public void setIntermediateCertificatesByFilename(CombinedCertificate cCert, String filename) {
		try {
			cCert.setIntermediateCertificates(getFileContent(filename));
		} catch (IOException e) {
			console.printlnfError("Error reading file %s: certificate file not readable?", filename);
		}
	}

	public void installCombinedCertificate(CombinedCertificate cCert) {
		try {
			Path tmpFile = Files.createTempFile("validate-combined-", ".pem");

			try (PrintWriter out = new PrintWriter(tmpFile.toFile())) {
				out.println(cCert.getCombinedCertificate());
			}

			if (checkCombinedCertificateFile(tmpFile.toString())) {
				Files.move(tmpFile, Paths.get(haProxyProcessor.getCertPath(), cCert.getCombinedCertificateFilename()));
			}

		} catch (Exception e) {
			console.printlnfError("Failed to install certificate: %s", e.getMessage());
		}
	}

	private boolean checkPrivKeyFile(String filename) {
		try {
			int exit = exec("openssl", "pkey", "-in", filename, "-pubout", "-outform", "pem");
			return exit == 0;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	private boolean checkPrivKeyString(String pemKey) throws IOException {

		File tmpFile = File.createTempFile("validate-key-", ".pem");

		try (PrintWriter out = new PrintWriter(tmpFile)) {
			out.println(pemKey);
		}

		boolean result = checkPrivKeyFile(tmpFile.getAbsolutePath());

		tmpFile.delete();

		return result;
	}

	private boolean checkCombinedCertificateFile(String filename) {
		try {
			int exit = exec("openssl", "verify", "-CAfile", filename, filename);
			return exit == 0;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	private String getFileContent(String filename) throws IOException {
		return new String(Files.readAllBytes(Paths.get(filename)));
	}
}
