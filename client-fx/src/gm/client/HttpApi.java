package gm.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The single place that talks HTTP to the server: every request of the client goes through here, so the
 * session cookie, the timeout, the closing of every response and the translation of a failed request
 * into a {@link ServerException} are written once.
 */
public class HttpApi {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType XML = MediaType.get("application/xml");
    private static final String NO_BODY = "";
    private static final String CONNECTION_TITLE = "Server not reachable";

    private final String baseUrl;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new SessionCookies())
            .connectTimeout(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
    /** The same client, with the shorter timeout of the automatic updates; it shares the session and the threads. */
    private final OkHttpClient refreshClient = client.newBuilder()
            .connectTimeout(Constants.REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

    public HttpApi() {
        this(Constants.BASE_URL);
    }

    /**
     * @param baseUrl the address of the web application of the server, without a trailing slash
     */
    public HttpApi(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * @param path   the part of the address after the base, for example {@code /events}
     * @param params the query parameters; may be empty
     * @return the body of the answer
     * @throws ServerException with the message of the server when it refused the request, or when it could
     *                         not be reached
     */
    public String get(String path, Map<String, String> params) {
        return execute(new Request.Builder().url(url(path, params)).get().build());
    }

    /**
     * A request that changes data. The details travel in the query, or as JSON in the body.
     *
     * @param json the body, or {@code null} for a request that has none
     */
    public String post(String path, Map<String, String> params, String json) {
        RequestBody body = RequestBody.create(json == null ? NO_BODY : json, JSON);
        return execute(new Request.Builder().url(url(path, params)).post(body).build());
    }

    /**
     * Uploads one file as a multipart request, the way the course example does.
     *
     * @param fileName the name of the file, as the user knows it
     * @param content  the whole content of the file
     */
    public String upload(String path, String fileName, byte[] content) {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, RequestBody.create(content, XML))
                .build();
        return execute(new Request.Builder().url(url(path, Map.of())).post(body).build());
    }

    /**
     * Sends a GET request without waiting for its answer, the way the course example does: the callback is
     * called on a thread of the HTTP client once the server answered or could not be reached. The callback
     * must close the response it receives. Only the automatic updates send their requests this way, so
     * the request waits for the server up to {@link Constants#REFRESH_TIMEOUT_SECONDS} seconds.
     */
    public void getAsync(String path, Map<String, String> params, Callback callback) {
        refreshClient.newCall(new Request.Builder().url(url(path, params)).get().build()).enqueue(callback);
    }

    /**
     * Releases the threads and the connections of the HTTP client, when the application exits.
     */
    public void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    private HttpUrl url(String path, Map<String, String> params) {
        HttpUrl.Builder url = HttpUrl.get(baseUrl + path).newBuilder();
        params.forEach(url::addQueryParameter);
        return url.build();
    }

    /**
     * Sends the request and closes the response whatever happens, even when its body is not read.
     */
    private String execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            String body = response.body() == null ? NO_BODY : response.body().string();
            if (!response.isSuccessful()) {
                throw refusal(response.code(), body);
            }
            return body;
        } catch (IOException e) {
            throw new ServerException(CONNECTION_TITLE, "The server could not be reached at " + baseUrl
                    + ". Start Tomcat and check that it listens on localhost:8080.", e);
        }
    }

    /**
     * The application answers a refusal with a JSON object that has a title and a message. Anything
     * else is an error page of Tomcat itself, which is not a message for the user.
     */
    private static ServerException refusal(int status, String body) {
        try {
            JsonObject answer = JsonParser.parseString(body).getAsJsonObject();
            return new ServerException(answer.get("title").getAsString(), answer.get("message").getAsString());
        } catch (RuntimeException notOurs) {
            return new ServerException("Request refused",
                    "The server refused the request with the status " + status + ".");
        }
    }

    /**
     * Keeps the cookies the server sets (the session) and sends them back with every request.
     */
    private static final class SessionCookies implements CookieJar {

        private final List<Cookie> cookies = new ArrayList<>();

        @Override
        public synchronized void saveFromResponse(HttpUrl url, List<Cookie> received) {
            for (Cookie cookie : received) {
                cookies.removeIf(saved -> saved.name().equals(cookie.name()));
                cookies.add(cookie);
            }
        }

        @Override
        public synchronized List<Cookie> loadForRequest(HttpUrl url) {
            return cookies.stream().filter(cookie -> cookie.matches(url)).toList();
        }
    }
}
