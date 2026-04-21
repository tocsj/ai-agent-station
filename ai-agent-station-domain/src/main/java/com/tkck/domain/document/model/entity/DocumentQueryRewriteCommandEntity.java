package com.tkck.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQueryRewriteCommandEntity {

    private String taskType;

    private Map<String, Object> taskParams;

    private String originalQuestion;
}
