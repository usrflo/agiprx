package de.agitos.agiprx.db;

import java.math.BigInteger;
import java.sql.Connection;

public class DataSourceConnection {

	private Connection conn;
	private boolean isInErrorState;
	private boolean isTransactional;
	private BigInteger taLevel;

	public static DataSourceConnection newTransactionalConnection(Connection conn) {
		DataSourceConnection dsConn = new DataSourceConnection(true);
		dsConn.setConn(conn);
		dsConn.setTaLevel(BigInteger.ONE);
		dsConn.setInErrorState(false);
		return dsConn;
	}

	public static DataSourceConnection newNonTransactionalConnection(Connection conn) {
		DataSourceConnection dsConn = new DataSourceConnection(false);
		dsConn.setConn(conn);
		dsConn.setInErrorState(false);
		return dsConn;
	}

	private DataSourceConnection(boolean isTransactional) {
		this.isTransactional = isTransactional;
	}

	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public boolean isInErrorState() {
		return isInErrorState;
	}

	public void setInErrorState(boolean isInErrorState) {
		this.isInErrorState = isInErrorState;
	}

	public boolean isTransactional() {
		return isTransactional;
	}

	public void setTransactional(boolean isTransactional) {
		this.isTransactional = isTransactional;
	}

	public BigInteger getTaLevel() {
		return taLevel;
	}

	public void setTaLevel(BigInteger taLevel) {
		this.taLevel = taLevel;
	}

	public void addTaLevel() {
		taLevel = taLevel.add(BigInteger.ONE);
	}

	public void reduceTaLevel() {
		taLevel = taLevel.subtract(BigInteger.ONE);
	}

	public boolean isNestedTransaction() {
		return taLevel.compareTo(BigInteger.ZERO) > 0;
	}
}
