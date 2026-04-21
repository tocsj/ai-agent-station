package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditEventPageEntity {

    private Integer page;
    private Integer pageSize;
    private Long total;
    private List<AuditEventEntity> items;
}
