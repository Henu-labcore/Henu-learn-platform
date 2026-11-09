package com.hwz.user.service;

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
import java.nio.file.StandardCopyOption;

@Service
public class LearningTemplateStorageService {

    private final Path storageRoot;

    public LearningTemplateStorageService(
            @Value("${labcore.learning.template-storage-dir:runtime/learning-templates}") String storageDir
    ) {
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    public String storeTemplate(Long itemId, MultipartFile file) throws IOException {
        if (itemId == null || itemId <= 0) {
            throw new IOException("Invalid learning item id");
        }
        if (file == null || file.isEmpty()) {
            throw new IOException("Template notebook is required");
        }
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName) || !originalName.toLowerCase().endsWith(".ipynb")) {
            throw new IOException("Only .ipynb notebook templates are supported");
        }

        Path targetDir = storageRoot.resolve("item-" + itemId);
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve("template.ipynb");
        Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        return storageRoot.relativize(targetFile).toString().replace('\\', '/');
    }

    public boolean templateExists(String templatePath) {
        if (!StringUtils.hasText(templatePath)) {
            return false;
        }
        return Files.exists(resolvePath(templatePath));
    }

    public Resource loadTemplate(String templatePath) {
        return new FileSystemResource(resolvePath(templatePath));
    }

    private Path resolvePath(String templatePath) {
        if (!StringUtils.hasText(templatePath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\u6a21\u677f\u8def\u5f84\u4e0d\u80fd\u4e3a\u7a7a");
        }
        Path relativePath = Paths.get(templatePath);
        if (relativePath.isAbsolute()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\u6a21\u677f\u8def\u5f84\u4e0d\u5408\u6cd5");
        }
        Path resolvedPath = storageRoot.resolve(relativePath).normalize();
        if (!resolvedPath.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\u6a21\u677f\u8def\u5f84\u4e0d\u5408\u6cd5");
        }
        return resolvedPath;
    }
}
