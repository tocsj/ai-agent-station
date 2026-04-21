package com.tkck.domain.agent.service.armory.factory.element;

import com.alibaba.fastjson.JSON;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RagAnswerAdvisor implements BaseAdvisor {
    private static final int MAX_QUERY_LENGTH = 4000;

    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;
    private final String userTextAdvise;

    public RagAnswerAdvisor(VectorStore vectorStore, SearchRequest searchRequest) {
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.userTextAdvise = "\nContext information is below, surrounded by ---------------------\n\n---------------------\n{question_answer_context}\n---------------------\n\nGiven the context and provided history information and not prior knowledge,\nreply to the user comment. If the answer is not in the context, inform\nthe user that you can't answer the question.\n";
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        HashMap<String, Object> context = new HashMap<>(chatClientRequest.context());

        String userText = chatClientRequest.prompt().getUserMessage().getText();
        String advisedUserText = userText + System.lineSeparator() + this.userTextAdvise;

        String query = extractQueryText(userText);

        SearchRequest searchRequestToUse = SearchRequest.from(this.searchRequest)
                .query(query)
                .filterExpression(this.doGetFilterExpression(context))
                .build();
        List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
        context.put("qa_retrieved_documents", documents);

        String documentContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));
        Map<String, Object> advisedUserParams = new HashMap<>(chatClientRequest.context());
        advisedUserParams.put("question_answer_context", documentContext);

        return ChatClientRequest.builder()
                .prompt(Prompt.builder()
                        .messages(
                                new UserMessage(advisedUserText),
                                new AssistantMessage(JSON.toJSONString(advisedUserParams)))
                        .build())
                .context(advisedUserParams)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        chatResponseBuilder.metadata("qa_retrieved_documents", chatClientResponse.context().get("qa_retrieved_documents"));
        ChatResponse chatResponse = chatResponseBuilder.build();

        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(chatClientResponse.context())
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(this.before(chatClientRequest, callAdvisorChain));
        return this.after(chatClientResponse, callAdvisorChain);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return BaseAdvisor.super.adviseStream(chatClientRequest, streamAdvisorChain);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
        return context.containsKey("qa_filter_expression") && StringUtils.hasText(context.get("qa_filter_expression").toString())
                ? new FilterExpressionTextParser().parse(context.get("qa_filter_expression").toString())
                : this.searchRequest.getFilterExpression();
    }

    private String extractQueryText(String userText) {
        if (!StringUtils.hasText(userText)) {
            return "";
        }

        String normalized = userText.replace("\r\n", "\n");
        String[] markers = new String[]{
                "用户原始问题:",
                "用户原始问题：",
                "用户问题:",
                "用户问题：",
                "问题:",
                "问题："
        };

        for (String marker : markers) {
            int questionIndex = normalized.indexOf(marker);
            if (questionIndex >= 0) {
                String tail = normalized.substring(questionIndex + marker.length()).trim();
                String extracted = extractFirstSection(tail);
                if (StringUtils.hasText(extracted)) {
                    return truncateQuery(extracted);
                }
            }
        }

        String firstSection = extractFirstSection(normalized);
        if (StringUtils.hasText(firstSection)) {
            return truncateQuery(firstSection);
        }

        return truncateQuery(normalized);
    }

    private String extractFirstSection(String text) {
        String[] lines = text.split("\n");
        StringBuilder builder = new StringBuilder();
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (!StringUtils.hasText(line)) {
                if (!builder.isEmpty()) {
                    break;
                }
                continue;
            }
            if (!builder.isEmpty() && line.startsWith("**")) {
                break;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(line);
        }
        return builder.toString().trim();
    }

    private String truncateQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }
        return query.length() <= MAX_QUERY_LENGTH ? query : query.substring(0, MAX_QUERY_LENGTH);
    }
}
