package com.crmbank.erp.crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.crmbank.erp.crm.dto.CallMonitorDto;
import com.crmbank.erp.crm.dto.CallMstDto;
import com.crmbank.erp.crm.dto.CtiEscalationDto;
import com.crmbank.erp.crm.dto.TotalCallLogDto;
import com.crmbank.erp.crm.mapper.inbound.InboundMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundService {

    private final InboundMapper inboundMapper;
    private final GeminiAiService geminiAiService;

    @Value("${asterisk.recording.path:/var/spool/asterisk/monitor}")
    private String recordingPath;

    /**
     * 🤖 [AI 비동기 처리] 통화 종료 후 백그라운드에서 요약 및 업데이트
     */
    @Async
    @Transactional
    public void processAiSummaryAsync(String cmpycd, String svcno, String fullPath, String updemp, String customerPhone) {
        try {
            // 🚀 [해결 3] 파일이 완전히 저장될 때까지 5초간 넉넉히 대기 (오류 방지)
            Thread.sleep(5000); 
            log.info("🤖 [AI Async] 분석 시작: {} (File: {})", svcno, fullPath);
            
            java.io.File file = new java.io.File(fullPath);
            if (!file.exists() || file.length() < 100) {
                log.warn("⚠️ [AI] 분석할 파일이 아직 준비되지 않음: {}", fullPath);
                return;
            }
            Map<String, String> aiResult = geminiAiService.analyzeAudio(fullPath, customerPhone);

            CallMstDto updateDto = new CallMstDto();
            updateDto.setCmpycd(cmpycd);
            updateDto.setSvcno(svcno);
            updateDto.setTrb_ment(aiResult.get("trb_ment")); // 🚀 고객 요청 분리 저장
            updateDto.setAns_ment(aiResult.get("ans_ment")); // 🚀 상담원 답변 분리 저장
            updateDto.setAi_summary(aiResult.get("summary"));
            updateDto.setUpdemp(updemp);

            inboundMapper.updateCallMstAiResult(updateDto);
            log.info("✅ [AI Async] 분석 완료 및 반영 성공: {}", svcno);
        } catch (Exception e) {
            log.error("❌ [AI Async] 분석 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 💡 MyBatis 결과를 소문자 Key로 변환 (프론트엔드 호환성)
     */
    private List<Map<String, Object>> toLowerCase(List<Map<String, Object>> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (list == null) return result;

        for (Map<String, Object> map : list) {
            if (map == null) continue;
            Map<String, Object> lowerMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    lowerMap.put(entry.getKey().toLowerCase(), entry.getValue());
                }
            }
            result.add(lowerMap);
        }
        return result;
    }

    public Map<String, Object> getCustomerByCustCd(String cmpycd, String custcd) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("custcd", custcd);
        return inboundMapper.findCustomerByCustCd(params);
    }

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

    /**
     * 💡 인바운드 상담 통합 저장 (소문자 표준화 적용)
     */
    @Transactional
    public String saveCallMst(CallMstDto dto, List<String> recordings, String svcymd, String agentDeptCd) {
        // 💡 [교정] 하드코딩된 회사코드 제거 및 소문자 표준화 적용
        if (dto.getCmpycd() == null || dto.getCmpycd().isEmpty()) {
            dto.setCmpycd("");
        }
        dto.setHappycall_yn("n");

        if (dto.getCall_telno() != null) dto.setCall_telno(dto.getCall_telno().trim());
        if (dto.getCall_usernm() != null) dto.setCall_usernm(dto.getCall_usernm().trim());
        if (dto.getCall_email() != null) dto.setCall_email(dto.getCall_email().trim());
        if (dto.getIono() != null) dto.setIono(dto.getIono().trim());

        if (dto.getStart_time() == null) dto.setStart_time(LocalDateTime.now().minusMinutes(5));
        if (dto.getEnd_time() == null) dto.setEnd_time(LocalDateTime.now());

        String targetDeptCd = dto.getDeptcd();
        dto.setDeptcd(agentDeptCd);

        // 1. 접수번호 채번
        boolean isNew = false;
        if (dto.getSvcno() == null || dto.getSvcno().trim().isEmpty()) {
            isNew = true;
            dto.setSvcymd(svcymd);
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", dto.getCmpycd());
            params.put("svcymd", svcymd);
            dto.setSvcno(inboundMapper.generateSvcNo(params));
        }

        // 🚀 recordings 전달값 또는 linkedid를 기반으로 녹취 파일명을 최우선 확보하여 call_mst_tbl에 100% 반영 보장
        if (dto.getRec_file() == null || dto.getRec_file().trim().isEmpty() || "null".equalsIgnoreCase(dto.getRec_file())) {
            if (recordings != null && !recordings.isEmpty() && recordings.get(0) != null && !recordings.get(0).trim().isEmpty()) {
                dto.setRec_file(recordings.get(0));
            } else if (dto.getLinkedid() != null && !dto.getLinkedid().trim().isEmpty() && !dto.getLinkedid().equals("MANUAL")) {
                File recordingsRoot = new File(recordingPath);
                File found = findRecordingFile(recordingsRoot, dto.getLinkedid() + ".wav");
                if (found != null && found.exists()) {
                    dto.setRec_file(found.getName());
                }
            }
        }

        // 2. 상담 마스터 저장
        if (isNew) {
            inboundMapper.insertCallMst(dto);
        } else {
            inboundMapper.updateCallMst(dto);
        }

        // 3. 타 부서 이관 처리
        if ("y".equalsIgnoreCase(dto.getEscalation_yn()) && dto.getEsc_memo() != null && !dto.getEsc_memo().trim().isEmpty()) {
            CtiEscalationDto escDto = CtiEscalationDto.builder()
                    .svcno(dto.getSvcno())
                    .linkedid(dto.getLinkedid())
                    .sender_id(dto.getConsultid())
                    .deptcd(targetDeptCd)
                    .summary(dto.getAi_summary())
                    .esc_memo(dto.getEsc_memo())
                    .build();

            inboundMapper.insertEscalation(escDto);

            if (escDto.getEscalation_no() != null) {
                dto.setEscalation_no(escDto.getEscalation_no().toString());
                inboundMapper.updateCallMst(dto);
            }
        }

        // 4. 녹취 파일 정보 저장
        if (recordings != null && !recordings.isEmpty()) {
            Map<String, Object> delParams = new HashMap<>();
            delParams.put("cmpycd", dto.getCmpycd());
            delParams.put("svcno", dto.getSvcno());
            inboundMapper.deleteCallMonitor(delParams);

            for (int i = 0; i < recordings.size(); i++) {
                inboundMapper.insertCallMonitor(CallMonitorDto.builder()
                        .cmpycd(dto.getCmpycd())
                        .svcno(dto.getSvcno())
                        .row(String.format("%03d", i + 1))
                        .moniternm(recordings.get(i))
                        .updemp(dto.getConsultid())
                        .build());
            }
        }

        // 5. 통합 인터랙션 로그
        String interactionId = dto.getInteraction_id();
        if (interactionId == null || interactionId.isEmpty()) {
            interactionId = "manual_" + UUID.randomUUID().toString().substring(0, 8);
            dto.setInteraction_id(interactionId);
        }

        String srcNo = dto.getCall_telno() != null && !dto.getCall_telno().trim().isEmpty() ? dto.getCall_telno() : dto.getCustcd();

        TotalCallLogDto logDto = TotalCallLogDto.builder()
                .uniqueid(interactionId)
                .cmpycd(dto.getCmpycd())
                .media_type("call")
                .linkedid(dto.getLinkedid())
                .direction("in")
                .src_no(srcNo)
                .dst_no(dto.getConsultid())
                .keyword(srcNo)
                .start_time(dto.getStart_time())
                .result_cd("100")
                .rec_file(dto.getRec_file())
                .build();

        inboundMapper.insertTotalInteractionLog(logDto);

        return dto.getSvcno();
    }

    /**
     * 💡 ARS 콜백 로그 저장
     */
    @Transactional
    public void insertTotalInteractionLog(TotalCallLogDto logDto) {
        inboundMapper.insertTotalInteractionLog(logDto);
    }

    public List<Map<String, Object>> getStatusList(String cmpycd, String fromdt, String todt, String custnm) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("fromdt", fromdt);
        params.put("todt", todt);
        params.put("custnm", custnm);
        return toLowerCase(inboundMapper.selectStatusList(params));
    }

    public List<Map<String, Object>> getItemList(String cmpycd, String custcd) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("custcd", custcd);
        return toLowerCase(inboundMapper.selectItemList(params));
    }

    public List<Map<String, Object>> getCallHistory(String cmpycd, String custcd) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("custcd", custcd);
        return toLowerCase(inboundMapper.selectCallHistory(params));
    }

    public List<Map<String, Object>> getServiceHistory(String cmpycd, String custcd) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("custcd", custcd);
        return toLowerCase(inboundMapper.selectServiceHistory(params));
    }

    public List<Map<String, Object>> getSettleHistory(String cmpycd, String custcd) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("custcd", custcd);
        return toLowerCase(inboundMapper.selectSettleHistory(params));
    }
}
