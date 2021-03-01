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

public class AbstractTool {

	public static final String USER_ID = "AGIPRX_USER_ID";

	protected ConsoleWrapper console;

	public AbstractTool() {
		console = ConsoleWrapper.getBean();
	}

	/*
	 * public static Long getUserId(String[] args) throws Exception { if
	 * (args.length == 0 || !args[0].startsWith(USER_ID+"=")) {
	 * System.out.println("First parameter "+USER_ID+"=<ID> needs to be set"); throw
	 * new RuntimeException("First parameter "+USER_ID+"=<ID> needs to be set"); }
	 * 
	 * try { return Long.valueOf(args[0].substring((USER_ID+"=").length())); } catch
	 * (NumberFormatException nfe) {
	 * System.out.println("The given id in "+USER_ID+"=<ID> is not a number"); throw
	 * new RuntimeException("The given id in "+USER_ID+"=<ID> is not a number"); } }
	 */
}
