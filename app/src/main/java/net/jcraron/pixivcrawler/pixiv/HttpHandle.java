package net.jcraron.pixivcrawler.pixiv;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.net.ssl.SSLException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;

import net.jcraron.pixivcrawler.util.HttpUtil;

public class HttpHandle implements Closeable, Serializable {

	private static final long serialVersionUID = 2594387467278113960L;
	protected final CookieStore cookieStore;
	protected final String userAgent;
	protected transient CloseableHttpClient client;

	public HttpHandle() {
		this(new BasicCookieStore(), HttpUtil.rendomUserAgent());
	}

	public HttpHandle(CookieStore cookieStore, String userAgent) {
		this.cookieStore = cookieStore;
		this.userAgent = userAgent;
	}

	protected CloseableHttpClient newHttpClient() {
		return HttpClients.custom().setDefaultCookieStore(cookieStore).build();
	}

	protected final CloseableHttpClient getHttpClient() {
		if (client == null) {
			client = this.newHttpClient();
		}
		return client;
	}

	public HttpResponse getResponse(String url, @Nullable Consumer<HttpGet> getRequestConfig) {
		return response(new HttpGet(url), getRequestConfig);
	}

	public HttpResponse postResponse(String url, @Nullable Consumer<HttpPost> postRequestConfig) {
		return response(new HttpPost(url), postRequestConfig);
	}

	public synchronized <R extends HttpUriRequest> HttpResponse response(R request, @Nullable Consumer<R> requestConfig) {
		BasicHttpResponse responseCopy = null;
		requiredRequest(request);
		if (requestConfig != null) {
			requestConfig.accept(request);
		}
		try (CloseableHttpResponse response = getHttpClient().execute(request)) {
			BufferedHttpEntity responseContent = new BufferedHttpEntity(response.getEntity());
			EntityUtils.consume(response.getEntity());
			responseCopy = new BasicHttpResponse(response.getStatusLine());
			responseCopy.setEntity(responseContent);
		} catch (SSLException e) {
			try {
				if (client != null) {
					client.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.err.println("HttpClient Exception : try to set new HttpClient");
			client = this.newHttpClient();
			return response(request, requestConfig);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseCopy;
	}

	public static boolean isError_400(HttpResponse reponse) {
		return reponse.getStatusLine().getStatusCode() / 100 == 4;
	}

	public static String entityToString(HttpEntity entity) {
		String str = "";
		try {
			str = EntityUtils.toString(entity, "utf-8");
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return str;
	}

	protected void requiredRequest(HttpUriRequest request) {
		request.setHeader(HttpHeaders.USER_AGENT, userAgent);
	}

	@Override
	public void close() throws IOException {
		getHttpClient().close();
	}

	public CookieStore getCookieStore() {
		return cookieStore;
	}

}
