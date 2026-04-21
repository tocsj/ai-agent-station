package com.tkck.test.resume;

import com.tkck.app.resume.ResumeInterviewStructuredResult;
import com.tkck.app.resume.ResumeInterviewStructuredResultParser;
import org.junit.Assert;
import org.junit.Test;

public class ResumeInterviewStructuredResultParserTest {

    @Test
    public void should_parse_continue_round_result() {
        String summary = """
                本轮评分: 8/10
                本轮点评: 回答覆盖了缓存一致性的主流程，但对延迟双删的适用边界解释不够完整。
                优势: 能说清先更新数据库再删缓存的主线流程。
                薄弱点: 没有展开并发写入场景下的脏数据窗口控制。
                命中简历片段: 在电商订单项目中负责 Redis 缓存设计与热点 Key 治理。
                追问意图: 继续验证候选人是否真正理解高并发下一致性方案。
                下一轮问题: 如果出现并发写导致旧值回填，你会怎么设计补偿或兜底策略？
                面试状态: CONTINUE
                """;

        ResumeInterviewStructuredResult result = ResumeInterviewStructuredResultParser.parse(summary, false);

        Assert.assertEquals("8/10", result.getScore());
        Assert.assertEquals("回答覆盖了缓存一致性的主流程，但对延迟双删的适用边界解释不够完整。", result.getFeedback());
        Assert.assertEquals("能说清先更新数据库再删缓存的主线流程。", result.getStrengths());
        Assert.assertEquals("没有展开并发写入场景下的脏数据窗口控制。", result.getWeaknesses());
        Assert.assertEquals("在电商订单项目中负责 Redis 缓存设计与热点 Key 治理。", result.getResumeEvidence());
        Assert.assertEquals("继续验证候选人是否真正理解高并发下一致性方案。", result.getFollowUpIntent());
        Assert.assertEquals("如果出现并发写导致旧值回填，你会怎么设计补偿或兜底策略？", result.getNextQuestion());
        Assert.assertFalse(result.isFinished());
        Assert.assertNull(result.getFinalReport());
    }

    @Test
    public void should_parse_finished_round_result() {
        String summary = """
                本轮评分: 7/10
                本轮点评: 能说明项目主线，但链路治理与性能压测细节偏弱。
                优势: 项目表达完整，能结合真实经历回答。
                薄弱点: 对可观测性与容量评估的量化指标回答不够具体。
                命中简历片段: 简历中写有 Spring Boot 多模块项目与 Docker 部署经历。
                追问意图: 已完成终轮评估，重点输出综合结论。
                最终面试总结: 具备 Java 后端基础与项目表达能力，适合进入下一轮实习面试，但建议补强缓存一致性、压测指标与故障定位方法。
                面试状态: FINISHED
                """;

        ResumeInterviewStructuredResult result = ResumeInterviewStructuredResultParser.parse(summary, true);

        Assert.assertEquals("7/10", result.getScore());
        Assert.assertEquals("项目表达完整，能结合真实经历回答。", result.getStrengths());
        Assert.assertEquals("具备 Java 后端基础与项目表达能力，适合进入下一轮实习面试，但建议补强缓存一致性、压测指标与故障定位方法。", result.getFinalReport());
        Assert.assertTrue(result.isFinished());
        Assert.assertNull(result.getNextQuestion());
    }
}
