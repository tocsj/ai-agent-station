package com.tkck.app.content;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tkck.app.content.publish.CnblogsMetaWeblogClient;
import com.tkck.app.content.publish.CnblogsPublishRequest;
import com.tkck.app.content.publish.DevtoApiClient;
import com.tkck.domain.content.adapter.repository.IContentPublishChannelRepository;
import com.tkck.domain.content.model.entity.ChannelVerifyResultEntity;
import com.tkck.domain.content.model.entity.ContentPublishChannelConfigEntity;
import com.tkck.domain.content.model.entity.ContentPublishRecordEntity;
import com.tkck.domain.content.service.IContentPublishChannelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ContentPublishChannelServiceImpl implements IContentPublishChannelService {

    private static final String JUEJIN_VERIFY_URL = "https://api.juejin.cn/aicoding_api/v1/verify_token";

    @Resource
    private IContentPublishChannelRepository contentPublishChannelRepository;

    private final RestClient restClient;
    private final CnblogsMetaWeblogClient cnblogsMetaWeblogClient;
    private final DevtoApiClient devtoApiClient;

    @Autowired
    public ContentPublishChannelServiceImpl(RestClient.Builder restClientBuilder,
                                            CnblogsMetaWeblogClient cnblogsMetaWeblogClient,
                                            DevtoApiClient devtoApiClient) {
        this(restClientBuilder.build(), cnblogsMetaWeblogClient, devtoApiClient);
    }

    public ContentPublishChannelServiceImpl(RestClient restClient) {
        this(restClient, new CnblogsMetaWeblogClient(restClient), new DevtoApiClient(restClient));
    }

    public ContentPublishChannelServiceImpl(RestClient restClient,
                                            CnblogsMetaWeblogClient cnblogsMetaWeblogClient,
                                            DevtoApiClient devtoApiClient) {
        this.restClient = restClient;
        this.cnblogsMetaWeblogClient = cnblogsMetaWeblogClient;
        this.devtoApiClient = devtoApiClient;
    }

    @Override
    public ContentPublishChannelConfigEntity saveOrUpdateConfig(String channel, String token) {
        return saveOrUpdateConfig(channel, token, null, null, null, null);
    }

    @Override
    public ContentPublishChannelConfigEntity saveOrUpdateConfig(String channel,
                                                                String token,
                                                                String blogApp,
                                                                String blogId,
                                                                String username,
                                                                String endpoint) {
        String normalizedChannel = normalizeChannel(channel);
        ContentPublishChannelConfigEntity config = ContentPublishChannelConfigEntity.builder()
                .channelCode(normalizedChannel)
                .channelName(channelName(normalizedChannel))
                .authType(authType(normalizedChannel))
                .credentialJson(buildCredentialJson(normalizedChannel, token, blogApp, blogId, username, endpoint))
                .verifyStatus("UNVERIFIED")
                .verifyMessage("待验证")
                .status(1)
                .build();
        if (contentPublishChannelRepository.existsChannelConfig(normalizedChannel)) {
            contentPublishChannelRepository.updateChannelConfig(config);
        } else {
            contentPublishChannelRepository.insertChannelConfig(config);
        }
        return queryConfig(normalizedChannel);
    }

    @Override
    public ContentPublishChannelConfigEntity queryConfig(String channel) {
        String normalizedChannel = normalizeChannel(channel);
        ContentPublishChannelConfigEntity config = contentPublishChannelRepository.queryConfig(normalizedChannel);
        if (config != null) {
            return config;
        }
        return ContentPublishChannelConfigEntity.builder()
                .channelCode(normalizedChannel)
                .channelName(channelName(normalizedChannel))
                .authType(authType(normalizedChannel))
                .verifyStatus("UNCONFIGURED")
                .verifyMessage("未配置")
                .status(0)
                .build();
    }

    @Override
    public ChannelVerifyResultEntity verifyJuejinConfig() {
        ContentPublishChannelConfigEntity config = queryConfig("juejin");
        String token = extractToken(config.getCredentialJson());
        if (token.isBlank()) {
            updateVerifyStatus("juejin", "UNCONFIGURED", "未配置 token");
            return ChannelVerifyResultEntity.builder()
                    .channel("juejin")
                    .verified(false)
                    .verifyStatus("UNCONFIGURED")
                    .message("未配置 token")
                    .build();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(JUEJIN_VERIFY_URL)
                    .header("X-Aicoding-Token", token)
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .body(Map.class);
            int errNo = response == null || response.get("err_no") == null ? -1 : ((Number) response.get("err_no")).intValue();
            String errMsg = response == null ? "empty response" : String.valueOf(response.getOrDefault("err_msg", ""));
            if (errNo == 0) {
                updateVerifyStatus("juejin", "VERIFIED", "token 可用");
                return ChannelVerifyResultEntity.builder()
                        .channel("juejin")
                        .verified(true)
                        .verifyStatus("VERIFIED")
                        .message("token 可用")
                        .build();
            }
            String message = errNo == 403 ? "token 已过期，请重新生成" : errMsg;
            updateVerifyStatus("juejin", "FAILED", message);
            return ChannelVerifyResultEntity.builder()
                    .channel("juejin")
                    .verified(false)
                    .verifyStatus("FAILED")
                    .message(message)
                    .build();
        } catch (Exception ex) {
            if ("VERIFIED".equalsIgnoreCase(config.getVerifyStatus())) {
                return ChannelVerifyResultEntity.builder()
                        .channel("juejin")
                        .verified(true)
                        .verifyStatus("VERIFIED")
                        .message(config.getVerifyMessage())
                        .build();
            }
            String message = ex.getMessage() == null ? "验证失败" : ex.getMessage();
            updateVerifyStatus("juejin", "FAILED", message);
            return ChannelVerifyResultEntity.builder()
                    .channel("juejin")
                    .verified(false)
                    .verifyStatus("FAILED")
                    .message(message)
                    .build();
        }
    }

    @Override
    public ChannelVerifyResultEntity verifyCnblogsConfig() {
        ContentPublishChannelConfigEntity config = queryConfig("cnblogs");
        CnblogsPublishRequest request = toCnblogsVerifyRequest(config);
        if (request.getEndpoint().isBlank() || request.getBlogId().isBlank()
                || request.getUsername().isBlank() || request.getToken().isBlank()) {
            updateVerifyStatus("cnblogs", "UNCONFIGURED", "博客园 MetaWeblog 配置不完整");
            return ChannelVerifyResultEntity.builder()
                    .channel("cnblogs")
                    .verified(false)
                    .verifyStatus("UNCONFIGURED")
                    .message("博客园 MetaWeblog 配置不完整")
                    .build();
        }
        try {
            cnblogsMetaWeblogClient.verifyCredential(request);
            updateVerifyStatus("cnblogs", "VERIFIED", "博客园 MetaWeblog 配置可用");
            return ChannelVerifyResultEntity.builder()
                    .channel("cnblogs")
                    .verified(true)
                    .verifyStatus("VERIFIED")
                    .message("博客园 MetaWeblog 配置可用")
                    .build();
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "博客园 MetaWeblog 验证失败" : ex.getMessage();
            updateVerifyStatus("cnblogs", "FAILED", message);
            return ChannelVerifyResultEntity.builder()
                    .channel("cnblogs")
                    .verified(false)
                    .verifyStatus("FAILED")
                    .message(message)
                    .build();
        }
    }

    @Override
    public ChannelVerifyResultEntity verifyDevtoConfig() {
        ContentPublishChannelConfigEntity config = queryConfig("devto");
        JSONObject jsonObject = parseCredential(config.getCredentialJson());
        String token = read(jsonObject, "token");
        String baseUrl = read(jsonObject, "baseUrl");
        String username = read(jsonObject, "username");
        log.info("开始验证 Dev.to 配置, channel=devto, baseUrl={}, username={}, tokenLength={}, tokenPrefix={}",
                baseUrl, username, token.length(), maskTokenPrefix(token));
        if (token.isBlank()) {
            updateVerifyStatus("devto", "UNCONFIGURED", "Dev.to API Key 未配置");
            log.warn("Dev.to 配置验证失败, 原因=未配置 API Key, baseUrl={}, username={}", baseUrl, username);
            return ChannelVerifyResultEntity.builder()
                    .channel("devto")
                    .verified(false)
                    .verifyStatus("UNCONFIGURED")
                    .message("Dev.to API Key 未配置")
                    .build();
        }
        try {
            Map<String, Object> profile = devtoApiClient.verify(baseUrl, token);
            String verifiedUsername = profile.get("username") == null ? "unknown" : String.valueOf(profile.get("username"));
            updateVerifyStatus("devto", "VERIFIED", "Dev.to API Key 可用，当前账号：" + verifiedUsername);
            log.info("Dev.to 配置验证成功, channel=devto, baseUrl={}, username={}", baseUrl, verifiedUsername);
            return ChannelVerifyResultEntity.builder()
                    .channel("devto")
                    .verified(true)
                    .verifyStatus("VERIFIED")
                    .message("Dev.to API Key 可用")
                    .build();
        } catch (Exception ex) {
            String message = formatDevtoVerifyError(ex);
            updateVerifyStatus("devto", "FAILED", message);
            log.warn("Dev.to 配置验证失败, channel=devto, baseUrl={}, username={}, errorType={}, message={}",
                    baseUrl, username, ex.getClass().getSimpleName(), message);
            return ChannelVerifyResultEntity.builder()
                    .channel("devto")
                    .verified(false)
                    .verifyStatus("FAILED")
                    .message(message)
                    .build();
        }
    }

    @Override
    public void recordPublishAttempt(ContentPublishRecordEntity record) {
        contentPublishChannelRepository.savePublishRecord(record);
    }

    @Override
    public List<ContentPublishRecordEntity> queryPublishRecords(Long taskId) {
        return contentPublishChannelRepository.queryPublishRecords(taskId);
    }

    private void updateVerifyStatus(String channel, String verifyStatus, String verifyMessage) {
        contentPublishChannelRepository.updateVerifyStatus(normalizeChannel(channel), verifyStatus, verifyMessage);
    }

    private String normalizeChannel(String channel) {
        return channel == null || channel.isBlank() ? "juejin" : channel.trim().toLowerCase();
    }

    private String channelName(String channel) {
        if ("juejin".equalsIgnoreCase(channel)) {
            return "掘金";
        }
        if ("cnblogs".equalsIgnoreCase(channel)) {
            return "博客园";
        }
        if ("devto".equalsIgnoreCase(channel)) {
            return "Dev.to";
        }
        return channel;
    }

    private String authType(String channel) {
        if ("cnblogs".equalsIgnoreCase(channel)) {
            return "metaweblog";
        }
        if ("devto".equalsIgnoreCase(channel)) {
            return "api_key";
        }
        return "token";
    }

    private String buildCredentialJson(String channel, String token, String blogApp, String blogId, String username, String endpoint) {
        if ("devto".equalsIgnoreCase(channel)) {
            return JSON.toJSONString(Map.of(
                    "token", clean(token),
                    "baseUrl", clean(endpoint).isBlank() ? "https://dev.to" : clean(endpoint),
                    "username", clean(username),
                    "defaultTags", "",
                    "publishMode", "draft"
            ));
        }
        if (!"cnblogs".equalsIgnoreCase(channel)) {
            return JSON.toJSONString(Map.of("token", clean(token)));
        }
        String normalizedBlogApp = clean(blogApp);
        String normalizedBlogId = clean(blogId).isBlank() ? normalizedBlogApp : clean(blogId);
        String normalizedEndpoint = clean(endpoint).isBlank()
                ? "https://rpc.cnblogs.com/metaweblog/" + normalizedBlogId
                : clean(endpoint);
        return JSON.toJSONString(Map.of(
                "token", clean(token),
                "blogApp", normalizedBlogApp,
                "blogId", normalizedBlogId,
                "username", clean(username),
                "endpoint", normalizedEndpoint
        ));
    }

    private CnblogsPublishRequest toCnblogsVerifyRequest(ContentPublishChannelConfigEntity config) {
        JSONObject jsonObject = parseCredential(config.getCredentialJson());
        return CnblogsPublishRequest.builder()
                .endpoint(read(jsonObject, "endpoint"))
                .blogId(read(jsonObject, "blogId"))
                .blogApp(read(jsonObject, "blogApp"))
                .username(read(jsonObject, "username"))
                .token(read(jsonObject, "token"))
                .build();
    }

    private String extractToken(String credentialJson) {
        JSONObject jsonObject = parseCredential(credentialJson);
        return read(jsonObject, "token");
    }

    private JSONObject parseCredential(String credentialJson) {
        if (credentialJson == null || credentialJson.isBlank()) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(credentialJson);
        } catch (Exception ignore) {
            return new JSONObject();
        }
    }

    private String read(JSONObject jsonObject, String key) {
        String value = jsonObject == null ? null : jsonObject.getString(key);
        return clean(value);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String maskTokenPrefix(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        return token.substring(0, Math.min(4, token.length()));
    }

    private String formatDevtoVerifyError(Exception ex) {
        if (ex instanceof HttpClientErrorException.Forbidden
                || (ex instanceof HttpClientErrorException httpEx && httpEx.getStatusCode().value() == 403)) {
            return "Dev.to 配置验证失败，HTTP 403 Forbidden。请检查 API Key 是否具备访问权限，或请求特征是否被 Dev.to 拒绝。";
        }
        if (ex instanceof HttpClientErrorException.Unauthorized
                || (ex instanceof HttpClientErrorException httpEx && httpEx.getStatusCode().value() == 401)) {
            return "Dev.to 配置验证失败，HTTP 401 Unauthorized。请检查 API Key 是否正确或已失效。";
        }
        String message = ex.getMessage() == null ? "" : ex.getMessage().trim();
        if (message.isBlank()) {
            return "Dev.to API Key 验证失败";
        }
        return "Dev.to API Key 验证失败: " + message;
    }
}
