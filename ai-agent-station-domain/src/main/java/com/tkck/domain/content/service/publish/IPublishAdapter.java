package com.tkck.domain.content.service.publish;

import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;

public interface IPublishAdapter {

    String getChannel();

    PublishResultEntity publish(PublishCommandEntity command);
}
