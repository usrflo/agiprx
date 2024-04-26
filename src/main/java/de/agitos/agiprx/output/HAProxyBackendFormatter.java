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
package de.agitos.agiprx.output;

import com.mysql.cj.util.StringUtils;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.bean.processor.HAProxyProcessor;
import de.agitos.agiprx.model.Backend;
import de.agitos.agiprx.model.BackendContainer;
import de.agitos.agiprx.util.Assert;

public class HAProxyBackendFormatter implements DependencyInjector {

	private static HAProxyBackendFormatter BEAN;

	private Integer haProxyHttpsRedirectCode;

	public HAProxyBackendFormatter() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		haProxyHttpsRedirectCode = Config.getBean().getInteger("haproxy.httpsRedirectCode", 301);
	}

	@Override
	public void postConstruct() {
	}

	public static HAProxyBackendFormatter getBean() {
		return BEAN;
	}

	public void formatBackend(Backend backend, StringBuilder buf) {
		formatBackend(backend, buf, "");
	}

	public void formatBackend(Backend backend, StringBuilder buf, String linePrefix) {

		buf.append(linePrefix).append("backend ").append(backend.getFQLabel()).append("\n");

		boolean paramsExist = !StringUtils.isNullOrEmpty(backend.getParams());
		if (paramsExist && backend.getParams().contains(HAProxyProcessor.MAINTENANCE)) {

			// maintenance mode overwrites any other backend configuration
			buf.append(linePrefix).append("\t").append("errorfile 503 /etc/haproxy/errors/503-maintenance.http")
					.append("\n");

			addConditionalSSLRedirect(buf, linePrefix);

		} else {

			if (paramsExist) {
				buf.append(convertAndIndentMultilineStringToBlock(backend.getParams(), 1, linePrefix));
			}

			addConditionalSSLRedirect(buf, linePrefix);

			for (BackendContainer backendContainer : backend.getBackendContainers()) {

				// set back-reference
				backendContainer.getContainer().setProject(backend.getProject());

				buf.append(linePrefix).append("\t").append("server ");
				buf.append(backendContainer.getContainer().getHost().getHostname()).append("_")
						.append(backendContainer.getContainer().getFQLabel()).append(" ");
				buf.append("[").append(backendContainer.getContainer().getIpv6()).append("]:")
						.append(backend.getPort());
				if (!StringUtils.isNullOrEmpty(backendContainer.getParams())) {
					buf.append(" ").append(backendContainer.getParams());
				}
				buf.append("\n");
			}
		}
	}

	private void addConditionalSSLRedirect(StringBuilder buf, String linePrefix) {
		buf.append(linePrefix).append("\t").append("redirect scheme https code ").append(haProxyHttpsRedirectCode)
				.append(" if !{ ssl_fc } ");
		buf.append("{ req.hdr(host),lower,map_str(")
				.append(HAProxyProcessor.CONFIG_PATH + HAProxyProcessor.DOMAIN_TO_CERT_FILE).append(") -m found }")
				.append("\n");
	}

	private String convertAndIndentMultilineStringToBlock(String multiline, int tabIndentLevel, String linePrefix) {

		String indent = linePrefix;
		for (int i = 0; i < tabIndentLevel; i++) {
			indent += "\t";
		}

		if (StringUtils.isNullOrEmpty(multiline)) {
			return indent + "\n";
		}

		String[] multilineArray = multiline.split("\n");

		StringBuilder buf = new StringBuilder();

		for (int i = 0; i < multilineArray.length; i++) {
			String line = multilineArray[i];
			if (StringUtils.isNullOrEmpty(line)) {
				continue;
			}
			if (line.startsWith(HAProxyProcessor.USERAUTH + " ")) {
				// USERAUTH <userlist>
				String userlistName = line.substring(HAProxyProcessor.USERAUTH.length() + 1);
				// acl auth_ok http_auth(imx_users)
				buf.append(indent).append("acl auth_ok http_auth(").append(userlistName).append(")\n");
				buf.append(indent).append("http-request auth realm UserAuth if !auth_ok\n");
			} else {
				buf.append(indent).append(line).append("\n");
			}
		}

		return buf.toString();
	}
}
