package com.tkck.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentTaskPO {

    private Long id;
    private String taskCode;
    private String executionMode;
    private String topic;
    private String platform;
    private String style;
    private String keywords;
    private String channel;
    private String status;
    private String currentStep;
    private String title;
    private String outlineText;
    private String draftContent;
    private String finalContent;
    private String complianceResult;
    private String publishStatus;
    private String publishExternalId;
    private String publishExternalUrl;
    private String summaryText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
