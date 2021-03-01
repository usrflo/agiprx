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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

import de.agitos.agiprx.AbstractTool;
import de.agitos.agiprx.ConfigTool;
import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.util.EmailSender;
import jline.console.ConsoleReader;

public class ConfigToolShellFactory implements ShellFactory {

	@Override
	public Command createShell(ChannelSession channel) throws IOException {
		return new ConfigToolShell();
	}

	private class ConfigToolShell implements Command, Runnable {

		private static final String SHELL_THREAD_NAME = "ConfigToolShell";

		private InputStream in;
		private OutputStream out;
		private OutputStream err;
		private ExitCallback callback;
		private Environment environment;
		private Thread thread;
		private ConsoleReader reader;
		private SshTerminal sshTerminal;

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

			environment = env;
			// add first environment variable from ssh authorized_keys file to the session
			// environment; this should be AbstractTool.USER_ID and the according user ID

			Long userId = SshConsoleUtil.getUserIdFromChannel(channel);

			environment.getEnv().put(AbstractTool.USER_ID, userId + "");

			thread = new Thread(this, SHELL_THREAD_NAME);
			thread.start();
		}

		@Override
		public void run() {
			try {

				FilterOutputStream filteredOut = new FilterOutputStream(out) {
					public void write(final int i) throws IOException {
						super.write(i);

						// workaround: reset line after CR
						if (i == ConsoleReader.CR.toCharArray()[0]) {
							super.write(ConsoleReader.RESET_LINE);
						}
					}
				};

				this.sshTerminal = new SshTerminal();
				this.sshTerminal.init();

				this.reader = new ConsoleReader(in, filteredOut, sshTerminal);
				// reader.setHandleUserInterrupt(true);

				this.environment.addSignalListener(new SignalListener() {

					@Override
					public void signal(Channel channel, Signal signal) {
						if (signal == org.apache.sshd.server.Signal.WINCH) {
							try {
								ConfigToolShell.this.sshTerminal.resetTerminalSize(ConfigToolShell.this.environment);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						// TODO: implement special interruption signal handling
						// see Apache Karaf, org.apache.karaf.shell.ssh.* / SshTerminal for a Jline3
						// implementation

//						if (signal == org.apache.sshd.server.Signal.INT) {
//							raise(Signal.INT);
//						} else if (signal == org.apache.sshd.server.Signal.QUIT) {
//							raise(Signal.QUIT);
//						} else if (signal == org.apache.sshd.server.Signal.TSTP) {
//							raise(Signal.TSTP);
//						} else if (signal == org.apache.sshd.server.Signal.CONT) {
//							raise(Signal.CONT);
//						}						
					}
				});
				this.sshTerminal.resetTerminalSize(this.environment);

				new ConfigTool(environment.getEnv().get(AbstractTool.USER_ID), reader, new PrintStream(filteredOut),
						sshTerminal);

			} catch (InterruptedIOException e) {

				// Ignore

			} catch (Exception e) {

				new PrintStream(err)
						.println(ConsoleWrapper.ANSI_RED + "An error occured, the administrator will be informed: "
								+ e.getMessage() + ConsoleWrapper.ANSI_RESET);

				try {
					err.flush();
				} catch (IOException e1) {
					// ignore
				}

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);

				EmailSender.getBean().sendMailToAdmin("AgiPrx error", sw.toString());

				throw new RuntimeException(e);

			} finally {

				callback.onExit(0);

			}
		}

		@Override
		public void destroy(ChannelSession channel) throws Exception {
			if (reader != null) {
				reader.shutdown();
			}
			thread.interrupt();
		}
	}
}
