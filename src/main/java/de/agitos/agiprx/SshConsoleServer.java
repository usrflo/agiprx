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
package de.agitos.agiprx;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.config.keys.DefaultAuthorizedKeysAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.ssh.ConfigToolCommandFactory;
import de.agitos.agiprx.ssh.ConfigToolShellFactory;

public class SshConsoleServer {

	private static SshConsoleServer BEAN;

	private ConsoleWrapper console;

	// @Value("${agiprx.user:agiprx}")
	private String agiPrxUser;

	// @Value("${agiprx.port:2223}")
	private int agiPrxPort;

	// @Value("${agiprx.authorizedAccessKeys:/opt/agiprx/.ssh/authorized_keys}")
	private String authorizedKeysFullpath;

	// @Value("${agiprx.hostKeys:/opt/agiprx/.ssh/hostkey.ser}")
	private String hostKeys;

	public static SshConsoleServer getBean() {
		if (BEAN == null) {
			BEAN = new SshConsoleServer();

		}
		return BEAN;
	}

	private SshConsoleServer() {

		Config config = Config.getBean();

		agiPrxUser = config.getString("agiprx.user", "agiprx");
		agiPrxPort = config.getInteger("agiprx.port", 2223);
		authorizedKeysFullpath = config.getString("agiprx.authorizedAccessKeys", "/opt/agiprx/.ssh/authorized_keys");
		hostKeys = config.getString("agiprx.hostKeys", "/opt/agiprx/.ssh/hostkey.ser");

		console = ConsoleWrapper.getBean();
	}

	public void runServer() throws IOException {

		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setPort(agiPrxPort);
		sshd.setPublickeyAuthenticator(
				new DefaultAuthorizedKeysAuthenticator(agiPrxUser, Paths.get(authorizedKeysFullpath), false));
		SimpleGeneratorHostKeyProvider keyPairProvider = new SimpleGeneratorHostKeyProvider(Paths.get(hostKeys));
		keyPairProvider.setAlgorithm(org.apache.sshd.common.config.keys.KeyUtils.EC_ALGORITHM);
		sshd.setKeyPairProvider(keyPairProvider);
		sshd.setShellFactory(new ConfigToolShellFactory());
		sshd.setCommandFactory(new ConfigToolCommandFactory());
		sshd.start();

		console.printlnf("Started SSH daemon for user access at port %d", agiPrxPort);
	}

}
