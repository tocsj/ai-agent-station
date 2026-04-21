package com.tkck.app.content.publish;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DevtoApiClient {

    private static final String USER_AGENT = "ai-agent-station/1.0";

    private final RestClient restClient;

    @Autowired
    public DevtoApiClient(RestClient.Builder restClientBuilder) {
        this(restClientBuilder.build());
    }

    public DevtoApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> verify(String baseUrl, String token) {
        Map<String, Object> response = restClient.get()
                .uri(normalizeBaseUrl(baseUrl) + "/api/users/me")
                .header("api-key", token)
                .header("Accept", "application/vnd.forem.api-v1+json")
                .header("User-Agent", USER_AGENT)
                .retrieve()
                .body(Map.class);
        return response == null ? Map.of() : response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createDraft(DevtoPublishRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", request.getTitle());
        payload.put("body_markdown", request.getBodyMarkdown());
        payload.put("published", false);
        payload.put("description", blankToNull(request.getDescription()));
        payload.put("tags", blankToNull(request.getTags()));

        Map<String, Object> article = restClient.post()
                .uri(normalizeBaseUrl(request.getBaseUrl()) + "/api/articles")
                .header("api-key", request.getToken())
                .header("Accept", "application/vnd.forem.api-v1+json")
                .header("User-Agent", USER_AGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("article", payload))
                .retrieve()
                .body(Map.class);
        return article == null ? Map.of() : article;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "https://dev.to" : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
