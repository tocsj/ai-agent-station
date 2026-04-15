package com.tkck.app.content.publish;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import com.tkck.domain.content.service.publish.IPublishAdapter;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public class DevtoPublishAdapter implements IPublishAdapter {

    private final IContentPublishChannelService contentPublishChannelService;
    private final DevtoApiClient devtoApiClient;

    public DevtoPublishAdapter(IContentPublishChannelService contentPublishChannelService, DevtoApiClient devtoApiClient) {
        this.contentPublishChannelService = contentPublishChannelService;
        this.devtoApiClient = devtoApiClient;
    }

    @Override
    public String getChannel() {
        return "devto";
    }

    @Override
    public PublishResultEntity publish(PublishCommandEntity command) {
        ContentPublishChannelConfigEntity config = contentPublishChannelService.queryConfig("devto");
        if (!"VERIFIED".equalsIgnoreCase(config.getVerifyStatus())) {
            return PublishResultEntity.builder()
                    .success(false)
                    .channel("devto")
                    .status("BLOCKED")
                    .message("Dev.to API Key 未验证通过，无法保存草稿")
                    .build();
        }
        try {
            DevtoPublishRequest request = toPublishRequest(config, command);
            Map<String, Object> article = devtoApiClient.createDraft(request);
            String externalId = String.valueOf(article.get("id"));
            String externalUrl = article.get("url") == null ? null : String.valueOf(article.get("url"));
            return PublishResultEntity.builder()
                    .success(true)
                    .channel("devto")
                    .status("DRAFT_SAVED")
                    .externalId(externalId)
                    .externalUrl(externalUrl)
                    .message("Dev.to 草稿已保存，草稿 ID=" + externalId)
                    .build();
        } catch (Exception ex) {
            return PublishResultEntity.builder()
                    .success(false)
                    .channel("devto")
                    .status("ERROR")
                    .message("Dev.to 草稿保存失败：" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()))
                    .build();
        }
    }

    private DevtoPublishRequest toPublishRequest(ContentPublishChannelConfigEntity config, PublishCommandEntity command) {
        JSONObject jsonObject = JSON.parseObject(config.getCredentialJson());
        return DevtoPublishRequest.builder()
                .baseUrl(text(jsonObject, "baseUrl"))
                .token(text(jsonObject, "token"))
                .username(text(jsonObject, "username"))
                .title(command.getTitle())
                .bodyMarkdown(command.getContent())
                .description(command.getSummary())
                .tags(mergeTags(text(jsonObject, "defaultTags"), command.getTags()))
                .build();
    }

    private String mergeTags(String defaultTags, String taskTags) {
        Set<String> merged = new LinkedHashSet<>();
        addTags(merged, defaultTags);
        addTags(merged, taskTags);
        return String.join(",", merged);
    }

    private void addTags(Set<String> target, String source) {
        if (source == null || source.isBlank()) {
            return;
        }
        for (String item : source.split(",")) {
            String value = item.trim();
            if (!value.isBlank()) {
                target.add(value);
            }
        }
    }

    private String text(JSONObject jsonObject, String key) {
        String value = jsonObject == null ? null : jsonObject.getString(key);
        return value == null ? "" : value.trim();
    }
}
