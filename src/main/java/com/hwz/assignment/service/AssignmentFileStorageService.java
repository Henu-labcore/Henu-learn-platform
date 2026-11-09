package com.hwz.assignment.service;

import com.hwz.assignment.entity.AssignmentSubmissionFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AssignmentFileStorageService {

    private static final Set<String> DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "doc", "docx", "pdf", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "rar"
    ));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mov", "avi", "mkv", "webm"
    ));

    private final Path storageRoot;

    public AssignmentFileStorageService(@Value("${labcore.assignment.upload-dir:runtime/assignment-submissions}") String storageDir) {
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    public AssignmentSubmissionFile store(Long assignmentId, Long studentId, Long submissionId,
                                          String fileType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的文件");
        }
        String normalizedType = normalizeFileType(fileType);
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
        String extension = extensionOf(originalName);
        validateExtension(normalizedType, extension);

        try {
            Path targetDir = storageRoot
                    .resolve("assignment-" + assignmentId)
                    .resolve("student-" + studentId)
                    .resolve("submission-" + submissionId)
                    .normalize();
            if (!targetDir.startsWith(storageRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件路径不合法");
            }
            Files.createDirectories(targetDir);
            String storedName = normalizedType + "-" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
            Path targetFile = targetDir.resolve(storedName).normalize();
            file.transferTo(targetFile.toFile());

            String storedPath = storageRoot.relativize(targetFile).toString().replace('\\', '/');
            return AssignmentSubmissionFile.builder()
                    .submissionId(submissionId)
                    .fileType(normalizedType)
                    .originalName(originalName)
                    .storedPath(storedPath)
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败，请稍后重试");
        }
    }

    public Resource load(AssignmentSubmissionFile file) {
        Path path = storageRoot.resolve(file.getStoredPath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        return new FileSystemResource(path);
    }

    public void delete(AssignmentSubmissionFile file) {
        if (file == null || !StringUtils.hasText(file.getStoredPath())) {
            return;
        }
        Path path = storageRoot.resolve(file.getStoredPath()).normalize();
        if (!path.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件路径不合法");
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件删除失败，请稍后重试");
        }
    }

    private String normalizeFileType(String fileType) {
        String normalized = StringUtils.hasText(fileType) ? fileType.trim().toUpperCase(Locale.ROOT) : "";
        if (!"DOCUMENT".equals(normalized) && !"VIDEO".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件类型必须是文档或视频");
        }
        return normalized;
    }

    private void validateExtension(String fileType, String extension) {
        Set<String> allowed = "VIDEO".equals(fileType) ? VIDEO_EXTENSIONS : DOCUMENT_EXTENSIONS;
        if (!allowed.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持该文件类型");
        }
    }

    private String extensionOf(String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == name.length() - 1) {
            return "";
        }
        return name.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
