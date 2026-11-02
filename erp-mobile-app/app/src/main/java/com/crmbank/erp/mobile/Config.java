package com.crmbank.erp.mobile;

public class Config {
    // =====================================================================
    // 💡 접속 환경 설정 (사용할 환경의 주석을 풀고 나머지는 주석 처리하세요)
    // =====================================================================

    // [1] 노트북 로컬 개발 (에뮬레이터 사용 시)
    // 10.0.2.2는 에뮬레이터에서 노트북(호스트)으로 접속하는 가상 IP입니다.
   //  public static final String SERVER_IP = "10.0.2.2";
   //  public static final String SERVER_PORT = "8080";

    // [2] 노트북 로컬 개발 (실제 핸드폰 + 같은 와이파이 사용 시)
    // 사용자님의 노트북 IP인 172.30.1.14를 입력합니다.
    // public static final String SERVER_IP = "172.30.1.14";
    // public static final String SERVER_PORT = "8080";

    // [3] 실제 서버 테스트 (서버 172.30.1.11 / 내부망 혹은 외부망)
    // 현재는 도메인과 외부 포트(18080)를 사용하는 배포 모드를 기본으로 설정했습니다.
    public static final String SERVER_IP = "smart-erp.duckdns.org";
    public static final String SERVER_PORT = "18090";

    // =====================================================================

    // 🌐 웹뷰 화면용 포트 (필요 시)
    public static final String WEB_PORT = "18889";

    // 💡 뿌리 주소에 /api/ 를 기본으로 포함합니다.
    public static final String BASE_URL = "http://" + SERVER_IP + ":" + SERVER_PORT + "/api/";
}