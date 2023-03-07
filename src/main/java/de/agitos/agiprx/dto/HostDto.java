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
package de.agitos.agiprx.dto;

import javax.json.bind.annotation.JsonbTransient;

import de.agitos.agiprx.model.Host;

public class HostDto {

	@JsonbTransient
	private Host host;

	public HostDto(Host host) {
		this.host = host;
	}

	public String getHostname() {
		return host.getHostname();
	}

	public String getIpv6() {
		return host.getIpv6();
	}

	public Integer getVersion() {
		return host.getVersion();
	}
}
