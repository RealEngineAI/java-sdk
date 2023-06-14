package ai.realengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RealEngineAIClientBuilder {

    private String token;

    private Duration connectTimeout = Duration.ofMillis(500);
    private Duration readTimeout = Duration.ofSeconds(2);
    private Duration writeTimeout = Duration.ofSeconds(2);

    private int maxIdleConnections = 5;
    private int maxConcurrentRequests = 5;
    private Duration keepAliveDuration = Duration.ofMinutes(5);

    private String rootUrl = "https://api.realengine.ai";

    private ObjectMapper objectMapper;
    private ScheduledExecutorService executorService;
    private int maxRetries = 5;

    /**
     * Set the authentication token to use.
     *
     * @param token the token to use
     */
    public RealEngineAIClientBuilder setToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or blank");
        }

        this.token = token;
        return this;
    }

    /**
     * Set the connect timeout.
     * The connect timeout is the timeout for establishing a TCP connection.
     * The default value is 500ms.
     */
    public RealEngineAIClientBuilder setConnectTimeout(Duration connectTimeout) {
        if (connectTimeout.toMillis() < 0) {
            throw new IllegalArgumentException("Connect timeout must not be negative");
        }

        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Set the read timeout.
     * The read timeout is the timeout for reading data from the server.
     * The default value is 2s.
     */
    public RealEngineAIClientBuilder setReadTimeout(Duration readTimeout) {
        if (readTimeout.toMillis() < 0) {
            throw new IllegalArgumentException("Read timeout must not be negative");
        }

        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * Set the write timeout.
     * The write timeout is the timeout for writing data to the server.
     * The default value is 2s.
     */
    public RealEngineAIClientBuilder setWriteTimeout(Duration writeTimeout) {
        if (writeTimeout.toMillis() < 0) {
            throw new IllegalArgumentException("Write timeout must not be negative");
        }

        this.writeTimeout = writeTimeout;
        return this;
    }

    /**
     * Set the maximum number of idle connections.
     * The default value is 5.
     */
    public RealEngineAIClientBuilder setMaxIdleConnections(int maxIdleConnections) {
        if (maxIdleConnections < 0) {
            throw new IllegalArgumentException("Max idle connections must not be negative");
        }

        this.maxIdleConnections = maxIdleConnections;
        return this;
    }

    /**
     * Set the maximum number of concurrent requests.
     * The default value is 5.
     */
    public RealEngineAIClientBuilder setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
        return this;
    }

    /**
     * Set the keep alive duration.
     * The keep alive duration is the duration after which idle connections are closed.
     * The default value is 5 minutes.
     */
    public RealEngineAIClientBuilder setKeepAliveDuration(Duration keepAliveDuration) {
        if (keepAliveDuration.toMillis() < 0) {
            throw new IllegalArgumentException("Keep alive duration must not be negative");
        }

        this.keepAliveDuration = keepAliveDuration;
        return this;
    }

    /**
     * Set the root URL.
     * The default value is <a href="https://api.realengine.ai">https://api.realengine.ai</a>
     */
    public RealEngineAIClientBuilder setRootUrl(String rootUrl) {
        if (rootUrl == null || rootUrl.isBlank()) {
            throw new IllegalArgumentException("Root URL must not be null or blank");
        }

        this.rootUrl = rootUrl;
        return this;
    }

    /**
     * Set the object mapper.
     */
    public RealEngineAIClientBuilder setObjectMapper(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("Object mapper must not be null");
        }

        this.objectMapper = objectMapper;
        return this;
    }

    /**
     * Set the executor service.
     * The executor service is used for scheduling the polling of the task status.
     */
    public RealEngineAIClientBuilder setExecutorService(ScheduledExecutorService executorService) {
        if (executorService == null) {
            throw new IllegalArgumentException("Executor service must not be null");
        }

        this.executorService = executorService;
        return this;
    }

    /**
     * Set the maximum number of retries.
     * The default value is 5.
     */
    public RealEngineAIClientBuilder setMaxRetries(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries must not be negative");
        }

        this.maxRetries = maxRetries;
        return this;
    }

    public RealEngineAIClient build() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Token must be set");
        }

        var dispatcher = new okhttp3.Dispatcher();
        dispatcher.setMaxRequests(maxConcurrentRequests);
        dispatcher.setMaxRequestsPerHost(maxConcurrentRequests);

        var httpClient = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout)
                .connectionPool(new ConnectionPool(
                        maxIdleConnections,
                        keepAliveDuration.toMillis(),
                        TimeUnit.MILLISECONDS))
                .build();

        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        if (executorService == null) {
            executorService = Executors.newSingleThreadScheduledExecutor();
        }

        return new RealEngineAIClient(httpClient,
                rootUrl,
                objectMapper,
                executorService,
                token,
                maxRetries);
    }

}
