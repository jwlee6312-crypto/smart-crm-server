package com.crmbank.erp.haaa.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface HaaaMapper {
    List<Map<String, Object>> HAAA_010U_STR(Map<String, Object> params);
    List<Map<String, Object>> HAAA_010U_SELECT(Map<String, Object> params);
    int HAAA_010U_UPSERT(Map<String, Object> params);
    int HAAA_010U_UPDATE(Map<String, Object> params);

    List<Map<String, Object>> HAAA_800U_SELECT(Map<String, Object> params);
    int HAAA_800U_INSERT(Map<String, Object> params);
    int HAAA_800U_UPDATE(Map<String, Object> params);
    int HAAA_800U_DELETE(Map<String, Object> params);

    List<Map<String, Object>> HAAA_810U_SELECT(Map<String, Object> params);
    int HAAA_810U_UPSERT(Map<String, Object> params);
    int HAAA_810U_UPDATE(Map<String, Object> params);
    int HAAA_810U_DELETE_CASCADE(Map<String, Object> params);
}
