package com.tkck.app.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResumeInterviewStructuredResult {

    private String score;

    private String feedback;

    private String strengths;

    private String weaknesses;

    private String resumeEvidence;

    private String followUpIntent;

    private String nextQuestion;

    private String finalReport;

    private boolean finished;
}
