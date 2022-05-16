package uk.gov.di.ipv.cri.kbv.api.helper;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.Header;

import java.util.Collections;
import java.util.Map;

public class ApiGatewayResponse implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final String JSON_CONTENT_TYPE_VALUE = "application/json";
    private static final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule()) // Fix time formats
                    .registerModule(new Jdk8Module()); // Allow Java 8 types (eg: Optional)
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiGatewayResponseGenerator.class);

    public <T> APIGatewayProxyResponseEvent proxyJsonResponse(int statusCode, T body) {
        Map<String, String> responseHeaders = Map.of(Header.CONTENT_TYPE, JSON_CONTENT_TYPE_VALUE);

        try {
            return proxyResponse(statusCode, generateResponseBody(body), responseHeaders);
        } catch (JsonProcessingException e) {
            LOGGER.error("Unable to generateApiGatewayProxyErrorResponse", e);
            return proxyResponse(500, "Internal server error", Collections.emptyMap());
        }
    }

    public static APIGatewayProxyResponseEvent proxyResponse(
            int statusCode, String body, Map<String, String> headers) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent =
                new APIGatewayProxyResponseEvent();
        apiGatewayProxyResponseEvent.setHeaders(headers);
        apiGatewayProxyResponseEvent.setStatusCode(statusCode);
        apiGatewayProxyResponseEvent.setBody(body);

        return apiGatewayProxyResponseEvent;
    }

    protected <T> String generateResponseBody(T body) throws JsonProcessingException {
        return objectMapper.writeValueAsString(body);
    }

    public static APIGatewayProxyResponseEvent proxyJwtResponse(int statusCode, String payload) {
        Map<String, String> responseHeaders = Map.of(HttpHeaders.CONTENT_TYPE, "application/jwt");
        return proxyResponse(statusCode, payload, responseHeaders);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        return null;
    }
}
