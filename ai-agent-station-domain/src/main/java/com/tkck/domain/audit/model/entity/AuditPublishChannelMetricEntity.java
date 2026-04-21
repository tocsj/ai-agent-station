package com.tkck.domain.audit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditPublishChannelMetricEntity {

    private String channel;
    private String channelName;
    private Integer attemptTotal;
    private Integer successTotal;
    private Integer failedTotal;
    private Double successRate;
    private String lastStatus;
    private String lastMessage;
    private String lastExternalUrl;
}
