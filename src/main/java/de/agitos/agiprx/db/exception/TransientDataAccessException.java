package de.agitos.agiprx.db.exception;

/**
 * Root of the hierarchy of data access exceptions that are considered transient -
 * where a previously failed operation might be able to succeed when the operation
 * is retried without any intervention by application-level functionality.
 *
 * @author Thomas Risberg
 * @since 2.5
 * @see java.sql.SQLTransientException
 */
@SuppressWarnings("serial")
public abstract class TransientDataAccessException extends DataAccessException {

	/**
	 * Constructor for TransientDataAccessException.
	 * @param msg the detail message
	 */
	public TransientDataAccessException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for TransientDataAccessException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using a underlying
	 * data access API such as JDBC)
	 */
	public TransientDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
