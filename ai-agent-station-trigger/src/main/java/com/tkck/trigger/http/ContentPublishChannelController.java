package com.tkck.trigger.http;

import com.tkck.api.dto.ContentPublishChannelConfigResponseDTO;
import com.tkck.api.dto.ContentPublishChannelConfigSaveRequestDTO;
import com.tkck.api.dto.ContentPublishRecordResponseDTO;
import com.tkck.api.dto.ContentPublishVerifyResponseDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.content.model.entity.ChannelVerifyResultEntity;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import com.tkck.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/content/channel")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class ContentPublishChannelController {

    @Resource
    private IContentPublishChannelService contentPublishChannelService;

    @PostMapping("/config/save")
    public Response<ContentPublishChannelConfigResponseDTO> saveConfig(@RequestBody ContentPublishChannelConfigSaveRequestDTO request) {
        return Response.<ContentPublishChannelConfigResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toConfigResponse(contentPublishChannelService.saveOrUpdateConfig(
                        request.getChannel(),
                        request.getToken(),
                        request.getBlogApp(),
                        request.getBlogId(),
                        request.getUsername(),
                        request.getEndpoint())))
                .build();
    }

    @GetMapping("/config/{channel}")
    public Response<ContentPublishChannelConfigResponseDTO> queryConfig(@PathVariable("channel") String channel) {
        return Response.<ContentPublishChannelConfigResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toConfigResponse(contentPublishChannelService.queryConfig(channel)))
                .build();
    }

    @PostMapping("/juejin/verify")
    public Response<ContentPublishVerifyResponseDTO> verifyJuejin() {
        return Response.<ContentPublishVerifyResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toVerifyResponse(contentPublishChannelService.verifyJuejinConfig()))
                .build();
    }

    @PostMapping("/cnblogs/verify")
    public Response<ContentPublishVerifyResponseDTO> verifyCnblogs() {
        return Response.<ContentPublishVerifyResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toVerifyResponse(contentPublishChannelService.verifyCnblogsConfig()))
                .build();
    }

    @PostMapping("/devto/verify")
    public Response<ContentPublishVerifyResponseDTO> verifyDevto() {
        return Response.<ContentPublishVerifyResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toVerifyResponse(contentPublishChannelService.verifyDevtoConfig()))
                .build();
    }

    @GetMapping("/record/{taskId}")
    public Response<List<ContentPublishRecordResponseDTO>> queryPublishRecords(@PathVariable("taskId") Long taskId) {
        return Response.<List<ContentPublishRecordResponseDTO>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(contentPublishChannelService.queryPublishRecords(taskId).stream()
                        .map(this::toRecordResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private ContentPublishChannelConfigResponseDTO toConfigResponse(ContentPublishChannelConfigEntity entity) {
        return ContentPublishChannelConfigResponseDTO.builder()
                .channel(entity.getChannelCode())
                .channelName(entity.getChannelName())
                .authType(entity.getAuthType())
                .verifyStatus(entity.getVerifyStatus())
                .verifyMessage(entity.getVerifyMessage())
                .status(entity.getStatus())
                .build();
    }

    private ContentPublishVerifyResponseDTO toVerifyResponse(ChannelVerifyResultEntity result) {
        return ContentPublishVerifyResponseDTO.builder()
                .channel(result.getChannel())
                .verified(result.getVerified())
                .verifyStatus(result.getVerifyStatus())
                .message(result.getMessage())
                .build();
    }

    private ContentPublishRecordResponseDTO toRecordResponse(ContentPublishRecordEntity record) {
        return ContentPublishRecordResponseDTO.builder()
                .id(record.getId())
                .taskId(record.getTaskId())
                .channelCode(record.getChannelCode())
                .action(record.getAction())
                .status(record.getStatus())
                .externalId(record.getExternalId())
                .externalUrl(record.getExternalUrl())
                .errorMessage(record.getErrorMessage())
                .createTime(record.getCreateTime())
                .build();
    }
}
