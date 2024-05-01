package net.jcraron.pixivcrawler.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.jcraron.pixivcrawler.util.LazyObject;

public class FlagTable extends DatabaseTable {
	private static final String COL_KEY = "key";
	private static final String COL_VALUE = "value";

	public FlagTable(Connection connection, String tableName) {
		super(connection, tableName);
	}

	public void create() throws SQLException {
		StringBuilder builder = new StringBuilder();
		builder.append(COL_KEY + " TEXT PRIMARY KEY");
		builder.append(" , ");
		builder.append(COL_VALUE + " BLOB NOT NULL");
		getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS " + getTableName() + " (" + builder + ")");
	}

	private LazyObject<PreparedStatement> insert = createLazyObject(() -> getConnection().prepareStatement("INSERT INTO " + getTableName() + " ( " + COL_KEY + "," + COL_VALUE + ") VALUES (?,?)"));

	public void insert(String key, byte[] value) throws SQLException, IllegalArgumentException {
		if (key == null || value == null) {
			throw new IllegalArgumentException("key: " + key);
		}
		PreparedStatement _insert = insert.get();
		_insert.setString(1, key);
		_insert.setBytes(2, value);
		_insert.executeUpdate();
	}

	private LazyObject<PreparedStatement> update = createLazyObject(() -> getConnection().prepareStatement("UPDATE " + getTableName() + " SET " + COL_VALUE + " = ? WHERE " + COL_KEY + " = ? "));

	public void update(String key, byte[] value) throws SQLException, IllegalArgumentException {
		if (key == null || value == null) {
			throw new IllegalArgumentException("key: " + key);
		}
		PreparedStatement _update = update.get();
		_update.setString(2, key);
		_update.setBytes(1, value);
		_update.executeUpdate();
	}

	private LazyObject<PreparedStatement> select = createLazyObject(() -> getConnection().prepareStatement("SELECT * FROM " + getTableName() + " WHERE " + COL_KEY + " = ? "));

	public byte[] select(String key) throws SQLException, IllegalArgumentException {
		PreparedStatement _getValue = select.get();
		_getValue.setString(1, key);
		try (ResultSet resultset = _getValue.executeQuery()) {
			if (resultset.next()) {
				return resultset.getBytes(COL_VALUE);
			}
		}
		throw new IllegalArgumentException("key: " + key);
	}

	public boolean isExist(String key) throws SQLException {
		PreparedStatement _getValue = select.get();
		_getValue.setString(1, key);
		try (ResultSet resultset = _getValue.executeQuery()) {
			return resultset.next();
		}
	}

}
