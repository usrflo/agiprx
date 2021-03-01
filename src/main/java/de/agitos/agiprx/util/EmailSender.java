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

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;

public class EmailSender implements DependencyInjector {

	private static EmailSender BEAN;

	private String sender;
	private String adminReceiver;
	private String username;
	private String password;
	private String server;
	private int port;
	private String emailFooter;

	public EmailSender() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		Config config = Config.getBean();

		sender = config.getString("email.sender");
		adminReceiver = config.getString("email.adminReceiver");
		username = config.getString("email.username");
		password = config.getString("email.password");
		server = config.getString("email.server");
		port = config.getInteger("email.smtpport", 25);
		emailFooter = config.getString("email.footer");
	}

	@Override
	public void postConstruct() {
	}

	public static EmailSender getBean() {
		return BEAN;
	}

	public String getEmailFooter() {
		return emailFooter;
	}

	public void sendMailToAdmin(String subject, String text) {
		sendMailToUser(adminReceiver, null, subject, text);
	}

	public void sendMailToUser(String receiverEmailAddress, String receiverName, String subject, String text) {

		try {
			MultiPartEmail email = new MultiPartEmail();
			if (username != null && password != null) {
				email.setAuthentication(username, password);
				email.setStartTLSEnabled(true);
			}
			email.setHostName(server);
			email.setSmtpPort(port);
			email.setFrom(sender);
			email.addTo(receiverEmailAddress, receiverName);
			email.setCharset("UTF-8");
			email.setSubject(subject);
			email.setMsg(text);
			// email.attach(new ByteArrayDataSource(anhangInputStream, anhangContentType),
			// anhangDateiName, anhangBeschreibung, EmailAttachment.ATTACHMENT);
			email.send();

		} catch (EmailException e) {
			throw new RuntimeException("Error sending mail to " + receiverEmailAddress + ": " + e.getMessage());
		}
	}
}
