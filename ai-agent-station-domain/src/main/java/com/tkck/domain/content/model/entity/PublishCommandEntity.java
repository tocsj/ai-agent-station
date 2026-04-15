package com.tkck.domain.content.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishCommandEntity {

    private String channel;

    private String action;

    private String title;

    private String summary;

    private String content;

    private String tags;

    private Long taskId;
}
