package net.jcraron.pixivcrawler.pixiv;

import net.jcraron.pixivcrawler.ImageInfo;

public interface Pixiv {
	public ImageInfo getImageInfo(String imageId);

	public ImageInfo[] getImageInfo_artist(String authorId);

	public ImageInfo[] getImageInfo_search(String search, int page);

	/** @return artist id */
	public String[] getFollowing(int page);

	public ImageInfo[] getNewestImageFromFollowing(int page);

}
