package com.tkck.domain.agent.service.execute;

import com.tkck.domain.agent.model.entity.ExecuteCommandEntity;

public interface IExecuteStrategy {
    void execute(ExecuteCommandEntity requestParameter) throws Exception;
}
