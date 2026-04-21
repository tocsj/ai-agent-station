package com.tkck.app.resume;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@DependsOn("resumeWorkflowSchemaInitializer")
public class JobStandardDataInitializer implements InitializingBean {

    private static final String JAVA_BACKEND_JOB_CODE = "java_backend";

    @Resource(name = "pgVectorJdbcTemplate")
    private JdbcTemplate pgVectorJdbcTemplate;

    @Resource(name = "jobStandardVectorStore")
    private VectorStore jobStandardVectorStore;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Override
    public void afterPropertiesSet() {
        Integer count = pgVectorJdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM job_standard_item WHERE job_code = ?",
                Integer.class,
                JAVA_BACKEND_JOB_CODE);
        if (count != null && count > 0) {
            return;
        }

        long profileId = insertProfile();
        List<JobStandardItemSeed> seeds = buildJavaBackendSeeds();
        for (JobStandardItemSeed seed : seeds) {
            long itemId = insertItem(profileId, seed);
            List<Document> splitDocuments = tokenTextSplitter.apply(List.of(new Document(buildVectorContent(seed))));
            for (int i = 0; i < splitDocuments.size(); i++) {
                Document document = splitDocuments.get(i);
                Map<String, Object> metadata = JobStandardMetadataSupport.buildChunkMetadata(
                        seed.jobCode(), seed.skillKey(), seed.category(), seed.importance(), i);
                metadata.put("itemId", String.valueOf(itemId));
                metadata.forEach(document.getMetadata()::put);
            }
            jobStandardVectorStore.accept(splitDocuments);
        }
    }

    private long insertProfile() {
        return pgVectorJdbcTemplate.queryForObject(
                """
                        INSERT INTO job_standard_profile(job_code, job_name, job_family, job_level, description, status)
                        VALUES (?, ?, ?, ?, ?, 1)
                        RETURNING id
                        """,
                Long.class,
                JAVA_BACKEND_JOB_CODE,
                "Java后端开发工程师",
                "backend",
                "junior_to_mid",
                "面向常见互联网 Java 后端岗位的静态评估标准，覆盖技术基础、中间件、数据库、工程化与项目真实性。");
    }

    private long insertItem(long profileId, JobStandardItemSeed seed) {
        return pgVectorJdbcTemplate.queryForObject(
                """
                        INSERT INTO job_standard_item(
                            profile_id, job_code, category, skill_key, skill_name, importance,
                            expected_level, standard_summary, detailed_requirement, scoring_points,
                            risk_signals, status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                        RETURNING id
                        """,
                Long.class,
                profileId,
                seed.jobCode(),
                seed.category(),
                seed.skillKey(),
                seed.skillName(),
                seed.importance(),
                seed.expectedLevel(),
                seed.standardSummary(),
                seed.detailedRequirement(),
                seed.scoringPoints(),
                seed.riskSignals());
    }

    private String buildVectorContent(JobStandardItemSeed seed) {
        return """
                岗位：%s
                技能项：%s
                分类：%s
                重要级别：%s
                期望等级：%s
                标准描述：%s
                详细要求：%s
                打分点：%s
                风险信号：%s
                """.formatted(
                "Java后端开发工程师",
                seed.skillName(),
                seed.category(),
                seed.importance(),
                seed.expectedLevel(),
                seed.standardSummary(),
                seed.detailedRequirement(),
                seed.scoringPoints(),
                seed.riskSignals());
    }

    private List<JobStandardItemSeed> buildJavaBackendSeeds() {
        List<JobStandardItemSeed> seeds = new ArrayList<>();
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "backend_core", "java_basic", "Java基础", "high", "L2",
                "掌握 Java 面向对象、集合、异常、泛型、常用 API。",
                "能够结合实际项目解释集合选型、异常边界和常见数据结构使用场景。",
                "是否能说清 ArrayList/HashMap/线程安全差异；是否能结合项目举例。",
                "只写“熟悉Java”，无法落到具体知识点或项目使用场景。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "backend_core", "juc", "并发编程", "high", "L2",
                "具备 Java 并发基础，理解线程池、锁、并发容器。",
                "至少能够解释线程池参数、锁竞争、线程安全处理方式，并能结合项目说明。",
                "是否提到线程池配置、Future/CompletableFuture、并发问题定位。",
                "只会背术语，无法说明真实业务里为什么需要并发控制。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "backend_core", "jvm", "JVM基础", "medium", "L1",
                "了解类加载、内存结构、垃圾回收和常见排查思路。",
                "不要求深入调优，但需要说明线上问题如何用日志、监控、dump 辅助定位。",
                "是否知道堆/栈/元空间；是否能说明 Full GC 风险。",
                "完全不会 JVM，简历却写性能优化或高并发项目。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "framework", "spring", "Spring / Spring Boot", "high", "L2",
                "能独立完成基于 Spring Boot 的接口开发、配置管理、异常处理和模块拆分。",
                "需要理解 IOC、AOP、自动装配、参数校验、统一返回和常见分层设计。",
                "是否能把项目中的控制层、服务层、DAO 层职责讲清楚。",
                "只写 CRUD，没有说明框架机制和真实项目职责。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "framework", "mybatis", "MyBatis / ORM", "medium", "L2",
                "能够进行基础表映射、动态 SQL 编写和分页查询。",
                "需要说明复杂查询、索引命中、分页与批量写入时的注意事项。",
                "是否有真实 SQL 场景；是否了解 N+1、动态条件、批量处理。",
                "只会生成器代码，不能解释实际 SQL 或性能影响。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "database", "mysql", "MySQL", "high", "L2",
                "具备事务、索引、常见 SQL 优化和表设计基础。",
                "能解释项目里表结构设计、索引选择、慢查询定位和事务边界。",
                "是否提到联合索引、回表、事务隔离级别、慢 SQL 排查。",
                "只会写简单查询，无法说明索引与事务在项目里的具体使用。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "middleware", "redis", "Redis", "high", "L2",
                "理解缓存场景、过期策略和常见问题，如缓存穿透、击穿、雪崩。",
                "能说明项目里为什么要加缓存、缓存粒度、失效策略和一致性方案。",
                "是否能解释缓存命中率、热点数据、双写一致性。",
                "只说“用了 Redis 提高性能”，没有具体策略。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "middleware", "mq", "消息队列", "medium", "L1",
                "了解异步削峰、解耦、重试和幂等基础。",
                "如果简历写到 MQ，需要能说明消息发送、消费失败重试和幂等处理。",
                "是否能解释为什么使用 MQ 而不是同步调用。",
                "写了 Kafka/RabbitMQ，但答不清业务场景和失败处理。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "architecture", "microservice", "微服务", "medium", "L1",
                "了解服务拆分、配置中心、注册发现、网关与基本治理。",
                "若项目为单体也可接受，但要清楚单体与微服务的取舍。",
                "是否能说明服务边界、接口依赖和调用链问题。",
                "盲目堆技术栈，实际项目规模并不支持微服务。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "engineering", "api_design", "接口设计", "high", "L2",
                "具备 RESTful 接口设计、参数校验、错误码与幂等意识。",
                "能说明接口字段设计、统一响应结构、异常处理和兼容性考虑。",
                "是否有真实接口开发经验，能否说明接口文档与联调问题。",
                "只写接口开发，没有提到任何设计原则。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "engineering", "linux_docker", "Linux / Docker", "medium", "L1",
                "能完成基础部署、日志查看、容器化运行和常见命令操作。",
                "不要求重度运维，但要有实际运行服务和排查问题的经验。",
                "是否提到容器部署、日志排查、端口映射、环境变量。",
                "完全没有部署经验，却宣称独立负责项目上线。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "engineering", "observability", "日志与可观测性", "medium", "L1",
                "具备日志规范、错误定位和基础监控意识。",
                "至少能说明日志分级、问题复盘、关键指标和异常排查路径。",
                "是否能说出接口耗时、错误率、Trace/监控面板等概念。",
                "项目写得很复杂，但没有任何可观测和定位手段。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "project", "project_authenticity", "项目真实性", "high", "L2",
                "项目经历必须能说清背景、职责、难点、取舍、结果。",
                "重点看是否能给出真实业务约束、关键决策原因和结果指标。",
                "是否能说清自己做了什么，而不是只背项目介绍。",
                "项目描述过于完美、技术堆砌严重、无法解释关键实现。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "project", "performance", "性能优化意识", "medium", "L1",
                "具备基础性能定位与优化意识。",
                "即使没有大型性能优化，也应能说明慢接口、慢 SQL、缓存或批处理优化。",
                "是否提到压测、瓶颈、优化前后变化。",
                "写了“高性能、高并发”，却没有任何量化依据。"));
        seeds.add(new JobStandardItemSeed(JAVA_BACKEND_JOB_CODE, "ai_engineering", "rag_agent", "AI Agent / RAG 工程加分项", "low", "L1",
                "如果候选人做过 AI Agent、RAG、向量检索项目，可作为加分项。",
                "需要能说清模型调用、检索流程、向量库、重排或评估流程，而不是只会调 API。",
                "是否提到知识切片、检索增强、召回质量、成本控制。",
                "只会用大模型生成内容，无法解释工程落地链路。"));
        return seeds;
    }

    private record JobStandardItemSeed(String jobCode,
                                       String category,
                                       String skillKey,
                                       String skillName,
                                       String importance,
                                       String expectedLevel,
                                       String standardSummary,
                                       String detailedRequirement,
                                       String scoringPoints,
                                       String riskSignals) {
    }
}
