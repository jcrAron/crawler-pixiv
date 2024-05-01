package net.jcraron.pixivcrawler.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import net.jcraron.pixivcrawler.ImageInfo;
import net.jcraron.pixivcrawler.util.LazyObject;

public class ImaeInfoTable extends DatabaseTable {


	public ImaeInfoTable(Connection connection, String tableName) {
		super(connection, tableName);

	}

	public void create() throws SQLException {
		StringBuilder builder = new StringBuilder();
		builder.append(COL_IMAGE_ID + " TEXT PRIMARY KEY");
		builder.append(" , ");
		builder.append(COL_ARTIST_ID + " TEXT NOT NULL");
		builder.append(" , ");
		builder.append(COL_ARTIST_NAME + " TEXT NOT NULL");
		builder.append(" , ");
		builder.append(COL_IMAGE_NAME + " TEXT NOT NULL");
		builder.append(" , ");
		builder.append(COL_IMAGE_COUNT + " INTEGER NOT NULL DEFAULT 1");
		builder.append(" , ");
		builder.append(COL_IS_ANIMATION + " BOOLEAN NOT NULL DEFAULT false");
		builder.append(" , ");
		builder.append(COL_TAGS + " TEXT NOT NULL DEFAULT '[]'");

		getConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS " + getTableName() + " (" + builder + ")");
	}

	protected final static String COL_ARTIST_ID = "artist_id";
	protected final static String COL_IMAGE_ID = "image_id";
	protected final static String COL_ARTIST_NAME = "artist_name";
	protected final static String COL_IMAGE_NAME = "image_name";
	protected final static String COL_IMAGE_COUNT = "image_count";
	protected final static String COL_IS_ANIMATION = "is_animation";
	protected final static String COL_TAGS = "tags";
	protected final static String ImageInfoAllColumns = createAllColumns(COL_ARTIST_ID, COL_IMAGE_ID, COL_ARTIST_NAME, COL_IMAGE_NAME, COL_IMAGE_COUNT, COL_IS_ANIMATION, COL_TAGS);

	public static String createAllColumns(String... columns) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < columns.length; i++) {
			String col = columns[i];
			builder.append(col);
			if (i < columns.length - 1) {
				builder.append(",");
			}
		}
		return builder.toString();
	}

	private LazyObject<PreparedStatement> insert = createLazyObject(() -> getConnection().prepareStatement("INSERT INTO " + getTableName() + " (" + ImageInfoAllColumns + ") VALUES (?,?,?,?,?,?,?)"));

	public boolean insert(ImageInfo image) throws SQLException {
		if (!image.isValidImageId()) {
			return false;
		}
		String imageId = image.getImageId();
		PreparedStatement _insert = insert.get();
		_insert.setString(1, image.getArtistId());
		_insert.setString(2, imageId);
		_insert.setString(3, image.getArtistName());
		_insert.setString(4, image.getImageName());
		_insert.setInt(5, image.getImageCount());
		_insert.setBoolean(6, image.isAnimation());
		_insert.setString(7, new JSONArray(image.getTags()).toString());
		_insert.executeUpdate();
		return true;
	}

	private LazyObject<PreparedStatement> getByImageId = createLazyObject(() -> getConnection().prepareStatement("SELECT * FROM " + getTableName() + " WHERE " + COL_IMAGE_ID + " = ? "));

	public List<ImageInfo> getByImageId(String ImageId) throws SQLException {
		synchronized (getByImageId) {
			PreparedStatement _getByImageId = getByImageId.get();
			_getByImageId.setString(1, ImageId);
			List<ImageInfo> list = new ArrayList<>();
			ResultSet resultset = _getByImageId.executeQuery();
			if (resultset != null) {
				while (resultset.next()) {
					list.add(resultSetToImageInfo(resultset));
				}
			}
			return list;
		}
	}

	public boolean isExist(ImageInfo imageInfo) throws SQLException {
		String imageId = imageInfo.getImageId();
		synchronized (getByImageId) {
			PreparedStatement _getByImageId = getByImageId.get();
			_getByImageId.setString(1, imageId);
			try (ResultSet resultset = _getByImageId.executeQuery()) {
				return resultset.next();
			}
		}
	}

	private LazyObject<PreparedStatement> imageCount_artist = createLazyObject(() -> getConnection().prepareStatement("SELECT COUNT(*) FROM " + getTableName() + " WHERE " + COL_ARTIST_ID + " = ?"));

	public int imageCount_artist(String artistId) throws SQLException {
		PreparedStatement _imageCount_artist = imageCount_artist.get();
		_imageCount_artist.setString(1, artistId);
		ResultSet resultset = _imageCount_artist.executeQuery();
		return resultset.next() ? resultset.getInt(1) : 0;
	}

	protected ImageInfo resultSetToImageInfo(ResultSet resultSet) throws SQLException {
		String artistId = resultSet.getString(COL_ARTIST_ID);
		String imageId = resultSet.getString(COL_IMAGE_ID);
		String artistName = resultSet.getString(COL_ARTIST_NAME);
		String imageName = resultSet.getString(COL_IMAGE_NAME);
		int imageCount = resultSet.getInt(COL_IMAGE_COUNT);
		boolean isAnimation = resultSet.getBoolean(COL_IS_ANIMATION);
		String[] tags = new JSONArray(resultSet.getString(COL_TAGS)).toList().toArray(i -> new String[i]);
		return new ImageInfoData(artistId, imageId, artistName, imageName, imageCount, isAnimation, tags);
	}
}
