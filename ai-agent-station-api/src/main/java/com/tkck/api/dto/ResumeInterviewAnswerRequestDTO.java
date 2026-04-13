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
public class ResumeInterviewAnswerRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long interviewSessionId;

    private Integer roundNo;

    private String answer;

    private String sessionId;

    private Integer maxStep;
}
