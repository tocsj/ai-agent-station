package com.tkck.domain.document.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQueryRewriteResultEntity {

    private String originalQuestion;

    private String rewrittenQuery;

    private String rewriteReason;

    private String clientId;

    private String modelCode;
}
