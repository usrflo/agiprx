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
package de.agitos.agiprx.bean.maintenance;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.bean.processor.LesslCertProcessor;
import de.agitos.agiprx.bean.processor.ProxySyncProcessor;
import de.agitos.agiprx.dns.DomainIpChecker;
import de.agitos.agiprx.model.User;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.EmailSender;
import de.agitos.agiprx.util.UserContext;

/**
 * Maintenance jobs for daily batch execution by MaintenanceTool.
 */

public class MainMaintenanceBean implements DependencyInjector {

	private static MainMaintenanceBean BEAN;

	private UserContext userContext;

	private DomainIpChecker domainIpChecker;

	private CertChecker certChecker;

	private LesslCertProcessor lesslCertProcessor;

	private HAProxyProcessor haProxyProcessor;

	private ProxySyncProcessor proxySyncProcessor;

	private EmailSender emailSender;

	// @Value("${email.subjectMaintenanceTool:agiprx maintenance status}")
	private String subjectMaintenanceTool;

	public MainMaintenanceBean() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		subjectMaintenanceTool = Config.getBean().getString("email.subjectMaintenanceTool",
				"agiprx maintenance status");
	}

	@Override
	public void postConstruct() {
		userContext = UserContext.getBean();
		domainIpChecker = DomainIpChecker.getBean();
		certChecker = CertChecker.getBean();
		lesslCertProcessor = LesslCertProcessor.getBean();
		haProxyProcessor = HAProxyProcessor.getBean();
		proxySyncProcessor = ProxySyncProcessor.getBean();
		emailSender = EmailSender.getBean();
	}

	public static MainMaintenanceBean getBean() {
		return BEAN;
	}

	// @Scheduled(cron = "${cron.maintenancejob:0 5 0 * * *}")
	public void runScheduled() {

		// always run as superuser
		userContext.registerUser(User.SUPERUSER_ID);

		try {

			run();

		} catch (Exception e) {

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			emailSender.sendMailToAdmin("AgiPrx MaintenanceTool error", sw.toString());

			throw new RuntimeException(e);

		} finally {
			userContext.unregister();
		}
	}

	public void run() throws Exception {

		if (!proxySyncProcessor.isMasterInstance()) {
			// slave instances are sync'ed by their master, so quit
			return;
		}

		Set<String> domainsInErrorState = new HashSet<String>();
		List<String> warningMessages = new ArrayList<String>();

		// check if IPs of productive domain names are equal to the proxy IPs, fill
		// list domainsInErrorState
		domainIpChecker.run(warningMessages, domainsInErrorState);

		// check if configured certs will reach end-of-life in configured notification
		// period for domains not in error, notify in case of end-of-live
		certChecker.run(warningMessages, domainsInErrorState);

		// generate LESSL certs if configured and not available for domains not in error
		// state + renew existing certs
		lesslCertProcessor.run(warningMessages, domainsInErrorState);

		// generate new SSL cert mappings
		haProxyProcessor.generateDomainToCertMap();

		// sync to slave instances
		proxySyncProcessor.syncToSlaveInstances(false, warningMessages);

		if (warningMessages.size() > 0) {
			emailSender.sendMailToAdmin(subjectMaintenanceTool, String.join("\n", warningMessages));
		}
	}
}
