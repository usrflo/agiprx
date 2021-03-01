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
package de.agitos.agiprx.dns;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.util.Assert;

/**
 * This maintenance job informs about a mismatch of configured domain IPs and
 * the proxy IP. It should by executed daily indirectly by
 * MainMaintenanceBean/MaintenanceTool.
 */

public class DomainIpChecker implements DependencyInjector {

	private static DomainIpChecker BEAN;

	private HAProxyProcessor haProxyProcessor;

	protected Set<String> trustedIpSet;

	protected String nameServer;

	private DnsClient dnsClient = null;

	public DomainIpChecker() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		trustedIpSet = Config.getBean().getStringSet("domain.trustedIps", Config.DEFAULT_LIST_SPLIT_EXP);
		nameServer = Config.getBean().getString("proxy.nameServer", "localhost");
	}

	@Override
	public void postConstruct() {
		haProxyProcessor = HAProxyProcessor.getBean();
	}

	public static DomainIpChecker getBean() {
		return BEAN;
	}

	public void run(List<String> warningMessages, Set<String> domainsInErrorState) {

		try {

			// check if productive domains are still configured
			for (String domainName : haProxyProcessor.getProductiveDomains()) {
				if (!checkDomain(warningMessages, domainName)) {
					domainsInErrorState.add(domainName);
				}
			}

		} catch (IOException e) {
			warningMessages.add("Error reading productive domains from HAProxy configuration: " + e.getMessage());
		}

	}

	public boolean checkDomain(List<String> warningMessages, String domainName) {
		try {

			if (trustedIpSet != null) {

				if (dnsClient == null) {
					dnsClient = new DnsClient(new String[] { nameServer }, 2000, 3);
				}

				String ipv4 = dnsLookupIpDirect(domainName, ResourceRecord.TYPE_A);
				if (ipv4 == null) {
					warningMessages.add("Domain " + domainName + " does not resolve to an IPv4 address");
				}

				String ipv6 = dnsLookupIpDirect(domainName, ResourceRecord.TYPE_AAAA);

				if (!trustedIpSet.contains(ipv4) && !trustedIpSet.contains(ipv6)) {

					StringBuilder buf = new StringBuilder();
					buf.append("Domain ").append(domainName).append(" resolves to ");
					if (ipv4 != null) {
						buf.append("IPv4 ").append(ipv4);
					}
					if (ipv6 != null) {
						buf.append("IPv6 ").append(ipv6);
					}
					buf.append(" instead of trusted IP(s) ");
					buf.append(stringSetToString(trustedIpSet));

					warningMessages.add(buf.toString());
					return false;
				}
			}

		} catch (NamingException ne) {
			warningMessages.add("Exception resolving IPs by nameserver " + nameServer + " on domain " + domainName
					+ ": " + ne.getMessage());
			return false;
		}
		return true;
	}

	public String dnsLookupIpDirect(String domainName, int type) throws NamingException {

		if (dnsClient == null) {
			dnsClient = new DnsClient(new String[] { nameServer }, 2000, 3);
		}

		ResourceRecords records = dnsClient.query(new DnsName(domainName), ResourceRecord.CLASS_INTERNET, type, true,
				false);

		if (records.getLastAnsType() != type) {
			return null;
		}

		return records.answer.lastElement().getRdata().toString();
	}

	private String stringSetToString(Set<String> set) {
		StringBuilder buf = new StringBuilder();
		if (set.size() == 0) {
			return "";
		}
		for (String s : set) {
			buf.append(s).append(", ");
		}
		return buf.substring(0, buf.length() - 2);
	}
}
