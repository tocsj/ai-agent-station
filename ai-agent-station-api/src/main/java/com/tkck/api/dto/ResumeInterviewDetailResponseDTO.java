package com.tkck.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeInterviewDetailResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long interviewSessionId;

    private Long resumeId;

    private Long knowledgeSpaceId;

    private String sessionCode;

    private Integer currentRound;

    private Integer totalRounds;

    private String status;

    private String openingQuestions;

    private String finalReport;

    private List<RoundItem> rounds;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoundItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

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
}
