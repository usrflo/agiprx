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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import de.agitos.agiprx.AgiPrx;
import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.util.Assert;

public class Config implements DependencyInjector {

	public static final String DEFAULT_LIST_SPLIT_EXP = "[ ,;]";

	private static Config BEAN;

	protected Properties properties;

	public Config() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		Path appPropertiesFile = Paths.get(AgiPrx.agiPrxRootDirectory, "etc", "application.properties");
		Assert.isTrue(Files.exists(appPropertiesFile),
				"Applications properties file '" + appPropertiesFile + "' needs to exist");

		// try (InputStream input =
		// this.getClass().getClassLoader().getResourceAsStream("application.properties"))
		// {
		try (InputStream input = Files.newInputStream(appPropertiesFile)) {

			if (input == null) {
				throw new IOException("Config file application.properties cannot be found");
			}

			properties = new Properties();
			properties.load(input);

		} catch (IOException e) {
			throw new RuntimeException("Error reading config file '" + appPropertiesFile + "'", e);
		}
	}

	@Override
	public void postConstruct() {
	}

	public static Config getBean() {
		return BEAN;
	}

	public String getString(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public String getString(String key) {
		return properties.getProperty(key);
	}

	public String[] getStringArray(String key, String splitExpression) {
		String value = properties.getProperty(key);
		return value == null || value.isBlank() ? null : value.split(splitExpression);
	}

	public Set<String> getStringSet(String key, String splitExpression) {
		String[] value = getStringArray(key, splitExpression);
		if (value == null) {
			return null;
		}

		return new HashSet<String>(Arrays.asList(value));
	}

	public List<String> getStringList(String key, String splitExpression) {
		String[] value = getStringArray(key, splitExpression);
		if (value == null) {
			return null;
		}

		return Arrays.asList(value);
	}

	public Integer getInteger(String key, Integer defaultValue) {
		try {
			return Integer.parseInt(properties.getProperty(key));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public Integer getInteger(String key) {
		try {
			return Integer.parseInt(properties.getProperty(key));
		} catch (Exception e) {
			throw new RuntimeException("Config value '" + key + "' needs to be definied");
		}
	}

	public Boolean getBoolean(String key, Boolean defaultValue) {
		String value = properties.getProperty(key);

		if ("true".equals(value)) {
			return Boolean.TRUE;
		}

		if ("false".equals(value)) {
			return Boolean.FALSE;
		}

		return defaultValue;
	}

}
