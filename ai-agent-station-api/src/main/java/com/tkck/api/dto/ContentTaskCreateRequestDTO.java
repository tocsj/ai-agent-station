package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTaskCreateRequestDTO {

    private String topic;

    private String platform;

    private String style;

    private String keywords;

    private String channel;
}
