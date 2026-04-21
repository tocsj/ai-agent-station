package com.tkck.test.content;

import com.tkck.app.content.publish.CnblogsMetaWeblogClient;
import com.tkck.app.content.publish.CnblogsPublishAdapter;
import com.tkck.app.content.publish.CnblogsPublishRequest;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CnblogsPublishAdapterTest {

    @Test
    public void shouldSaveCnblogsDraftWhenConfigVerified() {
        IContentPublishChannelService channelService = mock(IContentPublishChannelService.class);
        CnblogsMetaWeblogClient client = mock(CnblogsMetaWeblogClient.class);
        when(channelService.queryConfig("cnblogs")).thenReturn(ContentPublishChannelConfigEntity.builder()
                .channelCode("cnblogs")
                .authType("metaweblog")
                .credentialJson("""
                        {"endpoint":"https://rpc.cnblogs.com/metaweblog/demo-blog","blogId":"demo-blog","blogApp":"demo-blog","username":"demo-user","token":"demo-token"}
                        """)
                .verifyStatus("VERIFIED")
                .build());
        when(client.createDraft(org.mockito.ArgumentMatchers.any(CnblogsPublishRequest.class))).thenReturn("123456");

        CnblogsPublishAdapter adapter = new CnblogsPublishAdapter(channelService, client);
        PublishResultEntity result = adapter.publish(PublishCommandEntity.builder()
                .taskId(21L)
                .channel("cnblogs")
                .action("save_draft")
                .title("标题")
                .content("正文")
                .build());

        ArgumentCaptor<CnblogsPublishRequest> captor = ArgumentCaptor.forClass(CnblogsPublishRequest.class);
        verify(client).createDraft(captor.capture());
        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals("DRAFT_SAVED", result.getStatus());
        Assert.assertEquals("123456", result.getExternalId());
        Assert.assertEquals("demo-user", captor.getValue().getUsername());
        Assert.assertEquals("标题", captor.getValue().getTitle());
    }
}
