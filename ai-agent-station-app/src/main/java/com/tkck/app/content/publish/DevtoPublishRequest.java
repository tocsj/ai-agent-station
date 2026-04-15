package com.tkck.app.content.publish;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevtoPublishRequest {

    private String baseUrl;
    private String token;
    private String username;
    private String title;
    private String bodyMarkdown;
    private String description;
    private String tags;
}
