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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;
import org.apache.sshd.server.shell.UnknownCommand;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.bean.maintenance.MainMaintenanceBean;
import de.agitos.agiprx.dao.UserDao;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.model.UserRoleType;

public class ConfigToolCommandFactory implements CommandFactory {

	@Override
	public Command createCommand(ChannelSession channel, String command) throws IOException {

		if ("maintenance".equals(command)) {
			return new ConfigToolMaintenanceCommand();
		}

		return new UnknownCommand(command);
	}

	private class ConfigToolMaintenanceCommand implements Command {

		private InputStream in;
		private OutputStream out;
		private OutputStream err;
		private ExitCallback callback;

		@Override
		public void setInputStream(InputStream in) {
			this.in = in;
		}

		@Override
		public void setOutputStream(OutputStream out) {
			this.out = out;
		}

		@Override
		public void setErrorStream(OutputStream err) {
			this.err = err;
		}

		@Override
		public void setExitCallback(ExitCallback callback) {
			this.callback = callback;
		}

		@Override
		public void start(ChannelSession channel, Environment env) throws IOException {

			try {

				Long userId = SshConsoleUtil.getUserIdFromChannel(channel);

				User user = UserDao.getBean().find(userId);

				if (UserRoleType.ADMIN != user.getRole()) {
					new PrintStream(err).println(ConsoleWrapper.ANSI_RED + "Insufficient permissions to run commands"
							+ ConsoleWrapper.ANSI_RESET);
					return;
				}

				// run maintenance job
				MainMaintenanceBean.getBean().runScheduled();

			} finally {
				callback.onExit(0);
			}
		}

		@Override
		public void destroy(ChannelSession channel) throws Exception {
		}

	}

}
