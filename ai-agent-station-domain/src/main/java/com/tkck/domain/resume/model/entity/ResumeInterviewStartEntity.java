package com.tkck.domain.resume.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeInterviewStartEntity {

    private Long interviewSessionId;

    private Integer currentRound;

    private Integer totalRounds;

    private String status;

    private String openingQuestions;
}
