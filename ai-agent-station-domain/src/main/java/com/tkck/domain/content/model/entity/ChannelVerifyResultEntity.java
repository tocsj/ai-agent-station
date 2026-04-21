package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelVerifyResultEntity {

    private String channel;
    private Boolean verified;
    private String verifyStatus;
    private String message;
}
