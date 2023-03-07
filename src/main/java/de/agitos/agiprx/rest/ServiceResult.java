package de.agitos.agiprx.rest;

import java.util.List;

public class ServiceResult {

	private boolean isSuccess;

	private Object payload;

	private List<String> warnings;

	public static ServiceResult createError(Object payload, List<String> warnings) {
		ServiceResult sr = new ServiceResult();
		sr.isSuccess = false;
		sr.payload = payload;
		sr.warnings = warnings;
		return sr;
	}

	public static ServiceResult create(Object payload, List<String> warnings) {
		ServiceResult sr = new ServiceResult();
		sr.isSuccess = true;
		sr.payload = payload;
		sr.warnings = warnings;
		return sr;
	}

	public static ServiceResult create(boolean isSuccess, Object payload, List<String> warnings) {
		ServiceResult sr = new ServiceResult();
		sr.isSuccess = isSuccess;
		sr.payload = payload;
		sr.warnings = warnings;
		return sr;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public Object getPayload() {
		return payload;
	}

	public List<String> getWarnings() {
		return warnings;
	}
}
