package com.tkck.config;

import com.tkck.infrastructure.dao.IAiClientApiDao;
import com.tkck.infrastructure.dao.IAiClientModelDao;
import com.tkck.infrastructure.dao.po.AiClientApi;
import com.tkck.infrastructure.dao.po.AiClientModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AiAgentConfig {

    public static final String RESUME_VECTOR_TABLE = "resume_vector_store";
    public static final int RESUME_EMBEDDING_DIMENSIONS = 1024;

    private final IAiClientApiDao aiClientApiDao;
    private final IAiClientModelDao aiClientModelDao;

    public AiAgentConfig(IAiClientApiDao aiClientApiDao, IAiClientModelDao aiClientModelDao) {
        this.aiClientApiDao = aiClientApiDao;
        this.aiClientModelDao = aiClientModelDao;
    }

    /**
     * -- 删除旧的表（如果存在）
     * DROP TABLE IF EXISTS public.vector_store_openai;
     * <p>
     * -- 创建新的表，使用UUID作为主键
     * CREATE TABLE public.vector_store_openai (
     * id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     * content TEXT NOT NULL,
     * metadata JSONB,
     * embedding VECTOR(1536)
     * );
     * <p>
     * SELECT * FROM vector_store_openai
     */
    @Bean("vectorStore")
    public PgVectorStore pgVectorStore(@Value("${spring.ai.openai.base-url}") String baseUrl,
                                       @Value("${spring.ai.openai.api-key}") String apiKey,
                                       @Value("${spring.ai.openai.embedding-model-id:2005}") String embeddingModelId,
                                       @Value("${spring.ai.openai.embedding-model:text-embedding-3-small}") String embeddingModelName,
                                       @Value("${spring.ai.openai.completions-path:v1/chat/completions}") String completionsPath,
                                       @Value("${spring.ai.openai.embeddings-path:v1/embeddings}") String embeddingsPath,
                                       @Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {

        AiClientModel embeddingModelConfig = aiClientModelDao.queryByModelId(embeddingModelId);
        if (embeddingModelConfig != null && Integer.valueOf(1).equals(embeddingModelConfig.getStatus())) {
            embeddingModelName = embeddingModelConfig.getModelName();
            AiClientApi apiConfig = aiClientApiDao.queryByApiId(embeddingModelConfig.getApiId());
            if (apiConfig != null && Integer.valueOf(1).equals(apiConfig.getStatus())) {
                baseUrl = normalizeBaseUrl(apiConfig.getBaseUrl());
                apiKey = apiConfig.getApiKey();
                completionsPath = apiConfig.getCompletionsPath();
                embeddingsPath = apiConfig.getEmbeddingsPath();
            }
        }

        baseUrl = normalizeBaseUrl(baseUrl);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .completionsPath(completionsPath)
                .embeddingsPath(embeddingsPath)
                .build();

        OpenAiEmbeddingOptions embeddingOptions = new OpenAiEmbeddingOptions();
        embeddingOptions.setModel(embeddingModelName);

        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, embeddingOptions);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(RESUME_VECTOR_TABLE)
                .dimensions(RESUME_EMBEDDING_DIMENSIONS)
                .initializeSchema(true)
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return baseUrl;
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

}
