package de.agitos.agiprx.db;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapSqlParameterSource extends AbstractSqlParameterSource implements SqlParameterSource {
	
	private final Map<String, Object> values = new LinkedHashMap<>();

	/**
	 * Create an empty MapSqlParameterSource,
	 * with values to be added via {@code addValue}.
	 * @see #addValue(String, Object)
	 */
	public MapSqlParameterSource() {
	}
	
	/**
	 * Create a new MapSqlParameterSource based on a Map.
	 * @param values a Map holding existing parameter values (can be {@code null})
	 */
	public MapSqlParameterSource(Map<String, ?> values) {
		addValues(values);
	}

	public void addValue(String paramName, Object value) {
		this.values.put(paramName, value);
	}

	@Override
	public boolean hasValue(String paramName) {
		return this.values.containsKey(paramName);
	}

	@Override
	public Object getValue(String paramName) throws IllegalArgumentException {
		return this.values.get(paramName);
	}

	/**
	 * Add a Map of parameters to this parameter source.
	 * @param values a Map holding existing parameter values (can be {@code null})
	 * @return a reference to this parameter source,
	 * so it's possible to chain several calls together
	 */
	public MapSqlParameterSource addValues(Map<String, ?> values) {
		if (values != null) {
			values.forEach((key, value) -> {
				this.values.put(key, value);
				if (value instanceof SqlParameterValue) {
					registerSqlType(key, ((SqlParameterValue) value).getSqlType());
				}
			});
		}
		return this;
	}
}
