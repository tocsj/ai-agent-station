package com.tkck.domain.agent.service;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.alibaba.fastjson.JSON;
import com.tkck.domain.agent.model.entity.ArmoryCommandEntity;
import com.tkck.domain.agent.model.valobj.AiAgentEnumVO;
import com.tkck.domain.agent.model.valobj.AiClientAdvisorTypeEnumVO;
import com.tkck.domain.agent.model.valobj.AiClientAdvisorVO;
import com.tkck.domain.agent.service.armory.factory.DefaultArmoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AiClientAdvisorNode extends AbstractArmorySupport{

    @Resource
    private VectorStore vectorStore;

    @Resource
    private AiClientNode aiClientNode;

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建，Advisor 顾问角色{}", JSON.toJSONString(requestParameter));
        List<AiClientAdvisorVO> aiClientAdvisorList = dynamicContext.getValue(dataName());
        if (aiClientAdvisorList == null || aiClientAdvisorList.isEmpty()) {
            log.warn("没有需要被初始化的 ai client advisor");
            return router(requestParameter, dynamicContext);
        }
        for (AiClientAdvisorVO advisorVO : aiClientAdvisorList) {
            Advisor advisor = createAdvisor(advisorVO);
            // 注册 Advisor 对象
            registerBean(beanName(advisorVO.getAdvisorId()), Advisor.class, advisor);
        }

        return router(requestParameter, dynamicContext);
    }

    private Advisor createAdvisor(AiClientAdvisorVO advisorVO) {
        String advisorType = advisorVO.getAdvisorType();
        AiClientAdvisorTypeEnumVO byCode = AiClientAdvisorTypeEnumVO.getByCode(advisorType);
        return byCode.createAdvisor(advisorVO,vectorStore);
    }
    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientNode;
    }

    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_ADVISOR.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_ADVISOR.getDataName();
    }

}
