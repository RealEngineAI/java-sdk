package ai.realengine;

import ai.realengine.dto.RealEngineAIResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * A client for the RealEngine AI service.
 */
public class RealEngineAIClient {

    private static final TypeReference<RealEngineAIResponse<String>> STRING_RESPONSE_TYPE = new TypeReference<>() {
    };

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_ACCEPTED = 202;
    private static final int SERVER_ERROR = 500;

    private static final long DEFAULT_WAIT_MS = 1000;
    private static final long MAX_BASE_WAIT_MS = TimeUnit.MINUTES.toMillis(1);

    private static final String LOCATION_HEADER = "Location";
    private static final String RETRY_AFTER_HEADER = "X-Retry-After";

    private final OkHttpClient httpClient;
    private final HttpUrl rootUrl;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService executorService;
    private final String token;
    private final int maxRetries;

    /**
     * Create a new client.
     *
     * @param httpClient      the http client to use
     * @param rootUrl         the root url of the service
     * @param mapper          the object mapper to use
     * @param executorService the executor service to use
     * @param token           the token to use
     * @param maxRetries      the maximum number of retries to perform
     */
    public RealEngineAIClient(OkHttpClient httpClient,
                              String rootUrl,
                              ObjectMapper mapper,
                              ScheduledExecutorService executorService,
                              String token,
                              int maxRetries) {
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient must not be null");
        }

        if (rootUrl == null || rootUrl.isEmpty()) {
            throw new IllegalArgumentException("rootUrl must not be null or empty");
        }

        if (mapper == null) {
            throw new IllegalArgumentException("mapper must not be null");
        }

        if (executorService == null) {
            throw new IllegalArgumentException("executorService must not be null");
        }

        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("token must not be null or empty");
        }

        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }

        this.maxRetries = maxRetries;
        HttpUrl parsedRootUrl = HttpUrl.parse(rootUrl);
        if (parsedRootUrl == null) {
            throw new IllegalArgumentException("The rootUrl provided is not valid");
        }

        this.httpClient = httpClient;
        this.rootUrl = parsedRootUrl;
        this.mapper = mapper;
        this.executorService = executorService;
        this.token = token;
    }

    public static RealEngineAIClientBuilder newBuilder() {
        return new RealEngineAIClientBuilder();
    }

    /**
     * Get the caption for an image at the given url.
     *
     * @param url the url of the image to caption
     * @return a future that will be completed with the caption, or an exception if the captioning failed
     */
    public CompletableFuture<String> getCaption(String url) {
        var deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        var requestUrl = rootUrl.newBuilder()
                .addPathSegment("caption")
                .addQueryParameter("url", url)
                .addQueryParameter("deadline", String.valueOf(deadline))
                .build();
        var request = buildRequest(requestUrl);
        var callback = new Callback<>(STRING_RESPONSE_TYPE);

        return call(request, callback);
    }

    private <T> void retryLater(Callback<T> callback, Response response, int retryCount) {
        var baseWaitTime = (long) (DEFAULT_WAIT_MS * Math.pow(2, retryCount));
        var jitter = ThreadLocalRandom.current().nextDouble(0.5, 1.5);
        var retryAfter = (long) (Math.min(MAX_BASE_WAIT_MS, baseWaitTime) * jitter);
        var future = executorService.schedule(
                () -> call(response.request(), callback),
                retryAfter,
                TimeUnit.MILLISECONDS);
        callback.getResult().exceptionally(th -> {
            future.cancel(false);
            return null;
        });
    }

    private <T> void getTaskResult(Callback<T> callback, Response response) {
        var retryAfter = getRetryAfterMs(response);
        var location = getLocation(response);
        if (location == null) {
            callback.getResult()
                    .completeExceptionally(new RealEngineAIException("Location header is missing",
                            response.code(),
                            response.request()
                                    .url()
                                    .encodedPath()));
            return;
        }

        var future = executorService.schedule(
                () -> call(buildRequest(location), callback),
                retryAfter,
                TimeUnit.MILLISECONDS);
        // If the future will be cancelled, cancel the future call
        callback.getResult().exceptionally(th -> {
            future.cancel(false);
            return null;
        });
    }

    private HttpUrl getLocation(Response response) {
        var location = response.header(LOCATION_HEADER);
        if (location == null) {
            return null;
        }

        if (location.startsWith("http")) {
            return HttpUrl.parse(location);
        }

        return rootUrl.resolve(location);
    }

    private long getRetryAfterMs(Response response) {
        var retryHeader = response.header(RETRY_AFTER_HEADER);
        if (retryHeader == null) {
            return DEFAULT_WAIT_MS;
        }

        try {
            var seconds = Double.parseDouble(retryHeader);
            return (long) (seconds * 1000);
        } catch (NumberFormatException e) {
            return DEFAULT_WAIT_MS;
        }
    }

    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    private <T> CompletableFuture<T> call(Request request, Callback<T> callback) {
        var call = httpClient.newCall(request);
        // If the future will be cancelled, cancel the call
        callback.getResult().exceptionally(th -> {
            call.cancel();
            return null;
        });
        call.enqueue(callback);
        return callback.getResult();
    }

    private class Callback<T> implements okhttp3.Callback {
        final CompletableFuture<T> result;
        final TypeReference<RealEngineAIResponse<T>> responseType;

        volatile int retryCount = 0;

        private Callback(TypeReference<RealEngineAIResponse<T>> responseType) {
            this.result = new CompletableFuture<>();
            this.responseType = responseType;
        }

        public CompletableFuture<T> getResult() {
            return result;
        }

        @Override
        public void onResponse(Call call, Response response) {
            try (response) {
                var statusCode = response.code();
                var path = response.request()
                        .url()
                        .encodedPath();

                if (statusCode == HTTP_TOO_MANY_REQUESTS || statusCode >= SERVER_ERROR) {
                    if (retryCount >= maxRetries) {
                        throw new RealEngineAIException("Too many retries",
                                statusCode,
                                path);
                    }

                    // It's ok to increment the retry count without additional synchronization
                    // because there are no concurrent requests
                    // noinspection NonAtomicOperationOnVolatileField
                    retryCount++;
                    retryLater(this, response, retryCount);
                    return;
                }

                retryCount = 0;
                if (statusCode == HTTP_ACCEPTED) {
                    getTaskResult(this, response);
                    return;
                }

                var apiResponse = read(response);
                if (!apiResponse.isSuccess()) {
                    var error = apiResponse.getError();
                    if (error == null) {
                        throw new RealEngineAIException("The response is not successful but the error is null",
                                statusCode,
                                path);
                    }

                    throw new RealEngineAIException(error,
                            statusCode,
                            path);
                }

                result.complete(apiResponse.getData());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        }

        @Override
        public void onFailure(Call call, IOException e) {
            result.completeExceptionally(e);
        }

        private RealEngineAIResponse<T> read(Response response) throws IOException {
            var body = response.body();
            if (body == null) {
                var path = response.request()
                        .url()
                        .encodedPath();
                throw new RealEngineAIException("The response body is null",
                        response.code(),
                        path);
            }

            return mapper.readValue(
                    body.byteStream(),
                    responseType);
        }
    }

}
