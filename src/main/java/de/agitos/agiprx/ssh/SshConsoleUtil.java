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
package de.agitos.agiprx.ssh;

import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.server.channel.ChannelSession;

import de.agitos.agiprx.AbstractTool;

public class SshConsoleUtil {

	public static Long getUserIdFromChannel(ChannelSession channel) {

		AuthorizedKeyEntry authorizedKeyEntry = channel.getSession().getAttribute(
				org.apache.sshd.server.auth.pubkey.AuthorizedKeyEntriesPublickeyAuthenticator.AUTHORIZED_KEY);

		String userEnvironment = authorizedKeyEntry.getLoginOptions().get("environment");
		if (userEnvironment == null) {
			throw new RuntimeException("Authorized key is missing environment configuration for AgiPrx user ID");
		}
		String[] userEnvSplitted = userEnvironment.split("=");
		if (userEnvSplitted.length != 2) {
			throw new RuntimeException("Authorized key's environment configuration has an invalid format");
		}

		if (!AbstractTool.USER_ID.equals(userEnvSplitted[0])) {
			throw new RuntimeException("Parameters of authorized key's environment needs to be in format "
					+ AbstractTool.USER_ID + "=<ID>");
		}

		try {
			return Long.valueOf(userEnvSplitted[1]);
		} catch (NumberFormatException nfe) {
			throw new RuntimeException(
					"The " + AbstractTool.USER_ID + " in authorized key's environment is not a number");
		}
	}

}
