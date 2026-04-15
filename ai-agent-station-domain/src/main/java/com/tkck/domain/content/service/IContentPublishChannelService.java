package com.tkck.domain.content.service;

import com.tkck.domain.content.model.entity.ChannelVerifyResultEntity;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;

import java.util.List;

public interface IContentPublishChannelService {

    ContentPublishChannelConfigEntity saveOrUpdateConfig(String channel, String token);

    ContentPublishChannelConfigEntity queryConfig(String channel);

    ChannelVerifyResultEntity verifyJuejinConfig();

    void recordPublishAttempt(ContentPublishRecordEntity record);

    List<ContentPublishRecordEntity> queryPublishRecords(Long taskId);
}
