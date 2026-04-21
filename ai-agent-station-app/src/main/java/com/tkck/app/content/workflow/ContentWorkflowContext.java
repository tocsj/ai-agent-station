package com.tkck.app.content.workflow;

import com.tkck.domain.content.model.entity.ContentTaskEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentWorkflowContext {

    private ContentTaskEntity task;

    private ResponseBodyEmitter emitter;

    @Builder.Default
    private Map<String, Object> values = new HashMap<>();

    private PublishCommandEntity publishCommand;

    private PublishResultEntity publishResult;

    public <T> void setValue(String key, T value) {
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {
        return (T) values.get(key);
    }
}
