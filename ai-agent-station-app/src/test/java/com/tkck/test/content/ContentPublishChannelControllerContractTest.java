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

        when(service.saveOrUpdateConfig("juejin", "abc")).thenReturn(ContentPublishChannelConfigEntity.builder()
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
}
