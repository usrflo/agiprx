package de.agitos.agiprx.db;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.agitos.agiprx.DependencyInjector;
import de.agitos.agiprx.bean.Config;
import de.agitos.agiprx.db.exception.CannotGetJdbcConnectionException;
import de.agitos.agiprx.db.exception.InvalidDataAccessResourceUsageException;
import de.agitos.agiprx.util.Assert;

public class DataSourceUtils implements DependencyInjector {

	// private static final Logger LOG =
	// Logger.getLogger(DataSourceUtils.class.getName());

	private static DataSourceUtils BEAN;

	private HikariDataSource ds;

	private ThreadLocal<DataSourceConnection> dsConnection = new ThreadLocal<>();

	public DataSourceUtils() {

		Assert.singleton(this, BEAN);
		BEAN = this;

		HikariConfig poolConfig = new HikariConfig();

		Config config = Config.getBean();

		poolConfig.setJdbcUrl(config.getString("db.url"));
		poolConfig.setUsername(config.getString("db.user"));
		poolConfig.setPassword(config.getString("db.password"));

		poolConfig.addDataSourceProperty("cachePrepStmts", "true");
		poolConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		poolConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

		ds = new HikariDataSource(poolConfig);
	}

	@Override
	public void postConstruct() {
	}

	public static DataSourceUtils getBean() {
		return BEAN;
	}

	public DataSource getDataSource() {
		return ds;
	}

	// Connection / Transaction Management

	public static Connection getConnection() {
		return BEAN.getDsConnection();
	}

	private Connection getDsConnection() {
		DataSourceConnection dsConn = dsConnection.get();
		if (dsConn != null) {
			return dsConn.getConn();
		}
		return createNonTransactionalDsConnection();
	}

	private Connection createNonTransactionalDsConnection() {
		try {

			Connection conn = ds.getConnection();
			dsConnection.set(DataSourceConnection.newNonTransactionalConnection(conn));
			return conn;

		} catch (SQLException ex) {
			throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
		} catch (IllegalStateException ex) {
			throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection: " + ex.getMessage());
		}
	}

	public void startTransaction() {

		DataSourceConnection dsConn = dsConnection.get();
		try {
			if (dsConn == null /* || dsConn.getConn().isClosed() */) {

				Connection conn = ds.getConnection();
				conn.createStatement().execute("START TRANSACTION");
				dsConnection.set(DataSourceConnection.newTransactionalConnection(conn));
				return;
			}
		} catch (SQLException ex) {
			throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", ex);
		} catch (IllegalStateException ex) {
			throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection: " + ex.getMessage());
		}
		dsConn.addTaLevel();
	}

	public static void releaseConnection(boolean inSuccessState) {

		try {
			if (inSuccessState) {
				BEAN.commitAndReleaseConnection();
			} else {
				BEAN.rollbackAndReleaseConnection();
			}
		} catch (SQLException e) {
			throw new InvalidDataAccessResourceUsageException("DB connection release error", e);
		}
	}

	private void commitAndReleaseConnection() throws SQLException {

		DataSourceConnection dsConn = dsConnection.get();
		if (dsConn == null) {
			throw new RuntimeException("Running connection commit-release for a connection that was not started (?!)");
		}

		if (!dsConn.isTransactional()) {

			if (!dsConn.isInErrorState() || !dsConn.getConn().isClosed()) {
				dsConn.getConn().close();
			}
			dsConnection.remove();

			return;
		}

		dsConn.reduceTaLevel();

		if (dsConn.isNestedTransaction()) {
			return;
		}

		if (!dsConn.isInErrorState()) {
			dsConn.getConn().createStatement().execute("COMMIT");
			dsConn.getConn().close();
		} else {
			if (!dsConn.getConn().isClosed()) {
				dsConn.getConn().close();
			}
		}
		dsConnection.remove();
	}

	private void rollbackAndReleaseConnection() throws SQLException {

		DataSourceConnection dsConn = dsConnection.get();
		if (dsConn == null) {
			throw new RuntimeException(
					"Running connection rollback-release for a connection that was not started (?!)");
		}

		dsConn.setInErrorState(true);

		if (!dsConn.isTransactional()) {

			dsConn.getConn().close();
			dsConnection.remove();

			return;
		}

		// rollback disregarding the transaction level
		dsConn.getConn().createStatement().execute("ROLLBACK");
		dsConn.getConn().close();
		// dsConnection.remove();
	}
}
