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

import java.io.PrintStream;

import de.agitos.agiprx.executor.MainExecutor;
import de.agitos.agiprx.ssh.SshTerminal;
import de.agitos.agiprx.util.UserContext;
import jline.console.ConsoleReader;

public class ConfigTool extends AbstractTool {

	public ConfigTool(String userIdString, ConsoleReader in, PrintStream out, SshTerminal sshTerminal)
			throws Exception {

		super();

		Long userId = null;
		try {
			userId = Long.valueOf(userIdString);
		} catch (NumberFormatException nfe) {
			throw new Exception("The given id in " + AbstractTool.USER_ID + "=<ID> is not a number", nfe);
		}

		UserContext userContext = UserContext.getBean();
		userContext.registerUser(userId);

		ConsoleWrapper consoleWrapper = ConsoleWrapper.getBean();
		consoleWrapper.registerClient(in, out, true, sshTerminal);

		try {
			MainExecutor.getBean().run();
		} finally {
			consoleWrapper.unRegisterClient();
			userContext.unregister();
		}
	}
}
