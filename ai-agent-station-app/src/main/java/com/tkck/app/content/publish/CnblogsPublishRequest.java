package com.tkck.app.content.publish;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CnblogsPublishRequest {

    private String endpoint;
    private String blogId;
    private String blogApp;
    private String username;
    private String token;
    private String title;
    private String contentMarkdown;
}
