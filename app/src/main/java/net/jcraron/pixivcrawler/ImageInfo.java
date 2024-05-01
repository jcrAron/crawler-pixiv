package net.jcraron.pixivcrawler;

import java.io.InputStream;
import java.util.Iterator;

public interface ImageInfo {

	public String getArtistName();

	public String getImageName();

	public String getArtistId();

	public String getImageId();

	public String[] getTags();

	public Iterator<InputStream> getImages();

	public int getImageCount();

	public void setBookmark();

	/** @return true if image of ImageId is exist */
	public boolean isValidImageId();

	public boolean isAnimation();
}
