package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTaskCreateResponseDTO {

    private Long taskId;

    private String taskCode;

    private String executionMode;

    private String status;

    private String currentStep;
}
