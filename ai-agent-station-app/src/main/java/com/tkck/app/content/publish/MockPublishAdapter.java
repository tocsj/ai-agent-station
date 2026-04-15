package com.tkck.app.content.publish;

import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.publish.IPublishAdapter;
import org.springframework.stereotype.Service;

@Service
public class MockPublishAdapter implements IPublishAdapter {

    @Override
    public String getChannel() {
        return "mock";
    }

    @Override
    public PublishResultEntity publish(PublishCommandEntity command) {
        String externalId = command.getTaskId() == null
                ? "draft_" + Math.abs((command.getTitle() == null ? "content" : command.getTitle()).hashCode())
                : "draft_" + command.getTaskId();
        return PublishResultEntity.builder()
                .success(true)
                .channel(command.getChannel())
                .status("DRAFT_SAVED")
                .externalId(externalId)
                .externalUrl("https://mock-publish.local/draft/" + externalId)
                .message("mock draft saved")
                .build();
    }
}
