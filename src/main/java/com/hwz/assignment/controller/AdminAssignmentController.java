package com.hwz.assignment.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.assignment.dto.AssignmentDtos;
import com.hwz.assignment.entity.AssignmentMaterial;
import com.hwz.assignment.entity.AssignmentSubmissionFile;
import com.hwz.assignment.service.AssignmentService;
import com.hwz.common.PageResponse;
import com.hwz.common.Result;
import com.hwz.common.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@RestController
@RequestMapping("/admin/assignments")
public class AdminAssignmentController {

    private final AssignmentService assignmentService;
    private final AdminAccessService accessService;

    public AdminAssignmentController(AssignmentService assignmentService, AdminAccessService accessService) {
        this.assignmentService = assignmentService;
        this.accessService = accessService;
    }

    @GetMapping
    public Result<PageResponse<AssignmentDtos.AssignmentSummary>> list(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String status,
                                                                       @RequestParam(defaultValue = "1") long page,
                                                                       @RequestParam(defaultValue = "10") long pageSize) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.pageAdmin(keyword, status, normalizePage(page), normalizePageSize(pageSize)));
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.adminStats(keyword, status));
    }

    @GetMapping("/{assignmentId}")
    public Result<AssignmentDtos.AssignmentDetail> detail(@PathVariable Long assignmentId) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.getAdminDetail(assignmentId));
    }

    @PostMapping
    public Result<AssignmentDtos.AssignmentDetail> create(@RequestBody AssignmentDtos.AssignmentSaveRequest request) {
        User operator = accessService.requireAdmin();
        return Result.ok(assignmentService.create(request, operator));
    }

    @PutMapping("/{assignmentId}")
    public Result<AssignmentDtos.AssignmentDetail> update(@PathVariable Long assignmentId,
                                                          @RequestBody AssignmentDtos.AssignmentSaveRequest request) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.update(assignmentId, request));
    }

    @PutMapping("/{assignmentId}/status")
    public Result<AssignmentDtos.AssignmentDetail> updateStatus(@PathVariable Long assignmentId,
                                                                @RequestBody Map<String, String> payload) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.updateStatus(assignmentId, payload == null ? null : payload.get("status")));
    }

    @PostMapping("/{assignmentId}/materials")
    public Result<AssignmentDtos.MaterialDetail> uploadMaterial(@PathVariable Long assignmentId,
                                                                @RequestParam("materialType") String materialType,
                                                                @RequestParam(required = false) String title,
                                                                @RequestParam("file") MultipartFile file) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.uploadMaterial(assignmentId, materialType, title, file));
    }

    @DeleteMapping("/materials/{materialId}")
    public Result<Void> deleteMaterial(@PathVariable Long materialId) {
        accessService.requireAdmin();
        assignmentService.deleteMaterial(materialId);
        return Result.ok(null);
    }

    @GetMapping("/{assignmentId}/submissions")
    public Result<PageResponse<AssignmentDtos.SubmissionSummary>> submissions(@PathVariable Long assignmentId,
                                                                              @RequestParam(required = false) String status,
                                                                              @RequestParam(defaultValue = "1") long page,
                                                                              @RequestParam(defaultValue = "10") long pageSize) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.pageSubmissions(assignmentId, status, normalizePage(page), normalizePageSize(pageSize)));
    }

    @GetMapping("/{assignmentId}/submissions/files/download")
    public ResponseEntity<StreamingResponseBody> downloadSubmissionFiles(@PathVariable Long assignmentId,
                                                                         @RequestParam(required = false) String status) {
        accessService.requireAdmin();
        assignmentService.assertSubmissionFilesZipDownloadable(assignmentId, status);
        StreamingResponseBody body = outputStream -> assignmentService.writeSubmissionFilesZip(assignmentId, status, outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(assignmentService.submissionFilesZipName(assignmentId)))
                .body(body);
    }

    @GetMapping("/{assignmentId}/submissions/files/download-info")
    public Result<AssignmentDtos.BatchDownloadInfo> submissionFilesDownloadInfo(@PathVariable Long assignmentId,
                                                                                @RequestParam(required = false) String status) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.getSubmissionFilesBatchDownloadInfo(assignmentId, status));
    }

    @GetMapping("/submissions/{submissionId}")
    public Result<AssignmentDtos.SubmissionDetail> submission(@PathVariable Long submissionId) {
        accessService.requireAdmin();
        return Result.ok(assignmentService.getAdminSubmission(submissionId));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public Result<AssignmentDtos.SubmissionDetail> grade(@PathVariable Long submissionId,
                                                         @RequestBody AssignmentDtos.GradeRequest request) {
        User operator = accessService.requireAdmin();
        return Result.ok(assignmentService.grade(submissionId, request, operator));
    }

    @PostMapping("/submissions/{submissionId}/return")
    public Result<AssignmentDtos.SubmissionDetail> returnSubmission(@PathVariable Long submissionId,
                                                                    @RequestBody AssignmentDtos.GradeRequest request) {
        User operator = accessService.requireAdmin();
        return Result.ok(assignmentService.returnSubmission(submissionId, request, operator));
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        User user = accessService.requireAdmin();
        AssignmentSubmissionFile file = assignmentService.getFileForDownload(fileId, user, true);
        Resource resource = assignmentService.loadFile(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(file.getOriginalName()))
                .body(resource);
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long materialId) {
        User user = accessService.requireAdmin();
        AssignmentMaterial material = assignmentService.getMaterialForDownload(materialId, user, true);
        Resource resource = assignmentService.loadMaterial(material);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(material.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(material.getOriginalName()))
                .body(resource);
    }

    private long normalizePage(long page) {
        return page < 1 ? 1 : page;
    }

    private long normalizePageSize(long pageSize) {
        if (pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private String contentDisposition(String fileName) {
        try {
            return "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return "attachment";
        }
    }

    private MediaType resolveMediaType(String mimeType) {
        try {
            return mimeType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
