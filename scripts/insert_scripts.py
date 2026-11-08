import subprocess

scripts = [
    ('01_welcome', '주간 메인 인사말', '반갑습니다. 최고 품질의 컴퓨팅 솔루션, CRM BANK입니다. 정직한 견적과 최상의 서비스로 보답하겠습니다.', 'admin'),
    ('02_menu', '부서 선택 안내', '구매 및 견적 문의는 1번, 기술 지원 및 A/S 접수는 2번, 상담원 연결은 0번, 연락처를 남기시려면 4번을 눌러주세요.', 'admin'),
    ('05_callback', '콜백 접수 안내', '현재 모든 상담원이 통화 중입니다. 성함과 연락처를 남겨주시면 담당자가 확인 후 신속히 연락드리겠습니다. 삐 소리후 말씀해 주세요.', 'admin'),
    ('08_night', '야간 안내 멘트', '업무 시간이 종료되었습니다. 긴급 당직자 연결은 1번, 연락처를 남기시려면 2번을 눌러주세요.', 'admin'),
    ('09_holiday', '공휴일 안내 멘트', '공휴일 휴무 안내입니다. 긴급 기술 지원이 필요하신 경우 1번 비상 연락망으로 연결해 드립니다.', 'admin')
]

for s in scripts:
    sql = f"INSERT INTO ars_scripts (id, description, script_text, upd_user) VALUES ('{s[0]}', '{s[1]}', '{s[2]}', '{s[3]}');"
    cmd = ["isql", "-v", "asterisk-connector", "root", "gkdldhs12#$"]
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    out, err = p.communicate(input=sql)
    print(f"Result for {s[0]}: {out} {err}")
