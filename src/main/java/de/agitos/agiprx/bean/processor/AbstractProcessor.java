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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

import de.agitos.agiprx.exception.AbortionException;

public class AbstractProcessor {

	protected int exec(String... commandArray) throws IOException, InterruptedException {
		// Process process = Runtime.getRuntime().exec(commandArray);
		Process process = Runtime.getRuntime().exec(String.join(" ", commandArray));
		return process.waitFor();
	}

	protected int execWithInput(String command, String input) throws IOException, InterruptedException {
		Process process = Runtime.getRuntime().exec(command);
		OutputStream out = process.getOutputStream();
		out.write(input.getBytes());
		out.close();
		return process.waitFor();
	}

	protected int exec(StringBuilder output, StringBuilder errorOutput, String... commandArray)
			throws IOException, InterruptedException {
		// Process process = Runtime.getRuntime().exec(commandArray);
		Process process = Runtime.getRuntime().exec(String.join(" ", commandArray));
		InputStream inputStream = process.getInputStream();
		InputStream errorStream = process.getErrorStream();
		int exitCode = process.waitFor();

		appendInputStreamToStringBuilder(inputStream, output);
		appendInputStreamToStringBuilder(errorStream, errorOutput);

		return exitCode;
	}

	private void appendInputStreamToStringBuilder(InputStream inputStream, StringBuilder out) throws IOException {
		Reader in = new InputStreamReader(inputStream, "UTF-8");

		char[] buffer = new char[1024];
		for (;;) {
			int rsz = in.read(buffer, 0, buffer.length);
			if (rsz < 0)
				break;
			out.append(buffer, 0, rsz);
		}
	}

	protected void exec(int expectedStatusCode, String... commandArray)
			throws IOException, InterruptedException, AbortionException {
		int statusCode = exec(commandArray);
		if (statusCode != expectedStatusCode) {
			throw new AbortionException(
					"Execution of '" + String.join(" ", commandArray) + "' failed with exit code " + statusCode);
		}
	}

	protected void exec(int expectedStatusCode, String[] removeStringsFromErrorMessage, String... commandArray)
			throws IOException, InterruptedException, AbortionException {
		int statusCode = exec(commandArray);
		if (statusCode != expectedStatusCode) {

			String cmdForOutput = String.join(" ", commandArray);
			for (String removeString : removeStringsFromErrorMessage) {
				cmdForOutput = cmdForOutput.replaceAll(removeString, "...");
			}

			throw new AbortionException("Execution of '" + cmdForOutput + "' failed with exit code " + statusCode);
		}
	}
}
