package com.tkck.domain.resume.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeInterviewDetailEntity {

    private Long interviewSessionId;

    private Long resumeId;

    private Long knowledgeSpaceId;

    private String sessionCode;

    private Integer currentRound;

    private Integer totalRounds;

    private String status;

    private String openingQuestions;

    private String finalReport;

    private List<ResumeInterviewRoundEntity> rounds;
}
