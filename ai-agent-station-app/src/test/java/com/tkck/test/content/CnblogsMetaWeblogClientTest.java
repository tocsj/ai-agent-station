package com.tkck.test.content;

import com.tkck.app.content.publish.CnblogsMetaWeblogClient;
import com.tkck.app.content.publish.CnblogsPublishRequest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class CnblogsMetaWeblogClientTest {

    @Test
    public void shouldCreateDraftPostViaMetaWeblog() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CnblogsMetaWeblogClient client = new CnblogsMetaWeblogClient(builder.build());

        server.expect(requestTo("https://rpc.cnblogs.com/metaweblog/demo-blog"))
                .andExpect(header("Content-Type", containsString("text/xml")))
                .andExpect(content().string(containsString("<methodName>metaWeblog.newPost</methodName>")))
                .andExpect(content().string(containsString("<string>demo-blog</string>")))
                .andExpect(content().string(containsString("<string>demo-user</string>")))
                .andExpect(content().string(containsString("<name>title</name>")))
                .andExpect(content().string(containsString("<boolean>0</boolean>")))
                .andRespond(withSuccess("""
                        <?xml version="1.0"?>
                        <methodResponse>
                          <params>
                            <param><value><string>123456</string></value></param>
                          </params>
                        </methodResponse>
                        """, MediaType.TEXT_XML));

        String postId = client.createDraft(CnblogsPublishRequest.builder()
                .endpoint("https://rpc.cnblogs.com/metaweblog/demo-blog")
                .blogId("demo-blog")
                .username("demo-user")
                .token("demo-token")
                .title("MetaWeblog Draft")
                .contentMarkdown("# Hello\n\ncontent")
                .build());

        Assert.assertEquals("123456", postId);
        server.verify();
    }
}
