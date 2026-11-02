package com.crmbank.erp.hsaa.mapper;

import com.crmbank.erp.hsaa.dto.Hsaa600tDto;
import com.crmbank.erp.hsaa.dto.SalesUserDto;
import com.crmbank.erp.hsaa.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface HsaaMapper {
    List<SalesUserDto> getSalesUserList(@Param("cmpycd") String cmpycd);
    List<Hsaa600tDto> getSalesTargetList(@Param("cmpycd") String cmpycd, @Param("yyyy") String yyyy, @Param("userid") String userid);
    int checkExists(Hsaa600tDto dto);
    void updateSalesTarget(Hsaa600tDto dto);
    void insertSalesTarget(Hsaa600tDto dto);

    // --- HSAA100U 통합 관리 확장 ---
    // 1. 영업마스터 (HSAA200T)
    List<Hsaa200tDto> selectSalesMasterList(@Param("cmpycd") String cmpycd, @Param("fromdt") String fromdt, @Param("todt") String todt, @Param("schcustnm") String schcustnm, @Param("userid") String userid);
    Hsaa200tDto selectSalesMaster(Hsaa200tDto dto);
    void insertSalesMaster(Hsaa200tDto dto);
    void updateSalesMaster(Hsaa200tDto dto);
    void deleteSalesMaster(Hsaa200tDto dto);
    String generateSalesId(@Param("cmpycd") String cmpycd, @Param("yymm") String yymm);

    // 2. 상담일지 (HSAA300T)
    List<Hsaa300tDto> selectDiaryList(Hsaa300tDto dto);
    Hsaa300tDto selectDiary(Hsaa300tDto dto);
    void insertDiary(Hsaa300tDto dto);
    void updateDiary(Hsaa300tDto dto);
    void deleteDiary(Hsaa300tDto dto);
    Integer nextDiarySer(Hsaa300tDto dto);
    void deleteManualVisitPlan(Map<String, Object> params);

    // 3. 단계변동 (HSAA310T)
    List<Hsaa310tDto> selectStageList(Hsaa310tDto dto);
    void insertStage(Hsaa310tDto dto);
    void deleteStage(Hsaa310tDto dto);
    Integer nextStageSer(Hsaa310tDto dto);

    // 4. Keyman (HSAA100T)
    List<Hsaa100tDto> selectKeymanList(Hsaa100tDto dto);
    void insertKeyman(Hsaa100tDto dto);
    void updateKeyman(Hsaa100tDto dto);
    void deleteKeyman(Hsaa100tDto dto);
    String generateKeymanId(@Param("cmpycd") String cmpycd);

    // 5. 문서관리 (HSAA320T)
    List<Hsaa320tDto> selectDocsList(Hsaa320tDto dto);
    void insertDoc(Hsaa320tDto dto);
    void deleteDoc(Hsaa320tDto dto);
    Integer nextDocSer(Hsaa320tDto dto);

    // 6. 품목관리 (HSAA810T)
    List<Hsaa810tDto> selectItemList(Hsaa810tDto dto);
    void insertItem(Hsaa810tDto dto);
    void deleteItemsBySalesId(Hsaa810tDto dto);

    // --- HSAA200U 영업상담이관 ---
    List<CallMstDto> selectCallMstList(@Param("cmpycd") String cmpycd, @Param("sdate") String sdate, @Param("edate") String edate, @Param("gubun") String gubun, @Param("userid") String userid);
    void updateCallMstTransfer(@Param("cmpycd") String cmpycd, @Param("feedbackuser") String feedbackuser, @Param("escalationno") String escalationno, @Param("updemp") String updemp);

    // --- HSAA300U 영업담당자 변경 ---
    List<Hsaa200tDto> selectSalesForTransfer(@Param("cmpycd") String cmpycd, @Param("userid") String userid);
    SalesUserDto selectSalesUser(@Param("cmpycd") String cmpycd, @Param("userid") String userid);
    void insertHsaa210t(Hsaa210tDto dto);
    Integer nextHsaa210tSer(Hsaa210tDto dto);
    void updateHsaa200tManager(@Param("cmpycd") String cmpycd, @Param("salesid") String salesid, @Param("userid") String userid, @Param("deptcd") String deptcd, @Param("updemp") String updemp);

    // --- HSAA390S 주간영업활동실적현황 ---
    List<Hsaa390sDto> selectWeeklyActivityStatus(@Param("cmpycd") String cmpycd, @Param("stdymd") String stdymd, @Param("deptcd") String deptcd);

    // --- HSAA380S 주간 영업단계 현황 ---
    List<Hsaa380sDto> selectWeeklyStageStatus(@Param("cmpycd") String cmpycd, @Param("stdymd") String stdymd, @Param("deptcd") String deptcd);

    // --- HSAA370S 성공실패원인분석 ---
    List<Hsaa370sDto> selectCauseAnalysis(@Param("cmpycd") String cmpycd, @Param("sdate") String sdate, @Param("edate") String edate, @Param("deptcd") String deptcd, @Param("item") String item);

    // --- HSAA340S 영업단계별 진행현황 ---
    List<Hsaa340sDto> selectStageProgressStatus(@Param("cmpycd") String cmpycd, @Param("sdate") String sdate, @Param("edate") String edate, @Param("deptcd") String deptcd);

    // --- HSAA310S 신규계약현황 ---
    List<Hsaa310sDto> selectContractStatus(@Param("cmpycd") String cmpycd, @Param("yyyy") String yyyy, @Param("deptcd") String deptcd, @Param("item") String item);

    // --- HSAA200S 영업종합현황 ---
    List<HsaaStatDto> selectDashboardStats(@Param("cmpycd") String cmpycd, @Param("yymm") String yymm, @Param("userid") String userid, @Param("day1") String day1, @Param("day2") String day2);
    List<Hsaa200tDto> selectDashboardDetailList(
            @Param("cmpycd") String cmpycd, @Param("yymm") String yymm, @Param("userid") String userid, 
            @Param("gubun") String gubun, @Param("code") String code, @Param("code1") String code1, @Param("code2") String code2, 
            @Param("day1") String day1, @Param("day2") String day2,
            @Param("sdate") String sdate, @Param("edate") String edate, @Param("deptcd") String deptcd,
            @Param("item") String item, @Param("itemcd") String itemcd, @Param("custgbn") String custgbn,
            @Param("stdymd") String stdymd, @Param("channelkind") String channelkind);

    // --- HSAA400S 기간별 영업상담 내역 ---
    List<Hsaa300tDto> selectConsultationList(@Param("cmpycd") String cmpycd, @Param("sdate") String sdate, @Param("edate") String edate, @Param("schcustnm") String schcustnm, @Param("userid") String userid, @Param("offset") int offset, @Param("limit") int limit);
    int countConsultationList(@Param("cmpycd") String cmpycd, @Param("sdate") String sdate, @Param("edate") String edate, @Param("schcustnm") String schcustnm, @Param("userid") String userid);
    void updateSalesCoaching(Map<String, Object> params);
    void confirmCoachingRead(Map<String, Object> params);

    void syncHsba130t(Hsaa100tDto dto); // HSBA130T_TBL (통합 주소록)
    void syncPhoneNumberMap(@Param("phoneno") String phoneno, @Param("phonetype") String phonetype, @Param("custcd") String custcd, @Param("name") String name, @Param("updemp") String updemp); // phone_number_map (전화번호부)
    // void syncDiaryPlan(@Param("userid") String userid, @Param("subject") String subject, @Param("startdate") String startdate, @Param("content") String content, @Param("keyword") String keyword); // erp_diary
    
    List<Map<String, Object>> selectHsbaCodes(@Param("cmpycd") String cmpycd, @Param("cdgbn") String cdgbn);

    List<Map<String, Object>> selectActiveDealsForVisitPlan(@Param("cmpycd") String cmpycd, @Param("userid") String userid);
    List<Map<String, Object>> selectImportanceCycles(@Param("cmpycd") String cmpycd);
    List<Map<String, Object>> selectHolidaysForMonth(@Param("cmpycd") String cmpycd, @Param("ym") String ym);
    List<Map<String, Object>> selectExistingVisitPlan(@Param("cmpycd") String cmpycd, @Param("userid") String userid, @Param("ym") String ym);
    void upsertVisitPlan(Map<String, Object> params);

    Map<String, Object> selectWeeklyReport(@Param("cmpycd") String cmpycd, @Param("userid") String userid, @Param("weekid") String weekid);
    void upsertWeeklyReport(Map<String, Object> params);
    List<Map<String, Object>> selectLastWeekConsultations(@Param("cmpycd") String cmpycd, @Param("userid") String userid, @Param("sdate") String sdate, @Param("edate") String edate);
    List<Map<String, Object>> selectNextWeekVisitPlans(@Param("cmpycd") String cmpycd, @Param("userid") String userid, @Param("sdate") String sdate, @Param("edate") String edate);
}

