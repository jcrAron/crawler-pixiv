package net.jcraron.pixivcrawler.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import net.jcraron.pixivcrawler.util.LazyObject;

public abstract class DatabaseTable {
	private final Connection connection;
	private final String tableName;

	public DatabaseTable(Connection connection, String tableName) {
		this.connection = connection;
		this.tableName = tableName;
	}

	public Connection getConnection() {
		return connection;
	}

	public String getTableName() {
		return tableName;
	}

	public abstract void create() throws SQLException;

	protected static <T> LazyObject<T> createLazyObject(Callable<T> callable) {
		return LazyObject.of(callable, null, null);
	}
}
