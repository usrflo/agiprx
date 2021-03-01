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

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.dao.ApiUserDao;
import de.agitos.agiprx.dao.UserDao;
import de.agitos.agiprx.model.ApiUser;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.model.UserRoleType;

public class UserContext implements DependencyInjector {

	private static UserContext BEAN;

	private UserDao userDao;

	private ApiUserDao apiUserDao;

	public UserContext() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
		userDao = UserDao.getBean();
		apiUserDao = ApiUserDao.getBean();
	}

	public static UserContext getBean() {
		return BEAN;
	}

	private ThreadLocal<User> userWrapper = new ThreadLocal<>();

	private ThreadLocal<ApiUser> apiUserWrapper = new ThreadLocal<>();

	private ThreadLocal<AgiPrxPermission> agiPrxPermissionWrapper = new ThreadLocal<>();

	public void registerUser(Long id) {
		User user = userDao.find(id);
		userWrapper.set(user);
		agiPrxPermissionWrapper.set(new AgiPrxPermission(user.getRole(), user.getAgiPrxPermission()));
	}

	public void registerApiUser(String username) {
		ApiUser apiUser = apiUserDao.findByUsername(username);
		apiUserWrapper.set(apiUser);
		agiPrxPermissionWrapper.set(new AgiPrxPermission(UserRoleType.USER, apiUser.getAgiPrxPermission()));
	}

	public ApiUser getApiUser() {
		return apiUserWrapper.get();
	}

	public User getUser() {
		return userWrapper.get();
	}

	public boolean isAdmin() {
		User user = userWrapper.get();
		if (user == null) {
			return false;
		}
		return UserRoleType.ADMIN.equals(user.getRole());
	}

	public boolean isUserAllowed(String projectLabel) {
		AgiPrxPermission agiPrxPermission = agiPrxPermissionWrapper.get();
		if (agiPrxPermission == null) {
			return false;
		}
		return agiPrxPermission.isUserAllowed(projectLabel);
	}

	public void unregister() {
		userWrapper.remove();
		apiUserWrapper.remove();
		agiPrxPermissionWrapper.remove();
	}
}
