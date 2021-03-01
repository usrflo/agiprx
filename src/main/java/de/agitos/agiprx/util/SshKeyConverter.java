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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import de.agitos.agiprx.util.SshPublicKeyReaderUtil.PublicKeyParseException;

public class SshKeyConverter {

	// ssh-rsa AAAAB3NzaC1yc2...ABC test@example.org
	public static String getNormalizedSSHKey(String key, StringBuilder errorInfo) {

		if (key == null) {
			return null;
		}

		String line = key.trim();
		int startPos = line.indexOf("ssh-");
		if (startPos == -1) {
			errorInfo.append(line);
			return null;
		}
		line = line.substring(startPos);

		List<String> parts = normalizeParts(line.split(" "));

		if (parts == null) {
			errorInfo.append(line);
			return null;
		}

		if (parts.size() < 2 || parts.size() > 3) {
			errorInfo.append(line);
			return null;
		}

		if (!"ssh-rsa".equals(parts.get(0)) && "ssh-dss".equals(parts.get(0))) {
			errorInfo.append(line);
			return null;
		}

		try {
			Base64.getDecoder().decode(parts.get(1));
		} catch (IllegalArgumentException e) {
			errorInfo.append(line);
			return null;
		}

		// FSTODO:
		// http://stackoverflow.com/questions/2494450/ssh-rsa-public-key-validation-using-a-regular-expression
		String normalizedKey = parts.get(0) + " " + parts.get(1);
		if (parts.size() == 3) {
			normalizedKey += " " + parts.get(2);
		}

		try {
			SshPublicKeyReaderUtil.load(normalizedKey);
		} catch (PublicKeyParseException e) {
			String keyShort = getNormalizedSSHKeyShort(line, new StringBuilder(), 50);
			if (keyShort == null) {
				errorInfo.append(keyShort);
			} else {
				errorInfo.append(line);
			}
		}

		return normalizedKey;
	}

	// ssh-rsa AAAAB3NzaC1yc2...ABC test@example.org
	public static String getNormalizedSSHKeyShort(String key, StringBuilder errorInfo, int keyLength) {

		if (key == null)
			return null;

		String line = key.trim();
		int startPos = line.indexOf("ssh-");
		if (startPos == -1) {
			errorInfo.append(line);
			return null;
		}
		line = line.substring(startPos);

		List<String> parts = normalizeParts(line.split(" "));

		if (parts == null) {
			errorInfo.append(line);
			return null;
		}

		if (parts.size() < 2 || parts.size() > 3) {
			errorInfo.append(line);
			return null;
		}

		if (!"ssh-rsa".equals(parts.get(0)) && "ssh-dss".equals(parts.get(0))) {
			errorInfo.append(line);
			return null;
		}

		String normalizedKey = parts.get(0) + " ";

		if (parts.get(1).length() < keyLength) {
			normalizedKey += parts.get(1);
		} else {
			normalizedKey += parts.get(1).substring(0, (int) keyLength / 2) + "..."
					+ parts.get(1).substring(parts.get(1).length() - (int) keyLength / 2);
		}

		if (parts.size() == 3) {
			normalizedKey += " " + parts.get(2);
		}

		return normalizedKey;
	}

	private static List<String> normalizeParts(String[] parts) {

		List<String> result = new ArrayList<String>();

		for (String part : parts) {
			if (part.contains("\"")) {
				return null;
			}
			result.add(part.trim());
		}
		return result;
	}
}
