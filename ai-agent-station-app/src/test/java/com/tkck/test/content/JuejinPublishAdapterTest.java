package com.tkck.test.content;

import com.tkck.app.content.publish.JuejinPublishAdapter;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JuejinPublishAdapterTest {

    @Test
    public void shouldBlockWhenOfficialArticlePublishApiIsUnavailable() {
        IContentPublishChannelService channelService = mock(IContentPublishChannelService.class);
        when(channelService.queryConfig("juejin")).thenReturn(ContentPublishChannelConfigEntity.builder()
                .channelCode("juejin")
                .verifyStatus("VERIFIED")
                .verifyMessage("token 可用")
                .build());
        JuejinPublishAdapter adapter = new JuejinPublishAdapter(channelService);

        PublishResultEntity result = adapter.publish(PublishCommandEntity.builder()
                .channel("juejin")
                .action("save_draft")
                .title("标题")
                .content("正文")
                .taskId(21L)
                .build());

        Assert.assertEquals("BLOCKED", result.getStatus());
        Assert.assertTrue(result.getMessage().contains("官方公开 API"));
    }
}
