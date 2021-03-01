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
package de.agitos.agiprx.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.agitos.agiprx.model.UserRoleType;

public class AgiPrxPermission {

	private boolean admin = false;

	private List<Pattern> permissionPatterns = null;

	public AgiPrxPermission(UserRoleType userRole, String[] agiPrxPermissions) {

		switch (userRole) {
		case ADMIN:
			this.admin = true;
			break;
		case USER:
			// split and set matchers on permission string
			permissionPatterns = new ArrayList<Pattern>();

			for (String permission : agiPrxPermissions) {
				permissionPatterns.add(Pattern.compile(permission));
			}
			break;
		default:
			throw new RuntimeException("User role " + userRole.name() + " is not allowed to access AgiPrx");
		}
	}

	public boolean isUserAllowed(String projectLabel) {

		if (this.admin) {
			return true;
		}

		// match project-label
		for (Pattern pattern : permissionPatterns) {
			if (pattern.matcher(projectLabel).matches()) {
				return true;
			}
		}

		return false;
	}
}
