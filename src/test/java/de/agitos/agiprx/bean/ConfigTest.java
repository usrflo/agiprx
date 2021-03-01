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
package de.agitos.agiprx.bean;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConfigTest {

	private Config config;

	@BeforeClass
	public static void beforeClass() {
		de.agitos.agiprx.AgiPrx.agiPrxRootDirectory = "src/test/resources";
		new Config();
	}

	@Before
	public void perTestSetup() {
		config = Config.getBean();

		// test value setup
		config.properties.put("emptyList", "");
		config.properties.put("simpleList", "1 2 3");
		config.properties.put("singleStringSetValue", "127.0.0.1");
	}

	@Test
	public void getNonExistingStringArrayTest() {
		Assert.assertNull(config.getStringArray("nonExistingKey", Config.DEFAULT_LIST_SPLIT_EXP));
	}

	@Test
	public void getEmptyStringArrayTest() {
		Assert.assertNull(config.getStringArray("emptyList", Config.DEFAULT_LIST_SPLIT_EXP));
	}

	@Test
	public void getSimpleStringArrayTest() {
		Assert.assertArrayEquals(new String[] { "1", "2", "3" },
				config.getStringArray("simpleList", Config.DEFAULT_LIST_SPLIT_EXP));
	}

	@Test
	public void getSingleStringSetTest() {
		Assert.assertEquals(new HashSet<String>(Arrays.asList("127.0.0.1")),
				config.getStringSet("singleStringSetValue", Config.DEFAULT_LIST_SPLIT_EXP));
	}
}
