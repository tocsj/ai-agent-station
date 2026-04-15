package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentCreateCommandEntity {

    private String topic;

    private String platform;

    private String style;

    private String keywords;

    private String channel;
}
