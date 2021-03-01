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

import de.agitos.agiprx.util.SshKeyConverter;

public class User extends AbstractModel {

	public static final Long SUPERUSER_ID = 1L;

	private String fullname;

	private String email;

	private String sshPublicKey;

	private UserRoleType role;

	private String[] defaultPermission;

	private String[] agiPrxPermission;

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSshPublicKey() {
		return sshPublicKey;
	}

	public void setSshPublicKey(String sshPublicKey) {
		this.sshPublicKey = sshPublicKey;
	}

	public UserRoleType getRole() {
		return role;
	}

	public void setRole(UserRoleType role) {
		this.role = role;
	}

	public String[] getDefaultPermission() {
		return defaultPermission;
	}

	public void setDefaultPermission(String[] defaultPermission) {
		this.defaultPermission = defaultPermission;
	}

	public String[] getAgiPrxPermission() {
		return agiPrxPermission;
	}

	public void setAgiPrxPermission(String[] agiPrxPermission) {
		this.agiPrxPermission = agiPrxPermission;
	}

	@Override
	public String toString() {
		
		String keyShort = SshKeyConverter.getNormalizedSSHKeyShort(sshPublicKey, new StringBuilder(), 30);
		if (keyShort == null) {
			keyShort = sshPublicKey.substring(0, 25) + "...";
		}

		return (id == SUPERUSER_ID ? "Superuser" : "User") + " [id=" + id + ", fullname=" + fullname + ", email="
				+ email + ", role=" + role + ", defaultPerm="
				+ Arrays.toString(defaultPermission) + ", agiPrxPerm="
				+ (UserRoleType.ADMIN.equals(role) ? "<admin-global>" : Arrays.toString(agiPrxPermission)) + ",\n\t\tsshPublicKey=" + keyShort + "]";
	}
}
