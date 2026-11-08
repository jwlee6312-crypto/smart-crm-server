package com.crmbank.erp.haaa.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.haaa.mapper.HaaaMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * [HAAA] 공통코드/시스템관리 통합 컨트롤러 (사용자 정의 최종 표준형)
 */
@SuppressWarnings("unused")
@Slf4j
@RestController
@RequestMapping("/haaa")
@RequiredArgsConstructor
public class HaaaController {

    private final HaaaMapper haaaMapper;
    private final SqlSession sqlSession;

    // ==========================================
    // 1. U_STR 프로시저 (마스터/디테일 표준화)
    // ==========================================

    @PostMapping("/HAAA_010U_STR")
    public ResponseEntity<?> callHAAA_010U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
        injectSession(params, session);
        String actkind = String.valueOf(params.getOrDefault("actkind", "S1")).toUpperCase();

        if ("S1".equals(actkind) || "S2".equals(actkind) || "S3".equals(actkind) || "SR".equals(actkind)) {
            List<Map<String, Object>> raw = haaaMapper.HAAA_010U_SELECT(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("I1".equals(actkind)) {
            haaaMapper.HAAA_010U_UPSERT(params);
        } else if ("U1".equals(actkind)) {
            haaaMapper.HAAA_010U_UPDATE(params);
        }
        return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "성공")));
    }

    @PostMapping("/HAAA_800U_STR")
    public ResponseEntity<?> callHAAA_800U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
        injectSession(params, session);
        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();

        if ("S0".equals(actkind)) {
            List<Map<String, Object>> raw = haaaMapper.HAAA_800U_SELECT(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("A0".equals(actkind)) {
            haaaMapper.HAAA_800U_INSERT(params);
        } else if ("U0".equals(actkind)) {
            haaaMapper.HAAA_800U_UPDATE(params);
        } else if ("D0".equals(actkind)) {
            haaaMapper.HAAA_800U_DELETE(params);
        }
        return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "성공")));
    }

    @PostMapping("/HAAA_810U_STR")
    public ResponseEntity<?> callHAAA_810U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
        injectSession(params, session);
        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();

        if ("S0".equals(actkind) || "S1".equals(actkind) || "L".equals(actkind)) {
            List<Map<String, Object>> raw = haaaMapper.HAAA_810U_SELECT(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("A0".equals(actkind)) {
            haaaMapper.HAAA_810U_UPSERT(params);
        } else if ("U0".equals(actkind)) {
            String useyn = String.valueOf(params.getOrDefault("useyn", "Y"));
            if ("N".equalsIgnoreCase(useyn)) {
                haaaMapper.HAAA_810U_DELETE_CASCADE(params);
            } else {
                haaaMapper.HAAA_810U_UPDATE(params);
            }
        } else if ("D0".equals(actkind)) {
            haaaMapper.HAAA_810U_DELETE_CASCADE(params);
        }
        return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "성공")));
    }

    // ==========================================
    // 2. 공통 유틸리티 헬퍼 메서드
    // ==========================================

    private Map<String, Object> mapToAlias(Map<String, Object> rawRow, String col1Alias, String col2Alias) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        int i = 1;
        for (Map.Entry<String, Object> entry : rawRow.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.startsWith("col") || key.isEmpty()) {
                if (i == 1) key = col1Alias;
                else if (i == 2) key = col2Alias;
            }
            newMap.put(key, entry.getValue() == null ? "" : entry.getValue());
            i++;
        }
        return newMap;
    }

    private void injectSession(Map<String, Object> params, HttpSession session) {
        UserSession user = (UserSession) session.getAttribute("user_session");
        if (user != null) {
            if (params.get("cmpycd") == null || params.get("cmpycd").toString().trim().isEmpty()) {
                params.put("cmpycd", user.getCmpycd());
            }
            if (params.get("userid") == null || params.get("userid").toString().trim().isEmpty()) {
                params.put("userid", user.getUserid());
            }
            params.put("updemp", user.getUserid());
        }
    }

    private void fillMissingParameters(String proc, Map<String, Object> params) {
        try {
            String statementId = HaaaMapper.class.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return;
            MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(statementId);
            BoundSql boundSql = ms.getBoundSql(params);

            for (ParameterMapping pm : boundSql.getParameterMappings()) {
                String prop = pm.getProperty();
                if (prop != null && !prop.startsWith("_") && !prop.contains(".")) {
                    String cleanProp = prop.trim();
                    if (!params.containsKey(cleanProp) || params.get(cleanProp) == null || params.get(cleanProp).toString().trim().isEmpty()) {
                        params.put(cleanProp, "");
                    }
                    if (!cleanProp.equals(prop)) params.put(prop, params.get(cleanProp));
                }
            }
        } catch (Exception e) { log.warn("🛠 missing parameter alarm ({}): {}", proc, e.getMessage()); }
    }

    private String buildPositionalSql(String proc, Map<String, Object> params) {
        try {
            String statementId = HaaaMapper.class.getName() + "." + proc;
            if (!sqlSession.getConfiguration().hasStatement(statementId)) return "EXEC " + proc;
            BoundSql boundSql = sqlSession.getConfiguration().getMappedStatement(statementId).getBoundSql(params);
            List<String> values = new ArrayList<>();

            for (ParameterMapping pm : boundSql.getParameterMappings()) {
                Object val = params.get(pm.getProperty().trim());
                String valStr = (val == null || "null".equals(String.valueOf(val))) ? "''" : "N'" + val.toString().replace("'", "''").trim() + "'";
                values.add(valStr);
            }
            return String.format("EXEC %s %s", proc, String.join(", ", values));
        } catch (Exception e) { return "EXEC " + proc; }
    }

    private List<Map<String, Object>> convertToLowerCaseKeys(List<Map<String, Object>> list) {
        if (list == null) return new ArrayList<>();
        List<Map<String, Object>> newList = new ArrayList<>();
        for (Map<String, Object> map : list) {
            Map<String, Object> newMap = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                newMap.put(entry.getKey().toLowerCase(), entry.getValue());
            }
            newList.add(newMap);
        }
        return newList;
    }
}
