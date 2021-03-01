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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.bean.processor.HAProxyProcessor.CertInfo;
import de.agitos.agiprx.dao.DomainDao;
import de.agitos.agiprx.dns.DomainIpChecker;
import de.agitos.agiprx.dto.DomainDto;
import de.agitos.agiprx.model.Domain;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.FileSystemOperation;

public class LesslCertProcessor extends AbstractProcessor implements DependencyInjector {

	private static LesslCertProcessor BEAN;

	private final static String LE_CONFIG_RENEWAL_PATH = "/etc/letsencrypt/renewal/";
	private final static String LE_CONFIG_ARCHIVE_PATH = "/etc/letsencrypt/archive/";
	private final static String LE_CONFIG_LIVE_PATH = "/etc/letsencrypt/live/";

	// @Value("${cert.certbotnewcertcommand}")
	String certbotNewCertCommand;

	// @Value("${cert.certbotrenewcertscommand}")
	String certbotRenewCertsCommand;

	private HAProxyProcessor haProxyProcessor;

	private DomainIpChecker domainIpChecker;

	private DomainDao domainDao;

	public LesslCertProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		Config config = Config.getBean();
		certbotNewCertCommand = config.getString("cert.certbotnewcertcommand");
		certbotRenewCertsCommand = config.getString("cert.certbotrenewcertscommand");
	}

	@Override
	public void postConstruct() {
		haProxyProcessor = HAProxyProcessor.getBean();
		domainIpChecker = DomainIpChecker.getBean();
		domainDao = DomainDao.getBean();
	}

	public static LesslCertProcessor getBean() {
		return BEAN;
	}

	public void run(List<String> warningMessages, Set<String> domainsInErrorState) {

		Map<String, CertInfo> productiveDomainToCert = haProxyProcessor.fetchDomainToCertFiles(null);

		for (Domain domainObj : domainDao.findAll()) {

			String domain = domainObj.getDomain();

			if (domainsInErrorState.contains(domain)) {
				continue;
			}

			if (!Boolean.TRUE.equals(domainObj.getLetsEncrypt())) {
				continue;
			}

			if (productiveDomainToCert.containsKey(domain)) {

				CertInfo certInfo = productiveDomainToCert.get(domain);

				if (!certInfo.isLesslCert()) {
					if (createNewLesslCert(warningMessages, domain)) {
						warningMessages.add("As cert " + certInfo.getFilename() + " is of issuer "
								+ certInfo.getIssuerName() + " instead of "
								+ certInfo.getLesslIssuerDefaultPartialName() + " a LESSL cert was generated.");
					}
				} else {

					List<String> errorMessages = new ArrayList<String>();
					List<Path> existingConfigFiles = FileSystemOperation.getMatchingPaths(
							Paths.get(LE_CONFIG_RENEWAL_PATH), domain + "{.conf,-*.conf}", errorMessages);

					if (errorMessages.isEmpty() && existingConfigFiles.isEmpty()) {
						// renewal information is missing so recreate LESSL cert
						if (createNewLesslCert(warningMessages, domain)) {
							warningMessages.add("As the renewal config of the LESSL cert for " + domain
									+ " was missing, the certificate was recreated.");
						}
					}

				}
			} else {
				createNewLesslCert(warningMessages, domain);
			}
		}

		// renew certs
		renewLesslCerts(warningMessages);

		// assure the domain-CertInfo mapping will be re-fetched
		haProxyProcessor.resetDomainToCertFiles();
	}

	public boolean checkCertAndOptCreate(List<String> warningMessages, String domainName) {

		if (domainIpChecker.checkDomain(warningMessages, domainName)) {

			// check if LESSL valid cert is already available
			Map<String, CertInfo> domainCertMap = haProxyProcessor.fetchDomainToCertFiles(warningMessages);
			if (domainCertMap.containsKey(domainName)) {
				CertInfo certInfo = domainCertMap.get(domainName);
				if (certInfo.isLesslCert()) {
					if (certInfo.isCurrentlyValid()) {
						warningMessages.add("Valid LESSL cert already exists for " + domainName + ", nothing to do");
						return true;
					} else {
						warningMessages
								.add("Invalid LESSL cert already exists for " + domainName + ", so generate a new one");
					}
				} else {
					warningMessages.add("A Non-LESSL cert already exists for " + domainName + " in file "
							+ certInfo.getFilename() + "; the new LESSL cert will get precedence");
				}
			}

			return createNewLesslCert(warningMessages, domainName);
		}
		return false;
	}

	private boolean createNewLesslCert(List<String> warningMessages, String domainName) {

		try {
			StringBuilder output = new StringBuilder();
			StringBuilder errorOutput = new StringBuilder();
			int exitCode = exec(output, errorOutput, String.format(certbotNewCertCommand, domainName));
			if (exitCode != 0 && (output.length() > 0 || errorOutput.length() > 0)) {
				errorOutput.append("\nexit code was " + exitCode);
				warningMessages.add("Failed execution of certbot for new cert generation on " + domainName + ": "
						+ output.toString() + " " + errorOutput.toString());
				return false;
			}

			// Add new cert to domain-CertInfo-map in memory (only)
			Path combinedCert = getLESSLCertFilePath(domainName);
			haProxyProcessor.addLesslDomainToMap(combinedCert);

		} catch (IOException | InterruptedException e) {
			warningMessages.add("Exception, Failed execution of certbot for new cert generation on " + domainName + ": "
					+ e.getMessage());
			return false;
		}

		return true;
	}

	// check list of removed domain names if removal of LESSL cert configuration is
	// required; call removal in case
	public void cleanupLesslCerts(List<DomainDto> removedDomains, List<String> warningMessages) throws IOException {

		for (DomainDto removedDomain : removedDomains) {

			if (removedDomain.getLetsEncrypt()) {
				deleteLesslCert(removedDomain.getDomainName(), warningMessages);
			} else {
				CertInfo certInfo = haProxyProcessor.getCertInfoForDomainName(removedDomain.getDomainName());
				if (certInfo != null && certInfo.isLesslCert()) {
					deleteLesslCert(removedDomain.getDomainName(), warningMessages);
				}
			}
		}
	}

	// remove LESSL cert configuration for specific domain name
	public void deleteLesslCert(String domainName, List<String> warningMessages) throws IOException {

		// remove renewal config
		List<Path> filesToBeDeleted = FileSystemOperation.getMatchingPaths(Paths.get(LE_CONFIG_RENEWAL_PATH),
				domainName + "{.conf,-*.conf}", warningMessages);
		for (Path fileToBeDeleted : filesToBeDeleted) {
			FileSystemOperation.deleteFile(fileToBeDeleted,
					Paths.get(LE_CONFIG_RENEWAL_PATH, domainName).toAbsolutePath().toString(), warningMessages);
		}

		// remove live config
		List<Path> pathsToBeDeleted = FileSystemOperation.getMatchingPaths(Paths.get(LE_CONFIG_LIVE_PATH),
				domainName + "{,-*}", warningMessages);
		for (Path pathToBeDeleted : pathsToBeDeleted) {
			FileSystemOperation.deletePathRecursively(pathToBeDeleted,
					Paths.get(LE_CONFIG_LIVE_PATH, domainName).toAbsolutePath().toString(), warningMessages);
		}

		// remove archive config
		pathsToBeDeleted = FileSystemOperation.getMatchingPaths(Paths.get(LE_CONFIG_ARCHIVE_PATH), domainName + "{,-*}",
				warningMessages);
		for (Path pathToBeDeleted : pathsToBeDeleted) {
			FileSystemOperation.deletePathRecursively(pathToBeDeleted,
					Paths.get(LE_CONFIG_ARCHIVE_PATH, domainName).toAbsolutePath().toString(), warningMessages);
		}

		// remove combined PEM file from haproxy certs directory
		Path combinedCert = getLESSLCertFilePath(domainName);
		try {
			Files.delete(combinedCert);
			haProxyProcessor.fetchDomainToCertFiles(null).remove(domainName);
		} catch (Exception e) {
			warningMessages.add(combinedCert.toString() + " could not be deleted: " + e.getMessage());
		}
	}

	private Path getLESSLCertFilePath(String domainName) {
		return Paths.get(haProxyProcessor.getCertPath(), domainName + ".pem");
	}

	private boolean renewLesslCerts(List<String> warningMessages) {

		try {
			StringBuilder output = new StringBuilder();
			StringBuilder errorOutput = new StringBuilder();
			int exitCode = exec(output, errorOutput, String.format(certbotRenewCertsCommand));
			if (exitCode != 0 || output.length() > 0 || errorOutput.length() > 0) {
				if (exitCode != 0 && output.length() == 0 && errorOutput.length() == 0) {
					errorOutput.append("exit code was " + exitCode);
				}
				warningMessages.add("Failed execution of certbot cert renewal: " + output.toString() + " "
						+ errorOutput.toString());
				return false;
			}
		} catch (IOException | InterruptedException e) {
			warningMessages.add("Exception, Failed execution of certbot cert renewal: " + e.getMessage());
			return false;
		}

		return true;
	}
}
