package com.tkck.test.content;

import com.tkck.app.content.ContentPublishChannelServiceImpl;
import com.tkck.app.content.publish.CnblogsMetaWeblogClient;
import com.tkck.app.content.publish.DevtoApiClient;
import com.tkck.domain.content.adapter.repository.IContentPublishChannelRepository;
import com.tkck.domain.content.model.entity.ChannelVerifyResultEntity;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentPublishChannelServiceTest {

    @Test
    public void shouldSaveQueryVerifyAndRecordChannelConfig() {
        IContentPublishChannelRepository repository = mock(IContentPublishChannelRepository.class);
        RestClient restClient = mock(RestClient.class);
        ContentPublishChannelServiceImpl service = new ContentPublishChannelServiceImpl(restClient);
        ReflectionTestUtils.setField(service, "contentPublishChannelRepository", repository);

        when(repository.existsChannelConfig("juejin")).thenReturn(true);
        when(repository.queryConfig("juejin"))
                .thenReturn(ContentPublishChannelConfigEntity.builder()
                        .id(1L)
                        .channelCode("juejin")
                        .channelName("掘金")
                        .authType("token")
                        .credentialJson("{\"token\":\"abc\"}")
                        .verifyStatus("VERIFIED")
                        .verifyMessage("token 可用")
                        .status(1)
                        .build());
        when(repository.queryPublishRecords(21L))
                .thenReturn(List.of(ContentPublishRecordEntity.builder()
                        .id(10L)
                        .taskId(21L)
                        .channelCode("juejin")
                        .action("save_draft")
                        .status("BLOCKED")
                        .errorMessage("官方公开 API 暂不支持文章发布")
                        .build()));

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

    @Test
    public void shouldClassifyDevtoForbiddenErrorClearly() {
        IContentPublishChannelRepository repository = mock(IContentPublishChannelRepository.class);
        RestClient restClient = mock(RestClient.class);
        DevtoApiClient devtoApiClient = mock(DevtoApiClient.class);
        ContentPublishChannelServiceImpl service = new ContentPublishChannelServiceImpl(
                restClient,
                mock(CnblogsMetaWeblogClient.class),
                devtoApiClient);
        ReflectionTestUtils.setField(service, "contentPublishChannelRepository", repository);

        when(repository.queryConfig("devto"))
                .thenReturn(ContentPublishChannelConfigEntity.builder()
                        .id(2L)
                        .channelCode("devto")
                        .channelName("Dev.to")
                        .authType("api_key")
                        .credentialJson("{\"token\":\"devto-token\",\"baseUrl\":\"https://dev.to\",\"username\":\"tester\"}")
                        .verifyStatus("UNVERIFIED")
                        .verifyMessage("待验证")
                        .status(1)
                        .build());
        when(devtoApiClient.verify("https://dev.to", "devto-token"))
                .thenThrow(new HttpClientErrorException(org.springframework.http.HttpStatus.FORBIDDEN, "Forbidden"));

        ChannelVerifyResultEntity result = service.verifyDevtoConfig();

        Assert.assertEquals("FAILED", result.getVerifyStatus());
        Assert.assertTrue(result.getMessage().contains("HTTP 403"));
        Assert.assertTrue(result.getMessage().contains("API Key"));
    }
}
