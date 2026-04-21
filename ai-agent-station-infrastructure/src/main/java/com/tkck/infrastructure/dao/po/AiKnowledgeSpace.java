package com.tkck.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiKnowledgeSpace {

    private Long id;
    private String spaceId;
    private String spaceName;
    private String spaceType;
    private String description;
    private Integer status;
    private Integer documentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
