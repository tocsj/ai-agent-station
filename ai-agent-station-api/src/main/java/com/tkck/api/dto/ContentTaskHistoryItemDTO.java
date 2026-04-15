package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTaskHistoryItemDTO {

    private Long taskId;
    private String taskCode;
    private String topic;
    private String platform;
    private String channel;
    private String status;
    private String currentStep;
    private String title;
    private String publishStatus;
    private String createTime;
    private String updateTime;
}
