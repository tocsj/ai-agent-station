package com.tkck.test.content;

import com.tkck.api.dto.ContentPublishChannelConfigResponseDTO;
import com.tkck.api.dto.ContentPublishChannelConfigSaveRequestDTO;
import com.tkck.api.dto.ContentPublishRecordResponseDTO;
import com.tkck.api.dto.ContentPublishVerifyResponseDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.content.model.entity.ChannelVerifyResultEntity;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import com.tkck.trigger.http.ContentPublishChannelController;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentPublishChannelControllerContractTest {

    @Test
    public void shouldSaveQueryVerifyAndListPublishRecords() {
        IContentPublishChannelService service = mock(IContentPublishChannelService.class);
        ContentPublishChannelController controller = new ContentPublishChannelController();
        ReflectionTestUtils.setField(controller, "contentPublishChannelService", service);

        when(service.saveOrUpdateConfig("juejin", "abc", null, null, null, null)).thenReturn(ContentPublishChannelConfigEntity.builder()
                .channelCode("juejin")
                .channelName("掘金")
                .authType("token")
                .verifyStatus("UNVERIFIED")
                .verifyMessage("待验证")
                .status(1)
                .build());
        when(service.queryConfig("juejin")).thenReturn(ContentPublishChannelConfigEntity.builder()
                .channelCode("juejin")
                .channelName("掘金")
                .authType("token")
                .verifyStatus("VERIFIED")
                .verifyMessage("token 可用")
                .status(1)
                .build());
        when(service.verifyJuejinConfig()).thenReturn(ChannelVerifyResultEntity.builder()
                .channel("juejin")
                .verified(true)
                .verifyStatus("VERIFIED")
                .message("token 可用")
                .build());
        when(service.queryPublishRecords(21L)).thenReturn(List.of(
                ContentPublishRecordEntity.builder()
                        .id(10L)
                        .taskId(21L)
                        .channelCode("juejin")
                        .action("save_draft")
                        .status("BLOCKED")
                        .errorMessage("官方公开 API 暂不支持文章发布")
                        .build()
        ));

        Response<ContentPublishChannelConfigResponseDTO> saveResponse = controller.saveConfig(ContentPublishChannelConfigSaveRequestDTO.builder()
                .channel("juejin")
                .token("abc")
                .build());
        Response<ContentPublishChannelConfigResponseDTO> queryResponse = controller.queryConfig("juejin");
        Response<ContentPublishVerifyResponseDTO> verifyResponse = controller.verifyJuejin();
        Response<List<ContentPublishRecordResponseDTO>> recordsResponse = controller.queryPublishRecords(21L);

        Assert.assertEquals("0000", saveResponse.getCode());
        Assert.assertEquals("juejin", saveResponse.getData().getChannel());
        Assert.assertEquals("VERIFIED", queryResponse.getData().getVerifyStatus());
        Assert.assertTrue(verifyResponse.getData().getVerified());
        Assert.assertEquals(1, recordsResponse.getData().size());
    }

    @Test
    public void shouldSaveAndVerifyCnblogsChannelConfig() {
        IContentPublishChannelService service = mock(IContentPublishChannelService.class);
        ContentPublishChannelController controller = new ContentPublishChannelController();
        ReflectionTestUtils.setField(controller, "contentPublishChannelService", service);

        when(service.saveOrUpdateConfig(
                "cnblogs",
                "meta-token",
                "demo-blog",
                "demo-blog",
                "demo-user",
                "https://rpc.cnblogs.com/metaweblog/demo-blog"))
                .thenReturn(ContentPublishChannelConfigEntity.builder()
                        .channelCode("cnblogs")
                        .channelName("博客园")
                        .authType("metaweblog")
                        .verifyStatus("UNVERIFIED")
                        .verifyMessage("待验证")
                        .status(1)
                        .build());
        when(service.verifyCnblogsConfig()).thenReturn(ChannelVerifyResultEntity.builder()
                .channel("cnblogs")
                .verified(true)
                .verifyStatus("VERIFIED")
                .message("MetaWeblog 配置可用")
                .build());

        Response<ContentPublishChannelConfigResponseDTO> saveResponse = controller.saveConfig(ContentPublishChannelConfigSaveRequestDTO.builder()
                .channel("cnblogs")
                .token("meta-token")
                .blogApp("demo-blog")
                .blogId("demo-blog")
                .username("demo-user")
                .endpoint("https://rpc.cnblogs.com/metaweblog/demo-blog")
                .build());
        Response<ContentPublishVerifyResponseDTO> verifyResponse = controller.verifyCnblogs();

        Assert.assertEquals("0000", saveResponse.getCode());
        Assert.assertEquals("cnblogs", saveResponse.getData().getChannel());
        Assert.assertEquals("metaweblog", saveResponse.getData().getAuthType());
        Assert.assertTrue(verifyResponse.getData().getVerified());
    }

    @Test
    public void shouldSaveAndVerifyDevtoChannelConfig() {
        IContentPublishChannelService service = mock(IContentPublishChannelService.class);
        ContentPublishChannelController controller = new ContentPublishChannelController();
        ReflectionTestUtils.setField(controller, "contentPublishChannelService", service);

        when(service.saveOrUpdateConfig(
                "devto",
                "devto-token",
                null,
                null,
                "tester",
                "https://dev.to"))
                .thenReturn(ContentPublishChannelConfigEntity.builder()
                        .channelCode("devto")
                        .channelName("Dev.to")
                        .authType("api_key")
                        .verifyStatus("UNVERIFIED")
                        .verifyMessage("待验证")
                        .status(1)
                        .build());
        when(service.verifyDevtoConfig()).thenReturn(ChannelVerifyResultEntity.builder()
                .channel("devto")
                .verified(true)
                .verifyStatus("VERIFIED")
                .message("Dev.to API Key 可用")
                .build());

        Response<ContentPublishChannelConfigResponseDTO> saveResponse = controller.saveConfig(ContentPublishChannelConfigSaveRequestDTO.builder()
                .channel("devto")
                .token("devto-token")
                .username("tester")
                .endpoint("https://dev.to")
                .build());
        Response<ContentPublishVerifyResponseDTO> verifyResponse = controller.verifyDevto();

        Assert.assertEquals("0000", saveResponse.getCode());
        Assert.assertEquals("devto", saveResponse.getData().getChannel());
        Assert.assertEquals("api_key", saveResponse.getData().getAuthType());
        Assert.assertTrue(verifyResponse.getData().getVerified());
    }
}
