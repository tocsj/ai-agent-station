package com.tkck.test.document;

import com.tkck.app.document.DocumentWorkspaceMetadataSupport;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class DocumentWorkspaceMetadataSupportTest {

    @Test
    public void should_build_document_chunk_metadata() {
        Map<String, Object> metadata = DocumentWorkspaceMetadataSupport.buildChunkMetadata(
                "dws_001",
                "doc_001",
                "architecture.md",
                2
        );

        Assert.assertEquals("dws_001", metadata.get("spaceId"));
        Assert.assertEquals("doc_001", metadata.get("docId"));
        Assert.assertEquals("architecture.md", metadata.get("fileName"));
        Assert.assertEquals("2", metadata.get("chunkIndex"));
        Assert.assertEquals("document", metadata.get("docType"));
        Assert.assertEquals("document_knowledge_space", metadata.get("knowledge"));
    }
}
