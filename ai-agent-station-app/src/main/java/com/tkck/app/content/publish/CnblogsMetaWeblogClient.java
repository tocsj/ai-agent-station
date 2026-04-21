package com.tkck.app.content.publish;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

@Component
public class CnblogsMetaWeblogClient {

    private final RestClient restClient;

    @Autowired
    public CnblogsMetaWeblogClient(RestClient.Builder restClientBuilder) {
        this(restClientBuilder.build());
    }

    public CnblogsMetaWeblogClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean verifyCredential(CnblogsPublishRequest request) {
        String response = postXml(request.getEndpoint(), buildGetUsersBlogsXml(request));
        assertNoFault(response);
        return true;
    }

    public String createDraft(CnblogsPublishRequest request) {
        String response = postXml(request.getEndpoint(), buildNewPostXml(request, false));
        assertNoFault(response);
        return firstValueText(response);
    }

    private String postXml(String endpoint, String xml) {
        return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.TEXT_XML)
                .body(xml)
                .retrieve()
                .body(String.class);
    }

    private String buildGetUsersBlogsXml(CnblogsPublishRequest request) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <methodCall>
                  <methodName>blogger.getUsersBlogs</methodName>
                  <params>
                    <param><value><string></string></value></param>
                    <param><value><string>%s</string></value></param>
                    <param><value><string>%s</string></value></param>
                  </params>
                </methodCall>
                """.formatted(xml(request.getUsername()), xml(request.getToken()));
    }

    private String buildNewPostXml(CnblogsPublishRequest request, boolean publish) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <methodCall>
                  <methodName>metaWeblog.newPost</methodName>
                  <params>
                    <param><value><string>%s</string></value></param>
                    <param><value><string>%s</string></value></param>
                    <param><value><string>%s</string></value></param>
                    <param>
                      <value>
                        <struct>
                          <member><name>title</name><value><string>%s</string></value></member>
                          <member><name>description</name><value><string>%s</string></value></member>
                        </struct>
                      </value>
                    </param>
                    <param><value><boolean>%s</boolean></value></param>
                  </params>
                </methodCall>
                """.formatted(
                xml(request.getBlogId()),
                xml(request.getUsername()),
                xml(request.getToken()),
                xml(request.getTitle()),
                xml(markdownToHtml(request.getContentMarkdown())),
                publish ? "1" : "0"
        );
    }

    private String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        String[] lines = markdown.replace("\r\n", "\n").split("\n");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith("# ")) {
                html.append("<h1>").append(xml(line.substring(2).trim())).append("</h1>");
            } else if (line.startsWith("## ")) {
                html.append("<h2>").append(xml(line.substring(3).trim())).append("</h2>");
            } else {
                html.append("<p>").append(xml(line.trim())).append("</p>");
            }
        }
        return html.toString();
    }

    private String firstValueText(String xml) {
        Document document = parse(xml);
        NodeList values = document.getElementsByTagName("value");
        if (values.getLength() == 0) {
            throw new IllegalStateException("博客园 MetaWeblog 返回为空，未拿到草稿 ID");
        }
        return values.item(0).getTextContent().trim();
    }

    private void assertNoFault(String xml) {
        Document document = parse(xml);
        NodeList faults = document.getElementsByTagName("fault");
        if (faults.getLength() > 0) {
            throw new IllegalStateException("博客园 MetaWeblog 调用失败：" + faults.item(0).getTextContent().trim());
        }
    }

    private Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml == null ? "" : xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("博客园 MetaWeblog XML 解析失败：" + e.getMessage(), e);
        }
    }

    private String xml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
