package com.tkck.trigger.http;

import com.tkck.api.dto.DocumentAskRequestDTO;
import com.tkck.api.dto.DocumentFollowupRequestDTO;
import com.tkck.api.dto.DocumentQuizRequestDTO;
import com.tkck.api.dto.DocumentRetrievedChunkDTO;
import com.tkck.api.dto.DocumentSummaryRequestDTO;
import com.tkck.api.dto.DocumentTaskResultResponseDTO;
import com.tkck.api.dto.DocumentUploadResponseDTO;
import com.tkck.api.dto.DocumentWorkspaceCreateRequestDTO;
import com.tkck.api.dto.DocumentWorkspaceCreateResponseDTO;
import com.tkck.api.dto.DocumentWorkspaceDetailResponseDTO;
import com.tkck.api.dto.DocumentWorkspaceListItemDTO;
import com.tkck.api.response.Response;
import com.tkck.domain.document.model.entity.DocumentFileEntity;
import com.tkck.domain.document.model.entity.DocumentTaskResultEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceDetailEntity;
import com.tkck.domain.document.model.entity.DocumentWorkspaceEntity;
import com.tkck.domain.document.service.IDocumentWorkspaceService;
import com.tkck.types.enums.ResponseCode;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/document")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class DocumentWorkspaceController {

    @Resource
    private IDocumentWorkspaceService documentWorkspaceService;

    @PostMapping("/workspace/create")
    public Response<DocumentWorkspaceCreateResponseDTO> createWorkspace(@RequestBody DocumentWorkspaceCreateRequestDTO request) {
        DocumentWorkspaceEntity result = documentWorkspaceService.createWorkspace(request.getWorkspaceName(), request.getDescription());
        return Response.<DocumentWorkspaceCreateResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(DocumentWorkspaceCreateResponseDTO.builder()
                        .workspaceId(result.getWorkspaceId())
                        .workspaceName(result.getWorkspaceName())
                        .description(result.getDescription())
                        .status(result.getStatus())
                        .build())
                .build();
    }

    @GetMapping("/workspace/list")
    public Response<List<DocumentWorkspaceListItemDTO>> listWorkspaces() {
        return Response.<List<DocumentWorkspaceListItemDTO>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(documentWorkspaceService.listWorkspaces().stream()
                        .map(this::toWorkspaceListItem)
                        .collect(Collectors.toList()))
                .build();
    }

    @PostMapping("/upload")
    public Response<DocumentUploadResponseDTO> upload(@RequestParam("workspaceId") String workspaceId,
                                                      @RequestParam("file") MultipartFile file) throws Exception {
        DocumentFileEntity result = documentWorkspaceService.upload(workspaceId, file);
        return Response.<DocumentUploadResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(DocumentUploadResponseDTO.builder()
                        .workspaceId(result.getWorkspaceId())
                        .docId(result.getDocId())
                        .fileName(result.getFileName())
                        .fileType(result.getFileType())
                        .chunkCount(result.getChunkCount())
                        .parseStatus(result.getParseStatus())
                        .vectorStatus(result.getVectorStatus())
                        .build())
                .build();
    }

    @GetMapping("/workspace/{workspaceId}")
    public Response<DocumentWorkspaceDetailResponseDTO> workspaceDetail(@PathVariable("workspaceId") String workspaceId) {
        DocumentWorkspaceDetailEntity detail = documentWorkspaceService.queryWorkspaceDetail(workspaceId);
        return Response.<DocumentWorkspaceDetailResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(DocumentWorkspaceDetailResponseDTO.builder()
                        .workspaceId(detail.getWorkspaceId())
                        .workspaceName(detail.getWorkspaceName())
                        .description(detail.getDescription())
                        .status(detail.getStatus())
                        .documentCount(detail.getDocumentCount())
                        .documents(detail.getDocuments() == null ? null : detail.getDocuments().stream()
                                .map(this::toDocumentItem)
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }

    @PostMapping("/ask")
    public Response<DocumentTaskResultResponseDTO> ask(@RequestBody DocumentAskRequestDTO request) {
        return Response.<DocumentTaskResultResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toTaskResult(documentWorkspaceService.ask(
                        request.getWorkspaceId(),
                        request.getDocId(),
                        request.getQuestion())))
                .build();
    }

    @PostMapping("/summary")
    public Response<DocumentTaskResultResponseDTO> summary(@RequestBody DocumentSummaryRequestDTO request) {
        return Response.<DocumentTaskResultResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toTaskResult(documentWorkspaceService.summary(
                        request.getWorkspaceId(),
                        request.getDocId(),
                        request.getSummaryMode())))
                .build();
    }

    @PostMapping("/followup")
    public Response<DocumentTaskResultResponseDTO> followup(@RequestBody DocumentFollowupRequestDTO request) {
        return Response.<DocumentTaskResultResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toTaskResult(documentWorkspaceService.followup(
                        request.getWorkspaceId(),
                        request.getDocId(),
                        request.getPerspective())))
                .build();
    }

    @PostMapping("/quiz")
    public Response<DocumentTaskResultResponseDTO> quiz(@RequestBody DocumentQuizRequestDTO request) {
        return Response.<DocumentTaskResultResponseDTO>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(toTaskResult(documentWorkspaceService.quiz(
                        request.getWorkspaceId(),
                        request.getDocId(),
                        request.getQuestionCount(),
                        request.getQuizType())))
                .build();
    }

    private DocumentWorkspaceDetailResponseDTO.DocumentItem toDocumentItem(DocumentFileEntity document) {
        return DocumentWorkspaceDetailResponseDTO.DocumentItem.builder()
                .docId(document.getDocId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .parseStatus(document.getParseStatus())
                .chunkCount(document.getChunkCount())
                .vectorStatus(document.getVectorStatus())
                .build();
    }

    private DocumentTaskResultResponseDTO toTaskResult(DocumentTaskResultEntity result) {
        return DocumentTaskResultResponseDTO.builder()
                .answer(result.getAnswer())
                .rewrittenQuery(result.getRewrittenQuery())
                .retrievalScope(result.getRetrievalScope())
                .finalContext(result.getFinalContext())
                .retrievedChunks(result.getRetrievedChunks())
                .retrievedChunkDetails(result.getRetrievedChunkDetails() == null ? null : result.getRetrievedChunkDetails().stream()
                        .map(item -> DocumentRetrievedChunkDTO.builder()
                                .workspaceId(item.getWorkspaceId())
                                .docId(item.getDocId())
                                .fileName(item.getFileName())
                                .chunkIndex(item.getChunkIndex())
                                .preview(item.getPreview())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private DocumentWorkspaceListItemDTO toWorkspaceListItem(DocumentWorkspaceEntity workspace) {
        return DocumentWorkspaceListItemDTO.builder()
                .workspaceId(workspace.getWorkspaceId())
                .workspaceName(workspace.getWorkspaceName())
                .description(workspace.getDescription())
                .status(workspace.getStatus())
                .documentCount(workspace.getDocumentCount())
                .updateTime(workspace.getUpdateTime())
                .build();
    }
}
