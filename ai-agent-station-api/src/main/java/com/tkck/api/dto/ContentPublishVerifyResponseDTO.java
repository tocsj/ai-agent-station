package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPublishVerifyResponseDTO {

    private String channel;
    private Boolean verified;
    private String verifyStatus;
    private String message;
}
