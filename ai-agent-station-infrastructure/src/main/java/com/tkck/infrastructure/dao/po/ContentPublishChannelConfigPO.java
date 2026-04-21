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
public class ContentPublishChannelConfigPO {

    private Long id;
    private String channelCode;
    private String channelName;
    private String authType;
    private String credentialJson;
    private String verifyStatus;
    private String verifyMessage;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
