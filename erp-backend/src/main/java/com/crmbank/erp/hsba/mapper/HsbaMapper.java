package com.crmbank.erp.hsba.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface HsbaMapper {
    List<Map<String, Object>> HSBA_010U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_010U_SELECT(Map<String, Object> params);
    int HSBA_010U_INSERT(Map<String, Object> params);
    int HSBA_010U_UPDATE(Map<String, Object> params);
    List<Map<String, Object>> HSBA_020U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_030U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_040U_STR(Map<String, Object> params);

    List<Map<String, Object>> HSBA_050U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_060U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_065U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_090U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_070U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_070U_SELECT(Map<String, Object> params);
    int HSBA_070U_UPDATE_CUST(Map<String, Object> params);
    int HSBA_070U_UPSERT_SUB(Map<String, Object> params);
    int HSBA_070U_INSERT_CUST(Map<String, Object> params);
    int HSBA_070U_INSERT_SUB(Map<String, Object> params);
    List<Map<String, Object>> HSBA_130U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_130U_SELECT(Map<String, Object> params);
    List<Map<String, Object>> HSBA_130U_SELECT_CUST(Map<String, Object> params);
    int HSBA_130U_INSERT(Map<String, Object> params);
    int HSBA_130U_UPDATE(Map<String, Object> params);
    int HSBA_130U_DELETE(Map<String, Object> params);
    List<Map<String, Object>> selectUngeocodedAddresses(@Param("cmpycd") String cmpycd);
    int updateLatLon(Map<String, Object> params);

    List<Map<String, Object>> HSBA_140U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_170U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_190U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_210U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_211U_STR(Map<String, Object> params);

    List<Map<String, Object>> HSBA_280U_STR(Map<String, Object> params);

    List<Map<String, Object>> HSBA_700U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_701U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_710U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_711U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_720U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_721U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_730U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_740U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_750U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_800U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_810U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_820U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_830U_STR(Map<String, Object> params);

    List<Map<String, Object>> HSBA_900U_STR(Map<String, Object> params);
    List<Map<String, Object>> HSBA_900U_SELECT_CODES(Map<String, Object> params);
    List<Map<String, Object>> HSBA_900U_SELECT_GROUPS(Map<String, Object> params);
    int HSBA_900U_UPSERT(Map<String, Object> params);
    int HSBA_900U_UPDATE(Map<String, Object> params);


}
