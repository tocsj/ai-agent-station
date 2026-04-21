package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentPublishChannelConfigEntity {

    private Long id;
    private String channelCode;
    private String channelName;
    private String authType;
    private String credentialJson;
    private String verifyStatus;
    private String verifyMessage;
    private Integer status;
    private String createTime;
    private String updateTime;
}
