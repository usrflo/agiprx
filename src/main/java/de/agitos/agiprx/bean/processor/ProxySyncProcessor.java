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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.util.Assert;

public class ProxySyncProcessor extends AbstractProcessor implements DependencyInjector {

	private static ProxySyncProcessor BEAN;

	protected ConsoleWrapper console;

	private final boolean isMasterInstance;

	private final List<String> slaveIpList;

	private final String masterIp;

	private final String slaveSyncCommand;

	public ProxySyncProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		isMasterInstance = Config.getBean().getBoolean("agiprx.masterinstance", Boolean.TRUE);

		slaveIpList = Config.getBean().getStringList("agiprx.slaveIpList", Config.DEFAULT_LIST_SPLIT_EXP);

		masterIp = Config.getBean().getString("agiprx.masterIp");

		slaveSyncCommand = Config.getBean().getString("agiprx.slaveSyncCommand");
	}

	@Override
	public void postConstruct() {
		console = ConsoleWrapper.getBean();
	}

	public static ProxySyncProcessor getBean() {
		return BEAN;
	}

	public boolean isMasterInstance() {
		return isMasterInstance;
	}

	public String getMasterIp() {
		return masterIp;
	}

	public boolean isSyncRequired() {
		return isMasterInstance && slaveIpList != null && slaveIpList.size() > 0;
	}

	public void syncToSlaveInstances(boolean verbose, List<String> warningMessages) {

		if (!isSyncRequired()) {
			// nothing to synchronize
			return;
		}

		Assert.isTrue(verbose || warningMessages != null,
				"Use either verbose output or return warning messages by List<String>");

		ExecutorService executorService = Executors.newCachedThreadPool();
		List<Callable<List<String>>> tasks = new ArrayList<Callable<List<String>>>();
		for (String slaveIp : slaveIpList) {

			if (verbose) {
				console.printlnfStress("Sync to %s", slaveIp);
			}

			Callable<List<String>> c = new Callable<List<String>>() {
				@Override
				public List<String> call() throws Exception {
					return syncToSlaveInstance(slaveIp, verbose);
				}
			};
			tasks.add(c);
		}
		try {
			List<Future<List<String>>> threadResults = executorService.invokeAll(tasks);

			for (Future<List<String>> threadResult : threadResults) {
				if (verbose) {
					for (String warning : threadResult.get()) {
						console.printlnfError(warning);
					}
				} else {
					warningMessages.addAll(threadResult.get());
				}
			}

		} catch (ExecutionException | InterruptedException e) {
			if (verbose) {
				console.printlnfError("sync to slave instances failed, " + e.getMessage());
			} else {
				warningMessages.add("sync to slave instances failed, " + e.getMessage());
			}
		}

		if (verbose) {
			console.printlnfStress("Updated %d slave instance(s)", slaveIpList.size());
		}

	}

	private List<String> syncToSlaveInstance(String slaveIp, boolean verbose) {

		List<String> warningMessages = new ArrayList<String>();

		try {

			/*
			 * The slaveSyncCommand needs to (a) sync /etc/letsencrypt, /etc/haproxy and
			 * database to target/slave server (b) restart agiprx on target server (with
			 * implicit config reprocessing)
			 */

			StringBuilder output = new StringBuilder();
			StringBuilder errorOutput = new StringBuilder();
			exec(output, errorOutput, new String[] { slaveSyncCommand, slaveIp });

			if (verbose) {
				console.printf("%s", output.toString());
			}
			if (errorOutput.length() > 0) {
				if (verbose) {
					console.printlnfError("%s", errorOutput.toString());
				} else {
					warningMessages.add("Sync to " + slaveIp + " : " + errorOutput.toString());
				}
			}

		} catch (IOException | InterruptedException e) {
			String errorMsg = "Sync to " + slaveIp + " failed: " + e.getMessage();
			if (verbose) {
				console.printlnfError(errorMsg);
			} else {
				warningMessages.add(errorMsg);
			}
		}

		return warningMessages;
	}

}
