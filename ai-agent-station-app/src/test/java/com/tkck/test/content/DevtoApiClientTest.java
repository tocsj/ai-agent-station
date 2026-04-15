package com.tkck.test.content;

import com.tkck.app.content.publish.DevtoApiClient;
import com.tkck.app.content.publish.DevtoPublishRequest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class DevtoApiClientTest {

    @Test
    public void shouldVerifyTokenAndCreateDraftArticle() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DevtoApiClient client = new DevtoApiClient(builder.build());

        server.expect(requestTo("https://dev.to/api/users/me"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header("api-key", "devto-token"))
                .andExpect(header("User-Agent", "ai-agent-station/1.0"))
                .andRespond(withSuccess("""
                        {"id":1,"name":"tester","username":"tester"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://dev.to/api/articles"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("api-key", "devto-token"))
                .andExpect(header("User-Agent", "ai-agent-station/1.0"))
                .andExpect(content().string(containsString("\"published\":false")))
                .andExpect(content().string(containsString("\"title\":\"Dev.to Draft\"")))
                .andRespond(withSuccess("""
                        {"id":321,"title":"Dev.to Draft","path":"/tester/devto-draft","url":"https://dev.to/tester/devto-draft","published":false}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> profile = client.verify("https://dev.to", "devto-token");
        Map<String, Object> article = client.createDraft(DevtoPublishRequest.builder()
                .baseUrl("https://dev.to")
                .token("devto-token")
                .title("Dev.to Draft")
                .bodyMarkdown("# hello")
                .description("summary")
                .tags("java,ai")
                .build());

        Assert.assertEquals("tester", profile.get("username"));
        Assert.assertEquals(321, article.get("id"));
        Assert.assertEquals(Boolean.FALSE, article.get("published"));
        server.verify();
    }
}
