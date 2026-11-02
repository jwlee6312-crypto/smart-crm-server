package com.crmbank.erp.hsaa.service;

import com.crmbank.erp.hsaa.dto.*;
import com.crmbank.erp.hsaa.mapper.HsaaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HsaaService {

    private final HsaaMapper hsaaMapper;
    private final com.crmbank.erp.comm.service.FileStorageService fileStorageService;

    public List<SalesUserDto> getSalesUserList(String cmpycd) {
        return hsaaMapper.getSalesUserList(cmpycd);
    }

    public List<Hsaa600tDto> getSalesTargetList(String cmpycd, String yyyy, String userid) {
        return hsaaMapper.getSalesTargetList(cmpycd, yyyy, userid);
    }

    @Transactional
    public void saveSalesTargets(List<Hsaa600tDto> targets, String cmpycd, String userid, String deptcd) {
        String curYmd = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        for (Hsaa600tDto target : targets) {
            target.setCmpycd(cmpycd);
            target.setUserid(userid);
            target.setDeptcd(deptcd);
            target.setUpdemp(userid);
            target.setAddtime(curYmd);
            target.setUpdtime(curYmd);
            
            if (hsaaMapper.checkExists(target) > 0) {
                hsaaMapper.updateSalesTarget(target);
            } else {
                hsaaMapper.insertSalesTarget(target);
            }
        }
    }

    // --- HSAA100U 통합 관리 서비스 ---

    public List<Hsaa200tDto> getSalesMasterList(String cmpycd, String fromdt, String todt, String schcustnm, String userid) {
        return hsaaMapper.selectSalesMasterList(cmpycd, fromdt.replace("-", ""), todt.replace("-", ""), schcustnm, userid);
    }

    public Map<String, Object> getSalesDetail(String cmpycd, String salesid) {
        Map<String, Object> result = new HashMap<>();
        Hsaa200tDto masterReq = new Hsaa200tDto();
        masterReq.setCmpycd(cmpycd);
        masterReq.setSalesid(salesid);
        
        result.put("master", hsaaMapper.selectSalesMaster(masterReq));
        
        Hsaa810tDto itemReq = new Hsaa810tDto();
        itemReq.setCmpycd(cmpycd);
        itemReq.setSalesid(salesid);
        result.put("items", hsaaMapper.selectItemList(itemReq));
        
        return result;
    }

    @Transactional
    public String saveSalesMaster(Hsaa200tDto master, List<Hsaa810tDto> items) {
        String curYmd = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        
        // 모든 일자 정보 8자리(YYYYMMDD)로 보정
        master.setAddtime(cleanDate(master.getAddtime()));
        master.setForedt(cleanDate(master.getForedt()));
        master.setForedelivdt(cleanDate(master.getForedelivdt()));
        master.setLastmtdt(cleanDate(master.getLastmtdt()));
        master.setRealdt(cleanDate(master.getRealdt()));
        master.setFaildt(cleanDate(master.getFaildt()));
        master.setHoldondt(cleanDate(master.getHoldondt()));
        master.setReportdt(cleanDate(master.getReportdt()));
        
        // 시스템 날짜 설정
        if (master.getAddtime() == null || master.getAddtime().isEmpty()) master.setAddtime(curYmd);
        if (master.getLastmtdt() == null || master.getLastmtdt().isEmpty()) {
            master.setLastmtdt(master.getAddtime());
        }
        master.setUpdtime(curYmd);

        // 데이터 길이 초과 방지를 위한 안전 처리
        if (master.getSalestitle() != null && master.getSalestitle().length() > 100) {
            master.setSalestitle(master.getSalestitle().substring(0, 100));
        }

        if (master.getSalesid() == null || master.getSalesid().isEmpty()) {
            String yymm = (master.getAddtime() != null && master.getAddtime().length() >= 6) ? master.getAddtime().substring(0, 6) : new java.text.SimpleDateFormat("yyyyMM").format(new java.util.Date());
            String newId = hsaaMapper.generateSalesId(master.getCmpycd(), yymm);
            master.setSalesid(newId);
            hsaaMapper.insertSalesMaster(master);

            // 신규 등록 시 초기 단계(100) 자동 생성
            Hsaa310tDto initialStage = new Hsaa310tDto();
            initialStage.setCmpycd(master.getCmpycd());
            initialStage.setCustcd(master.getCustcd());
            initialStage.setSalesid(newId);
            initialStage.setSer("001");
            initialStage.setChngdt(master.getAddtime());
            initialStage.setBfstate("100");
            initialStage.setState("100");
            initialStage.setRemark("영업건 신규 등록");
            initialStage.setAddtime(curYmd);
            initialStage.setUpdtime(curYmd);
            initialStage.setUpdemp(master.getUpdemp());
            hsaaMapper.insertStage(initialStage);
        } else {
            hsaaMapper.updateSalesMaster(master);
        }

        // 품목 정보 갱신 (전체 삭제 후 재등록)
        Hsaa810tDto itemDelReq = new Hsaa810tDto();
        itemDelReq.setCmpycd(master.getCmpycd());
        itemDelReq.setSalesid(master.getSalesid());
        hsaaMapper.deleteItemsBySalesId(itemDelReq);

        if (items != null) {
            int serIdx = 1;
            for (Hsaa810tDto item : items) {
                if (item.getItemnm() == null || item.getItemnm().isEmpty()) continue;
                item.setCmpycd(master.getCmpycd());
                item.setCustcd(master.getCustcd());
                item.setSalesid(master.getSalesid());
                item.setSer(String.format("%03d", serIdx++));
                item.setUpdemp(master.getUpdemp());
                hsaaMapper.insertItem(item);
            }
        }
        return master.getSalesid();
    }

    @Transactional
    public void deleteSalesMaster(Hsaa200tDto dto) {
        hsaaMapper.deleteSalesMaster(dto);
    }

    public List<Hsaa300tDto> getDiaryList(String cmpycd, String salesid) {
        Hsaa300tDto req = new Hsaa300tDto();
        req.setCmpycd(cmpycd);
        req.setSalesid(salesid);
        return hsaaMapper.selectDiaryList(req);
    }

    @Transactional
    public void saveDiary(Hsaa300tDto diary) {
        String curYmd = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        
        // 일자 정보 보정
        diary.setContdt(cleanDate(diary.getContdt()));
        diary.setChngdt(cleanDate(diary.getChngdt()));
        
        // STARTDATE가 필수인 경우 상담일자로 기본값 설정 (UI에서 제외되었으므로)
        if (diary.getStartdate() == null || diary.getStartdate().isEmpty()) {
            diary.setStartdate(diary.getContdt());
        } else {
            diary.setStartdate(cleanDate(diary.getStartdate()));
        }
        
        // DIARY_CONTENT 기본값 처리
        if (diary.getDiarycontent() == null) diary.setDiarycontent("");
        
        // 시스템 날짜 강제화
        diary.setAddtime(curYmd);
        diary.setUpdtime(curYmd);

        // Boolean 성격의 필드 처리 (truncation 방지)
        diary.setReportyn(parseYn(diary.getReportyn()));

        // 💡 만약 tostate가 없다면 마스터의 현재 상태를 가져와 저장
        if (diary.getTostate() == null || diary.getTostate().isEmpty()) {
            Hsaa200tDto masterReq = new Hsaa200tDto();
            masterReq.setCmpycd(diary.getCmpycd());
            masterReq.setSalesid(diary.getSalesid());
            Hsaa200tDto currentMaster = hsaaMapper.selectSalesMaster(masterReq);
            if (currentMaster != null) {
                diary.setTostate(currentMaster.getState());
            }
        }

        if (diary.getSer() == null || diary.getSer().isEmpty() || "0".equals(diary.getSer())) {
            diary.setSer(String.format("%03d", hsaaMapper.nextDiarySer(diary)));
            hsaaMapper.insertDiary(diary);
        } else {
            Hsaa300tDto oldDiary = hsaaMapper.selectDiary(diary);
            if (oldDiary != null && oldDiary.getNextMeetDt() != null && !oldDiary.getNextMeetDt().trim().isEmpty()) {
                String oldDt = cleanDate(oldDiary.getNextMeetDt());
                String newDt = cleanDate(diary.getNextMeetDt());
                if (newDt == null || !oldDt.equals(newDt)) {
                    Map<String, Object> delParams = new HashMap<>();
                    delParams.put("cmpycd", diary.getCmpycd());
                    delParams.put("userid", diary.getUserid());
                    delParams.put("salesid", diary.getSalesid());
                    delParams.put("plan_dt", oldDt);
                    hsaaMapper.deleteManualVisitPlan(delParams);
                }
            }
            hsaaMapper.updateDiary(diary);
        }

        // 2. 만약 next_meet_dt(다음 미팅 예정일)가 입력되었다면 hsaa_700t_tbl에 수동 등록(plan_source='M')으로 자동 Sync
        if (diary.getNextMeetDt() != null && !diary.getNextMeetDt().trim().isEmpty()) {
            Map<String, Object> manualPlan = new HashMap<>();
            manualPlan.put("cmpycd", diary.getCmpycd());
            manualPlan.put("userid", diary.getUserid());
            manualPlan.put("plan_dt", cleanDate(diary.getNextMeetDt()));
            manualPlan.put("salesid", diary.getSalesid());
            manualPlan.put("custcd", diary.getCustcd());

            Hsaa200tDto masterReq = new Hsaa200tDto();
            masterReq.setCmpycd(diary.getCmpycd());
            masterReq.setSalesid(diary.getSalesid());
            Hsaa200tDto master = hsaaMapper.selectSalesMaster(masterReq);
            if (master != null) {
                manualPlan.put("custnm", master.getCustnm());
                manualPlan.put("salestitle", master.getSalestitle() != null ? master.getSalestitle() : diary.getNextMeetAgenda());
                manualPlan.put("import_nm", "등급 " + (master.getImportrank() != null ? master.getImportrank() : "200"));
                manualPlan.put("succrate", master.getSuccrate());
                manualPlan.put("lastmtdt", diary.getContdt());
                manualPlan.put("address", master.getAddress());
                manualPlan.put("d_address", master.getD_address());
            } else {
                manualPlan.put("custnm", "거래처");
                manualPlan.put("salestitle", diary.getNextMeetAgenda() != null ? diary.getNextMeetAgenda() : "상담 후속 미팅");
                manualPlan.put("import_nm", "등급 200");
                manualPlan.put("succrate", "50");
                manualPlan.put("lastmtdt", diary.getContdt());
                manualPlan.put("address", "");
                manualPlan.put("d_address", "");
            }
            manualPlan.put("plan_source", "M");
            hsaaMapper.upsertVisitPlan(manualPlan);
        }

        // 1. 단계 변동 처리 및 마스터 업데이트
        Hsaa200tDto masterUpdate = new Hsaa200tDto();
        masterUpdate.setCmpycd(diary.getCmpycd());
        masterUpdate.setSalesid(diary.getSalesid());
        masterUpdate.setUpdemp(diary.getUpdemp());
        masterUpdate.setUpdtime(curYmd);
        masterUpdate.setLastmtdt(diary.getContdt());

        if (diary.getTostate() != null && !diary.getTostate().isEmpty()) {
            Hsaa310tDto stage = new Hsaa310tDto();
            stage.setCmpycd(diary.getCmpycd());
            stage.setCustcd(diary.getCustcd());
            stage.setSalesid(diary.getSalesid());
            stage.setSer(String.format("%03d", hsaaMapper.nextStageSer(stage)));
            stage.setState(diary.getTostate());
            stage.setChngdt(diary.getContdt()); // 변동일은 상담일 기준
            stage.setWincd(diary.getWincd());
            stage.setFailcd(diary.getFailcd());
            stage.setHoldcd(diary.getHoldcd());
            stage.setRealdt(cleanDate(diary.getRealdt()));
            stage.setRealamt(diary.getRealamt());
            stage.setRemark(diary.getRemark());
            stage.setAddtime(curYmd);
            stage.setUpdtime(curYmd);
            stage.setUpdemp(diary.getUpdemp());
            hsaaMapper.insertStage(stage);

            masterUpdate.setState(diary.getTostate());
            
            // 상태에 따른 일자 업데이트
            if ("900".equals(diary.getTostate())) {
                masterUpdate.setRealdt(cleanDate(diary.getRealdt()));
                masterUpdate.setRealamt(diary.getRealamt());
                masterUpdate.setWincd(diary.getWincd());
            } else if ("910".equals(diary.getTostate()) || "930".equals(diary.getTostate())) {
                masterUpdate.setFaildt(diary.getContdt());
                masterUpdate.setFailcd(diary.getFailcd());
            } else if ("920".equals(diary.getTostate())) {
                masterUpdate.setHoldondt(cleanDate(diary.getHoldondt()));
                masterUpdate.setHoldcd(diary.getHoldcd());
            }
        }
        hsaaMapper.updateSalesMaster(masterUpdate);
    }

    @Transactional
    public void saveStage(Hsaa310tDto stage) {
        String curYmd = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        stage.setChngdt(cleanDate(stage.getChngdt()));
        stage.setSer(String.format("%03d", hsaaMapper.nextStageSer(stage)));
        
        // 시스템 날짜 설정
        stage.setAddtime(curYmd);
        stage.setUpdtime(curYmd);
        
        hsaaMapper.insertStage(stage);

        // 영업 마스터 상태 및 결과 업데이트
        Hsaa200tDto masterUpdate = new Hsaa200tDto();
        masterUpdate.setCmpycd(stage.getCmpycd());
        masterUpdate.setSalesid(stage.getSalesid());
        masterUpdate.setState(stage.getState());
        masterUpdate.setWincd(stage.getWincd());
        masterUpdate.setFailcd(stage.getFailcd());
        masterUpdate.setHoldcd(stage.getHoldcd());
        masterUpdate.setSalesremark(stage.getRemark());
        masterUpdate.setUpdemp(stage.getUpdemp());
        masterUpdate.setUpdtime(curYmd);

        // 상태에 따른 일자/금액 매핑
        if ("900".equals(stage.getState())) {
            masterUpdate.setRealdt(cleanDate(stage.getRealdt()));
            masterUpdate.setRealamt(stage.getRealamt());
        } else if ("910".equals(stage.getState()) || "930".equals(stage.getState())) {
            masterUpdate.setFaildt(stage.getChngdt());
        }
        
        hsaaMapper.updateSalesMaster(masterUpdate);
    }

    @Transactional
    public void deleteDiary(Hsaa300tDto dto) {
        Hsaa300tDto oldDiary = hsaaMapper.selectDiary(dto);
        if (oldDiary != null && oldDiary.getNextMeetDt() != null && !oldDiary.getNextMeetDt().trim().isEmpty()) {
            Map<String, Object> delParams = new HashMap<>();
            delParams.put("cmpycd", dto.getCmpycd());
            delParams.put("userid", oldDiary.getUserid() != null ? oldDiary.getUserid() : dto.getUserid());
            delParams.put("salesid", oldDiary.getSalesid());
            delParams.put("plan_dt", cleanDate(oldDiary.getNextMeetDt()));
            hsaaMapper.deleteManualVisitPlan(delParams);
        }
        hsaaMapper.deleteDiary(dto);
    }

    public List<Hsaa100tDto> getKeymanList(String cmpycd, String custcd) {
        Hsaa100tDto req = new Hsaa100tDto();
        req.setCmpycd(cmpycd);
        req.setCustcd(custcd);
        return hsaaMapper.selectKeymanList(req);
    }

    @Transactional
    public void saveKeyman(Hsaa100tDto keyman) {
        keyman.setBirthday(cleanDate(keyman.getBirthday()));

        if (keyman.getCustid() == null || keyman.getCustid().isEmpty()) {
            keyman.setCustid(hsaaMapper.generateKeymanId(keyman.getCmpycd()));
            hsaaMapper.insertKeyman(keyman);
        } else {
            hsaaMapper.updateKeyman(keyman);
        }

        // 주소록 및 전화번호부 동기화
        hsaaMapper.syncHsba130t(keyman);
        if (keyman.getCusttel() != null && !keyman.getCusttel().isEmpty()) {
            hsaaMapper.syncPhoneNumberMap(keyman.getCusttel().replace("-", ""), "1", keyman.getCustcd(), keyman.getName(), keyman.getUpdemp());
        }
        if (keyman.getHpno() != null && !keyman.getHpno().isEmpty()) {
            hsaaMapper.syncPhoneNumberMap(keyman.getHpno().replace("-", ""), "2", keyman.getCustcd(), keyman.getName(), keyman.getUpdemp());
        }
    }

    @Transactional
    public void deleteKeyman(Hsaa100tDto dto) {
        hsaaMapper.deleteKeyman(dto);
    }

    public List<Hsaa310tDto> getStageList(String cmpycd, String salesid) {
        Hsaa310tDto req = new Hsaa310tDto();
        req.setCmpycd(cmpycd);
        req.setSalesid(salesid);
        return hsaaMapper.selectStageList(req);
    }

    public List<Hsaa320tDto> getDocsList(String cmpycd, String salesid) {
        Hsaa320tDto req = new Hsaa320tDto();
        req.setCmpycd(cmpycd);
        req.setSalesid(salesid);
        return hsaaMapper.selectDocsList(req);
    }

    @Transactional
    public void saveDoc(Hsaa320tDto doc, org.springframework.web.multipart.MultipartFile file) {
        String curYmd = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        
        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            doc.setFilename(originalFilename);
            
            // 💡 저장 경로 정책: storage/{cmpycd}/sfa/
            String subPath = doc.getCmpycd() + "/sfa";
            try {
                fileStorageService.saveFileToPath(file, subPath, originalFilename);
                log.info("📁 [HSAA] 파일 업로드 완료: {}/{}", subPath, originalFilename);
            } catch (java.io.IOException e) {
                log.error("❌ [HSAA] 파일 저장 실패: {}", e.getMessage());
                throw new RuntimeException("파일 물리 저장 중 오류가 발생했습니다.");
            }
        }

        if (doc.getSer() == null || doc.getSer().isEmpty() || "0".equals(doc.getSer())) {
            Integer nextSer = hsaaMapper.nextDocSer(doc);
            // 💡 3자리 문자열 포맷팅 (001, 002...) 적용
            doc.setSer(String.format("%03d", nextSer == null ? 1 : nextSer));
            doc.setRegdt(curYmd);
            doc.setAddtime(curYmd);
            doc.setUpdtime(curYmd);
            hsaaMapper.insertDoc(doc);
        }
    }

    // --- HSAA310S 신규계약현황 ---
    public List<Hsaa310sDto> getContractStatus(String cmpycd, String yyyy, String deptcd, String item) {
        return hsaaMapper.selectContractStatus(cmpycd, yyyy, deptcd, item);
    }

    // --- HSAA390S 주간영업활동실적현황 ---
    public List<Hsaa390sDto> getWeeklyActivityStatus(String cmpycd, String stdymd, String deptcd) {
        return hsaaMapper.selectWeeklyActivityStatus(cmpycd, stdymd.replace("-", ""), deptcd);
    }

    // --- HSAA380S 주간 영업단계 현황 ---
    public List<Hsaa380sDto> getWeeklyStageStatus(String cmpycd, String stdymd, String deptcd) {
        return hsaaMapper.selectWeeklyStageStatus(cmpycd, stdymd.replace("-", ""), deptcd);
    }

    // --- HSAA340S 영업단계별 진행현황 ---
    public List<Hsaa340sDto> getStageProgressStatus(String cmpycd, String sdate, String edate, String deptcd) {
        return hsaaMapper.selectStageProgressStatus(cmpycd, sdate, edate, deptcd);
    }

    // --- HSAA370S 성공실패원인분석 ---
    public List<Hsaa370sDto> getCauseAnalysis(String cmpycd, String sdate, String edate, String deptcd, String item) {
        return hsaaMapper.selectCauseAnalysis(cmpycd, sdate, edate, deptcd, item);
    }

    // --- HSAA200S 영업종합현황 (Dashboard) ---
    public List<HsaaStatDto> getDashboardStats(String cmpycd, String yymm, String userid, String day1, String day2) {
        return hsaaMapper.selectDashboardStats(cmpycd, yymm.replace("-", ""), userid, day1, day2);
    }

    public List<Hsaa200tDto> getDashboardDetailList(
            String cmpycd, String yymm, String userid, String gubun, String code, String code1, String code2, 
            String day1, String day2, String sdate, String edate, String deptcd, String item, 
            String itemcd, String custgbn, String stdymd, String channelKind) {
        return hsaaMapper.selectDashboardDetailList(
                cmpycd, yymm.replace("-", ""), userid, gubun, code, code1, code2, day1, day2,
                sdate, edate, deptcd, item, itemcd, custgbn, stdymd, channelKind);
    }

    // --- HSAA200U 영업상담이관 ---

    public List<CallMstDto> getCallMstList(String cmpycd, String sdate, String edate, String gubun, String userid) {
        return hsaaMapper.selectCallMstList(cmpycd, sdate, edate, gubun, userid);
    }

    @Transactional
    public void processTransfer(List<CallMstDto> items, String cmpycd, String updemp) {
        String curYmd = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());

        for (CallMstDto item : items) {
            // UI에서 체크박스로 선택된 항목만 처리하거나, useyn 필드 확인 (Tabulator의 selectable을 쓰는 경우)
            // 여기서는 frontend가 selectedData만 보내므로 useyn 체크는 선택사항

            if (!"X".equals(item.getAbandonyn()) && (item.getTransyn() == null || "N".equals(item.getTransyn()))) {
                // 1. SALESID 생성
                String yymm = curYmd.substring(0, 6);
                String salesid = hsaaMapper.generateSalesId(cmpycd, yymm);

                // 2. 영업마스터 저장 (HSAA200T)
                Hsaa200tDto master = new Hsaa200tDto();
                master.setCmpycd(cmpycd);
                master.setSalesid(salesid);
                master.setSalestitle(item.getCustnm() + "- 영업상담 이관 건");
                master.setState("100");
                master.setUserid(item.getUserid());
                master.setCustcd(item.getCustcd());
                master.setSuccrate(10);
                master.setImportrank("200");
                master.setLastmtdt(curYmd);
                master.setRtncd("010");
                master.setUsecd("010");
                master.setAddtime(curYmd); // 8자리로 저장
                master.setUpdemp(updemp);
                
                StringBuilder salesremark = new StringBuilder();
                salesremark.append("문의내용:").append(item.getTrbment()).append("\n");
                if (item.getAnsment() != null) salesremark.append("답변내용:").append(item.getAnsment()).append("\n");
                if (item.getTransmemo() != null) salesremark.append("전달메모:").append(item.getTransmemo()).append("\n");
                if (item.getRemark() != null) salesremark.append("추가요청:").append(item.getRemark()).append("\n");
                master.setSalesremark(salesremark.toString());
                master.setSvcno(item.getSvcno());
                
                hsaaMapper.insertSalesMaster(master);

                // 3. Keyman 저장 (HSAA100T)
                String keymanId = hsaaMapper.generateKeymanId(cmpycd); 
                Hsaa100tDto keyman = new Hsaa100tDto();
                keyman.setCmpycd(cmpycd);
                keyman.setCustcd(item.getCustcd());
                keyman.setCustid(keymanId);
                keyman.setName(item.getCustsnm());
                keyman.setKeyman("040");
                keyman.setCusttel(item.getTelno());
                keyman.setHpno(item.getHpno());
                keyman.setInnumber(item.getInnumber());
                keyman.setLevel("020");
                keyman.setMail(item.getEmailid());
                keyman.setRemark(item.getRemark());
                keyman.setUpdemp(item.getUserid());
                
                hsaaMapper.insertKeyman(keyman);
                
                // Keyman 동기화
                hsaaMapper.syncHsba130t(keyman);
                if (keyman.getCusttel() != null && !keyman.getCusttel().isEmpty()) {
                    hsaaMapper.syncPhoneNumberMap(keyman.getCusttel().replace("-", ""), "1", keyman.getCustcd(), keyman.getName(), updemp);
                }
                if (keyman.getHpno() != null && !keyman.getHpno().isEmpty()) {
                    hsaaMapper.syncPhoneNumberMap(keyman.getHpno().replace("-", ""), "2", keyman.getCustcd(), keyman.getName(), updemp);
                }

                // 4. 단계변동 저장 (HSAA310T)
                Hsaa310tDto stage = new Hsaa310tDto();
                stage.setCmpycd(cmpycd);
                stage.setSalesid(salesid);
                stage.setSer(String.format("%03d", hsaaMapper.nextStageSer(stage)));
                stage.setChngdt(curYmd);
                stage.setBfstate("100");
                stage.setState("100");
                stage.setRemark(master.getSalestitle());
                stage.setUpdemp(updemp);
                hsaaMapper.insertStage(stage);

                // 5. CALL_MST 업데이트
                hsaaMapper.updateCallMstTransfer(cmpycd, item.getUserid(), item.getEscalationno(), updemp);

            } else if ("X".equals(item.getAbandonyn())) {
                hsaaMapper.updateCallMstTransfer(cmpycd, "", item.getEscalationno(), updemp);
            }
        }
    }

    // --- HSAA300U 영업담당자 변경 ---
    public List<Hsaa200tDto> getSalesForTransfer(String cmpycd, String userid) {
        return hsaaMapper.selectSalesForTransfer(cmpycd, userid);
    }

    @Transactional
    public void changeSalesManager(List<Hsaa200tDto> items, String toUserid, String chngReason, String cmpycd, String updemp) {
        SalesUserDto toUser = hsaaMapper.selectSalesUser(cmpycd, toUserid);
        if (toUser == null) throw new RuntimeException("변경할 담당자 정보를 찾을 수 없습니다.");

        for (Hsaa200tDto item : items) {
            Hsaa210tDto history = new Hsaa210tDto();
            history.setCmpycd(cmpycd);
            history.setSalesid(item.getSalesid());
            history.setSer(String.format("%03d", hsaaMapper.nextHsaa210tSer(history)));
            history.setFromuserid(item.getUserid());
            history.setFromdeptcd(item.getDeptcd());
            history.setTouserid(toUserid);
            history.setTodeptcd(toUser.getDeptcd());
            history.setChngreason(chngReason);
            history.setUpdemp(updemp);
            hsaaMapper.insertHsaa210t(history);

            // 영업 마스터(200T) 담당자 정보만 업데이트
            hsaaMapper.updateHsaa200tManager(cmpycd, item.getSalesid(), toUserid, toUser.getDeptcd(), updemp);
        }
    }

    // --- HSAA400S 기간별 영업상담 내역 ---
    public Map<String, Object> getConsultationList(String cmpycd, String sdate, String edate, String schCustnm, String userid, int page, int limit) {
        int offset = (page - 1) * limit;
        List<Hsaa300tDto> list = hsaaMapper.selectConsultationList(cmpycd, sdate.replace("-", ""), edate.replace("-", ""), schCustnm, userid, offset, limit);
        int total = hsaaMapper.countConsultationList(cmpycd, sdate.replace("-", ""), edate.replace("-", ""), schCustnm, userid);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    @Transactional
    public void saveSalesCoaching(Map<String, Object> payload, String cmpycd, String updemp) {
        payload.put("cmpycd", cmpycd);
        payload.put("updemp", updemp);
        hsaaMapper.updateSalesCoaching(payload);
    }

    @Transactional
    public void confirmCoachingRead(Map<String, Object> payload, String cmpycd, String updemp) {
        payload.put("cmpycd", cmpycd);
        payload.put("updemp", updemp);
        hsaaMapper.confirmCoachingRead(payload);
    }

    public List<Map<String, Object>> getHsbaCodes(String cmpycd, String cdgbn) {
        return hsaaMapper.selectHsbaCodes(cmpycd, cdgbn);
    }

    private String cleanDate(String dt) {
        if (dt == null) return null;
        String cleaned = dt.replace("-", "").replace("/", "").replace(".", "").trim();
        return cleaned.length() > 8 ? cleaned.substring(0, 8) : cleaned;
    }

    private String parseYn(Object val) {
        if (val == null) return "N";
        String s = val.toString().toLowerCase();
        if ("true".equals(s) || "y".equals(s) || "1".equals(s)) return "Y";
        return "N";
    }

    public List<Map<String, Object>> getVisitPlan(String cmpycd, String ym, String userid) {
        return hsaaMapper.selectExistingVisitPlan(cmpycd, userid, ym);
    }

    @Transactional
    public List<Map<String, Object>> generateVisitPlan(String cmpycd, String ym, String userid, int dailyCapa) {
        if (ym == null || ym.length() != 6) {
            throw new IllegalArgumentException("기준년월(ym)은 YYYYMM 형식이어야 합니다.");
        }
        if (userid == null || userid.trim().isEmpty()) {
            throw new IllegalArgumentException("영업사원 ID(userid)가 누락되었습니다.");
        }
        if (dailyCapa <= 0) dailyCapa = 4;

        List<Map<String, Object>> rawDeals = hsaaMapper.selectActiveDealsForVisitPlan(cmpycd, userid);
        if (rawDeals == null || rawDeals.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> deals = new ArrayList<>();
        for (Map<String, Object> d : rawDeals) {
            String salesid = String.valueOf(d.getOrDefault("salesid", "")).trim();
            if (salesid.isEmpty()) continue;

            if (d.get("import") == null || String.valueOf(d.get("import")).trim().isEmpty()) {
                d.put("import", "200");
            }
            if (d.get("succrate") == null) {
                d.put("succrate", 50.0);
            }
            if (d.get("custnm") == null || String.valueOf(d.get("custnm")).trim().isEmpty()) {
                d.put("custnm", "미지정 거래처");
            }
            if (d.get("salestitle") == null || String.valueOf(d.get("salestitle")).trim().isEmpty()) {
                d.put("salestitle", "영업건 (" + salesid + ")");
            }
            if (d.get("lastmtdt") == null || String.valueOf(d.get("lastmtdt")).trim().isEmpty()) {
                d.put("lastmtdt", new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(new Date()));
            }
            deals.add(d);
        }

        List<Map<String, Object>> cycles = hsaaMapper.selectImportanceCycles(cmpycd);
        Map<String, Integer> cycleMap = new HashMap<>();
        Map<String, String> gradeNameMap = new HashMap<>();
        for (Map<String, Object> c : cycles) {
            String code = String.valueOf(c.getOrDefault("code", ""));
            String remark = String.valueOf(c.getOrDefault("remark", "30"));
            int days = 30;
            try { days = Integer.parseInt(remark.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            cycleMap.put(code, days);
            gradeNameMap.put(code, "등급 " + code);
        }

        List<Map<String, Object>> holidays = hsaaMapper.selectHolidaysForMonth(cmpycd, ym);
        java.util.Set<String> holidaySet = new java.util.HashSet<>();
        for (Map<String, Object> h : holidays) {
            holidaySet.add(String.valueOf(h.get("holi_dt")));
        }

        List<String> workDays = new ArrayList<>();
        try {
            int year = Integer.parseInt(ym.substring(0, 4));
            int month = Integer.parseInt(ym.substring(4, 6));
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(year, month - 1, 1);
            int maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault());

            for (int d = 1; d <= maxDay; d++) {
                cal.set(year, month - 1, d);
                int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
                if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) continue;
                String dateStr = sdf.format(cal.getTime());
                if (!holidaySet.contains(dateStr)) {
                    workDays.add(dateStr);
                }
            }
        } catch (Exception e) {
            log.error("Error generating work days: {}", e.getMessage());
        }

        if (workDays.isEmpty()) {
            throw new IllegalStateException("해당 월에 유효한 영업일(Working Day)이 존재하지 않습니다.");
        }

        for (Map<String, Object> deal : deals) {
            String imp = String.valueOf(deal.getOrDefault("import", "200"));
            int cycleDays = cycleMap.getOrDefault(imp, 30);
            deal.put("import_nm", gradeNameMap.getOrDefault(imp, "등급 " + imp));

            double succrate = 50.0;
            try { succrate = Double.parseDouble(String.valueOf(deal.getOrDefault("succrate", "50"))); } catch (Exception ignored) {}

            double score = cycleDays * (succrate / 100.0);
            deal.put("_score", score);
        }

        deals.sort((a, b) -> Double.compare((Double) b.get("_score"), (Double) a.get("_score")));

        List<Map<String, Object>> generatedPlan = new ArrayList<>();
        int dayIdx = 0;
        int countToday = 0;

        for (Map<String, Object> deal : deals) {
            if (dayIdx >= workDays.size()) break;
            String targetDate = workDays.get(dayIdx);

            Map<String, Object> planItem = new HashMap<>();
            planItem.put("cmpycd", cmpycd);
            planItem.put("userid", userid);
            planItem.put("plan_dt", targetDate);
            planItem.put("salesid", deal.get("salesid"));
            planItem.put("custcd", deal.get("custcd"));
            planItem.put("custnm", deal.get("custnm"));
            planItem.put("salestitle", deal.get("salestitle"));
            planItem.put("import_nm", deal.get("import_nm"));
            planItem.put("succrate", deal.get("succrate"));
            planItem.put("lastmtdt", deal.get("lastmtdt"));
            planItem.put("address", deal.get("address"));
            generatedPlan.add(planItem);

            countToday++;
            if (countToday >= dailyCapa) {
                countToday = 0;
                dayIdx++;
            }
        }

        return generatedPlan;
    }

    @Transactional
    public void confirmVisitPlan(Map<String, Object> payload, String cmpycd) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> plans = (List<Map<String, Object>>) payload.get("plans");
        if (plans == null) return;

        String userid = String.valueOf(payload.getOrDefault("userid", ""));
        for (Map<String, Object> plan : plans) {
            plan.put("cmpycd", cmpycd);
            plan.put("userid", userid);
            hsaaMapper.upsertVisitPlan(plan);
        }
    }

    public Map<String, Object> getWeeklyReport(String cmpycd, String userid, String weekid) {
        Map<String, Object> report = hsaaMapper.selectWeeklyReport(cmpycd, userid, weekid);
        if (report == null) {
            report = new HashMap<>();
            report.put("cmpycd", cmpycd);
            report.put("userid", userid);
            report.put("week_id", weekid);
            report.put("last_week_perf", "");
            report.put("next_week_plan", "");
            report.put("issues_comments", "");
            report.put("status", "1");
        }
        return report;
    }

    public Map<String, Object> autoGenerateWeeklyReport(String cmpycd, String userid, String weekId, String sdate, String edate, String nextSdate, String nextEdate) {
        List<Map<String, Object>> lastDiaries = hsaaMapper.selectLastWeekConsultations(cmpycd, userid, sdate, edate);
        StringBuilder sbPerf = new StringBuilder();
        sbPerf.append("■ [지난주 주요 영업 실적 및 상담 이력]\n");
        if (lastDiaries.isEmpty()) {
            sbPerf.append("- 등록된 상담 이력이 없습니다.\n");
        } else {
            int idx = 1;
            for (Map<String, Object> d : lastDiaries) {
                sbPerf.append(idx++).append(". [").append(d.get("contdt")).append("] ")
                      .append(d.get("custnm")).append(" - ").append(d.get("salestitle"))
                      .append("\n   - 상담내용: ").append(d.get("diary_content") != null ? d.get("diary_content").toString().replaceAll("\n", " ") : "")
                      .append("\n   - 후속조치: ").append(d.get("action_item") != null ? d.get("action_item") : "없음")
                      .append(" (").append("1".equals(d.get("action_status")) ? "완료" : "미해결").append(")\n");
            }
        }

        List<Map<String, Object>> nextPlans = hsaaMapper.selectNextWeekVisitPlans(cmpycd, userid, nextSdate, nextEdate);
        StringBuilder sbPlan = new StringBuilder();
        sbPlan.append("■ [금주/다음주 방문 및 미팅 예정 계획]\n");
        if (nextPlans.isEmpty()) {
            sbPlan.append("- 예정된 방문 계획이 없습니다.\n");
        } else {
            int idx = 1;
            for (Map<String, Object> p : nextPlans) {
                sbPlan.append(idx++).append(". [").append(p.get("plan_dt")).append("] ")
                      .append(p.get("custnm")).append(" - ").append(p.get("salestitle"))
                      .append(" (주소: ").append(p.get("address")).append(")\n");
            }
        }

        Map<String, Object> generated = new HashMap<>();
        generated.put("cmpycd", cmpycd);
        generated.put("userid", userid);
        generated.put("week_id", weekId);
        generated.put("sdate", sdate);
        generated.put("edate", edate);
        generated.put("last_week_perf", sbPerf.toString());
        generated.put("next_week_plan", sbPlan.toString());
        generated.put("issues_comments", "■ 특이사항 및 건의사항\n- 특이사항 없음");
        generated.put("status", "1");
        return generated;
    }

    @Transactional
    public void saveWeeklyReport(Map<String, Object> payload, String cmpycd) {
        payload.put("cmpycd", cmpycd);
        hsaaMapper.upsertWeeklyReport(payload);
    }
}
