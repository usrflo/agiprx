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

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.util.Assert;

public class HAProxyLesslBackendProcessor extends AbstractProcessor implements DependencyInjector {

	private static HAProxyLesslBackendProcessor BEAN;

	private final boolean isMasterInstance;

	private String masterIp;

	public HAProxyLesslBackendProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		isMasterInstance = Config.getBean().getBoolean("agiprx.masterinstance", Boolean.TRUE);

		masterIp = Config.getBean().getString("agiprx.masterIp");

		Assert.isTrue(isMasterInstance || masterIp != null,
				"This instance is not configured as master by config property 'agiprx.masterinstance=true', so it's a slave instance. Slave instances need a master IP defined by config property 'agiprx.masterIp'.");
	}

	@Override
	public void postConstruct() {
	}

	public static HAProxyLesslBackendProcessor getBean() {
		return BEAN;
	}

	public void generateLesslBackend(StringBuilder buf) {

		// LESSL validation requests need to contact the master instance
		buf.append("backend letsencrypt-backend\n");
		buf.append("\tserver letsencrypt [").append(isMasterInstance ? "127.0.0.1" : masterIp).append("]:8001\n\n");
	}
}
