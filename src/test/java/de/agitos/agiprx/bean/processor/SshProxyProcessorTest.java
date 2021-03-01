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
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.agitos.agiprx.exception.AbortionException;

@Ignore
public class SshProxyProcessorTest {

	private SshProxyProcessor sshProxyProcessor;

	@Before
	public void setup() {
		this.sshProxyProcessor = SshProxyProcessor.getBean();
	}

	@Test
	public void checkDomainTest() throws IOException, InterruptedException, AbortionException {

		Set<String> protectUsernames = new HashSet<String>();
		protectUsernames.add("sager");
		protectUsernames.add("fxworld");
		protectUsernames.add("keb");
		protectUsernames.add("keb3");
		protectUsernames.add("kebcms");
		protectUsernames.add("test");

		sshProxyProcessor.cleanupProxyUsers(protectUsernames);

		// Assert.isTrue(warningMessages.isEmpty(), "There should not be warnings,
		// instead: " + StringUtils
		// .arrayToDelimitedString(warningMessages.toArray(new
		// String[warningMessages.size()]), "; "));
	}
}
