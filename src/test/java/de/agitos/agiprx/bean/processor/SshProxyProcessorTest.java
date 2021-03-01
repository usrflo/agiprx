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
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.agitos.agiprx.bean.processor.SshProxyProcessor.SshProxyUser;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.util.Assert;

// Run this test in isolated environment, e.g. inside container
// hint: this test needs to be executed with 'useradd/userdel' permissions
@Ignore
public class SshProxyProcessorTest {

	private static final String TEST_ROOT_DIR = "/tmp/agiprx-home-test/";

	private SshProxyProcessor sshProxyProcessor;

	@Before
	public void setup() {
		sshProxyProcessor = SshProxyProcessor.getBean();
		sshProxyProcessor.homeRootDirectory = TEST_ROOT_DIR;
		new File(sshProxyProcessor.homeRootDirectory).mkdirs();
	}

	@Test
	public void checkDomainTest() throws IOException, InterruptedException, AbortionException {

		new File(sshProxyProcessor.homeRootDirectory + "aprxkeeptest").mkdir();

		SshProxyUser user1 = sshProxyProcessor.new SshProxyUser("aprxkeep-this_user1", null, "user1", null);
		sshProxyProcessor.createUser(user1);

		SshProxyUser user2 = sshProxyProcessor.new SshProxyUser("aprxremove-this_user2", null, "user2", null);
		sshProxyProcessor.createUser(user2);

		Set<String> protectUsernames = new HashSet<String>();
		protectUsernames.add("aprxkeeptest");
		protectUsernames.add("aprxkeep-this_userdir");

		sshProxyProcessor.cleanupProxyUsers(protectUsernames);

		Assert.isTrue(sshProxyProcessor.userExists("aprxkeep-this_user1"),
				"user 'aprxkeep-this_user1' needs to exist / be protected from cleanup");
		Assert.isTrue(!sshProxyProcessor.userExists("aprxremove-this_user2"),
				"user 'aprxremove-this_user2' needs to be removed by cleanup");
		Assert.isTrue(new File(sshProxyProcessor.homeRootDirectory + "aprxkeeptest").exists(),
				"dir 'aprxkeeptest' needs to exist after cleanup");
	}
}
