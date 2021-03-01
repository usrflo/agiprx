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
package de.agitos.agiprx.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import de.agitos.agiprx.dao.ApiUserDao;
import de.agitos.agiprx.model.ApiUser;
import de.agitos.agiprx.model.UserRoleType;
import io.helidon.security.providers.httpauth.SecureUserStore;

public class ApiUserStore implements SecureUserStore {

	private ApiUserDao apiUserDao;

	public ApiUserStore() {
		apiUserDao = ApiUserDao.getBean();
	}

	@Override
	public Optional<User> user(String login) {

		ApiUser apiUser = apiUserDao.findByUsername(login);

		if (apiUser == null) {
			return Optional.empty();
		}

		return Optional.of(new ApiUserDto(apiUser));
	}

	private class ApiUserDto implements User {

		private ApiUser apiUser;

		public ApiUserDto(ApiUser apiUser) {
			this.apiUser = apiUser;
		}

		@Override
		public String login() {
			return apiUser.getUsername();
		}

		@Override
		public boolean isPasswordValid(char[] password) {
			return new String(password).equals(apiUser.getPassword());
		}

		public Collection<String> roles() {
			Collection<String> roles = new ArrayList<String>(1);
			roles.add(UserRoleType.USER.name());
			return roles;
		}

	}

}
