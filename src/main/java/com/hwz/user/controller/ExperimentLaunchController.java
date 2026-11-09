package com.hwz.user.controller;

import com.hwz.user.dto.ExperimentLaunchResponse;
import com.hwz.admin.service.LearningContentAdminService;
import com.hwz.common.Result;
import com.hwz.user.entity.LearningItem;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/experiment")
public class ExperimentLaunchController {

    private static final MediaType NOTEBOOK_MEDIA_TYPE = MediaType.parseMediaType("application/x-ipynb+json");

    private final LearningContentAdminService learningContentAdminService;

    public ExperimentLaunchController(LearningContentAdminService learningContentAdminService) {
        this.learningContentAdminService = learningContentAdminService;
    }

    @GetMapping("/{id}/launch")
    public Result<ExperimentLaunchResponse> launch(@PathVariable Long id) {
        LearningItem item = learningContentAdminService.getItemOrThrow(id);
        if (!LearningContentAdminService.STATUS_PUBLISHED.equalsIgnoreCase(item.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment notebook not found");
        }
        String notebookFile = resolveNotebookFile(item);
        String workspaceId = resolveWorkspaceId(id);
        String notebookUrl = UriComponentsBuilder.fromPath("/api/experiment/")
                .path(String.valueOf(id))
                .path("/files/")
                .path(notebookFile)
                .build()
                .toUriString();
        String launchUrl = UriComponentsBuilder.fromPath("/lite/lab/index.html")
                .queryParam("workspace", workspaceId)
                .queryParam("experimentFile", notebookFile)
                .queryParam("templateUrl", notebookUrl)
                .queryParam("templateVersion", resolveTemplateVersion(item))
                .queryParam("itemPk", id)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        return Result.ok(new ExperimentLaunchResponse(launchUrl));
    }

    @GetMapping("/{id}/files/{filename:.+\\.ipynb}")
    public ResponseEntity<Resource> notebook(@PathVariable Long id, @PathVariable String filename) {
        LearningItem item = learningContentAdminService.getItemOrThrow(id);
        if (!LearningContentAdminService.STATUS_PUBLISHED.equalsIgnoreCase(item.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment notebook not found");
        }
        String notebookFile = resolveNotebookFile(item);
        if (!notebookFile.equals(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment notebook not found: " + filename);
        }
        Resource resource = learningContentAdminService.resolveTemplateResource(item);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(NOTEBOOK_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + notebookFile + "\"")
                .body(resource);
    }

    private String resolveNotebookFile(LearningItem item) {
        if (item == null || item.getItemPk() == null || item.getItemPk() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid experiment id");
        }
        if (!learningContentAdminService.hasTemplate(item)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment notebook not found");
        }
        return "exp" + item.getItemPk() + ".ipynb";
    }

    private String resolveWorkspaceId(Long id) {
        return "experiment-" + id;
    }

    private String resolveTemplateVersion(LearningItem item) {
        if (item.getUpdatedAt() != null) {
            return item.getUpdatedAt().toString();
        }
        if (item.getPublishedAt() != null) {
            return item.getPublishedAt().toString();
        }
        return String.valueOf(item.getItemPk());
    }
}
