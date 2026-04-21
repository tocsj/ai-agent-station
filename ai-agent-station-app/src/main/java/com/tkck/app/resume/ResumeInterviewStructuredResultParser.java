package com.tkck.app.resume;

public final class ResumeInterviewStructuredResultParser {

    private ResumeInterviewStructuredResultParser() {
    }

    public static ResumeInterviewStructuredResult parse(String text, boolean finalRound) {
        String score = ResumeInterviewSectionSupport.extractSectionValue(text, "本轮评分:", "本轮评分：", "评分:", "评分：");
        String feedback = ResumeInterviewSectionSupport.extractSectionValue(text, "本轮点评:", "本轮点评：");
        String strengths = ResumeInterviewSectionSupport.extractSectionValue(text, "优势:", "优势：");
        String weaknesses = ResumeInterviewSectionSupport.extractSectionValue(text, "薄弱点:", "薄弱点：");
        String resumeEvidence = ResumeInterviewSectionSupport.extractSectionValue(text, "命中简历片段:", "命中简历片段：");
        String followUpIntent = ResumeInterviewSectionSupport.extractSectionValue(text, "追问意图:", "追问意图：");
        String nextQuestion = finalRound ? null
                : ResumeInterviewSectionSupport.extractSectionValue(text, "下一轮问题:", "下一轮问题：", "下一题:", "下一题：");
        String finalReport = finalRound
                ? ResumeInterviewSectionSupport.extractSectionValue(text, "最终面试总结:", "最终面试总结：", "最终总结:", "最终总结：")
                : null;
        String status = ResumeInterviewSectionSupport.extractSectionValue(text, "面试状态:", "面试状态：");

        return ResumeInterviewStructuredResult.builder()
                .score(score)
                .feedback(feedback)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .resumeEvidence(resumeEvidence)
                .followUpIntent(followUpIntent)
                .nextQuestion(nextQuestion)
                .finalReport(finalReport)
                .finished("FINISHED".equalsIgnoreCase(status) || finalRound)
                .build();
    }
}
