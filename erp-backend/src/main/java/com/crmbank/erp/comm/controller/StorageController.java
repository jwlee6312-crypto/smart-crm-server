package com.crmbank.erp.comm.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;

@Slf4j
@RestController
public class StorageController {

    private final String storageDirectory = new File("/app/storage").exists() ? "/app/storage" : (System.getProperty("user.dir") + "/storage");

    @GetMapping({"/storage/**", "/api/storage/**"})
    public ResponseEntity<Resource> serveStorageFile(HttpServletRequest request) {
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        // URL에서 Prefix 제거
        String subPath = fullPath.replaceFirst("^/api", "").replaceFirst("^/storage", "");
        if (subPath.startsWith("/")) {
            subPath = subPath.substring(1);
        }

        File file = new File(storageDirectory, subPath);
        if (!file.exists()) {
            String[] parts = subPath.split("/", 2);
            if (parts.length > 0) {
                String upperCmpycd = parts[0].toUpperCase();
                String rest = parts.length > 1 ? parts[1] : "";
                file = new File(storageDirectory, upperCmpycd + "/" + rest);
            }
        }
        log.info("🎯 [StorageController 가동] 요청 URI: {}, 실제 타겟 경로: {}, 존재 여부: {}", fullPath, file.getAbsolutePath(), file.exists());

        if (!file.exists() || file.isDirectory()) {
            return ResponseEntity.notFound().build();
        }

        MediaType contentType = MediaTypeFactory.getMediaType(file.getName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(new FileSystemResource(file));
    }
}
