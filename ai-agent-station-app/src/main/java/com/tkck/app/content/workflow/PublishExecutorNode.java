package com.tkck.app.content.workflow;

import com.tkck.domain.agent.service.runtime.resilience.ExecutionStage;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import com.tkck.domain.content.service.publish.IPublishAdapter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(7)
public class PublishExecutorNode extends AbstractContentWorkflowNode {

    private final Map<String, IPublishAdapter> publishAdapterMap;
    private final IContentPublishChannelService contentPublishChannelService;

    public PublishExecutorNode(List<IPublishAdapter> publishAdapters, IContentPublishChannelService contentPublishChannelService) {
        this.publishAdapterMap = publishAdapters.stream()
                .collect(Collectors.toMap(IPublishAdapter::getChannel, Function.identity()));
        this.contentPublishChannelService = contentPublishChannelService;
    }

    @Override
    public int stepNo() {
        return 7;
    }

    @Override
    public String stepName() {
        return "publish_execute";
    }

    @Override
    public ExecutionStage stage() {
        return ExecutionStage.CONTENT_PUBLISH_EXECUTE;
    }

    @Override
    public String apply(ContentWorkflowContext context) {
        PublishCommandEntity command = context.getPublishCommand();
        PublishResultEntity result;
        if (command == null || "block".equalsIgnoreCase(command.getAction())) {
            result = PublishResultEntity.builder()
                    .success(false)
                    .channel(command == null ? context.getTask().getChannel() : command.getChannel())
                    .status("BLOCKED")
                    .message("publish blocked by planner")
                    .build();
        } else {
            IPublishAdapter adapter = publishAdapterMap.get(command.getChannel());
            if (adapter == null) {
                throw new IllegalArgumentException("no publish adapter found for channel=" + command.getChannel());
            }
            result = adapter.publish(command);
        }
        contentPublishChannelService.recordPublishAttempt(ContentPublishRecordEntity.builder()
                .taskId(context.getTask().getTaskId())
                .channelCode(result.getChannel())
                .action(command == null ? "block" : command.getAction())
                .requestSnapshot(command == null ? null : "title=" + safe(command.getTitle()) + "\nchannel=" + safe(command.getChannel()))
                .responseSnapshot("status=" + safe(result.getStatus()) + "\nmessage=" + safe(result.getMessage()))
                .status(result.getStatus())
                .externalId(result.getExternalId())
                .externalUrl(result.getExternalUrl())
                .errorMessage(result.getMessage())
                .build());
        context.setPublishResult(result);
        return "status=" + result.getStatus()
                + "\nexternalId=" + safe(result.getExternalId())
                + "\nexternalUrl=" + safe(result.getExternalUrl())
                + "\nmessage=" + safe(result.getMessage());
    }
}
