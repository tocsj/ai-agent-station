package com.tkck.infrastructure.dao;

import com.tkck.infrastructure.dao.po.AiKnowledgeChunk;
import com.tkck.infrastructure.dao.po.AiKnowledgeDocument;
import com.tkck.infrastructure.dao.po.AiKnowledgeSpace;
import com.tkck.infrastructure.dao.po.DocumentTaskRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IDocumentWorkspaceDao {

    int insertKnowledgeSpace(AiKnowledgeSpace knowledgeSpace);

    List<AiKnowledgeSpace> queryWorkspaceList();

    int insertKnowledgeDocument(AiKnowledgeDocument knowledgeDocument);

    int insertKnowledgeChunk(AiKnowledgeChunk knowledgeChunk);

    int updateKnowledgeDocumentParseResult(@Param("docId") String docId,
                                           @Param("parseStatus") String parseStatus,
                                           @Param("chunkCount") Integer chunkCount,
                                           @Param("vectorStatus") String vectorStatus);

    AiKnowledgeSpace queryKnowledgeSpaceBySpaceId(@Param("spaceId") String spaceId);

    List<AiKnowledgeDocument> queryKnowledgeDocumentsBySpaceId(@Param("spaceId") String spaceId);

    Integer countWorkspace(@Param("spaceId") String spaceId);

    Integer countDocument(@Param("spaceId") String spaceId, @Param("docId") String docId);

    int insertDocumentTaskRecord(DocumentTaskRecordPO record);

    List<DocumentTaskRecordPO> queryRecentTaskRecords(@Param("workspaceId") String workspaceId, @Param("limit") int limit);

    List<String> queryActiveDocumentIds(@Param("spaceId") String spaceId);
}
