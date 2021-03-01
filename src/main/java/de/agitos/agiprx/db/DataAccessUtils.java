package de.agitos.agiprx.db;

import java.util.Collection;

import de.agitos.agiprx.db.exception.EmptyResultDataAccessException;
import de.agitos.agiprx.db.exception.IncorrectResultSizeDataAccessException;

public class DataAccessUtils {

	public static <T> T nullableSingleResult(Collection<T> results) throws IncorrectResultSizeDataAccessException {
		// This is identical to the requiredSingleResult implementation but differs in the
		// semantics of the incoming Collection (which we currently can't formally express)
		if (results==null || results.isEmpty()) {
			throw new EmptyResultDataAccessException(1);
		}
		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1, results.size());
		}
		return results.iterator().next();
	}
	
}
