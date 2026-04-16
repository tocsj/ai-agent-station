package com.tkck.domain.workbench.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchPlatformStatusEntity {

    private String apiVersion;
    private String apiStatus;
    private String statusText;
}
