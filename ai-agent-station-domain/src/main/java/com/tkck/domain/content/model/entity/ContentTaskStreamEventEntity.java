package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentTaskStreamEventEntity {

    private String type;

    private Long taskId;

    private Integer stepNo;

    private String stepName;

    private String status;

    private String content;

    private boolean completed;

    private Long timestamp;
}
