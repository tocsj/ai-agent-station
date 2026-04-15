package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentTaskEntity {

    private Long taskId;

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

    private String createTime;

    private String updateTime;
}
