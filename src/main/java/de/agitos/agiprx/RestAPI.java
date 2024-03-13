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
package de.agitos.agiprx;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.bean.processor.ProxySyncProcessor;
import de.agitos.agiprx.rest.AdminService;
import de.agitos.agiprx.rest.ApiUserStore;
import de.agitos.agiprx.rest.BackendService;
import de.agitos.agiprx.rest.ContainerService;
import de.agitos.agiprx.rest.DomainService;
import de.agitos.agiprx.rest.MaintenanceService;
import de.agitos.agiprx.rest.PingService;
import de.agitos.agiprx.rest.ProjectService;
import de.agitos.agiprx.util.Assert;
import de.agitos.agiprx.util.EmailSender;
import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.openapi.OpenAPISupport;
import io.helidon.security.Security;
import io.helidon.security.integration.webserver.WebSecurity;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

public class RestAPI implements DependencyInjector {

	private static RestAPI BEAN;

	private ProxySyncProcessor proxySyncProcessor;

	private ConsoleWrapper console;

	private int serverPort;

	public RestAPI() {
		Assert.singleton(this, BEAN);
		BEAN = this;

		Config config = Config.getBean();
		serverPort = config.getInteger("server.port", 8002);

		proxySyncProcessor = ProxySyncProcessor.getBean();
	}

	@Override
	public void postConstruct() {
		console = ConsoleWrapper.getBean();
	}

	public static RestAPI getBean() {
		return BEAN;
	}

	public void runServer() {

		try {

			Routing.Builder routingBuilder = Routing.builder();

			// register Basic-Authentication provider
			routingBuilder.register(buildWebSecurity().securityDefaults(WebSecurity.authenticate()));

			boolean isMaster = proxySyncProcessor.isMasterInstance();

			// register services
			routingBuilder.register("/test", new PingService(isMaster));
			routingBuilder.register("/admin", new AdminService(isMaster));
			routingBuilder.register("/domains", new DomainService(isMaster));
			routingBuilder.register("/projects", new ProjectService(isMaster));
			routingBuilder.register("/containers", new ContainerService(isMaster));
			routingBuilder.register("/backends", new BackendService(isMaster));
			routingBuilder.register("/maintenance", new MaintenanceService(isMaster));

			// add OpenAPI support
			// io.helidon.config.Config apiConfig = io.helidon.config.Config.create();
			// routingBuilder.register(OpenAPISupport.create(apiConfig));
			routingBuilder.register(OpenAPISupport.create());

			// TODO: enable access logs?
			// routingBuilder.register(AccessLogSupport.create(config.get("server.access-log")));
			Routing routing = routingBuilder.build();

			// TODO: bind address should be configurable
			InetAddress wildCard = new InetSocketAddress(0).getAddress();

			JsonbSupport jsonbSupport = JsonbSupport.create();

			WebServer webServer = WebServer.builder(routing).bindAddress(wildCard).port(serverPort)
					.addMediaSupport(jsonbSupport).build();
			webServer.start().await(10, TimeUnit.SECONDS);

		} catch (Exception e) {

			console.printlnfError(ConsoleWrapper.ANSI_RED + "An error occured, the administrator will be informed: "
					+ e.getMessage() + ConsoleWrapper.ANSI_RESET);

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			EmailSender.getBean().sendMailToAdmin("AgiPrx error", sw.toString());

			throw new RuntimeException(e);

		}
	}

	private static WebSecurity buildWebSecurity() {
		Security security = Security.builder()
				.addAuthenticationProvider(
						HttpBasicAuthProvider.builder().realm("agiprx").userStore(new ApiUserStore()),
						"http-basic-auth")
				.build();
		return WebSecurity.create(security);
	}

}
