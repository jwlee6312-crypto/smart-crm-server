package com.crmbank.erp.asterisk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crmbank.erp.asterisk.mapper.AsteriskMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsteriskAdminService {

    private final AsteriskMapper asteriskMapper;

    @Transactional
    public void savePjsip(List<Map<String, Object>> list) {
        for (Map<String, Object> data : list) {
            asteriskMapper.insertPjsipAuth(data);
            asteriskMapper.insertPjsipAor(data);
            asteriskMapper.insertPjsipEndpoint(data);
        }
    }

    @Transactional
    public void saveQueues(List<Map<String, Object>> list) {
        for (Map<String, Object> data : list) {
            asteriskMapper.upsertQueue(data);
        }
    }

    @Transactional
    public void saveQueueMembers(String queueName, List<Map<String, Object>> members) {
        asteriskMapper.deleteQueueMembers(queueName);
        for (Map<String, Object> member : members) {
            member.put("queue_name", queueName);
            asteriskMapper.insertQueueMember(member);
        }
    }

    @Transactional
    public void saveExtensions(List<Map<String, Object>> list) {
        for (Map<String, Object> data : list) {
            asteriskMapper.deleteExtensions(data);
            asteriskMapper.insertExtension(data);
        }
    }

    @Transactional
    public void saveArsScripts(List<Map<String, Object>> list, String userId) {
        for (Map<String, Object> data : list) {
            data.put("upd_user", userId);
            asteriskMapper.upsertArsScript(data);
            
            String scriptId = String.valueOf(data.get("id"));
            String text = String.valueOf(data.get("script_text"));
            generateTtsFile(scriptId, text);
        }
    }

    @Value("${asterisk.tts.script.path:/app/scripts/generate_tts.py}")
    private String ttsScriptPath;

    private void generateTtsFile(String scriptId, String text) {
        try {
            log.info("🎙️ [TTS 생성 시작] scriptId: {}, text: {}, path: {}", scriptId, text, ttsScriptPath);
            ProcessBuilder pb = new ProcessBuilder("python3", ttsScriptPath, scriptId, text);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("🎙️ [Python TTS Output] {}", line);
            }
            
            int exitCode = process.waitFor();
            log.info("🎙️ [TTS 생성 완료] exitCode: {}", exitCode);
        } catch (Exception e) {
            log.error("❌ [TTS 생성 실패]: {}", e.getMessage(), e);
        }
    }

    @Value("${backend.internal.url:http://erp-backend:8080}")
    private String backendInternalUrl;

    public List<Map<String, Object>> getStandardIvrTemplate() {
        List<Map<String, Object>> template = new ArrayList<>();
        addExten(template, "from-internal", "999", 1, "NoOp", "### IVR Entry ###");
        addExten(template, "from-internal", "999", 2, "Answer", "");
        addExten(template, "from-internal", "999", 3, "Goto", "ivr-main,s,1");
        return template;
    }

    private void addExten(List<Map<String, Object>> list, String ctx, String ext, int pri, String app, String arg) {
        Map<String, Object> row = new HashMap<>();
        row.put("context", ctx);
        row.put("exten", ext);
        row.put("priority", pri);
        row.put("app", app);
        row.put("appdata", arg);
        list.add(row);
    }
}
