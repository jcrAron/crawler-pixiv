package net.jcraron.pixivcrawler.database;

import java.io.InputStream;
import java.util.Iterator;

import net.jcraron.pixivcrawler.ImageInfo;

public class ImageInfoData implements ImageInfo {

	private String artistName;
	private String imageName;
	private String artistId;
	private String imageId;
	private String[] tags;
	private int imageCount;
	private boolean isAnimation;

	public ImageInfoData(String artistName, String imageName, String artistId, String imageId, int imageCount, boolean isAnimation, String[] tags) {
		this.artistName = artistName;
		this.imageName = imageName;
		this.artistId = artistId;
		this.imageId = imageId;
		this.tags = tags;
		this.imageCount = imageCount;
		this.isAnimation = isAnimation;
	}

	@Override
	public String getArtistName() {
		return artistName;
	}

	@Override
	public String getImageName() {
		return imageName;
	}

	@Override
	public String getArtistId() {
		return artistId;
	}

	@Override
	public String getImageId() {
		return imageId;
	}

	@Override
	public String[] getTags() {
		return tags;
	}

	@Override
	public int getImageCount() {
		return imageCount;
	}

	@Override
	public boolean isAnimation() {
		return isAnimation;
	}

	@Override
	public boolean isValidImageId() {
		return true;
	}

	@Override
	public void setBookmark() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<InputStream> getImages() {
		throw new UnsupportedOperationException();
	}

}
