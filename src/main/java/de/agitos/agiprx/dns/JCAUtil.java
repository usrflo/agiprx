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
package de.agitos.agiprx.dns;

import java.security.SecureRandom;

/**
 * Collection of static utility methods used by the security framework.
 *
 * @author Andreas Sterbenz
 * @since 1.5
 */
public final class JCAUtil {

	private JCAUtil() {
		// no instantiation
	}

	// size of the temporary arrays we use. Should fit into the CPU's 1st
	// level cache and could be adjusted based on the platform
	private static final int ARRAY_SIZE = 4096;

	/**
	 * Get the size of a temporary buffer array to use in order to be cache
	 * efficient. totalSize indicates the total amount of data to be buffered. Used
	 * by the engineUpdate(ByteBuffer) methods.
	 */
	public static int getTempArraySize(int totalSize) {
		return Math.min(ARRAY_SIZE, totalSize);
	}

	// cached SecureRandom instance
	private static class CachedSecureRandomHolder {
		public static SecureRandom instance = new SecureRandom();
	}

	/**
	 * Get a SecureRandom instance. This method should be used by JDK internal code
	 * in favor of calling "new SecureRandom()". That needs to iterate through the
	 * provider table to find the default SecureRandom implementation, which is
	 * fairly inefficient.
	 */
	public static SecureRandom getSecureRandom() {
		return CachedSecureRandomHolder.instance;
	}

}
