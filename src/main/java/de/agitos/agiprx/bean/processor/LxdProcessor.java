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
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.agitos.agiprx.ConsoleWrapper;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.util.Assert;

public class LxdProcessor extends AbstractProcessor implements DependencyInjector {

	private static LxdProcessor BEAN;

	protected ConsoleWrapper console;

	// @Value("${lxd.fetchcontainers}")
	private String fetchContainersCommand;

	public LxdProcessor() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		fetchContainersCommand = Config.getBean().getString("lxd.fetchcontainers", "");
	}

	@Override
	public void postConstruct() {
		console = ConsoleWrapper.getBean();
	}

	public static LxdProcessor getBean() {
		return BEAN;
	}

	public Map<String, String> getAvailableContainers() {

		Map<String, String> result = new HashMap<String, String>();

		if (!fetchContainersCommand.trim().isEmpty()) {
			try {
				StringBuilder output = new StringBuilder();
				StringBuilder errorOutput = new StringBuilder();
				if (exec(output, errorOutput, fetchContainersCommand) != 0) {
					console.printlnfError("Unable to fetch containers from lxd: %s %s", output.toString(),
							errorOutput.toString());
				} else {
					JsonElement cResponse = new Gson().fromJson(output.toString(), JsonElement.class);
					JsonElement metadataElement = cResponse.getAsJsonObject().get("metadata");

					for (JsonElement containerElement : metadataElement.getAsJsonArray()) {
						JsonObject container = containerElement.getAsJsonObject();

						JsonObject state = container.get("state").getAsJsonObject();
						JsonObject network = state.get("network").getAsJsonObject();
						JsonObject firstNet = network.get("eth0").getAsJsonObject();
						JsonArray netAddresses = firstNet.get("addresses").getAsJsonArray();
						for (int i = 0; i < netAddresses.size(); i++) {
							JsonObject netDetail = netAddresses.get(i).getAsJsonObject();
							if ("inet6".equals(netDetail.get("family").getAsString())
									&& "global".equals(netDetail.get("scope").getAsString())) {
								result.put(container.get("name").getAsString(), netDetail.get("address").getAsString());
							}
						}
					}
				}
			} catch (IOException | InterruptedException | RuntimeException e) {
				console.printlnfError("Failed to fetch containers from lxd: %s", e.getMessage());
			}
		}

		return result;
	}

	// {"type":"sync","status":"Success","status_code":200,"operation":"","error_code":0,"error":"","metadata":[{"architecture":"x86_64","config":{"image.architecture":"amd64","image.description":"ubuntu
	// 18.04 LTS amd64 (release)
	// (20181206)","image.label":"release","image.os":"ubuntu","image.release":"bionic","image.serial":"20181206","image.version":"18.04","volatile.base_image":"84a71299044bc3c3563396bef153c0da83d494f6bf3d38fecc55d776b1e19bf9","volatile.eth0.hwaddr":"00:16:3e:7d:91:52","volatile.idmap.base":"0","volatile.idmap.next":"[{\"Isuid\":true,\"Isgid\":true,\"Hostid\":1000000,\"Nsid\":0,\"Maprange\":1000000000}]","volatile.last_state.idmap":"[{\"Isuid\":true,\"Isgid\":true,\"Hostid\":1000000,\"Nsid\":0,\"Maprange\":1000000000}]","volatile.last_state.power":"RUNNING"},"devices":{},"ephemeral":false,"profiles":["default"],"stateful":false,"description":"","created_at":"2018-12-19T16:49:16.567142412+01:00","expanded_config":{"image.architecture":"amd64","image.description":"ubuntu
	// 18.04 LTS amd64 (release)
	// (20181206)","image.label":"release","image.os":"ubuntu","image.release":"bionic","image.serial":"20181206","image.version":"18.04","volatile.base_image":"84a71299044bc3c3563396bef153c0da83d494f6bf3d38fecc55d776b1e19bf9","volatile.eth0.hwaddr":"00:16:3e:7d:91:52","volatile.idmap.base":"0","volatile.idmap.next":"[{\"Isuid\":true,\"Isgid\":true,\"Hostid\":1000000,\"Nsid\":0,\"Maprange\":1000000000}]","volatile.last_state.idmap":"[{\"Isuid\":true,\"Isgid\":true,\"Hostid\":1000000,\"Nsid\":0,\"Maprange\":1000000000}]","volatile.last_state.power":"RUNNING"},"expanded_devices":{"eth0":{"name":"eth0","nictype":"bridged","parent":"lxdbr0","type":"nic"},"root":{"path":"/","pool":"lxd","type":"disk"}},"name":"agiprxtest","status":"Running","status_code":103,"last_used_at":"2018-12-21T15:44:16.152946496+01:00","location":"none","backups":null,"state":{"status":"Running","status_code":103,"disk":{"root":{"usage":311725056}},"memory":{"usage":35155968,"usage_peak":213233664,"swap_usage":0,"swap_usage_peak":0},"network":{"eth0":{"addresses":[{"family":"inet","address":"10.43.180.76","netmask":"24","scope":"global"},{"family":"inet6","address":"fd42:7aa2:3726:eebd:216:3eff:fe7d:9152","netmask":"64","scope":"global"},{"family":"inet6","address":"fe80::216:3eff:fe7d:9152","netmask":"64","scope":"link"}],"counters":{"bytes_received":5667508,"bytes_sent":191399,"packets_received":7578,"packets_sent":2148},"hwaddr":"00:16:3e:7d:91:52","host_name":"vethEE3GHS","mtu":1500,"state":"up","type":"broadcast"},"lo":{"addresses":[{"family":"inet","address":"127.0.0.1","netmask":"8","scope":"local"},{"family":"inet6","address":"::1","netmask":"128","scope":"local"}],"counters":{"bytes_received":5489,"bytes_sent":5489,"packets_received":57,"packets_sent":57},"hwaddr":"","host_name":"","mtu":65536,"state":"up","type":"loopback"}},"pid":20029,"processes":26,"cpu":{"usage":37263130373}},"snapshots":null}]}
}
