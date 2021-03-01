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

import java.util.regex.Pattern;

import de.agitos.agiprx.DependencyInjector;

public class Validator implements DependencyInjector {

	private static Validator BEAN;

	public static final String LABEL_ERRORMSG = "Invalid label, please use ASCII lowercase letters or numbers without whitespace, min 2 characters, _ and - not allowed";

	private final Pattern domainName = Pattern.compile("^[a-z0-9]+(([\\.]{1}|[\\-]{1,2})[a-z0-9]+)*$");

	private final Pattern url = Pattern
			.compile("^(http:\\/\\/|https:\\/\\/)[a-z0-9]+(([\\.]{1}|[\\-]{1,2})[a-z0-9]+)*(:[0-9]{1,5})?(\\/.*)?$");

	private final Pattern ipv6 = Pattern.compile(
			"^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$");

	private final Pattern email = Pattern.compile(
			"^(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])$");

	private final Pattern label = Pattern.compile("^[a-z0-9]{2,}$");

	private final Pattern username = Pattern.compile("^[a-z0-9-_]{2,}$");

	// e.g. proj1-back2_user3
	private final Pattern sshProxyUsername = Pattern.compile("^[a-z0-9]{2,}-[a-z0-9]{2,}_[a-z0-9-_]{2,}$");

	public Validator() {

		Assert.singleton(this, BEAN);
		BEAN = this;
	}

	@Override
	public void postConstruct() {
	}

	public static Validator getBean() {
		return BEAN;
	}

	public boolean isDomainName(String s) {
		return domainName.matcher(s).matches();
	}

	public boolean isUrl(String s) {
		return url.matcher(s).matches();
	}

	public boolean isIPv6(String s) {
		return ipv6.matcher(s).matches();
	}

	public boolean isEmail(String s) {
		return email.matcher(s).matches();
	}

	public boolean isLabel(String s) {
		return label.matcher(s).matches();
	}

	public boolean isUsername(String s) {
		return username.matcher(s).matches();
	}

	public boolean isSshProxyUsername(String s) {
		return sshProxyUsername.matcher(s).matches();
	}
}
