package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishResultEntity {

    private Boolean success;

    private String channel;

    private String status;

    private String externalId;

    private String externalUrl;

    private String message;
}
