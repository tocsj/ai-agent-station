package com.tkck.app.resume;

import java.util.LinkedHashMap;
import java.util.Map;

public class JobStandardMetadataSupport {

    public static final String KNOWLEDGE_TAG = "job_standard_knowledge";

    private JobStandardMetadataSupport() {
    }

    public static Map<String, Object> buildChunkMetadata(String jobCode,
                                                         String skillKey,
                                                         String category,
                                                         String importance,
                                                         int chunkIndex) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("knowledge", KNOWLEDGE_TAG);
        metadata.put("jobCode", jobCode);
        metadata.put("skillKey", skillKey);
        metadata.put("category", category);
        metadata.put("importance", importance);
        metadata.put("docType", "job_standard");
        metadata.put("chunkIndex", String.valueOf(chunkIndex));
        return metadata;
    }

    public static String buildJobFilterExpression(String jobCode) {
        return "knowledge == '" + KNOWLEDGE_TAG + "' && jobCode == '" + jobCode + "'";
    }
}
