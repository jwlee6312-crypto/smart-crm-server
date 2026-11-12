package com.crmbank.erp.comm.handler;

import com.crmbank.erp.comm.dto.UserSession;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

/**
 * 🔒 [심플 세션 체크 필터]
 * 인증 예외 경로(로그인, 세션확인, 스토리지 파일, 오디오 재생, 웹소켓 등)를 제외한 API 요청에 대해 세션 유효성 검증 수행
 */
@Slf4j
@Component
@Order(1)
public class SessionCheckFilter implements Filter {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 💡 인증 예외 처리 대상 URL 패턴 정의
    private static final String[] EXCLUDE_PATTERNS = {
        "/api/comm/login",
        "/comm/login",
        "/api/comm/session",
        "/comm/session",
        "/api/ws/cti",
        "/ws/cti",
        "/api/storage/**",
        "/storage/**",
        "/api/crm/inbound/play-recording",
        "/crm/inbound/play-recording",
        "/api/crm/cti/play-recording",
        "/crm/cti/play-recording",
        "/api/crm/inbound/trigger-popup",
        "/inbound/trigger-popup",
        "/api/crm/inbound/log-callback",
        "/inbound/log-callback"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // 🚀 녹취 파일 재생 및 루트 /api 경로는 세션 체크 무조건 면제
        if (path.equals("/api/") || path.equals("/api") || path.contains("play-recording") || path.contains("storage") || path.contains("sounds") || path.contains("login") || path.contains("session")) {
            log.info("🚀 [SessionCheckFilter PASSED] 예외 경로 통과: path = [{}]", path);
            chain.doFilter(request, response);
            return;
        }

        // 1. 예외 경로 및 비-API 요청은 필터 검증 우회
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        for (String pattern : EXCLUDE_PATTERNS) {
            if (pathMatcher.match(pattern, path) || path.contains(pattern.replace("/**", ""))) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 2. 로그인 세션 및 필수 정보(회사코드, ID) 검증
        HttpSession session = req.getSession(false);
        if (session != null) {
            UserSession user = (UserSession) session.getAttribute("user_session");
            if (user != null && user.getCmpycd() != null && user.getUserid() != null) {
                chain.doFilter(request, response);
                return;
            }
        }

        // 3. 세션 유실 시 401 반환
        log.warn("❌ [SessionCheckFilter 401 UNAUTHORIZED] 차단된 요청 경로: path = [{}]", path);
        res.setStatus(401);
    }
}
