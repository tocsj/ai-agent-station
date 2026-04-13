package com.tkck.test.domain;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OpenAiTest {

    @Value("classpath:data/dog.png")
    private Resource imageResource;

    @Value("classpath:data/file.txt")
    private Resource textResource;

    @Value("classpath:data/article-prompt-words.txt")
    private Resource articlePromptWordsResource;

    @Value("classpath:data/grafana-mcp-tools-guide.md")
    private Resource grafanaMcpToolsGuideResource;

    @Autowired
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private PgVectorStore pgVectorStore;

    @Autowired
    @Qualifier("pgVectorJdbcTemplate")
    private JdbcTemplate pgVectorJdbcTemplate;

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.embedding-model}")
    private String ollamaEmbeddingModel;

    private static final String OLLAMA_VECTOR_TABLE = "vector_store_ollama";

    private final TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();

    @Test
    public void test_call() {
        ChatResponse response = openAiChatModel.call(new Prompt(
                "1+1",
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));
        log.info("测试结果(call):{}", JSON.toJSONString(response));
    }

    @Test
    public void test_call_images() {
        UserMessage userMessage = UserMessage.builder()
                .text("请描述这张图片的主要内容，并说明图中物品的可能用途。")
                .media(org.springframework.ai.content.Media.builder()
                        .mimeType(MimeType.valueOf(MimeTypeUtils.IMAGE_PNG_VALUE))
                        .data(imageResource)
                        .build())
                .build();

        ChatResponse response = openAiChatModel.call(new Prompt(
                userMessage,
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        log.info("测试结果(images):{}", JSON.toJSONString(response));
    }

    @Test
    public void test_stream() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Flux<ChatResponse> stream = openAiChatModel.stream(new Prompt(
                "1+1",
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        stream.subscribe(
                chatResponse -> {
                    AssistantMessage output = chatResponse.getResult().getOutput();
                    log.info("测试结果(stream): {}", JSON.toJSONString(output));
                },
                Throwable::printStackTrace,
                () -> {
                    countDownLatch.countDown();
                    log.info("测试结果(stream): done!");
                }
        );

        countDownLatch.await();
    }

    @Test
    public void upload() {
        // textResource、articlePromptWordsResource
        TikaDocumentReader reader = new TikaDocumentReader(grafanaMcpToolsGuideResource);

        List<Document> documents = reader.get();
        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

        documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", "grafana-mcp-tools-guide"));

        LocalOllamaEmbeddingModel embeddingModel = new LocalOllamaEmbeddingModel(ollamaBaseUrl, ollamaEmbeddingModel);
        ensureVectorTable(OLLAMA_VECTOR_TABLE, embeddingModel.dimensions());

        PgVectorStore ollamaVectorStore = PgVectorStore.builder(
                        pgVectorJdbcTemplate,
                        embeddingModel)
                .vectorTableName(OLLAMA_VECTOR_TABLE)
                .build();

        ollamaVectorStore.accept(documentSplitterList);

        log.info("上传完成");
    }

    @Test
    public void chat() {
        String message = "王大瓜今年几岁";

        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        SearchRequest request = SearchRequest.builder()
                .query(message)
                .topK(5)
                .filterExpression("knowledge == '知识库名称-v4'")
                .build();

        List<Document> documents = pgVectorStore.similaritySearch(request);

        String documentsCollectors = null == documents ? "" : documents.stream().map(Document::getText).collect(Collectors.joining());

        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        ChatResponse chatResponse = openAiChatModel.call(new Prompt(
                messages,
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        log.info("测试结果:{}", JSON.toJSONString(chatResponse));
    }

    private void ensureVectorTable(String tableName, int dimensions) {
        String qualifiedTable = "public." + tableName;
        try {
            Integer existingDims = pgVectorJdbcTemplate.queryForObject(
                    """
                            SELECT atttypmod - 4
                            FROM pg_attribute
                            WHERE attrelid = ?::regclass
                              AND attname = 'embedding'
                            """,
                    Integer.class,
                    qualifiedTable);
            if (existingDims != null && existingDims == dimensions) {
                return;
            }

            if (existingDims != null && existingDims != dimensions) {
                pgVectorJdbcTemplate.execute(
                        "ALTER TABLE " + qualifiedTable + " ALTER COLUMN embedding TYPE vector(" + dimensions + ");");
                return;
            }
        } catch (DataAccessException ignored) {
            // table does not exist yet
        }

        pgVectorJdbcTemplate.execute(
                """
                        CREATE TABLE IF NOT EXISTS %s (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            content TEXT NOT NULL,
                            metadata JSONB,
                            embedding VECTOR(%d)
                        )
                        """.formatted(qualifiedTable, dimensions));
    }

    /**
     * Minimal EmbeddingModel implementation that delegates to a local Ollama /api/embeddings endpoint.
     */
    private static class LocalOllamaEmbeddingModel implements EmbeddingModel {

        private final RestClient restClient;
        private final String model;
        private volatile Integer dimensions;

        LocalOllamaEmbeddingModel(String baseUrl, String model) {
            String normalizedBaseUrl = (baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))
                    ? baseUrl
                    : "http://" + baseUrl;
            this.restClient = RestClient.builder()
                    .baseUrl(normalizedBaseUrl)
                    .build();
            this.model = model;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<String> inputs = request.getInstructions();
            List<Embedding> embeddings = new ArrayList<>(inputs.size());
            for (int i = 0; i < inputs.size(); i++) {
                float[] vector = embed(inputs.get(i));
                embeddings.add(new Embedding(vector, i));
            }
            EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata();
            metadata.setModel(model);
            return new EmbeddingResponse(embeddings, metadata);
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        @Override
        public float[] embed(String text) {
            return embedWithOllama(text);
        }

        @Override
        public int dimensions() {
            if (this.dimensions == null) {
                this.dimensions = embed("ping").length;
            }
            return this.dimensions;
        }

        private float[] embedWithOllama(String prompt) {
            OllamaEmbeddingResponse response = this.restClient.post()
                    .uri("/api/embeddings")
                    .body(Map.of("model", model, "prompt", prompt))
                    .retrieve()
                    .body(OllamaEmbeddingResponse.class);
            if (response == null || response.getEmbedding() == null || response.getEmbedding().isEmpty()) {
                throw new IllegalStateException("Ollama embedding response is empty");
            }
            List<Double> rawEmbedding = response.getEmbedding();
            float[] vector = new float[rawEmbedding.size()];
            for (int i = 0; i < rawEmbedding.size(); i++) {
                vector[i] = rawEmbedding.get(i).floatValue();
            }
            this.dimensions = vector.length;
            return vector;
        }

        private static class OllamaEmbeddingResponse {
            private List<Double> embedding;

            public List<Double> getEmbedding() {
                return embedding;
            }

            public void setEmbedding(List<Double> embedding) {
                this.embedding = embedding;
            }
        }
    }

}
