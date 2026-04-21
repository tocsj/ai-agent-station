package com.tkck.infrastructure.adapter.repository;

import com.tkck.domain.content.adapter.repository.IContentPublishChannelRepository;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;
import com.tkck.infrastructure.dao.IContentPublishChannelDao;
import com.tkck.infrastructure.dao.po.ContentPublishChannelConfigPO;
import com.tkck.infrastructure.dao.po.ContentPublishRecordPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ContentPublishChannelRepository implements IContentPublishChannelRepository {

    @Resource
    private IContentPublishChannelDao contentPublishChannelDao;

    @Override
    public boolean existsChannelConfig(String channelCode) {
        Integer count = contentPublishChannelDao.countConfigByChannelCode(channelCode);
        return count != null && count > 0;
    }

    @Override
    public void insertChannelConfig(ContentPublishChannelConfigEntity config) {
        contentPublishChannelDao.insertChannelConfig(toConfigPO(config));
    }

    @Override
    public void updateChannelConfig(ContentPublishChannelConfigEntity config) {
        contentPublishChannelDao.updateChannelConfig(toConfigPO(config));
    }

    @Override
    public ContentPublishChannelConfigEntity queryConfig(String channelCode) {
        return toConfigEntity(contentPublishChannelDao.queryConfigByChannelCode(channelCode));
    }

    @Override
    public void updateVerifyStatus(String channelCode, String verifyStatus, String verifyMessage) {
        contentPublishChannelDao.updateVerifyStatus(channelCode, verifyStatus, verifyMessage);
    }

    @Override
    public void savePublishRecord(ContentPublishRecordEntity record) {
        contentPublishChannelDao.insertPublishRecord(toRecordPO(record));
    }

    @Override
    public List<ContentPublishRecordEntity> queryPublishRecords(Long taskId) {
        return contentPublishChannelDao.queryPublishRecords(taskId).stream()
                .map(this::toRecordEntity)
                .collect(Collectors.toList());
    }

    private ContentPublishChannelConfigPO toConfigPO(ContentPublishChannelConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        return ContentPublishChannelConfigPO.builder()
                .id(entity.getId())
                .channelCode(entity.getChannelCode())
                .channelName(entity.getChannelName())
                .authType(entity.getAuthType())
                .credentialJson(entity.getCredentialJson())
                .verifyStatus(entity.getVerifyStatus())
                .verifyMessage(entity.getVerifyMessage())
                .status(entity.getStatus())
                .build();
    }

    private ContentPublishChannelConfigEntity toConfigEntity(ContentPublishChannelConfigPO po) {
        if (po == null) {
            return null;
        }
        return ContentPublishChannelConfigEntity.builder()
                .id(po.getId())
                .channelCode(po.getChannelCode())
                .channelName(po.getChannelName())
                .authType(po.getAuthType())
                .credentialJson(po.getCredentialJson())
                .verifyStatus(po.getVerifyStatus())
                .verifyMessage(po.getVerifyMessage())
                .status(po.getStatus())
                .createTime(po.getCreateTime() == null ? null : String.valueOf(po.getCreateTime()))
                .updateTime(po.getUpdateTime() == null ? null : String.valueOf(po.getUpdateTime()))
                .build();
    }

    private ContentPublishRecordPO toRecordPO(ContentPublishRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return ContentPublishRecordPO.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .channelCode(entity.getChannelCode())
                .action(entity.getAction())
                .requestSnapshot(entity.getRequestSnapshot())
                .responseSnapshot(entity.getResponseSnapshot())
                .status(entity.getStatus())
                .externalId(entity.getExternalId())
                .externalUrl(entity.getExternalUrl())
                .errorMessage(entity.getErrorMessage())
                .build();
    }

    private ContentPublishRecordEntity toRecordEntity(ContentPublishRecordPO po) {
        if (po == null) {
            return null;
        }
        return ContentPublishRecordEntity.builder()
                .id(po.getId())
                .taskId(po.getTaskId())
                .channelCode(po.getChannelCode())
                .action(po.getAction())
                .requestSnapshot(po.getRequestSnapshot())
                .responseSnapshot(po.getResponseSnapshot())
                .status(po.getStatus())
                .externalId(po.getExternalId())
                .externalUrl(po.getExternalUrl())
                .errorMessage(po.getErrorMessage())
                .createTime(po.getCreateTime() == null ? null : String.valueOf(po.getCreateTime()))
                .build();
    }
}
