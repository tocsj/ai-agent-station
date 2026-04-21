package com.tkck.app.content.publish;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import com.tkck.domain.content.service.publish.IPublishAdapter;
import org.springframework.stereotype.Service;

@Service
public class CnblogsPublishAdapter implements IPublishAdapter {

    private final IContentPublishChannelService contentPublishChannelService;
    private final CnblogsMetaWeblogClient cnblogsMetaWeblogClient;

    public CnblogsPublishAdapter(IContentPublishChannelService contentPublishChannelService,
                                 CnblogsMetaWeblogClient cnblogsMetaWeblogClient) {
        this.contentPublishChannelService = contentPublishChannelService;
        this.cnblogsMetaWeblogClient = cnblogsMetaWeblogClient;
    }

    @Override
    public String getChannel() {
        return "cnblogs";
    }

    @Override
    public PublishResultEntity publish(PublishCommandEntity command) {
        ContentPublishChannelConfigEntity config = contentPublishChannelService.queryConfig("cnblogs");
        if (!"VERIFIED".equalsIgnoreCase(config.getVerifyStatus())) {
            return PublishResultEntity.builder()
                    .success(false)
                    .channel("cnblogs")
                    .status("BLOCKED")
                    .message("博客园 MetaWeblog 配置未验证通过，无法保存草稿")
                    .build();
        }
        try {
            CnblogsPublishRequest request = toPublishRequest(config, command);
            String postId = cnblogsMetaWeblogClient.createDraft(request);
            return PublishResultEntity.builder()
                    .success(true)
                    .channel("cnblogs")
                    .status("DRAFT_SAVED")
                    .externalId(postId)
                    .message("博客园草稿已保存，草稿 ID=" + postId)
                    .build();
        } catch (Exception ex) {
            return PublishResultEntity.builder()
                    .success(false)
                    .channel("cnblogs")
                    .status("ERROR")
                    .message("博客园草稿保存失败：" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()))
                    .build();
        }
    }

    private CnblogsPublishRequest toPublishRequest(ContentPublishChannelConfigEntity config, PublishCommandEntity command) {
        JSONObject jsonObject = JSON.parseObject(config.getCredentialJson());
        return CnblogsPublishRequest.builder()
                .endpoint(text(jsonObject, "endpoint"))
                .blogId(text(jsonObject, "blogId"))
                .blogApp(text(jsonObject, "blogApp"))
                .username(text(jsonObject, "username"))
                .token(text(jsonObject, "token"))
                .title(command.getTitle())
                .contentMarkdown(command.getContent())
                .build();
    }

    private String text(JSONObject jsonObject, String key) {
        String value = jsonObject == null ? null : jsonObject.getString(key);
        return value == null ? "" : value.trim();
    }
}
