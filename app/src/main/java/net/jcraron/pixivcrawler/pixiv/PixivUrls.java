package net.jcraron.pixivcrawler.pixiv;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.utils.URIBuilder;

public class PixivUrls {
	private final static String URL_LOGIN_ROOT = "https://accounts.pixiv.net";
	private final static String URL_LOGIN_PAGE = "https://accounts.pixiv.net/login";
	private final static String URL_LOGIN_POST = "https://accounts.pixiv.net/api/login";

	private final static String URL_IMAGE_INFO_PAGE = "https://www.pixiv.net/artworks/%s";
	private final static String URL_IMAGE_ORIGIN_PAGE = "https://www.pixiv.net/ajax/illust/%s/pages?lang=zh_tw";
	private final static String URL_PIXIV = "https://www.pixiv.net";
	private final static String URL_USER_HOME = "https://www.pixiv.net/users/%s";
	private final static String URL_USER_ALL_WORKS = "https://www.pixiv.net/ajax/user/%s/profile/all";
	private final static String URL_SET_LIKE = "https://www.pixiv.net/ajax/illusts/bookmarks/add";
	private final static String URL_FOLLOWING_ARTIST = "https://www.pixiv.net/ajax/user/%s/following";
	private final static String URL_FOLLOWING_ARTIST_TOTAL = "https://www.pixiv.net/ajax/user/extra";

	private final static String URL_FOLLOW_LATEST_IMAGE = "https://www.pixiv.net/ajax/follow_latest/illust";
	private final static String URL_FOLLOW_LATEST_IMAGE_REFERER = "https://www.pixiv.net/bookmark_new_illust.php";
	private final static String URL_SEARCH = "https://www.pixiv.net/ajax/search/artworks/%s";
	private final static String URL_SEARCH_REFERER = "https://www.pixiv.net/tags/%s/artworks";
	private final static String URL_UGOIRA_META = "https://www.pixiv.net/ajax/illust/%s/ugoira_meta";

	public static String getUgoiraMeta(String imageId) {
		return URL_UGOIRA_META.formatted(imageId).toString();
	}

	public static String getSearch_referer(String tag, int page) {
		String encode = URLEncoder.encode(tag, StandardCharsets.UTF_8);
		try {
			URIBuilder builder = new URIBuilder(URL_SEARCH_REFERER.formatted(encode).toString());
			builder.setParameter("s_mode", "s_tag");
			if (page > 1) {
				builder.setParameter("p", Integer.toString(page));
			}
			return builder.build().toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getSearch(String tag, int page) {
		String encode = URLEncoder.encode(tag, StandardCharsets.UTF_8);
		try {
			URIBuilder builder = new URIBuilder(URL_SEARCH.formatted(encode).toString());
			builder.setParameter("word", encode);
			builder.setParameter("order", "date_d");
			builder.setParameter("mode", "all");
			builder.setParameter("p", Integer.toString(page <= 0 ? 1 : page));
			builder.setParameter("s_mode", "s_tag");
			builder.setParameter("type", "all");
			return builder.build().toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** for "referer" of the "follow latest image" request header */
	public static String getFollowLatestImage_referer(int page) {
		if (page <= 1) {
			return URL_FOLLOW_LATEST_IMAGE_REFERER;
		}
		try {
			URIBuilder builder = new URIBuilder(URL_FOLLOW_LATEST_IMAGE_REFERER);
			builder.setParameter("p", Integer.toString(page));
			return builder.build().toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return URL_FOLLOW_LATEST_IMAGE_REFERER;
	}

	public static String getFollowLatestImage(int page) {
		if (page <= 0) {
			page = 1;
		}
		try {
			URIBuilder builder = new URIBuilder(URL_FOLLOW_LATEST_IMAGE);
			builder.setParameter("p", Integer.toString(page));
			builder.setParameter("mode", "all");
			return builder.build().toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getFollowingTotal() {
		return URL_FOLLOWING_ARTIST_TOTAL;
	}

	public static String getUserHome(String userId) {
		return URL_USER_HOME.formatted(userId).toString();
	}

	public static String getUserAllWorks(String userId) {
		return URL_USER_ALL_WORKS.formatted(userId).toString();
	}

	/** for URL of the "following" request */
	public static String getFollowingArtist(String userId, int offset, int limit) {
		try {
			URIBuilder builder = new URIBuilder(URL_FOLLOWING_ARTIST.formatted(userId).toString());
			builder.setParameter("offset", Integer.toString(offset));
			builder.setParameter("limit", Integer.toString(limit));
			builder.setParameter("rest", "show");
			return builder.build().toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** for "referer" of the "following artist" request header */
	public static String getFollowingArtist_referer(String userId, int page) {
		if (page <= 1) {
			return getFollowingArtist(userId);
		}
		try {
			URIBuilder builder = new URIBuilder(URL_FOLLOWING_ARTIST.formatted(userId).toString());
			builder.setParameter("p", Integer.toString(page));
			return builder.toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** without parameter */
	public static String getFollowingArtist(String userId) {
		return URL_FOLLOWING_ARTIST.formatted(userId).toString();
	}

	public static String getImageInfoPage(String imageId) {
		return String.format(URL_IMAGE_INFO_PAGE, imageId).toString();
	}

	public static String getLoginRoot() {
		return URL_LOGIN_ROOT;
	}

	public static String getLoginPage() {
		return URL_LOGIN_PAGE;
	}

	public static String getLoginPost() {
		return URL_LOGIN_POST;
	}

	public static String getImageOriginPage(String imageId) {
		return String.format(URL_IMAGE_ORIGIN_PAGE, imageId).toString();
	}

	public static String getPixiv() {
		return URL_PIXIV;
	}

	public static String getSetLike() {
		return URL_SET_LIKE;
	}

}
