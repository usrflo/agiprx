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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.util.Assert;

@Ignore
public class DomainIpCheckerTest {

	private DomainIpChecker domainIpChecker;

	@Before
	public void setup() {

		de.agitos.agiprx.AgiPrx.agiPrxRootDirectory = "src/test/resources";
		new Config();
		this.domainIpChecker = new DomainIpChecker();
	}

	@Test
	public void checkBaseDomainTest() {

		List<String> warningMessages = new ArrayList<String>();

		domainIpChecker.checkDomain(warningMessages, "agibase.agitos.de");

		Assert.isTrue(warningMessages.isEmpty(), "There should not be warnings, instead: "
				+ arrayToDelimitedString(warningMessages.toArray(new String[warningMessages.size()]), "; "));
	}

	@Test
	public void dnsLookupIpDirectTest() throws Exception {

		domainIpChecker.dnsLookupIpDirect("agibase.agitos.de", ResourceRecord.TYPE_A);

	}

	private String arrayToDelimitedString(String[] arr, String delim) {
		if (arr == null || arr.length == 0) {
			return "";
		}
		if (arr.length == 1) {
			return arr[0] + delim;
		}

		StringBuilder buf = new StringBuilder();
		for (String o : arr) {
			buf.append(o).append(delim);
		}
		return buf.toString();
	}
}
