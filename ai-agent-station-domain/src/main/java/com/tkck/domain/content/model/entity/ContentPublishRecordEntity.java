package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentPublishRecordEntity {

    private Long id;
    private Long taskId;
    private String channelCode;
    private String action;
    private String requestSnapshot;
    private String responseSnapshot;
    private String status;
    private String externalId;
    private String externalUrl;
    private String errorMessage;
    private String createTime;
}
