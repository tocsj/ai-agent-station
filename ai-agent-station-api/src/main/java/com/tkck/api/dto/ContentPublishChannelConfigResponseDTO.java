package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPublishChannelConfigResponseDTO {

    private String channel;
    private String channelName;
    private String authType;
    private String verifyStatus;
    private String verifyMessage;
    private Integer status;
}
