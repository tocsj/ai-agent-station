package com.tkck.domain.resume.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeInterviewRoundEntity {

    private Integer roundNo;

    private String questionContent;

    private String answerContent;

    private String feedbackContent;

    private String strengths;

    private String weaknesses;

    private String resumeEvidence;

    private String followUpIntent;

    private String nextQuestion;

    private String score;

    private Boolean finished;

    private String status;
}
