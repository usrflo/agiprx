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

import de.agitos.agiprx.model.User;
import de.agitos.agiprx.model.UserRoleType;

public class UserDto {

	private User user;

	public UserDto(User user) {
		this.user = user;
	}

	public String getFullname() {
		return user.getFullname();
	}

	public String getEmail() {
		return user.getEmail();
	}

	public String getSshPublicKey() {
		return user.getSshPublicKey();
	}

	public UserRoleType getRole() {
		return user.getRole();
	}

	public String[] getDefaultPermission() {
		return user.getDefaultPermission();
	}

	public String[] getAgiPrxPermission() {
		return user.getAgiPrxPermission();
	}

	public Integer getVersion() {
		return user.getVersion();
	}
}
