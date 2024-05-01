package net.jcraron.pixivcrawler.pixiv;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import net.jcraron.pixivcrawler.util.HttpUtil;

public class PixivHttpHandle extends HttpHandle implements Serializable {

	private static final long serialVersionUID = 3284625448208796057L;
	private String PHPSESSID;

	public PixivHttpHandle(CookieStore cookieStore) {
		super(cookieStore, HttpUtil.rendomUserAgent());
		this.PHPSESSID = this.cookieStore.getCookies().stream().filter(cookie -> cookie.getName().equals("PHPSESSID")).findFirst().orElseThrow().getValue();
	}

	@Deprecated
	protected String login(String username, String password) {
		String post_keyStr = getPostkey();
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("pixiv_id", username));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("post_key", post_keyStr));
		params.add(new BasicNameValuePair("return_to", PixivUrls.getPixiv()));
		return HttpHandle.entityToString(this.postResponse(PixivUrls.getLoginPost(), post -> {
			post.setHeader("referer", PixivUrls.getLoginPage());
			post.setEntity(new UrlEncodedFormEntity(params, Charset.forName("UTF-8")));
		}).getEntity());
	}

	@Deprecated
	private String getPostkey() {
		String responseContent = HttpHandle.entityToString(this.getResponse(PixivUrls.getLoginPage(), null).getEntity());
		Document doc = Jsoup.parse(responseContent);
		Element element = doc.select("input[class=json-data]").get(0);
		String jsonstr = element.attr("value");
		JSONObject json = new JSONObject(jsonstr);
		String postkey = json.get("pixivAccount.postKey").toString();
		return postkey;
	}

	public String getPHPSESSID() {
		return PHPSESSID;
	}

	@Override
	protected void requiredRequest(HttpUriRequest request) {
		super.requiredRequest(request);
//		request.setHeader("cookie", "PHPSESSID=" + PHPSESSID);
		request.setHeader("cookie", getCookieString());
	}

	protected String getCookieString() {
		return cookieToString(this.cookieStore);
	}

	public static String cookieToString(CookieStore cookieStore) {
		StringBuilder builder = new StringBuilder();
		List<Cookie> cookies = cookieStore.getCookies();
		for (int i = 0; i < cookies.size(); i++) {
			Cookie cookie = cookies.get(i);
			builder.append(cookie.getName());
			builder.append("=");
			builder.append(cookie.getValue());
			if (i != cookies.size() - 1) {
				builder.append("; ");
			}
		}
		return builder.toString();
	}

}
