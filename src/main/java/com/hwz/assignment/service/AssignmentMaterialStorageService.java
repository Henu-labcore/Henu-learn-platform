package com.hwz.assignment.service;

import com.hwz.assignment.entity.AssignmentMaterial;
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
public class AssignmentMaterialStorageService {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    ));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mov", "webm"
    ));
    private static final Set<String> DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "doc", "docx", "pdf", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "rar"
    ));

    private final Path storageRoot;

    public AssignmentMaterialStorageService(@Value("${labcore.assignment.material-dir:runtime/assignment-materials}") String storageDir) {
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    public AssignmentMaterial store(Long assignmentId, String materialType, String title, int sortOrder, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的材料");
        }
        String normalizedType = normalizeMaterialType(materialType);
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "material" : file.getOriginalFilename());
        String extension = extensionOf(originalName);
        validateExtension(normalizedType, extension);

        try {
            Path targetDir = storageRoot.resolve("assignment-" + assignmentId).normalize();
            if (!targetDir.startsWith(storageRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件路径不合法");
            }
            Files.createDirectories(targetDir);
            String storedName = normalizedType + "-" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
            Path targetFile = targetDir.resolve(storedName).normalize();
            file.transferTo(targetFile.toFile());

            return AssignmentMaterial.builder()
                    .assignmentId(assignmentId)
                    .materialType(normalizedType)
                    .title(StringUtils.hasText(title) ? title.trim() : originalName)
                    .originalName(originalName)
                    .storedPath(storageRoot.relativize(targetFile).toString().replace('\\', '/'))
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .sortOrder(sortOrder)
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "材料保存失败，请稍后重试");
        }
    }

    public Resource load(AssignmentMaterial material) {
        Path path = storageRoot.resolve(material.getStoredPath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "材料文件不存在");
        }
        return new FileSystemResource(path);
    }

    public void delete(AssignmentMaterial material) {
        if (material == null || !StringUtils.hasText(material.getStoredPath())) {
            return;
        }
        Path path = storageRoot.resolve(material.getStoredPath()).normalize();
        if (!path.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件路径不合法");
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "材料删除失败，请稍后重试");
        }
    }

    private String normalizeMaterialType(String materialType) {
        String normalized = StringUtils.hasText(materialType) ? materialType.trim().toUpperCase(Locale.ROOT) : "";
        if (!"IMAGE".equals(normalized) && !"VIDEO".equals(normalized) && !"DOCUMENT".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "材料类型必须是图片、视频或文档");
        }
        return normalized;
    }

    private void validateExtension(String materialType, String extension) {
        Set<String> allowed;
        if ("IMAGE".equals(materialType)) {
            allowed = IMAGE_EXTENSIONS;
        } else if ("VIDEO".equals(materialType)) {
            allowed = VIDEO_EXTENSIONS;
        } else {
            allowed = DOCUMENT_EXTENSIONS;
        }
        if (!allowed.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持该材料类型");
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
