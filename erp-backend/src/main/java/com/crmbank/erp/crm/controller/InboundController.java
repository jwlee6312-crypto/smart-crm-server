package com.crmbank.erp.crm.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crmbank.erp.crm.dto.*;
import com.crmbank.erp.comm.dto.*;
import com.crmbank.erp.crm.service.*;
import com.crmbank.erp.crm.mapper.inbound.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/crm/inbound")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService inboundService;
    private final GeminiAiService geminiAiService;
    private final InboundMapper inboundMapper;

    @Value("${asterisk.sounds.path}")
    private String soundsPath;

    @Value("${asterisk.recording.path}")
    private String recordingPath;

    private File findRecordingFile(File baseDir, String fileName) {
        if (!baseDir.exists() || !baseDir.isDirectory()) return null;
        File target = new File(baseDir, fileName);
        if (target.exists() && target.isFile()) return target;

        File[] children = baseDir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    File found = findRecordingFile(child, fileName);
                    if (found != null && found.exists()) return found;
                }
            }
        }
        return null;
    }

    private void collectWavFiles(File dir, List<File> fileList) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    collectWavFiles(f, fileList);
                } else if (f.isFile() && f.getName().endsWith(".wav")) {
                    fileList.add(f);
                }
            }
        }
    }

    @GetMapping("/play-recording")
    public void playRecording(@RequestParam(value = "file", required = false) String file, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        log.info("🔥 [PLAY-RECORDING 진입 성공!!!] 요청된 파일 파라미터 file = [{}]", file);
        if (file == null || file.trim().isEmpty()) {
            log.warn("⚠️ [PLAY-RECORDING] file 파라미터가 비어있음");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        String fileNameOnly = new File(file.contains("?") ? file.substring(0, file.indexOf("?")) : file).getName();
        File targetFile = new File(soundsPath, fileNameOnly);
        log.info("🎙️ [PLAY-RECORDING] 1차 soundsPath 탐색: {}, 존재여부: {}", targetFile.getAbsolutePath(), targetFile.exists());
        
        if (!targetFile.exists()) {
            log.info("🎙️ [PLAY-RECORDING] 2차 recordings(기본경로) 재귀 탐색 시작 (baseDir: {}, fileName: {})", recordingPath, fileNameOnly);
            targetFile = findRecordingFile(new File(recordingPath), fileNameOnly);
            if (targetFile != null) {
                log.info("🎙️ [PLAY-RECORDING] 재귀 탐색 결과 발견: {}, 존재여부: {}, 크기: {} bytes", targetFile.getAbsolutePath(), targetFile.exists(), targetFile.length());
            } else {
                log.warn("⚠️ [PLAY-RECORDING] 재귀 탐색 결과 null");
            }
            if (targetFile == null || !targetFile.exists()) {
                targetFile = new File(recordingPath, file);
                log.info("🎙️ [PLAY-RECORDING] 3차 직접결합 경로 탐색: {}, 존재여부: {}", targetFile.getAbsolutePath(), targetFile.exists());
            }
        }

        if (targetFile == null || !targetFile.exists()) {
            log.error("❌ [PLAY-RECORDING 음원 파일 최종 없음] 탐색된 최종 경로: {}", targetFile != null ? targetFile.getAbsolutePath() : "null");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        log.info("✅ [PLAY-RECORDING 파일 전송 시작] 경로: {}, 크기: {} bytes", targetFile.getAbsolutePath(), targetFile.length());
        response.reset();
        response.setContentType("audio/wav");
        response.setCharacterEncoding(null);
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentLength((int) targetFile.length());

        try (java.io.InputStream in = new java.io.FileInputStream(targetFile);
             jakarta.servlet.ServletOutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> save(@RequestBody SaveRequest request, HttpSession session) {
        try {
            UserSession user = (UserSession) session.getAttribute("user_session");
            String cmpycd = user != null ? user.getCmpycd() : "";
            String userid = user != null ? user.getUserid() : "system";
            CallMstDto dto = request.getDto();
            dto.setCmpycd(cmpycd); dto.setConsultid(userid); dto.setUpdemp(userid);
            
            LocalDateTime now = LocalDateTime.now();
            dto.setEnd_time(now);
            String svcymd = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            String svcno = inboundService.saveCallMst(dto, request.getRecordings(), svcymd, user != null ? user.getDeptcd() : "");
            return ResponseEntity.ok(Map.of("success", true, "svcno", svcno));
        } catch (Exception e) { 
            return ResponseEntity.status(500).build(); 
        }
    }

    @PostMapping("/recording-list-pop")
    public ResponseEntity<List<Map<String, Object>>> getRecordingListPop(@RequestBody Map<String, Object> params) {
        File dir = new File(recordingPath);
        List<File> fileList = new ArrayList<>();
        collectWavFiles(dir, fileList);
        List<Map<String, Object>> result = new ArrayList<>();
        if (!fileList.isEmpty()) {
            fileList.sort(Comparator.comparingLong(File::lastModified).reversed());
            for (int i = 0; i < Math.min(fileList.size(), 20); i++) {
                File f = fileList.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("filename", f.getName());
                map.put("mtime", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(f.lastModified()), java.time.ZoneId.of("Asia/Seoul"))));
                result.add(map);
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analyze-audio")
    public ResponseEntity<Map<String, String>> analyzeAudio(@RequestParam(required = false) String file, HttpSession session) {
        if (file == null || file.isEmpty()) return ResponseEntity.notFound().build();
        UserSession user = (UserSession) session.getAttribute("user_session");
        String customerPhone = (user != null) ? user.getHpno() : "";
        String fileNameOnly = new File(file).getName();
        File target = findRecordingFile(new File(recordingPath), fileNameOnly);
        if (target == null || !target.exists()) target = new File(recordingPath, file);
        return ResponseEntity.ok(geminiAiService.analyzeAudio(target.getAbsolutePath(), customerPhone));
    }

    @GetMapping("/status-list")
    public List<Map<String, Object>> getStatusList(@RequestParam String fromdt, @RequestParam String todt, @RequestParam(required = false) String custnm, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        return inboundService.getStatusList(user != null ? user.getCmpycd() : "", fromdt, todt, custnm);
    }

    @GetMapping("/customer-detail")
    public Map<String, Object> getCustomerDetail(@RequestParam String custcd, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        return inboundService.getCustomerByCustCd(user != null ? user.getCmpycd() : "", custcd);
    }

    @Data public static class SaveRequest { private CallMstDto dto; private List<String> recordings; private String ai_mode; }

    // 1. 콜백 목록 조회 API
    @GetMapping("/callback-list")
    public ResponseEntity<?> getCallbackList(
            @RequestParam String fromdt,
            @RequestParam String todt,
            @RequestParam(required = false) String src_no,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", user != null ? user.getCmpycd() : "");
        params.put("fromdt", fromdt.replace("-", ""));
        params.put("todt", todt.replace("-", ""));
        params.put("src_no", src_no != null ? src_no : "");
        return ResponseEntity.ok(inboundMapper.selectCallbackList(params));
    }

    // 2. 콜백 응대 결과 저장 API
    @PostMapping("/interaction/save-response")
    public ResponseEntity<?> saveCallbackResponse(@RequestBody Map<String, Object> payload, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        payload.put("cmpycd", user != null ? user.getCmpycd() : "");
        payload.put("userid", user != null ? user.getUserid() : "system");
        inboundMapper.updateCallbackResult(payload);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/item-list")
    public ResponseEntity<List<Map<String, Object>>> getItemList(@RequestParam String custcd, HttpSession session) {
        try {
            UserSession user = (UserSession) session.getAttribute("user_session");
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", user != null ? user.getCmpycd() : "");
            params.put("custcd", custcd);
            List<Map<String, Object>> list = inboundMapper.selectItemList(params);
            return ResponseEntity.ok(list != null ? list : Collections.emptyList());
        } catch (Exception e) {
            log.warn("⚠️ [Inbound] getItemList 예외 발생 (빈 리스트 반환): {}", e.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/call-history")
    public ResponseEntity<List<Map<String, Object>>> getCallHistory(@RequestParam String custcd, HttpSession session) {
        try {
            UserSession user = (UserSession) session.getAttribute("user_session");
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", user != null ? user.getCmpycd() : "");
            params.put("custcd", custcd);
            List<Map<String, Object>> list = inboundMapper.selectCallHistory(params);
            return ResponseEntity.ok(list != null ? list : Collections.emptyList());
        } catch (Exception e) {
            log.warn("⚠️ [Inbound] getCallHistory 예외 발생 (빈 리스트 반환): {}", e.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}
