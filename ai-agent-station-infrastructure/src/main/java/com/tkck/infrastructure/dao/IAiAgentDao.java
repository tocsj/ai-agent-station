package com.tkck.infrastructure.dao;

import com.tkck.infrastructure.dao.po.AiAgent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI智能体配置表 DAO
 * 提供 ai_agent 表的增删改查操作
 */
@Mapper
public interface IAiAgentDao {

    /**
     * 插入智能体配置
     * @param aiAgent 智能体实体
     * @return 影响行数
     */
    int insert(AiAgent aiAgent);

    /**
     * 根据主键更新智能体
     * @param aiAgent 智能体实体
     * @return 影响行数
     */
    int updateById(AiAgent aiAgent);

    /**
     * 根据 agentId 更新智能体
     * @param aiAgent 智能体实体
     * @return 影响行数
     */
    int updateByAgentId(AiAgent aiAgent);

    /**
     * 根据主键删除
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据 agentId 删除
     * @param agentId 智能体ID
     * @return 影响行数
     */
    int deleteByAgentId(String agentId);

    /**
     * 根据主键查询
     * @param id 主键ID
     * @return 智能体实体
     */
    AiAgent queryById(Long id);

    /**
     * 根据 agentId 查询
     * @param agentId 智能体ID
     * @return 智能体实体
     */
    AiAgent queryByAgentId(String agentId);

    /**
     * 根据名称模糊查询
     * @param agentName 智能体名称
     * @return 智能体列表
     */
    List<AiAgent> queryByAgentName(String agentName);

    /**
     * 根据渠道查询
     * @param channel 渠道类型
     * @return 智能体列表
     */
    List<AiAgent> queryByChannel(String channel);

    /**
     * 查询启用的智能体（status = 1）
     * @return 智能体列表
     */
    List<AiAgent> queryEnabledAgents();

    /**
     * 查询全部智能体，按创建时间倒序
     * @return 智能体列表
     */
    List<AiAgent> queryAll();

}
