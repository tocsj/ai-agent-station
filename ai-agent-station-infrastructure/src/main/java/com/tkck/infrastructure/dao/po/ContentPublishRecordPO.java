package com.tkck.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPublishRecordPO {

    private Long id;
    private Long taskId;
    private String channelCode;
    private String action;
    private String requestSnapshot;
    private String responseSnapshot;
    private String status;
    private String externalId;
    private String externalUrl;
    private String errorMessage;
    private Date createTime;
}
