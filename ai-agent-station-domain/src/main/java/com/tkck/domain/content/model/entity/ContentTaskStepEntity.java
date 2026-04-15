package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentTaskStepEntity {

    private Long id;

    private Long taskId;

    private Integer stepNo;

    private String stepName;

    private String stepStatus;

    private String outputText;

    private String metadataJson;

    private String createTime;
}
