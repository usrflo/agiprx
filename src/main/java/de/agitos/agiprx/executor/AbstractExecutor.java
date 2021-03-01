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
package de.agitos.agiprx.executor;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.db.exception.DuplicateKeyException;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.exception.ExitException;
import de.agitos.agiprx.exception.GotoTopException;
import de.agitos.agiprx.util.Validator;

public abstract class AbstractExecutor implements DependencyInjector {

	private static final String HELP_FORMAT = "\t%s%s%s : %s";

	protected static final String CMD_HELP = "_help";
	protected static final String CMD_TOP = "_top";
	protected static final String CMD_ABORT = "abort";
	protected static final String CMD_CANCEL = "cancel";
	protected static final String CMD_DIRUP = "_<";
	protected static final String CMD_CDUP = "cd ..";
	protected static final String CMD_EXIT = "exit";
	protected static final String CMD_QUIT = "_quit";
	protected static final String CMD_HOSTS = "h_osts";
	protected static final String CMD_USERS = "use_rs";
	protected static final String CMD_USER = "use_r";
	protected static final String CMD_APIUSERS = "_api-users";
	protected static final String CMD_PROJECTS = "_projects";
	protected static final String CMD_PROJECT = "_project";
	protected static final String CMD_CERTS = "certs";
	protected static final String CMD_GENHAPRX = "_genhaprx";
	protected static final String CMD_WRITESSHPRX = "_writesshprx";
	protected static final String CMD_SYNCSLAVES = "s_yncslaves";
	protected static final String CMD_LS = "_ls";
	protected static final String CMD_FIND = "_find";
	protected static final String CMD_ADD = "_add";
	protected static final String CMD_USE = "_use";
	protected static final String CMD_SHOW = "_show";
	protected static final String CMD_EDIT = "_edit";
	protected static final String CMD_DEL = "_del";
	protected static final String CMD_JUMP = "_jump";
	protected static final String CMD_CONTAINERS = "_containers";
	protected static final String CMD_CONTAINER = "_container";
	protected static final String CMD_BACKENDS = "_backends";
	protected static final String CMD_BACKEND = "_backend";
	protected static final String CMD_PERMISSIONS = "per_missions";
	protected static final String CMD_DOMAINS = "domai_ns";
	protected static final String CMD_DOMAIN = "domai_n";
	protected static final String CMD_GROUPADD = "groupadd";
	protected static final String CMD_INFORMALL = "informall";
	protected static final String CMD_INFORM = "_inform";
	protected static final String CMD_POSTGENLESSLCERTS = "_postgenlesslcerts";
	// protected static final String CMD_ = "";

	protected ConsoleWrapper console;

	protected Validator validator;

	@Override
	public void postConstruct() {
		console = ConsoleWrapper.getBean();
		validator = Validator.getBean();
	}

	protected boolean isYes(String s) {
		if ("y".equals(s) || "yes".equals(s)) {
			return true;
		}
		return false;
	}

	protected boolean isNo(String s) {
		if ("n".equals(s) || "no".equals(s)) {
			return true;
		}
		return false;
	}

	protected boolean isEdit(String s) {
		if ("e".equals(s) || "edit".equals(s)) {
			return true;
		}
		return false;
	}

	protected void handleExitOrAbort(String s) throws Exception {
		if (isCommand(CMD_TOP, s)) {
			throw new GotoTopException();
		} else if (isCommand(CMD_ABORT, s) || isCommand(CMD_CANCEL, s)) {
			console.printlnfStress("Operation canceled");
			throw new AbortionException();
		} else if (isCommand(CMD_DIRUP, s) || isCommand(CMD_CDUP, s)) {
			throw new AbortionException();
		} else if (isCommand(CMD_EXIT, s) || isCommand(CMD_QUIT, s)) {
			throw new ExitException();
		}
	}

	protected boolean isCommand(String command, String input) {

		if (input == null) {
			return false;
		}

		String realCommand = command;

		int shortcutPosition = command.indexOf("_");

		if (shortcutPosition >= 0) {

			realCommand = command.substring(0, shortcutPosition) + command.substring(shortcutPosition + 1);

			if (input.length() == 1 && input.equals(realCommand.substring(shortcutPosition, shortcutPosition + 1))) {
				return true;
			}
		}

		return input.regionMatches(true, 0, realCommand, 0, realCommand.length());
	}

	protected boolean isCommandWithParam(String command, String input) {

		if (input == null) {
			return false;
		}

		String realCommand = command;

		int shortcutPosition = command.indexOf("_");

		if (shortcutPosition >= 0) {

			realCommand = command.substring(0, shortcutPosition) + command.substring(shortcutPosition + 1);

			String cmpShort = realCommand.substring(shortcutPosition, shortcutPosition + 1) + " ";

			if (input.regionMatches(true, 0, cmpShort, 0, cmpShort.length())) {
				return true;
			}
		}

		String cmpLong = realCommand + " ";

		return input.regionMatches(true, 0, cmpLong, 0, cmpLong.length());
	}

	// gets str from <command> <str>
	protected String getStringParam(String s) {

		String[] parts = s.split("\\s", 2);

		if (parts.length < 2) {
			console.printlnfError("No parameter provided");
			return null;
		}

		return parts[1];
	}

	// gets ID from <command> <id>
	protected Long getIdParam(String s) {

		String[] parts = s.split("\\s");

		if (parts.length < 2) {
			console.printlnfError("No ID parameter provided");
			return null;
		}

		return getId(parts[1]);
	}

	// gets ID from <id>
	protected Long getId(String s) {

		try {
			return Long.parseLong(s);
		} catch (NumberFormatException nfe) {
			console.printlnfError("Invalid ID parameter " + s);
			return null;
		}
	}

	protected Long getIdOrNull(String s) {

		try {
			return Long.parseLong(s);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	protected void printHelp(String command, String description) {
		String cmdPraefix = ConsoleWrapper.ANSI_CYAN;
		String cmdSuffix = ConsoleWrapper.ANSI_RESET;
		/*
		 * if (!console.isColorMode()) { cmdPraefix = "> "; cmdSuffix = ""; }
		 */
		console.printlnf(HELP_FORMAT, cmdPraefix, markShortcutInCommand(command), cmdSuffix, description);
	}

	private String markShortcutInCommand(String command) {
		int underlinePosition = command.indexOf('_');
		if (underlinePosition < 0) {
			return command;
		}

		return command.substring(0, underlinePosition) + ConsoleWrapper.BOLD_START
				+ command.substring(underlinePosition + 1, underlinePosition + 2) + ConsoleWrapper.BOLD_END
				+ command.substring(underlinePosition + 2);
	}

	protected void handleCaughtException(Exception e) {
		String msg = e.getMessage();
		if (e instanceof DuplicateKeyException) {
			if (e.getCause() != null) {
				msg = e.getCause().getMessage();
			}
		}
		console.printlnfError("Operation canceled: " + msg);
	}

	abstract protected void setCommandCompletion();
}
