package com.crmbank.erp.hsaa.controller;

import com.crmbank.erp.comm.dto.ApiResponse;
import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hsaa.dto.*;
import com.crmbank.erp.hsaa.service.HsaaService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/hsaa")
@RequiredArgsConstructor
public class HsaaController {

    private final HsaaService hsaaService;
    private final ObjectMapper objectMapper;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<SalesUserDto>>> getSalesUserList(HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<SalesUserDto> result = hsaaService.getSalesUserList(user.getCmpycd());
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @GetMapping("/targets")
    public ResponseEntity<ApiResponse<List<Hsaa600tDto>>> getSalesTargetList(
            @RequestParam String yyyy,
            @RequestParam String userid,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa600tDto> result = hsaaService.getSalesTargetList(user.getCmpycd(), yyyy, userid);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping("/targets/save")
    public ResponseEntity<ApiResponse<?>> saveSalesTargets(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            String userid = (String) payload.get("userid");
            List<Hsaa600tDto> targets = objectMapper.convertValue(payload.get("targets"), new TypeReference<List<Hsaa600tDto>>() {});
            
            hsaaService.saveSalesTargets(targets, user.getCmpycd(), userid, user.getDeptcd());
            return ResponseEntity.ok(ApiResponse.success("저장 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 저장 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    // --- HSAA100U 통합 관리 확장 API ---

    @GetMapping("/master")
    public ResponseEntity<ApiResponse<List<Hsaa200tDto>>> getSalesMasterList(
            @RequestParam String fromdt,
            @RequestParam String todt,
            @RequestParam(required = false) String schcustnm,
            @RequestParam(required = false) String userid,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa200tDto> result = hsaaService.getSalesMasterList(user.getCmpycd(), fromdt, todt, schcustnm, userid);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesDetail(
            @RequestParam String salesid,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        Map<String, Object> result = hsaaService.getSalesDetail(user.getCmpycd(), salesid);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping("/master/save")
    public ResponseEntity<ApiResponse<String>> saveSalesMaster(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            Hsaa200tDto master = objectMapper.convertValue(payload.get("master"), Hsaa200tDto.class);
            List<Hsaa810tDto> items = objectMapper.convertValue(payload.get("items"), new TypeReference<List<Hsaa810tDto>>() {});
            
            master.setCmpycd(user.getCmpycd());
            master.setDeptcd(user.getDeptcd());
            master.setUpdemp(user.getUserid());
            
            String salesid = hsaaService.saveSalesMaster(master, items);
            return ResponseEntity.ok(ApiResponse.success(salesid, "저장 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 마스터 저장 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @DeleteMapping("/master/delete")
    public ResponseEntity<ApiResponse<?>> deleteSalesMaster(
            @RequestParam String salesid,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        Hsaa200tDto req = new Hsaa200tDto();
        req.setCmpycd(user.getCmpycd());
        req.setSalesid(salesid);
        hsaaService.deleteSalesMaster(req);
        return ResponseEntity.ok(ApiResponse.success("영업건 삭제 성공"));
    }

    @GetMapping("/diary")
    public ResponseEntity<ApiResponse<List<Hsaa300tDto>>> getDiaryList(
            @RequestParam String salesid,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa300tDto> result = hsaaService.getDiaryList(user.getCmpycd(), salesid);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping("/diary/save")
    public ResponseEntity<ApiResponse<?>> saveDiary(
            @RequestBody Hsaa300tDto diary,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            diary.setCmpycd(user.getCmpycd());
            diary.setUserid(user.getUserid());
            diary.setDeptcd(user.getDeptcd());
            diary.setUpdemp(user.getUserid());
            hsaaService.saveDiary(diary);
            return ResponseEntity.ok(ApiResponse.success("상담일지 저장 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 상담일지 저장 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @DeleteMapping("/diary/delete")
    public ResponseEntity<ApiResponse<?>> deleteDiary(
            @RequestParam String salesid,
            @RequestParam String ser,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        Hsaa300tDto req = new Hsaa300tDto();
        req.setCmpycd(user.getCmpycd());
        req.setSalesid(salesid);
        req.setSer(ser);
        hsaaService.deleteDiary(req);
        return ResponseEntity.ok(ApiResponse.success("상담일지 삭제 성공"));
    }

    @GetMapping("/keyman")
    public ResponseEntity<ApiResponse<List<Hsaa100tDto>>> getKeymanList(
            @RequestParam String custcd,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa100tDto> result = hsaaService.getKeymanList(user.getCmpycd(), custcd);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping("/keyman/save")
    public ResponseEntity<ApiResponse<?>> saveKeyman(
            @RequestBody Hsaa100tDto keyman,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            keyman.setCmpycd(user.getCmpycd());
            keyman.setUpdemp(user.getUserid());
            hsaaService.saveKeyman(keyman);
            return ResponseEntity.ok(ApiResponse.success("Keyman 저장 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] Keyman 저장 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @DeleteMapping("/keyman/delete")
    public ResponseEntity<ApiResponse<?>> deleteKeyman(
            @RequestParam String custid,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        Hsaa100tDto req = new Hsaa100tDto();
        req.setCmpycd(user.getCmpycd());
        req.setCustid(custid);
        hsaaService.deleteKeyman(req);
        return ResponseEntity.ok(ApiResponse.success("Keyman 삭제 성공"));
    }

    @GetMapping("/stages")
    public ResponseEntity<ApiResponse<List<Hsaa310tDto>>> getStageList(
            @RequestParam String salesid,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa310tDto> result = hsaaService.getStageList(user.getCmpycd(), salesid);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping("/stages/save")
    public ResponseEntity<ApiResponse<?>> saveStage(
            @RequestBody Hsaa310tDto stage,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            stage.setCmpycd(user.getCmpycd());
            stage.setUpdemp(user.getUserid());
            hsaaService.saveStage(stage);
            return ResponseEntity.ok(ApiResponse.success("단계변동 저장 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 단계변동 저장 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @GetMapping("/docs")
    public ResponseEntity<ApiResponse<List<Hsaa320tDto>>> getDocsList(
            @RequestParam String salesid,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa320tDto> result = hsaaService.getDocsList(user.getCmpycd(), salesid);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping(value = "/docs/save", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> saveDoc(
            @RequestPart("doc") String docJson,
            @RequestPart(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            Hsaa320tDto doc = objectMapper.readValue(docJson, Hsaa320tDto.class);
            doc.setCmpycd(user.getCmpycd());
            doc.setUpdemp(user.getUserid());
            hsaaService.saveDoc(doc, file);
            return ResponseEntity.ok(ApiResponse.success("문서 저장 및 업로드 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 문서 저장 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    // --- HSAA370S 성공실패원인분석 API ---

    @GetMapping("/cause/analysis")
    public ResponseEntity<ApiResponse<List<Hsaa370sDto>>> getCauseAnalysis(
            @RequestParam String sdate,
            @RequestParam String edate,
            @RequestParam(required = false) String deptcd,
            @RequestParam(required = false) String item,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa370sDto> result = hsaaService.getCauseAnalysis(user.getCmpycd(), sdate, edate, deptcd, item);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    // --- HSAA340S 영업단계별 진행현황 API ---

    @GetMapping("/stages/progress")
    public ResponseEntity<ApiResponse<List<Hsaa340sDto>>> getStageProgressStatus(
            @RequestParam String sdate,
            @RequestParam String edate,
            @RequestParam(required = false) String deptcd,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa340sDto> result = hsaaService.getStageProgressStatus(user.getCmpycd(), sdate, edate, deptcd);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    // --- HSAA310S 신규계약현황 API ---

    @GetMapping("/contracts/status")
    public ResponseEntity<ApiResponse<List<Hsaa310sDto>>> getContractStatus(
            @RequestParam String yyyy,
            @RequestParam(required = false) String deptcd,
            @RequestParam(required = false) String item,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa310sDto> result = hsaaService.getContractStatus(user.getCmpycd(), yyyy, deptcd, item);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    // --- HSAA200S 영업종합현황 (Dashboard) API ---

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<List<HsaaStatDto>>> getDashboardStats(
            @RequestParam String yymm,
            @RequestParam(required = false) String userid,
            @RequestParam(defaultValue = "30") String day1,
            @RequestParam(defaultValue = "30") String day2,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<HsaaStatDto> result = hsaaService.getDashboardStats(user.getCmpycd(), yymm, userid, day1, day2);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @GetMapping("/dashboard/list")
    public ResponseEntity<ApiResponse<List<Hsaa200tDto>>> getDashboardDetailList(
            @RequestParam String yymm,
            @RequestParam String gubun,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String code1,
            @RequestParam(required = false) String code2,
            @RequestParam(required = false) String userid,
            @RequestParam(defaultValue = "30") String day1,
            @RequestParam(defaultValue = "30") String day2,
            @RequestParam(required = false) String sdate,
            @RequestParam(required = false) String edate,
            @RequestParam(required = false) String deptcd,
            @RequestParam(required = false) String item,
            @RequestParam(required = false) String itemcd,
            @RequestParam(required = false) String custgbn,
            @RequestParam(required = false) String stdymd,
            @RequestParam(required = false) String channelKind,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa200tDto> result = hsaaService.getDashboardDetailList(
                user.getCmpycd(), yymm, userid, gubun, code, code1, code2, day1, day2,
                sdate, edate, deptcd, item, itemcd, custgbn, stdymd, channelKind);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    // --- HSAA390S 주간영업활동실적현황 API ---

    @GetMapping("/weekly/activity")
    public ResponseEntity<ApiResponse<List<Hsaa390sDto>>> getWeeklyActivityStatus(
            @RequestParam String stdymd,
            @RequestParam(required = false) String deptcd,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa390sDto> result = hsaaService.getWeeklyActivityStatus(user.getCmpycd(), stdymd, deptcd);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    // --- HSAA380S 주간 영업단계 현황 API ---

    @GetMapping("/weekly/stage")
    public ResponseEntity<ApiResponse<List<Hsaa380sDto>>> getWeeklyStageStatus(
            @RequestParam String stdymd,
            @RequestParam(required = false) String deptcd,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa380sDto> result = hsaaService.getWeeklyStageStatus(user.getCmpycd(), stdymd, deptcd);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    // --- HSAA200U 영업상담이관 API ---

    @GetMapping("/calls")
    public ResponseEntity<ApiResponse<List<CallMstDto>>> getCallMstList(
            @RequestParam String sdate,
            @RequestParam String edate,
            @RequestParam(required = false) String gubun,
            @RequestParam(required = false) String userid,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<CallMstDto> result = hsaaService.getCallMstList(user.getCmpycd(), sdate, edate, gubun, userid);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<?>> processTransfer(
            @RequestBody List<CallMstDto> items,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            hsaaService.processTransfer(items, user.getCmpycd(), user.getUserid());
            return ResponseEntity.ok(ApiResponse.success("이관 처리 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 이관 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    // --- HSAA300U 영업담당자 변경 API ---

    @GetMapping("/transfer-list")
    public ResponseEntity<ApiResponse<List<Hsaa200tDto>>> getSalesForTransfer(
            @RequestParam String userid,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Hsaa200tDto> result = hsaaService.getSalesForTransfer(user.getCmpycd(), userid);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping("/change-manager")
    public ResponseEntity<ApiResponse<?>> changeSalesManager(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            String toUserid = (String) payload.get("toUserid");
            String chngReason = (String) payload.get("chngReason");
            List<Hsaa200tDto> items = objectMapper.convertValue(payload.get("items"), new TypeReference<List<Hsaa200tDto>>() {});

            hsaaService.changeSalesManager(items, toUserid, chngReason, user.getCmpycd(), user.getUserid());
            return ResponseEntity.ok(ApiResponse.success("담당자 변경 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 담당자 변경 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    // --- HSAA400S 기간별 영업상담 내역 API ---

    @GetMapping("/consultations/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConsultationList(
            @RequestParam String sdate,
            @RequestParam String edate,
            @RequestParam(required = false) String schCustnm,
            @RequestParam(required = false) String userid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        Map<String, Object> result = hsaaService.getConsultationList(user.getCmpycd(), sdate, edate, schCustnm, userid, page, limit);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    @PostMapping("/consultations/coaching")
    public ResponseEntity<ApiResponse<?>> saveSalesCoaching(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            hsaaService.saveSalesCoaching(payload, user.getCmpycd(), user.getUserid());
            return ResponseEntity.ok(ApiResponse.success("영업코칭 저장 성공"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 코칭 저장 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    @PostMapping("/consultations/coaching/confirm")
    public ResponseEntity<ApiResponse<?>> confirmCoachingRead(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        try {
            hsaaService.confirmCoachingRead(payload, user.getCmpycd(), user.getUserid());
            return ResponseEntity.ok(ApiResponse.success("코칭 확인 완료"));
        } catch (Exception e) {
            log.error("❌ [HSAA] 코칭 확인 오류: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.serverError(e.getMessage()));
        }
    }

    // --- HSAA700U 방문 계획 자동 수립 API ---

    @GetMapping("/visit-plan")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getVisitPlan(
            @RequestParam String ym,
            @RequestParam(required = false) String userid,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        String targetUser = (userid == null || userid.isEmpty()) ? user.getUserid() : userid;
        List<Map<String, Object>> plan = hsaaService.getVisitPlan(user.getCmpycd(), ym, targetUser);
        return ResponseEntity.ok(ApiResponse.success(plan, "조회 성공"));
    }

    @PostMapping("/visit-plan/generate")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> generateVisitPlan(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        String ym = String.valueOf(payload.getOrDefault("ym", ""));
        String userid = String.valueOf(payload.getOrDefault("userid", user.getUserid()));
        int dailyCapa = Integer.parseInt(String.valueOf(payload.getOrDefault("dailyCapa", "4")));

        List<Map<String, Object>> plan = hsaaService.generateVisitPlan(user.getCmpycd(), ym, userid, dailyCapa);
        return ResponseEntity.ok(ApiResponse.success(plan, "방문 계획 자동 수립 성공"));
    }

    @PostMapping("/visit-plan/confirm")
    public ResponseEntity<ApiResponse<?>> confirmVisitPlan(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        hsaaService.confirmVisitPlan(payload, user.getCmpycd());
        return ResponseEntity.ok(ApiResponse.success("방문 계획 확정 성공"));
    }

    @GetMapping("/codes/{group}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHsbaCodes(
            @PathVariable("group") String group,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        List<Map<String, Object>> result = hsaaService.getHsbaCodes(user.getCmpycd(), group);
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"));
    }

    // --- HSAA500U 자동 주간 영업활동 보고서 API ---

    @GetMapping("/weekly-report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeeklyReport(
            @RequestParam(required = false) String weekid,
            @RequestParam(required = false) String userid,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        String targetUser = (userid == null || userid.isEmpty()) ? user.getUserid() : userid;
        Map<String, Object> report = hsaaService.getWeeklyReport(user.getCmpycd(), targetUser, weekid);
        return ResponseEntity.ok(ApiResponse.success(report, "조회 성공"));
    }

    @PostMapping("/weekly-report/auto-generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoGenerateWeeklyReport(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        String weekId = String.valueOf(payload.getOrDefault("weekId", ""));
        String userid = String.valueOf(payload.getOrDefault("userid", user.getUserid()));
        String sdate = String.valueOf(payload.getOrDefault("sdate", ""));
        String edate = String.valueOf(payload.getOrDefault("edate", ""));
        String nextSdate = String.valueOf(payload.getOrDefault("nextSdate", ""));
        String nextEdate = String.valueOf(payload.getOrDefault("nextEdate", ""));

        Map<String, Object> report = hsaaService.autoGenerateWeeklyReport(user.getCmpycd(), userid, weekId, sdate, edate, nextSdate, nextEdate);
        return ResponseEntity.ok(ApiResponse.success(report, "자동 생성 성공"));
    }

    @PostMapping("/weekly-report/save")
    public ResponseEntity<ApiResponse<?>> saveWeeklyReport(
            @RequestBody Map<String, Object> payload,
            HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user == null) return ResponseEntity.status(401).build();

        payload.put("cmpycd", user.getCmpycd());
        payload.put("userid", payload.getOrDefault("userid", user.getUserid()));
        hsaaService.saveWeeklyReport(payload, user.getCmpycd());
        return ResponseEntity.ok(ApiResponse.success("주간보고서 저장 성공"));
    }
}
