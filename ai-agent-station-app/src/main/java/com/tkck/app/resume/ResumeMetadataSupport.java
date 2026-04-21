package com.tkck.app.resume;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResumeMetadataSupport {

    public static final String KNOWLEDGE_TAG = "resume_knowledge_space";

    private ResumeMetadataSupport() {
    }

    public static Map<String, Object> buildChunkMetadata(Long knowledgeSpaceId,
                                                         Long resumeId,
                                                         String fileName,
                                                         int chunkIndex) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("knowledge", KNOWLEDGE_TAG);
        metadata.put("knowledgeSpaceId", String.valueOf(knowledgeSpaceId));
        metadata.put("resumeId", String.valueOf(resumeId));
        metadata.put("fileName", fileName);
        metadata.put("docType", "resume");
        metadata.put("chunkIndex", String.valueOf(chunkIndex));
        return metadata;
    }

    public static String buildKnowledgeFilterExpression(Long knowledgeSpaceId) {
        return "knowledge == '" + KNOWLEDGE_TAG + "' && knowledgeSpaceId == '" + knowledgeSpaceId + "'";
    }
}
