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

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import de.agitos.agiprx.exception.ExitException;
import de.agitos.agiprx.ssh.SshTerminal;
import de.agitos.agiprx.util.Assert;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import jline.internal.NonBlockingInputStream;

public class ConsoleWrapper implements DependencyInjector {

	private static ConsoleWrapper BEAN;

	private static final Logger LOG = Logger.getLogger(ConsoleWrapper.class.getName());

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public static final String BOLD_START = "\u001B[1m";
	public static final String UNDERLINE_START = "\u001B[4m";
	public static final String BOLD_END = "\u001B[22m";
	public static final String UNDERLINE_END = "\u001B[24m";

	public static final Completer defaultCompleter = new Completer() {
		@Override
		public boolean equals(Object obj) {
			return true;
		}

		@Override
		public int complete(String buffer, int cursor, List<CharSequence> candidates) {
			return 0;
		}
	};

	private Console console = null;
	private ThreadLocal<ConsoleReader> consoleReader = new ThreadLocal<>();
	private ThreadLocal<InputStreamReader> inputStreamReader = new ThreadLocal<>();
	private ThreadLocal<PrintStream> printStream = new ThreadLocal<>();
	private ThreadLocal<Boolean> asciiColors = new ThreadLocal<>();
	private ThreadLocal<SshTerminal> sshTerminal = new ThreadLocal<>();
	private ThreadLocal<AtomicInteger> nullInput = new ThreadLocal<>();
	private ThreadLocal<Queue<String>> inputBuffer = new ThreadLocal<>();

	private static final int NULL_INPUT_LOOP_LIMIT = 25;

	public ConsoleWrapper() {
		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
	}

	public static ConsoleWrapper getBean() {
		return BEAN;
	}

	public void registerClient(ConsoleReader consoleReader, PrintStream out, boolean asciiColors,
			SshTerminal sshTerminal) {
		this.consoleReader.set(consoleReader);
		this.printStream.set(out);
		this.asciiColors.set(asciiColors);
		this.sshTerminal.set(sshTerminal);
		this.nullInput.set(new AtomicInteger(0));
		this.inputBuffer.set(new LinkedList<String>());
	}

	public void registerClient(InputStream is, PrintStream out, boolean asciiColors) {
		this.inputStreamReader.set(new InputStreamReader(is));
		this.printStream.set(out);
		this.asciiColors.set(asciiColors);
		// this.sshTerminal.set(null);
		this.nullInput.set(new AtomicInteger(0));
		this.inputBuffer.set(new LinkedList<String>());
	}

	public void unRegisterClient() {
		consoleReader.remove();
		inputStreamReader.remove();
		printStream.remove();
		asciiColors.remove();
		if (sshTerminal.get() != null) {
			sshTerminal.remove();
		}
		nullInput.remove();
		inputBuffer.remove();
	}

	public String readLine() throws ExitException {
		return readLine(null, null);
	}

	public String readLine(String prompt) throws ExitException {
		return readLine(prompt, null);
	}

	public String readLine(String prompt, Object defaultValue) throws ExitException {
		return readLine(prompt, defaultValue, false);
	}

	public String readLine(String prompt, Object defaultValue, boolean readMultiline) throws ExitException {

		String bufferedInput = inputBuffer.get().poll();
		if (bufferedInput != null) {
			return bufferedInput;
		}

		String promptPraefix = ANSI_PURPLE;
		String promptSuffix = ANSI_RESET;

		if (!isColorMode()) {
			promptPraefix = "";
			promptSuffix = "";
		}

		String promptExtended;
		if (prompt == null) {
			promptExtended = promptPraefix + "> " + promptSuffix;
		} else if (defaultValue == null) {
			promptExtended = promptPraefix + prompt + " > " + promptSuffix;
		} else {
			promptExtended = promptPraefix + prompt + " (default " + defaultValue + ") > " + promptSuffix;
		}

		if (readMultiline) {

			if (consoleReader.get() != null) {

				try {

					StringBuilder result = new StringBuilder();

					int lineNumber = 1;
					String in;

					while (true) {
						if (lineNumber == 1) {
							in = consoleReader.get().readLine(promptExtended);
						} else {
							in = consoleReader.get()
									.readLine(promptPraefix + "line " + lineNumber + " > " + promptSuffix);
						}
						nullInputLoopDetection(in);
						if (in == null || in.length() == 0) {
							break;
						}
						result.append(in).append("\n");
						lineNumber++;
					}

					return result.toString();

				} catch (IOException e) {
					// if IO exception occurs the client connection is broken; exit to prevent
					// infinite loops
					throw new ExitException();
				}

			} else {

				printStream.get().print(promptExtended);

				Reader reader;

				if (this.console != null) {
					reader = this.console.reader();
				} else {
					reader = inputStreamReader.get();
				}
				try {
					BufferedReader bufferedReader = new BufferedReader(reader);

					StringBuilder result = new StringBuilder();
					while (true) {
						String in = bufferedReader.readLine();
						nullInputLoopDetection(in);
						if (in == null || in.length() == 0) {
							break;
						}
						result.append(in).append("\n");
					}

					return result.toString();

				} catch (IOException e) {
					// if IO exception occurs the client connection is broken; exit to prevent
					// infinite loops
					throw new ExitException();
				}
			}

		} else {
			if (consoleReader.get() != null) {

				try {
					String in = consoleReader.get().readLine(promptExtended);
					nullInputLoopDetection(in);
					return in;

				} catch (IOException e) {
					// if IO exception occurs the client connection is broken; exit to prevent
					// infinite loops
					throw new ExitException();
				}

			} else if (this.console != null) {
				return this.console.readLine(promptExtended);
			} else {
				printStream.get().print(promptExtended);

				BufferedReader bufferedReader = new BufferedReader(inputStreamReader.get());
				try {
					String in = bufferedReader.readLine();
					nullInputLoopDetection(in);
					return in;
				} catch (IOException e) {
					// if IO exception occurs the client connection is broken; exit to prevent
					// infinite loops
					throw new ExitException();
				}
			}
		}
	}

	private void nullInputLoopDetection(String in) throws ExitException {
		if (in == null || in.length() == 0) {
			if (nullInput.get().incrementAndGet() > NULL_INPUT_LOOP_LIMIT) {
				throw new ExitException();
			}
		} else {
			nullInput.get().set(0);
		}
	}

	public int readCharNonBlocking(int timeout) throws IOException {
		if (consoleReader.get() == null) {
			return -1;
		}

		// consoleReader.get().getInput()).peek(timeout);
		return ((NonBlockingInputStream) consoleReader.get().getInput()).read(timeout);
	}

	public void printlnf(String fmt, Object... s) {
		printf(false, false, fmt + "\n", s);
	}

	public void printlnfStress(String fmt, Object... s) {
		printf(true, false, fmt + "\n", s);
	}

	public void printlnfError(String fmt, Object... s) {
		printf(false, true, fmt + "\n", s);
	}

	public void printf(String fmt, Object... s) {
		printf(false, false, fmt, s);
	}

	public void printfStress(String fmt, Object... s) {
		printf(true, false, fmt, s);
	}

	private void printf(boolean highlight, boolean error, String fmt, Object... s) {
		if (this.console != null) {
			if (highlight) {
				this.console.printf(ANSI_GREEN);
			} else if (error) {
				this.console.printf(ANSI_RED);
			}
			this.console.printf(fmt, s);
			if (highlight || error) {
				this.console.printf(ANSI_RESET);
			}
		} else if (printStream.get() != null) {

			boolean colored = isColorMode();

			if (highlight) {
				printStream.get().print(colored ? ANSI_GREEN : "+ ");
			} else if (error) {
				printStream.get().print(colored ? ANSI_RED : "- ");
			}
			printStream.get().print(String.format(fmt, s));
			if (colored && (highlight || error)) {
				printStream.get().print(ANSI_RESET);
			}
			printStream.get().flush();
		} else {
			LOG.info("Last-Out: " + String.format(fmt, s));
		}
	}

	public boolean isColorMode() {
		return asciiColors.get();
	}

	public int getTerminalColumns() {
		if (sshTerminal.get() != null) {
			return sshTerminal.get().getWidth();
		}
		return SshTerminal.DEFAULT_WIDTH;
	}

	public void addBufferedInput(String input) {
		inputBuffer.get().add(input);
	}

	public Collection<Completer> setCommandCompletion(String... cmd) {

		if (consoleReader.get() != null) {

			if (cmd == null) {

				setCommandCompletion(defaultCompleter);

			} else {

				String cmdNormalized[] = new String[cmd.length];
				// remove underscore from commands
				for (int i = 0; i < cmd.length; i++) {
					cmdNormalized[i] = cmd[i].replace("_", "");
				}

				return setCommandCompletion(new StringsCompleter(cmdNormalized) {
					@Override
					public boolean equals(Object obj) {
						return true;
					}
				});
			}
		}

		return null;
	}

	public Collection<Completer> setCommandCompletion(Completer completer) {

		if (consoleReader.get() != null) {

			Collection<Completer> lastCompleters = consoleReader.get().getCompleters();

			consoleReader.get().removeCompleter(defaultCompleter);

			consoleReader.get().addCompleter(completer);

			return lastCompleters;
		}

		return null;
	}

	public void flush() {
		if (this.console != null) {
			this.console.flush();
		} else {
			printStream.get().flush();
		}
	}
}
