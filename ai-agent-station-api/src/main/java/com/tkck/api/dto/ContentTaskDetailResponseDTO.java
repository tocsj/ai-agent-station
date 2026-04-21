package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTaskDetailResponseDTO {

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepItem {
        private Long id;
        private Long taskId;
        private Integer stepNo;
        private String stepName;
        private String stepStatus;
        private String outputText;
        private String metadataJson;
        private String createTime;
    }
}
