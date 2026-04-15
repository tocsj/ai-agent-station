package com.tkck.test.content;

import com.tkck.app.content.publish.DevtoApiClient;
import com.tkck.app.content.publish.DevtoPublishAdapter;
import com.tkck.app.content.publish.DevtoPublishRequest;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DevtoPublishAdapterTest {

    @Test
    public void shouldSaveDevtoDraftWhenConfigVerified() {
        IContentPublishChannelService channelService = mock(IContentPublishChannelService.class);
        DevtoApiClient client = mock(DevtoApiClient.class);
        when(channelService.queryConfig("devto")).thenReturn(ContentPublishChannelConfigEntity.builder()
                .channelCode("devto")
                .authType("api_key")
                .credentialJson("""
                        {"token":"devto-token","baseUrl":"https://dev.to","username":"tester","defaultTags":"java,ai","publishMode":"draft"}
                        """)
                .verifyStatus("VERIFIED")
                .build());
        when(client.createDraft(any(DevtoPublishRequest.class))).thenReturn(Map.of(
                "id", 321,
                "url", "https://dev.to/tester/devto-draft",
                "path", "/tester/devto-draft",
                "published", false
        ));

        DevtoPublishAdapter adapter = new DevtoPublishAdapter(channelService, client);
        PublishResultEntity result = adapter.publish(PublishCommandEntity.builder()
                .taskId(22L)
                .channel("devto")
                .action("save_draft")
                .title("Dev.to Draft")
                .summary("summary")
                .content("# hello")
                .tags("agent")
                .build());

        ArgumentCaptor<DevtoPublishRequest> captor = ArgumentCaptor.forClass(DevtoPublishRequest.class);
        verify(client).createDraft(captor.capture());
        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals("DRAFT_SAVED", result.getStatus());
        Assert.assertEquals("321", result.getExternalId());
        Assert.assertEquals("https://dev.to/tester/devto-draft", result.getExternalUrl());
        Assert.assertEquals("Dev.to Draft", captor.getValue().getTitle());
        Assert.assertTrue(captor.getValue().getTags().contains("agent"));
    }
}
