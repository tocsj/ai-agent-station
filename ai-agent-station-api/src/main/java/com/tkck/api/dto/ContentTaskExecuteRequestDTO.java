package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTaskExecuteRequestDTO {

    private Long taskId;

    private String sessionId;

    private Integer maxStep;
}
