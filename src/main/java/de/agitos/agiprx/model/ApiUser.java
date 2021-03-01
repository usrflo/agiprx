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
package de.agitos.agiprx.model;

import java.util.Arrays;

public class ApiUser extends AbstractModel {

	private String username;

	private String password;

	private String email;

	private String[] agiPrxPermission;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String[] getAgiPrxPermission() {
		return agiPrxPermission;
	}

	public void setAgiPrxPermission(String[] agiPrxPermission) {
		this.agiPrxPermission = agiPrxPermission;
	}

	@Override
	public String toString() {
		return "ApiUser [id=" + id + ", username=" + username + ", email=" + email + ", agiPrxPerm="
				+ Arrays.toString(agiPrxPermission) + "]";
	}
}
