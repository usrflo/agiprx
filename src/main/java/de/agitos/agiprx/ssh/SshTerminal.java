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

import org.apache.sshd.server.Environment;

import jline.TerminalSupport;

public class SshTerminal extends TerminalSupport {

	private int columns = DEFAULT_WIDTH;
	private int lines = DEFAULT_HEIGHT;

	public SshTerminal() throws Exception {
		super(true);
	}

	public void resetTerminalSize(Environment environment) throws IOException, InterruptedException {
		this.columns = Integer.valueOf(environment.getEnv().get(Environment.ENV_COLUMNS));
		this.lines = Integer.valueOf(environment.getEnv().get(Environment.ENV_LINES));

		// this.getSettings().set(String.format("columns %d rows %d", w, h));
	}

	/**
	 * Remove line-buffered input by invoking "stty -icanon min 1" against the
	 * current terminal.
	 */
	@Override
	public void init() throws Exception {
		super.init();

		setAnsiSupported(true);
		setEchoEnabled(false);
	}

	@Override
	public int getWidth() {
		return columns < 1 ? DEFAULT_WIDTH : columns;
	}

	@Override
	public int getHeight() {
		return lines < 1 ? DEFAULT_HEIGHT : lines;
	}
}
