package com.tkck.app.content.publish;

import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.PublishCommandEntity;
import com.tkck.domain.content.model.entity.PublishResultEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import com.tkck.domain.content.service.publish.IPublishAdapter;
import org.springframework.stereotype.Service;

@Service
public class JuejinPublishAdapter implements IPublishAdapter {

    private final IContentPublishChannelService contentPublishChannelService;

    public JuejinPublishAdapter(IContentPublishChannelService contentPublishChannelService) {
        this.contentPublishChannelService = contentPublishChannelService;
    }

    @Override
    public String getChannel() {
        return "juejin";
    }

    @Override
    public PublishResultEntity publish(PublishCommandEntity command) {
        ContentPublishChannelConfigEntity config = contentPublishChannelService.queryConfig("juejin");
        if (!"VERIFIED".equalsIgnoreCase(config.getVerifyStatus())) {
            return PublishResultEntity.builder()
                    .success(false)
                    .channel("juejin")
                    .status("BLOCKED")
                    .message("掘金 token 未验证通过，无法执行发布")
                    .build();
        }
        return PublishResultEntity.builder()
                .success(false)
                .channel("juejin")
                .status("BLOCKED")
                .message("官方公开 API 暂不支持文章发布，当前仅接入 token 验证")
                .build();
    }
}
