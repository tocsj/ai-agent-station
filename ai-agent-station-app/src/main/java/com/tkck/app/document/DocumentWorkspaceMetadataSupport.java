package com.tkck.app.document;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DocumentWorkspaceMetadataSupport {

    public static final String KNOWLEDGE_TAG = "document_knowledge_space";

    private DocumentWorkspaceMetadataSupport() {
    }

    public static Map<String, Object> buildChunkMetadata(String spaceId,
                                                         String docId,
                                                         String fileName,
                                                         int chunkIndex) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("spaceId", spaceId);
        metadata.put("docId", docId);
        metadata.put("fileName", fileName);
        metadata.put("chunkIndex", String.valueOf(chunkIndex));
        metadata.put("docType", "document");
        metadata.put("knowledge", KNOWLEDGE_TAG);
        return metadata;
    }

    public static String buildWorkspaceFilterExpression(String spaceId) {
        return "knowledge == '" + KNOWLEDGE_TAG + "' && spaceId == '" + spaceId + "'";
    }

    public static String buildDocumentFilterExpression(String spaceId, String docId) {
        return buildWorkspaceFilterExpression(spaceId) + " && docId == '" + docId + "'";
    }
}
