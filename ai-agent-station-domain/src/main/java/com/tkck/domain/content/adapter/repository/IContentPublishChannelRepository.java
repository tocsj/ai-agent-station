package com.tkck.domain.content.adapter.repository;

import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;

import java.util.List;

public interface IContentPublishChannelRepository {

    boolean existsChannelConfig(String channelCode);

    void insertChannelConfig(ContentPublishChannelConfigEntity config);

    void updateChannelConfig(ContentPublishChannelConfigEntity config);

    ContentPublishChannelConfigEntity queryConfig(String channelCode);

    void updateVerifyStatus(String channelCode, String verifyStatus, String verifyMessage);

    void savePublishRecord(ContentPublishRecordEntity record);

    List<ContentPublishRecordEntity> queryPublishRecords(Long taskId);
}
