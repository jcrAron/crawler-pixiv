package net.jcraron.pixivcrawler.pixiv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import net.jcraron.pixivcrawler.ImageInfo;
import net.jcraron.pixivcrawler.util.StreamUtil;

public class HPixiv implements Pixiv {
	private PixivHttpHandle httpHandle;
	private final String userId;

	public HPixiv(PixivHttpHandle httpHandle) {
		this.httpHandle = httpHandle;
		this.userId = httpHandle.getPHPSESSID().split("_", 2)[0];
	}

	public PixivHttpHandle getHttpHandle() {
		return httpHandle;
	}

	public final String getUserId() {
		return userId;
	}

	@Override
	public ImageInfoImp getImageInfo(String imageId) {
		return new ImageInfoImp(imageId);
	}

	@Override
	public ImageInfoImp[] getImageInfo_artist(String authorId) {
		HttpEntity entity = this.httpHandle.getResponse(PixivUrls.getUserAllWorks(authorId), (get) -> {
			get.setHeader("referer", PixivUrls.getUserHome(authorId));
			get.setHeader("x-user-id", userId);
		}).getEntity();
		JSONObject json = checkJsonRequest("getImageInfo_artist", entity);
		JSONObject body = json.getJSONObject("body");
		Set<String> idSet = new HashSet<>();
		idSet.addAll(jsonObjToSet(body.opt("illusts")));
		idSet.addAll(jsonObjToSet(body.opt("manga")));
		return idSet.stream().map(this::getImageInfo).toArray(i -> new ImageInfoImp[i]);
	}

	private Set<String> jsonObjToSet(Object container) {
		if (container instanceof JSONObject) {
			return ((JSONObject) container).keySet();
		}
		return Set.of();
	}

	/**
	 * Up to 60 objects per page
	 * 
	 * @param page start from 1
	 */
	@Override
	public ImageInfoImp[] getImageInfo_search(String search, int page) {
		if (page <= 0) {
			page = 1;
		}
		final int finPage = page;
		HttpEntity entity = this.httpHandle.getResponse(PixivUrls.getSearch(search, finPage), (get) -> {
			get.setHeader("referer", PixivUrls.getSearch_referer(search, finPage));
			get.setHeader("x-user-id", userId);
		}).getEntity();
		JSONObject json = checkJsonRequest("getImageInfo_search", entity);
		return json.getJSONObject("body").getJSONObject("illustManga").getJSONArray("data").toList().stream().map(map -> new JSONObject((Map<?, ?>) map))
				.map(userObj -> this.getImageInfo(userObj.getString("id"))).toArray(i -> new ImageInfoImp[i]);
	}

	/**
	 * Unable to get all image. Up to 60 objects per page.
	 * 
	 * @param page start from 1; until 35(suggestion)
	 */
	@Override
	public ImageInfoImp[] getNewestImageFromFollowing(int page) {
		if (page <= 0) {
			page = 1;
		}
		final int finPage = page;
		HttpEntity entity = this.httpHandle.getResponse(PixivUrls.getFollowLatestImage(finPage), (get) -> {
			get.setHeader("referer", PixivUrls.getFollowLatestImage_referer(finPage));
			get.setHeader("x-user-id", userId);
		}).getEntity();
		JSONObject json = checkJsonRequest("getNewestImageFromFollowing", entity);
		return json.getJSONObject("body").getJSONObject("page").getJSONArray("ids").toList().stream().<ImageInfoImp>map(obj -> getImageInfo(Integer.toString((int) obj)))
				.toArray(i -> new ImageInfoImp[i]);
	}

	/**
	 * Up to 24 objects per page
	 * 
	 * @param page start from 1
	 */
	@Override
	public String[] getFollowing(int page) {
		if (page <= 0) {
			page = 1;
		}
		final int finPage = page;
		HttpEntity entity = this.httpHandle.getResponse(PixivUrls.getFollowingArtist(userId, 24 * finPage, 24), (get) -> {
			get.setHeader("referer", PixivUrls.getFollowingArtist_referer(userId, finPage));
			get.setHeader("x-user-id", userId);
		}).getEntity();
		JSONObject json = checkJsonRequest("getFollowing", entity);
		return json.getJSONObject("body").getJSONArray("users").toList().stream().map(map -> new JSONObject((Map<?, ?>) map)).map(userObj -> userObj.getString("userId")).toArray(i -> new String[i]);
	}

	public int getFollowingTotalAmount() {
		HttpEntity entity = this.httpHandle.getResponse(PixivUrls.getFollowingTotal(), (get) -> {
			get.setHeader("referer", PixivUrls.getUserHome(userId));
			get.setHeader("x-user-id", userId);
		}).getEntity();
		JSONObject json = checkJsonRequest("getFollowingTotal", entity);
		return json.getJSONObject("body").getInt("following");
	}

	protected JSONObject checkJsonRequest(String functionName, HttpEntity entity, @Nullable String reason) {
		JSONObject json = new JSONObject(HttpHandle.entityToString(entity));
		if (json.optBoolean("error", true)) {
			throw new RuntimeException(functionName + " request failed : " + json.optString("message", json.toString() + "; reason : " + reason != null ? reason : ""));
		}
		return json;
	}

	protected JSONObject checkJsonRequest(String functionName, HttpEntity entity) {
		return checkJsonRequest(functionName, entity, null);
	}

	public class ImageInfoImp implements ImageInfo {

		private JSONObject infoJson;
		private JSONObject globalJson;
		private String imageId;

		public ImageInfoImp(String imageId) {
			this.imageId = imageId;
		}

		public boolean isValidImageId() {
			try {
				getInfoJson();
				return true;
			} catch (RuntimeException e) {
				return false;
			}
		}

		protected void postJson() {
			HttpResponse reponse = httpHandle.getResponse(PixivUrls.getImageInfoPage(imageId), null);
			if (HttpHandle.isError_400(reponse)) {
				throw new RuntimeException("http error; status code:" + reponse.getStatusLine().getStatusCode());
			}
			String infoPage = HttpHandle.entityToString(reponse.getEntity());
			Document doc = Jsoup.parse(infoPage);
			Element globalData = doc.select("meta[name=global-data],meta[id=meta-global-data]").get(0);
			String globalJsonstr = globalData.attr("content");
			this.globalJson = new JSONObject(globalJsonstr);
			Element preloadData = doc.select("meta[name=preload-data],meta[id=meta-preload-data]").get(0);
			String preloadJsonstr = preloadData.attr("content");
			this.infoJson = new JSONObject(preloadJsonstr);
		}

		public JSONObject getInfoJson() {
			if (infoJson == null) {
				postJson();
			}
			return infoJson;
		}

		public JSONObject getGlobalJson() {
			if (globalJson == null) {
				postJson();
			}
			return globalJson;
		}

		@Override
		public String getImageId() {
			return imageId;
		}

		@Override
		public String getArtistId() {
			return getInfoJson().getJSONObject("illust").getJSONObject(imageId).getString("userId");
		}

		@Override
		public String getArtistName() {
			return getInfoJson().getJSONObject("illust").getJSONObject(imageId).getString("userName");
		}

		public String getImageName() {
			return getInfoJson().getJSONObject("illust").getJSONObject(imageId).getString("title");
		}

		@Override
		public String[] getTags() {
			return getInfoJson().getJSONObject("illust").getJSONObject(imageId).getJSONObject("tags").getJSONArray("tags").toList().stream().map(map -> new JSONObject((Map<?, ?>) map))
					.map(obj -> obj.getString("tag")).toArray((i -> new String[i]));
		}

		@Override
		public int getImageCount() {
			return getInfoJson().getJSONObject("illust").getJSONObject(imageId).getInt("pageCount");
		}

		@Override
		public boolean isAnimation() {
			return getInfoJson().getJSONObject("illust").getJSONObject(imageId).getJSONObject("urls").getString("original").contains("ugoira");
		}

		@Override
		public Iterator<InputStream> getImages() {
			if (isAnimation()) {
				JSONObject metaJsonBody = checkJsonRequest("getImages_ugoira", httpHandle.getResponse(PixivUrls.getUgoiraMeta(imageId), get -> {
					get.setHeader("referer", PixivUrls.getImageInfoPage(imageId));
					get.setHeader("x-user-id", userId);
				}).getEntity(), this.getImageId()).getJSONObject("body");

				HttpEntity zipEntity = httpHandle.getResponse(metaJsonBody.getString("originalSrc"), get -> {
					get.setHeader("referer", PixivUrls.getImageInfoPage(imageId));
					get.setHeader("x-user-id", userId);
				}).getEntity();

				InputStream retInputStream = null;
				try (InputStream originZip = zipEntity.getContent(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
					try (ZipOutputStream zip = new ZipOutputStream(output)) {
						StreamUtil.readZipedStream(originZip, zip);
						byte[] metaBytes = new JSONObject().put("ugokuIllustData", metaJsonBody).toString().getBytes();
						ZipEntry zipentry = new ZipEntry("animation.json");
						zip.putNextEntry(zipentry);
						zip.write(metaBytes);
						zip.closeEntry();
					}
					retInputStream = new ByteArrayInputStream(output.toByteArray());
				} catch (UnsupportedOperationException | IOException e) {
					e.printStackTrace();
				}
				return List.<InputStream>of(retInputStream == null ? ByteArrayInputStream.nullInputStream() : retInputStream).iterator();
			} else {
				HttpEntity buffer = httpHandle.getResponse(PixivUrls.getImageOriginPage(imageId), null).getEntity();
				JSONObject json = new JSONObject(HttpHandle.entityToString(buffer));
				Iterator<InputStream> inputs = json.getJSONArray("body").toList().stream().map(map -> new JSONObject((Map<?, ?>) map)).map(obj -> obj.getJSONObject("urls").getString("original"))
						.map(this::createImageInputStream).iterator();
				return inputs;
			}
		}

		private InputStream createImageInputStream(String url) {
			HttpEntity buffer = httpHandle.getResponse(url, (get) -> {
				get.setHeader("referer", PixivUrls.getPixiv());
			}).getEntity();
			try {
				return buffer.getContent();
			} catch (UnsupportedOperationException | IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void setBookmark() {
			JSONObject payload = new JSONObject();
			payload.put("illust_id", imageId);
			payload.put("restrict", 0);
			payload.put("comment", "");
			payload.put("tags", new JSONArray());
			HttpEntity entity = httpHandle.postResponse(PixivUrls.getSetLike(), post -> {
				post.setHeader("referer", PixivUrls.getImageInfoPage(imageId));
				post.setHeader("x-csrf-token", getGlobalJson().getString("token"));
				post.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
			}).getEntity();
			checkJsonRequest("setLike", entity, this.getImageId());
		}
	}
}
