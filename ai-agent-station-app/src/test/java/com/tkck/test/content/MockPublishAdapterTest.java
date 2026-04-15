package com.tkck.test.content;

import com.tkck.app.content.publish.MockPublishAdapter;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import org.junit.Assert;
import org.junit.Test;

public class MockPublishAdapterTest {

    @Test
    public void shouldReturnDraftSavedResult() {
        MockPublishAdapter adapter = new MockPublishAdapter();

        PublishResultEntity result = adapter.publish(PublishCommandEntity.builder()
                .taskId(18L)
                .channel("mock")
                .title("AI Agent 平台")
                .content("draft")
                .build());

        Assert.assertTrue(result.getSuccess());
        Assert.assertEquals("mock", result.getChannel());
        Assert.assertEquals("DRAFT_SAVED", result.getStatus());
        Assert.assertTrue(result.getExternalId().startsWith("draft_"));
        Assert.assertTrue(result.getExternalUrl().contains(result.getExternalId()));
    }
}
