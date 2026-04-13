package com.tkck.test.resume;

import com.tkck.app.resume.ResumeMetadataSupport;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ResumeMetadataSupportTest {

    @Test
    public void should_build_chunk_metadata_with_required_fields() {
        Map<String, Object> metadata = ResumeMetadataSupport.buildChunkMetadata(11L, 22L, "resume.pdf", 3);

        Assert.assertEquals("resume_knowledge_space", metadata.get("knowledge"));
        Assert.assertEquals("11", metadata.get("knowledgeSpaceId"));
        Assert.assertEquals("22", metadata.get("resumeId"));
        Assert.assertEquals("resume.pdf", metadata.get("fileName"));
        Assert.assertEquals("resume", metadata.get("docType"));
        Assert.assertEquals("3", metadata.get("chunkIndex"));
    }

    @Test
    public void should_build_filter_expression_for_single_knowledge_space() {
        String expression = ResumeMetadataSupport.buildKnowledgeFilterExpression(11L);

        Assert.assertEquals("knowledge == 'resume_knowledge_space' && knowledgeSpaceId == '11'", expression);
    }
}
