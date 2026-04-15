package com.tkck.test.content;

import com.tkck.app.content.ContentPublishChannelServiceImpl;
import com.tkck.domain.content.model.entity.ChannelVerifyResultEntity;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentPublishChannelServiceTest {

    @Test
    public void shouldSaveQueryVerifyAndRecordChannelConfig() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RestClient restClient = mock(RestClient.class);
        ContentPublishChannelServiceImpl service = new ContentPublishChannelServiceImpl(restClient);
        ReflectionTestUtils.setField(service, "mysqlJdbcTemplate", jdbcTemplate);

        when(jdbcTemplate.queryForMap("SELECT * FROM content_publish_channel_config WHERE channel_code = ?", "juejin"))
                .thenReturn(Map.of(
                        "id", 1L,
                        "channel_code", "juejin",
                        "channel_name", "掘金",
                        "auth_type", "token",
                        "credential_json", "{\"token\":\"abc\"}",
                        "verify_status", "VERIFIED",
                        "verify_message", "token 可用",
                        "status", 1
                ));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM content_publish_channel_config WHERE channel_code = ?"), eq(Integer.class), eq("juejin")))
                .thenReturn(1);
        when(jdbcTemplate.queryForList("SELECT * FROM content_publish_record WHERE task_id = ? ORDER BY id ASC", 21L))
                .thenReturn(List.of(
                        Map.of(
                                "id", 10L,
                                "task_id", 21L,
                                "channel_code", "juejin",
                                "action", "save_draft",
                                "status", "BLOCKED",
                                "error_message", "官方公开 API 暂不支持文章发布"
                        )
                ));

        ContentPublishChannelConfigEntity config = service.saveOrUpdateConfig("juejin", "abc");
        ContentPublishChannelConfigEntity queried = service.queryConfig("juejin");
        ChannelVerifyResultEntity verifyResult = service.verifyJuejinConfig();
        service.recordPublishAttempt(ContentPublishRecordEntity.builder()
                .taskId(21L)
                .channelCode("juejin")
                .action("save_draft")
                .status("BLOCKED")
                .errorMessage("官方公开 API 暂不支持文章发布")
                .build());
        List<ContentPublishRecordEntity> records = service.queryPublishRecords(21L);

        Assert.assertEquals("juejin", config.getChannelCode());
        Assert.assertEquals("VERIFIED", queried.getVerifyStatus());
        Assert.assertEquals("VERIFIED", verifyResult.getVerifyStatus());
        Assert.assertEquals(1, records.size());
        Assert.assertEquals("BLOCKED", records.get(0).getStatus());
    }
}
