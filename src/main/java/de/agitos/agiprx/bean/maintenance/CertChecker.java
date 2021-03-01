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
package de.agitos.agiprx.bean.maintenance;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.bean.processor.HAProxyProcessor.CertInfo;
import de.agitos.agiprx.util.Assert;

/**
 * This maintenance job informs about the expiration of certificates within
 * eolnotificationdays. It should be executed daily indirectly by
 * MainMaintenanceBean/MaintenanceTool.
 */

public class CertChecker implements DependencyInjector {

	private static CertChecker BEAN;

	private final static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	{
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	private HAProxyProcessor haProxyProcessor;

	// @Value("${cert.eolnotificationdays:14}")
	private Integer certEolNotificationDays;

	public CertChecker() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		certEolNotificationDays = Config.getBean().getInteger("cert.eolnotificationdays", 14);
	}

	@Override
	public void postConstruct() {
		haProxyProcessor = HAProxyProcessor.getBean();
	}

	public static CertChecker getBean() {
		return BEAN;
	}

	public void run(List<String> warningMessages, Set<String> domainsInErrorState) {

		Set<String> productiveDomainsWithPseudoWildcards = getProductiveDomainsWithPseudoWildcards();

		// reset domain to cert files to re-check available certificates with the
		// following fetch
		haProxyProcessor.resetDomainToCertFiles();

		for (Entry<String, CertInfo> domainToCert : haProxyProcessor.fetchDomainToCertFiles(warningMessages)
				.entrySet()) {
			if (domainsInErrorState.contains(domainToCert.getKey())) {
				continue;
			}

			// check cert lifetime
			CertInfo certInfo = domainToCert.getValue();
			if (!certInfo.isValidInDays(certEolNotificationDays)) {
				StringBuilder buf = new StringBuilder();

				if (productiveDomainsWithPseudoWildcards != null
						&& !isCertificateInUse(certInfo, productiveDomainsWithPseudoWildcards, domainToCert.getKey())) {
					// skip output of message if certificate is not in use
					continue;
				}

				if (certInfo.isLesslCert()) {
					buf.append("INFO: ");
				} else {
					buf.append("WARN: ");
				}

				buf.append("Certificate ").append(certInfo.getFilename()).append(" ends on ")
						.append(sdf.format(certInfo.getCert().getNotAfter())).append(".");

				try {
					buf.append(" It covers the domain names ")
							.append(String.join(", ", certInfo.getCoveredDomainnames()));
				} catch (CertificateParsingException e) {
					buf.append(" It covers at least the domain name ").append(domainToCert.getKey());
				}

				warningMessages.add(buf.toString());
			}
		}

	}

	/**
	 * Creates a set of domains that include pseudo wildcard domains, e.g. for a
	 * productive domain www.example.org it contains *.example.org. The pseudo
	 * wildcard domains facilitate a matching against wildcard certificates.
	 * 
	 * @return set of productive domains with pseudo wildcard domains
	 */
	private Set<String> getProductiveDomainsWithPseudoWildcards() {

		try {

			Set<String> productiveDomainsWithPseudoWildcards = new HashSet<String>();

			for (String domainname : haProxyProcessor.getProductiveDomains()) {

				productiveDomainsWithPseudoWildcards.add(domainname);
				int pointPos = domainname.indexOf(".");
				if (pointPos >= 0) {
					productiveDomainsWithPseudoWildcards.add("*" + domainname.substring(pointPos));
				}
			}

			return productiveDomainsWithPseudoWildcards;

		} catch (IOException e) {
			// if productive domains cannot be determined return null as "unknown"
			return null;
		}

	}

	/**
	 * Check if a certificate with its covered domain names is currently in use by a
	 * productive domain. Wildcart certificates are indirectly supported by a
	 * matching against a set of domains that include pseudo wildcard domains.
	 * 
	 * @param certInfo
	 * @param productiveDomainsWithPseudoWildcards
	 * @param fallbackToDomain
	 * @return
	 */
	private boolean isCertificateInUse(CertInfo certInfo, Set<String> productiveDomainsWithPseudoWildcards,
			String fallbackToDomain) {

		try {
			for (String coveredDomain : certInfo.getCoveredDomainnames()) {
				if (productiveDomainsWithPseudoWildcards.contains(coveredDomain)) {
					return true;
				}
			}
		} catch (CertificateParsingException e) {
			return productiveDomainsWithPseudoWildcards.contains(fallbackToDomain);
		}

		return false;
	}
}
