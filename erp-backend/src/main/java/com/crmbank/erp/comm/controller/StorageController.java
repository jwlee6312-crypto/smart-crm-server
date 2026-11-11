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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
public class StorageController {

    @GetMapping({"/storage/**", "/api/storage/**"})
    public ResponseEntity<Resource> serveStorageFile(HttpServletRequest request) {
        String uri = request.getRequestURI(); // 예: /api/storage/COIT/profile/... 또는 /storage/...
        log.info("🎯 [StorageController 진입] Request URI: {}", uri);

        // /api 및 /storage 프리픽스 제거하여 상대 경로 추출
        String cleanPath = uri.replaceFirst("^/api", "").replaceFirst("^/storage", "");
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        // 스토리지 루트 경로 결정 (도커 /app/storage 우선, 없으면 로컬 ./storage)
        Path rootPath = Paths.get("/app/storage");
        if (!java.nio.file.Files.exists(rootPath)) {
            rootPath = Paths.get(System.getProperty("user.dir"), "storage");
        }

        Path filePath = rootPath.resolve(cleanPath).normalize();
        
        // 보안 체크 (Path Traversal 방지)
        if (!filePath.startsWith(rootPath)) {
            return ResponseEntity.badRequest().build();
        }

        File file = filePath.toFile();

        // 대소문자 폴더명 폴백 (예: coit -> COIT)
        if (!file.exists() || file.isDirectory()) {
            String[] parts = cleanPath.split("/", 2);
            if (parts.length > 0) {
                String upperCmpycd = parts[0].toUpperCase();
                String rest = parts.length > 1 ? parts[1] : "";
                file = rootPath.resolve(upperCmpycd + "/" + rest).toFile();
            }
        }

        log.info("🎯 [StorageController 타겟] 절대 경로: {}, 존재 여부: {}, 읽기 가능: {}", 
                file.getAbsolutePath(), file.exists(), file.canRead());

        if (!file.exists() || file.isDirectory()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new FileSystemResource(file);
            MediaType contentType = MediaTypeFactory.getMediaType(file.getName())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(resource);
        } catch (Exception e) {
            log.error("❌ [StorageController] 파일 제공 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
