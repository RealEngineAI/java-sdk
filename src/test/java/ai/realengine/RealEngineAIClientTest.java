package ai.realengine;

import ai.realengine.dto.ErrorDTO;
import ai.realengine.dto.RealEngineAIResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RealEngineAIClientTest {

    static final ObjectMapper objectMapper = new ObjectMapper();

    RealEngineAIClient client;

    MockWebServer mockWebServer;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = RealEngineAIClient.newBuilder()
                .setToken("test-token")
                .setRootUrl(mockWebServer.url("/").toString())
                .build();
    }

    @AfterEach
    void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getCaptionSuccessTest() throws Exception {
        // Given
        var apiResponse = new RealEngineAIResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setData("This is a test caption");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(apiResponse)));

        // When
        var actualCaption = client.getCaption("http://example.com/testImage").get();

        // Then
        assertEquals("This is a test caption", actualCaption);

        var request = mockWebServer.takeRequest();
        var requestUrl = request.getRequestUrl();
        assertNotNull(requestUrl);
        assertEquals("/caption", requestUrl.encodedPath());
        assertEquals("GET", request.getMethod());
        assertEquals("http://example.com/testImage", requestUrl.queryParameter("url"));
        assertEquals("Bearer test-token", request.getHeader("Authorization"));
    }

    @Test
    void getCaptionFailureTest() throws Exception {
        // Given
        var error = new ErrorDTO();
        error.setId("test-error-id");
        error.setMsg("The link is not accessible");

        var apiResponse = new RealEngineAIResponse<>();
        apiResponse.setSuccess(false);
        apiResponse.setError(error);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody(objectMapper.writeValueAsString(apiResponse)));

        // When
        Throwable th = null;
        try {
            client.getCaption("http://example.com/testImage").get();
        } catch (ExecutionException e) {
            th = e.getCause();
        }

        // Then
        assertNotNull(th);
        assertEquals("Error id: test-error-id, message: The link is not accessible, http status: 400, path: /caption",
                th.getMessage());
    }

    @Test
    void getCaptionFailureRetries() throws Exception {
        // Given
        var error = new ErrorDTO();
        error.setId("test-error-id");
        error.setMsg("Something went wrong");

        var errorResponse = new RealEngineAIResponse<Void>();
        errorResponse.setError(error);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody(objectMapper.writeValueAsString(errorResponse)));

        var successResponse = new RealEngineAIResponse<>();
        successResponse.setSuccess(true);
        successResponse.setData("This is a test caption");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(successResponse)));

        // When
        var actualCaption = client.getCaption("http://example.com/testImage").get();

        // Then
        assertEquals("This is a test caption", actualCaption);

        for (int i = 0; i < 2; i++) {
            var request = mockWebServer.takeRequest();
            var requestUrl = request.getRequestUrl();
            assertNotNull(requestUrl);
            assertEquals("/caption", requestUrl.encodedPath());
            assertEquals("GET", request.getMethod());
            assertEquals("http://example.com/testImage", requestUrl.queryParameter("url"));
            assertEquals("Bearer test-token", request.getHeader("Authorization"));
        }
    }

    @Test
    void getCaptionNotReady() throws Exception {
        // Given
        var voidAPIResponse = new RealEngineAIResponse<Void>();
        voidAPIResponse.setSuccess(true);
        var response = new MockResponse()
                .setResponseCode(202)
                .addHeader("Location", "/task?id=test-task-id")
                .setBody(objectMapper.writeValueAsString(voidAPIResponse));

        // two not ready responses
        mockWebServer.enqueue(response);
        mockWebServer.enqueue(response);

        var apiResponse = new RealEngineAIResponse<String>();
        apiResponse.setSuccess(true);
        apiResponse.setData("This is a test caption");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(apiResponse)));

        // When
        var actualCaption = client.getCaption("http://example.com/testImage").get();

        // Then
        assertEquals("This is a test caption", actualCaption);

        // Check first request
        var request = mockWebServer.takeRequest();
        var requestUrl = request.getRequestUrl();
        assertNotNull(requestUrl);
        assertEquals("/caption", requestUrl.encodedPath());
        assertEquals("GET", request.getMethod());
        assertEquals("http://example.com/testImage", requestUrl.queryParameter("url"));
        assertEquals("Bearer test-token", request.getHeader("Authorization"));

        // Check two task requests
        for (int i = 0; i < 2; i++) {
            request = mockWebServer.takeRequest();
            requestUrl = request.getRequestUrl();
            assertNotNull(requestUrl);
            assertEquals("/task", requestUrl.encodedPath());
            assertEquals("GET", request.getMethod());
            assertEquals("test-task-id", requestUrl.queryParameter("id"));
            assertEquals("Bearer test-token", request.getHeader("Authorization"));
        }
    }

}