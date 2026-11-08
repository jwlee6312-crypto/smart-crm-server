package com.crmbank.erp.hsba.controller;

import com.crmbank.erp.comm.dto.UserSession;
import com.crmbank.erp.hsba.mapper.HsbaMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.SqlSession;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * [HSBA] 영업기준 통합 컨트롤러 (사용자 정의 최종 표준형)
 */
@SuppressWarnings("unused")
@Slf4j
@RestController
@RequestMapping("/hsba")
@RequiredArgsConstructor
public class HsbaController {

    private final HsbaMapper hsbaMapper;
    private final SqlSession sqlSession;

    // ==========================================
    // 1. U_STR 프로시저 (마스터/디테일 표준화)
    // ==========================================

    @PostMapping("/HSBA_010U_STR")
    public ResponseEntity<?> callHSBA_010U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
        injectSession(params, session);
        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();

        if ("S0".equals(actkind)) {
            List<Map<String, Object>> raw = hsbaMapper.HSBA_010U_SELECT(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("A0".equals(actkind)) {
            hsbaMapper.HSBA_010U_INSERT(params);
        } else if ("U0".equals(actkind)) {
            hsbaMapper.HSBA_010U_UPDATE(params);
        }
        return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "성공")));
    }

    @PostMapping("/HSBA_070U_STR")
    public ResponseEntity<?> callHSBA_070U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
        injectSession(params, session);
        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();

        if ("S0".equals(actkind) || "S1".equals(actkind)) {
            List<Map<String, Object>> raw = hsbaMapper.HSBA_070U_SELECT(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("U0".equals(actkind)) {
            hsbaMapper.HSBA_070U_UPDATE_CUST(params);
            hsbaMapper.HSBA_070U_UPSERT_SUB(params);
        } else if ("I0".equals(actkind)) {
            hsbaMapper.HSBA_070U_INSERT_CUST(params);
            hsbaMapper.HSBA_070U_INSERT_SUB(params);
        }
        return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "성공")));
    }

    @PostMapping("/HSBA_900U_STR")
    public ResponseEntity<?> callHSBA_900U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
        injectSession(params, session);
        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();

        if ("S0".equals(actkind)) {
            List<Map<String, Object>> raw = hsbaMapper.HSBA_900U_SELECT_CODES(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("S1".equals(actkind)) {
            List<Map<String, Object>> raw = hsbaMapper.HSBA_900U_SELECT_GROUPS(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("A0".equals(actkind)) {
            hsbaMapper.HSBA_900U_UPSERT(params);
        } else if ("U0".equals(actkind)) {
            hsbaMapper.HSBA_900U_UPDATE(params);
        }
        return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "성공")));
    }

//    @PostMapping("/HSBA_020U_STR")
//    public ResponseEntity<?> callHSBA_020U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_020U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_020U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_020U_STR(params);
//
//        if ( "S0".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }

//    @PostMapping("/HSBA_030U_STR")
//    public ResponseEntity<?> callHSBA_030U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_030U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_030U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_030U_STR(params);
//
//        if ( "S0".equals(actkind) || "S1".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }

//    @PostMapping("/HSBA_040U_STR")
//    public ResponseEntity<?> callHSBA_040U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_040U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_040U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_040U_STR(params);
//
//        if ("S".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_050U_STR")
//    public ResponseEntity<?> callHSBA_050U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_050U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_050U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_050U_STR(params);
//
//        if ( "S0".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_060U_STR")
//    public ResponseEntity<?> callHSBA_060U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_060U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_060U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_060U_STR(params);
//
//        if ("S".equals(actkind) || "S0".equals(actkind) || "S1".equals(actkind) || "L".equals(actkind)) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_065U_STR")
//    public ResponseEntity<?> callHSBA_065U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_065U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_065U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_065U_STR(params);
//
//        if ("S".equals(actkind) || "S0".equals(actkind) || "S1".equals(actkind) || "L".equals(actkind)) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "itemcd", "price");
//        String code = String.valueOf(resultRow.get("itemcd")).trim();
//        if ("000000".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("price")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//
//
//    @PostMapping("/HSBA_090U_STR")
//    public ResponseEntity<?> callHSBA_090U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_090U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_090U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_090U_STR(params);
//
//        if ( "S0".equals(actkind) || "S1".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
    @Transactional
    @PostMapping("/HSBA_130U_STR")
    public ResponseEntity<?> callHSBA_130U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
        injectSession(params, session);
        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();

        if ("S0".equals(actkind) || "S2".equals(actkind)) {
            List<Map<String, Object>> raw = hsbaMapper.HSBA_130U_SELECT(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("S1".equals(actkind)) {
            List<Map<String, Object>> raw = hsbaMapper.HSBA_130U_SELECT_CUST(params);
            return ResponseEntity.ok(convertToLowerCaseKeys(raw));
        } else if ("A0".equals(actkind) || "U0".equals(actkind)) {
            String address = String.valueOf(params.getOrDefault("address", ""));
            Map<String, Double> latLon = getLatLonFromAddress(address);
            params.put("latitude", latLon.get("lat"));
            params.put("longitude", latLon.get("lon"));

            if ("A0".equals(actkind)) {
                hsbaMapper.HSBA_130U_INSERT(params);
            } else {
                hsbaMapper.HSBA_130U_UPDATE(params);
            }
            return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "정상적으로 작업이 되었습니다.")));
        } else if ("D0".equals(actkind)) {
            hsbaMapper.HSBA_130U_DELETE(params);
            return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "정상적으로 작업이 되었습니다.")));
        }
        return ResponseEntity.ok(List.of(Map.of("result", "OK", "msg", "성공")));
    }

    private Map<String, Double> getLatLonFromAddress(String address) {
        Map<String, Double> coords = new HashMap<>();
        coords.put("lat", 37.5665);
        coords.put("lon", 126.9780);

        if (address == null || address.trim().isEmpty()) {
            return coords;
        }

        try {
            String encodedAddr = java.net.URLEncoder.encode(address, java.nio.charset.StandardCharsets.UTF_8);
            String urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=" + encodedAddr;
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "SmartERP-CRM/1.0");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    String json = sb.toString();
                    if (json.contains("\"lat\"") && json.contains("\"lon\"")) {
                        int latIdx = json.indexOf("\"lat\":\"");
                        if (latIdx != -1) {
                            int start = latIdx + 7;
                            int end = json.indexOf("\"", start);
                            coords.put("lat", Double.parseDouble(json.substring(start, end)));
                        }
                        int lonIdx = json.indexOf("\"lon\":\"");
                        if (lonIdx != -1) {
                            int start = lonIdx + 7;
                            int end = json.indexOf("\"", start);
                            coords.put("lon", Double.parseDouble(json.substring(start, end)));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Geocoding failed for address [{}]: {}", address, e.getMessage());
        }
        return coords;
    }
//
//    @PostMapping("/HSBA_140U_STR")
//    public ResponseEntity<?> callHSBA_140U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_140U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_140U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_140U_STR(params);
//
//        if ( "S0".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_170U_STR")
//    public ResponseEntity<?> callHSBA_170U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_170U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_170U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_170U_STR(params);
//
//        if ( "S0".equals(actkind) || "S1".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_190U_STR")
//    public ResponseEntity<?> callHSBA_190U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_190U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_190U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_190U_STR(params);
//
//        if ( "S0".equals(actkind) || "S1".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_210U_STR")
//    public ResponseEntity<?> callHSBA_210U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_210U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_210U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_210U_STR(params);
//
//        if ( "S0".equals(actkind) || "P0".equals(actkind) || "N0".equals(actkind)) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "yymm", "rowno");
//        String code = String.valueOf(resultRow.get("yymm")).trim();
//        if ("000000".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("rowno")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_211U_STR")
//    @SuppressWarnings("unchecked")
//    public ResponseEntity<?> callHSBA_211U_STR(@RequestBody Object inputDetails, HttpSession session) {
//        Map<String, Object> params = null;
//        if (inputDetails instanceof Map) {
//            params = (Map<String, Object>) inputDetails;
//        } else if (inputDetails instanceof List && !((List<?>) inputDetails).isEmpty()) {
//            params = (Map<String, Object>) ((List<?>) inputDetails).get(0);
//        }
//        if (params != null) {
//            String actkind = String.valueOf(params.getOrDefault("actkind", "S")).toUpperCase();
//            if (actkind.startsWith("S") || actkind.startsWith("L")) {
//                injectSession(params, session);
//                fillMissingParameters("HSBA_211U_STR", params);
//                List<Map<String, Object>> res = convertToLowerCaseKeys(hsbaMapper.HSBA_211U_STR(params));
//                log.info("📊 [HSBA_211U_STR SELECT 결과 (총 {}건)]: {}", res.size(), res);
//                return ResponseEntity.ok(res);
//            }
//        }
//        List<Map<String, Object>> details = new ArrayList<>();
//        if (inputDetails instanceof List) {
//            details = (List<Map<String, Object>>) inputDetails;
//        } else if (inputDetails instanceof Map) {
//            details.add((Map<String, Object>) inputDetails);
//        }
//        List<Map<String, Object>> totalResults = new ArrayList<>();
//        for (int i = 0; i < details.size(); i++) {
//            Map<String, Object> detail = details.get(i);
//            injectSession(detail, session);
//            fillMissingParameters("HSBA_211U_STR", detail);
//            log.info("📑 [Detail #{} SQL]: {}", i + 1, buildPositionalSql("HSBA_211U_STR", detail));
//            List<Map<String, Object>> raw = hsbaMapper.HSBA_211U_STR(detail);
//            if (raw != null && !raw.isEmpty()) {
//                Map<String, Object> resRow = convertToLowerCaseKeys(raw).getFirst();
//                if ("000000".equals(String.valueOf(resRow.getOrDefault("yymm", "")))) {
//                    throw new RuntimeException("상세 행 #" + (i+1) + " 오류: " + resRow.getOrDefault("rowno", "저장 실패"));
//                }
//                totalResults.add(resRow);
//            }
//        }
//        return ResponseEntity.ok(totalResults);
//    }
//
//    @PostMapping("/HSBA_280U_STR")
//    public ResponseEntity<?> callHSBA_280U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_280U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_280U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_280U_STR(params);
//
//        if ( "S0".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_700U_STR")
//    public ResponseEntity<?> callHSBA_700U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_700U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_700U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_700U_STR(params);
//
//        if ( "S0".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_701U_STR")
//    @SuppressWarnings("unchecked")
//    public ResponseEntity<?> callHSBA_701U_STR(@RequestBody Object inputDetails, HttpSession session) {
//        Map<String, Object> params = null;
//        if (inputDetails instanceof Map) {
//            params = (Map<String, Object>) inputDetails;
//        } else if (inputDetails instanceof List && !((List<?>) inputDetails).isEmpty()) {
//            params = (Map<String, Object>) ((List<?>) inputDetails).get(0);
//        }
//        if (params != null) {
//            String actkind = String.valueOf(params.getOrDefault("actkind", "S")).toUpperCase();
//            if (actkind.startsWith("S") || actkind.startsWith("L")) {
//                injectSession(params, session);
//                fillMissingParameters("HSBA_701U_STR", params);
//                List<Map<String, Object>> res = convertToLowerCaseKeys(hsbaMapper.HSBA_701U_STR(params));
//                log.info("📊 [HSBA_701U_STR SELECT 결과 (총 {}건)]: {}", res.size(), res);
//                return ResponseEntity.ok(res);
//            }
//        }
//        List<Map<String, Object>> details = new ArrayList<>();
//        if (inputDetails instanceof List) {
//            details = (List<Map<String, Object>>) inputDetails;
//        } else if (inputDetails instanceof Map) {
//            details.add((Map<String, Object>) inputDetails);
//        }
//        List<Map<String, Object>> totalResults = new ArrayList<>();
//        for (int i = 0; i < details.size(); i++) {
//            Map<String, Object> detail = details.get(i);
//            injectSession(detail, session);
//            fillMissingParameters("HSBA_701U_STR", detail);
//            log.info("📑 [Detail #{} SQL]: {}", i + 1, buildPositionalSql("HSBA_701U_STR", detail));
//            List<Map<String, Object>> raw = hsbaMapper.HSBA_701U_STR(detail);
//            if (raw != null && !raw.isEmpty()) {
//                Map<String, Object> resRow = convertToLowerCaseKeys(raw).getFirst();
//                if (!"OK".equals(String.valueOf(resRow.getOrDefault("result", "")))) {
//                    throw new RuntimeException("상세 행 #" + (i+1) + " 오류: " + resRow.getOrDefault("msg", "저장 실패"));
//                }
//                totalResults.add(resRow);
//            }
//        }
//        return ResponseEntity.ok(totalResults);
//    }
//
//    @PostMapping("/HSBA_710U_STR")
//    public ResponseEntity<?> callHSBA_710U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_710U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_710U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_710U_STR(params);
//
//        if ( "S0".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_711U_STR")
//    @SuppressWarnings("unchecked")
//    public ResponseEntity<?> callHSBA_711U_STR(@RequestBody Object inputDetails, HttpSession session) {
//        Map<String, Object> params = null;
//        if (inputDetails instanceof Map) {
//            params = (Map<String, Object>) inputDetails;
//        } else if (inputDetails instanceof List && !((List<?>) inputDetails).isEmpty()) {
//            params = (Map<String, Object>) ((List<?>) inputDetails).get(0);
//        }
//        if (params != null) {
//            String actkind = String.valueOf(params.getOrDefault("actkind", "S")).toUpperCase();
//            if (actkind.startsWith("S") || actkind.startsWith("L")) {
//                injectSession(params, session);
//                fillMissingParameters("HSBA_711U_STR", params);
//                List<Map<String, Object>> res = convertToLowerCaseKeys(hsbaMapper.HSBA_711U_STR(params));
//                log.info("📊 [HSBA_711U_STR SELECT 결과 (총 {}건)]: {}", res.size(), res);
//                return ResponseEntity.ok(res);
//            }
//        }
//        List<Map<String, Object>> details = new ArrayList<>();
//        if (inputDetails instanceof List) {
//            details = (List<Map<String, Object>>) inputDetails;
//        } else if (inputDetails instanceof Map) {
//            details.add((Map<String, Object>) inputDetails);
//        }
//        List<Map<String, Object>> totalResults = new ArrayList<>();
//        for (int i = 0; i < details.size(); i++) {
//            Map<String, Object> detail = details.get(i);
//            injectSession(detail, session);
//            fillMissingParameters("HSBA_711U_STR", detail);
//            log.info("📑 [Detail #{} SQL]: {}", i + 1, buildPositionalSql("HSBA_711U_STR", detail));
//            List<Map<String, Object>> raw = hsbaMapper.HSBA_711U_STR(detail);
//            if (raw != null && !raw.isEmpty()) {
//                Map<String, Object> resRow = convertToLowerCaseKeys(raw).getFirst();
//                if (!"OK".equals(String.valueOf(resRow.getOrDefault("result", "")))) {
//                    throw new RuntimeException("상세 행 #" + (i+1) + " 오류: " + resRow.getOrDefault("msg", "저장 실패"));
//                }
//                totalResults.add(resRow);
//            }
//        }
//        return ResponseEntity.ok(totalResults);
//    }
//
//    @PostMapping("/HSBA_720U_STR")
//    public ResponseEntity<?> callHSBA_720U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_720U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_720U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_720U_STR(params);
//
//        if ( "S0".equals(actkind)) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_721U_STR")
//    @SuppressWarnings("unchecked")
//    public ResponseEntity<?> callHSBA_721U_STR(@RequestBody Object inputDetails, HttpSession session) {
//        Map<String, Object> params = null;
//        if (inputDetails instanceof Map) {
//            params = (Map<String, Object>) inputDetails;
//        } else if (inputDetails instanceof List && !((List<?>) inputDetails).isEmpty()) {
//            params = (Map<String, Object>) ((List<?>) inputDetails).get(0);
//        }
//        if (params != null) {
//            String actkind = String.valueOf(params.getOrDefault("actkind", "S")).toUpperCase();
//            if (actkind.startsWith("S") || actkind.startsWith("L")) {
//                injectSession(params, session);
//                fillMissingParameters("HSBA_721U_STR", params);
//                List<Map<String, Object>> res = convertToLowerCaseKeys(hsbaMapper.HSBA_721U_STR(params));
//                log.info("📊 [HSBA_721U_STR SELECT 결과 (총 {}건)]: {}", res.size(), res);
//                return ResponseEntity.ok(res);
//            }
//        }
//        List<Map<String, Object>> details = new ArrayList<>();
//        if (inputDetails instanceof List) {
//            details = (List<Map<String, Object>>) inputDetails;
//        } else if (inputDetails instanceof Map) {
//            details.add((Map<String, Object>) inputDetails);
//        }
//        List<Map<String, Object>> totalResults = new ArrayList<>();
//        for (int i = 0; i < details.size(); i++) {
//            Map<String, Object> detail = details.get(i);
//            injectSession(detail, session);
//            fillMissingParameters("HSBA_721U_STR", detail);
//            log.info("📑 [Detail #{} SQL]: {}", i + 1, buildPositionalSql("HSBA_721U_STR", detail));
//            List<Map<String, Object>> raw = hsbaMapper.HSBA_721U_STR(detail);
//            if (raw != null && !raw.isEmpty()) {
//                Map<String, Object> resRow = convertToLowerCaseKeys(raw).getFirst();
//                if (!"OK".equals(String.valueOf(resRow.getOrDefault("result", "")))) {
//                    throw new RuntimeException("상세 행 #" + (i+1) + " 오류: " + resRow.getOrDefault("msg", "저장 실패"));
//                }
//                totalResults.add(resRow);
//            }
//        }
//        return ResponseEntity.ok(totalResults);
//    }
//
//    @PostMapping("/HSBA_730U_STR")
//    public ResponseEntity<?> callHSBA_730U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_730U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_730U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_730U_STR(params);
//
//        if ( "S0".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_740U_STR")
//    public ResponseEntity<?> callHSBA_740U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_740U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_740U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_740U_STR(params);
//
//        if ( "S0".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_750U_STR")
//    public ResponseEntity<?> callHSBA_750U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_750U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_750U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_750U_STR(params);
//
//        if ( "S0".equals(actkind)) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_800U_STR")
//    public ResponseEntity<?> callHSBA_800U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_800U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_800U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_800U_STR(params);
//
//        if ("S0".equals(actkind)) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_810U_STR")
//    public ResponseEntity<?> callHSBA_810U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_810U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_810U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_810U_STR(params);
//
//        if ( "S0".equals(actkind) || "S1".equals(actkind)) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_820U_STR")
//    public ResponseEntity<?> callHSBA_820U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_820U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_820U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_820U_STR(params);
//
//        if ( "S0".equals(actkind) || "S1".equals(actkind)) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }
//
//    @PostMapping("/HSBA_830U_STR")
//    public ResponseEntity<?> callHSBA_830U_STR(@RequestBody Map<String, Object> params, HttpSession session) {
//        injectSession(params, session);
//        fillMissingParameters("HSBA_830U_STR", params);
//        log.info("🏢 [Master SQL]: {}", buildPositionalSql("HSBA_830U_STR", params));
//
//        String actkind = String.valueOf(params.getOrDefault("actkind", "S0")).toUpperCase();
//        List<Map<String, Object>> raw = hsbaMapper.HSBA_830U_STR(params);
//
//        if ( "S0".equals(actkind) || "S1".equals(actkind) ) return ResponseEntity.ok(convertToLowerCaseKeys(raw));
//
//        if (raw == null || raw.isEmpty()) throw new RuntimeException("마스터 처리 결과가 없습니다.");
//
//        Map<String, Object> resultRow = mapToAlias(raw.getFirst(), "result", "msg");
//        String code = String.valueOf(resultRow.get("result")).trim();
//        if (!"OK".equals(code)) {
//            throw new RuntimeException(String.valueOf(resultRow.get("msg")));
//        }
//        return ResponseEntity.ok(List.of(resultRow));
//    }



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
            String statementId = HsbaMapper.class.getName() + "." + proc;
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
            String statementId = HsbaMapper.class.getName() + "." + proc;
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
