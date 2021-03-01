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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.agitos.agiprx.AbstractTool;
import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.dao.UserDao;
import de.agitos.agiprx.exception.AbortionException;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.model.UserRoleType;
import de.agitos.agiprx.util.Assert;

public class AgiPrxSshAuthProcessor extends AbstractProcessor implements DependencyInjector {

	private static AgiPrxSshAuthProcessor BEAN;

	// @Value("${agiprx.authorizedAccessKeys:/opt/agiprx/.ssh/authorized_keys}")
	private String authorizedKeysFullpath;

	private ConsoleWrapper console;

	private UserDao userDao;

	public AgiPrxSshAuthProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		authorizedKeysFullpath = Config.getBean().getString("agiprx.authorizedAccessKeys",
				"/opt/agiprx/.ssh/authorized_keys");

	}

	@Override
	public void postConstruct() {
		console = ConsoleWrapper.getBean();
		userDao = UserDao.getBean();
	}

	public static AgiPrxSshAuthProcessor getBean() {
		return BEAN;
	}

	public void manageConfiguration(boolean verbose) throws IOException, InterruptedException, AbortionException {
		if (verbose) {
			console.printlnfStress("Grant access to AgiPrx by SSH");
		}
		generateAuthorizedKeysFile(verbose);
	}

	private void generateAuthorizedKeysFile(boolean verbose)
			throws IOException, InterruptedException, AbortionException {
		List<String> lines = new ArrayList<>();

		for (User allowedUser : userDao.findAll()) {

			UserRoleType roleType = allowedUser.getRole();
			if (roleType == null || !(roleType == UserRoleType.ADMIN || roleType == UserRoleType.USER)) {
				continue;
			}

			if (verbose) {
				console.printlnf("      %s (%s)", allowedUser.getFullname(), allowedUser.getEmail());
			}

			lines.add(generateAuthorizedKeysLine(allowedUser));
		}

		Path file = Paths.get(authorizedKeysFullpath);
		Files.write(file, lines, Charset.forName("latin1"));
	}

	private String generateAuthorizedKeysLine(User allowedUser) {

		StringBuilder buf = new StringBuilder();

		// environment="AGIPRX_USER_ID=1"

		buf.append("environment=\"" + AbstractTool.USER_ID + "=");
		buf.append(allowedUser.getId());
		buf.append("\" ");

		// ssh-rsa
		// AAAAB3NzaC1yc2EAAAADAQABAAABAQDRA8T6Td/OGEyoxr0IK42K3hq6jcx8kYg9eJoa72lTcazOI7o4gTW1LRpdRzmc4VfdTbWtii8rIHtQG8AGFZHnlcCRqxG36QmWq8/RwexdbC3fLgSPJXfEyOSg5I99Os1ixjaqWomXaDf+YpFDM+oBIC0WfBedmZ44Ef95Nvo9HefotjBc+PwqX0vyn2wYczdJd7n9JeHi9HCbWcxtoxAgafWx9o77fUdDE6lfPaCV7NgjDaVkj/CLkKl3ICJ4R9j3SrCmdfmbzwm3i6n+v6zYCEzQhDYkBDaYdvKfEwuVdSvxWcjP3StwCdHSxonuIFVeTjWsXfT2DrZDZeztq5Ct
		// sager@agitos.de

		buf.append(allowedUser.getSshPublicKey());

		return buf.toString();
	}
}
