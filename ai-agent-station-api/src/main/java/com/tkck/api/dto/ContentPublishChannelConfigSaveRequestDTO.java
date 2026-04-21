package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPublishChannelConfigSaveRequestDTO {

    private String channel;
    private String token;
    private String blogApp;
    private String blogId;
    private String username;
    private String endpoint;
}
