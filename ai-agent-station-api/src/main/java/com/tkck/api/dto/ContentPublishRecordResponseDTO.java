package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPublishRecordResponseDTO {

    private Long id;
    private Long taskId;
    private String channelCode;
    private String action;
    private String status;
    private String externalId;
    private String externalUrl;
    private String errorMessage;
    private String createTime;
}
