package com.tkck.infrastructure.dao;

import com.tkck.infrastructure.dao.po.ContentPublishChannelConfigPO;
import com.tkck.infrastructure.dao.po.ContentPublishRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IContentPublishChannelDao {

    Integer countConfigByChannelCode(@Param("channelCode") String channelCode);

    int insertChannelConfig(ContentPublishChannelConfigPO config);

    int updateChannelConfig(ContentPublishChannelConfigPO config);

    ContentPublishChannelConfigPO queryConfigByChannelCode(@Param("channelCode") String channelCode);

    int updateVerifyStatus(@Param("channelCode") String channelCode,
                           @Param("verifyStatus") String verifyStatus,
                           @Param("verifyMessage") String verifyMessage);

    int insertPublishRecord(ContentPublishRecordPO record);

    List<ContentPublishRecordPO> queryPublishRecords(@Param("taskId") Long taskId);
}
