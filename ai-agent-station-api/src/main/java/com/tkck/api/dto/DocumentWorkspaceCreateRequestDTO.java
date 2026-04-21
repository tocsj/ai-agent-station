package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentWorkspaceCreateRequestDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String workspaceName;
    private String description;
}
