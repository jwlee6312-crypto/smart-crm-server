<!--
	=============================================================
	프로그램명	: 영업활동 통합 관리 (HSAA100U)
	작성일자	: 2025.03.14
	작성자      : AI Assistant
	설명        : 좌측 500px 고정 리스트 + 우측 상하 분할(폼/그리드) 탭 시스템
                (HSOD100U 표준 레이아웃 완벽 준수 및 누락 항목 전수 복원)
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />
  <Modal v-model:visible="modalVisible" :modalProps="modalProps" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 글로벌 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom px-2 bg-white">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-briefcase-fill me-2 text-primary" style="font-size: 18px;"></i>
        영업관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        영업활동 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">영업활동등록 (HSAA100U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initializeAll">전체 초기화</button>
        <button class="btn-erp btn-search" @click="searchMaster">전체 조회</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 (HSOD100U 표준: 수직 Flex) -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 통합 검색 필터 (균등 배분) -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 10%;" /><col style="width: 23%;" />
              <col style="width: 10%;" /><col style="width: 23%;" />
              <col style="width: 10%;" /><col style="width: 24%;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light fw-bold">등록기간</th>
                <td class="d-flex align-items-center border-0 gap-1" style="height: 32px;">
                  <DateForm v-model:fromdt="filter.fromdt" v-model:todt="filter.todt" />
                </td>
                <th class="text-center bg-light border-start fw-bold">거래처/건명</th>
                <td>
                  <input v-model="filter.schcustnm" class="form-control form-control-sm" placeholder="검색어 입력" @keyup.enter="searchMaster" />
                </td>
                <th class="text-center bg-light border-start fw-bold">영업담당</th>
                <td>
                  <select v-model="filter.userid" class="form-select form-select-sm" style="width: 150px;">
                    <option value="">전체</option>
                    <option v-for="user in userData" :key="user.userid" :value="user.userid">{{ user.usernm }}</option>
                  </select>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [하단] 2단 가로 분할 레이아웃 (HSOD100U 핵심 구조) -->
      <div class="d-flex flex-row gap-2 flex-grow-1 overflow-hidden bottom-layout-container" style="min-height: 0;">

        <!-- ⬅️ 좌측: 통합 마스터 리스트 (500px 너비 고정) -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden master-column-fixed">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center">
            <span class="fw-bold small text-dark">통합 영업 기회 리스트</span>
            <span class="badge bg-primary-subtle text-primary x-small">{{ masterListCount }} 건</span>
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
            <div ref="masterGridRef" class="tabulator-instance flex-grow-1"></div>
          </div>
        </div>

        <!-- ➡️ 우측: 탭 기반 작업 영역 (상: 편집폼 / 하: 이력그리드) -->
        <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden detail-column-expand" style="min-width: 0;">

          <!-- (A) 기능 탭 네비게이션 -->
          <div class="card border shadow-sm flex-shrink-0 bg-white">
            <div class="card-header bg-white p-0 border-0">
              <ul class="nav nav-tabs border-0 px-3 pt-1">
                <li class="nav-item" v-for="tab in tabs" :key="tab.id">
                  <a class="nav-link border-0 fw-bold small py-2 px-3"
                     :class="{ 'active text-primary border-bottom border-primary border-3': activeTab === tab.id }"
                     href="#" @click.prevent="switchTab(tab.id)">
                    <span class="me-1 fw-bolder">{{ tab.no }}.</span>{{ tab.name }}
                  </a>
                </li>
              </ul>
            </div>
          </div>

          <!-- (B) 탭별 독립 컨텐츠 (상하 분할 상세) -->
          <div class="flex-grow-1 d-flex flex-column gap-2 overflow-hidden">

            <!-- 🛑 상단: 상세 입력 폼 (HSAA100U 항목 전수 반영) -->
            <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
                <div class="card-header bg-white py-1 px-3 border-bottom d-flex justify-content-between align-items-center flex-shrink-0">
                    <span class="small fw-bold text-primary"><i class="bi bi-pencil-square me-1"></i> {{ currentTabName }} 상세 입력</span>
                    <div class="btn-group-erp d-flex gap-1">
                        <button class="btn-erp btn-init" @click="resetCurrentTab">신규</button>
                        <button class="btn-erp btn-save" @click="saveCurrentTab">저장</button>
                        <button class="btn-erp btn-delete" @click="deleteCurrentTab" :disabled="isDeleteDisabled">삭제</button>
                    </div>
                </div>
                <div class="card-body p-0 bg-white overflow-auto scrollbar-sm" style="max-height: 450px;">
                    <!-- 1. 영업건 등록 폼 (마스터) -->
                    <table v-if="activeTab === 'SALES'" class="erp-table-dense w-100">
                        <colgroup><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /></colgroup>
                        <tbody>
                            <tr class="bg-primary bg-opacity-5">
                                <th class="bg-light">거래처명</th>
                                <td colspan="3"><div class="input-group input-group-sm"><input v-model="formSales.custnm" class="form-control fw-bold text-primary" readonly /><button class="btn btn-outline-secondary" @click="handleOpenHelp('CUST')"><i class="bi bi-search"></i></button></div></td>
                                <th class="bg-light border-start">대표자</th><td><input v-model="formSales.bossnm" class="form-control" readonly /></td>
                                <th class="bg-light border-start">전화번호</th><td><input v-model="formSales.telno" class="form-control" readonly /></td>
                            </tr>
                            <tr>
                                <th class="bg-light">회사주소</th>
                                <td colspan="5">
                                    <AddressPopupForm
                                        v-model:trancd="formSales.trancd"
                                        v-model:postno="formSales.postno"
                                        v-model:address="formSales.address"
                                        v-model:d_address="formSales.d_address"
                                        @open-address="handleOpenHelp('ADDR')"
                                    />
                                </td>
                                <th class="bg-light border-start">팩스번호</th><td><input v-model="formSales.faxno" class="form-control" readonly /></td>
                            </tr>
                            <tr>
                                <th class="required bg-light">영업건명</th><td colspan="3"><input v-model="formSales.salestitle" class="form-control border-primary-subtle fw-bold" /></td>
                                <th class="bg-light border-start">영업ID</th><td><input v-model="formSales.salesid" class="form-control bg-light text-center fw-bold text-primary" readonly placeholder="자동생성" /></td>
                                <th class="required bg-light border-start">등록일자</th><td><input v-model="formSales.addtime" type="date" class="form-control" /></td>
                            </tr>
                            <tr>
                                <th class="required bg-light">영업담당</th>
                                <td><select v-model="formSales.userid" class="form-select"><option v-for="u in userData" :key="u.userid" :value="u.userid">{{ u.usernm }}</option></select></td>
                                <th class="bg-light border-start">담당부서</th><td><input v-model="formSales.deptnm" class="form-control bg-light" readonly /></td>
                                <th class="required bg-light border-start text-danger">진행상태</th>
                                <td><select v-model="formSales.state" class="form-select border-danger-subtle"><option value="">선택</option><option v-for="s in stateData" :key="s.code" :value="s.code">{{ s.cdnm }}</option></select></td>
                                <th class="required bg-light border-start">중요도</th>
                                <td><select v-model="formSales.importrank" class="form-select"><option value="">선택</option><option v-for="r in rankData" :key="r.code" :value="r.code">{{ r.cdnm }}</option></select></td>
                            </tr>
                            <tr>
                                <th class="required bg-light">예상수주액</th><td><input :value="fComma(formSales.foreamt)" @input="e => formSales.foreamt = pComma(e.target.value)" class="form-control text-end" /></td>
                                <th class="bg-light border-start">실수주액</th><td><input :value="fComma(formSales.realamt)" class="form-control text-end fw-bold text-success" readonly /></td>
                                <th class="required bg-light border-start">예상수주일</th><td><input v-model="formSales.foredt" type="date" class="form-control" /></td>
                                <th class="bg-light border-start text-danger">유치경로</th><td><select v-model="formSales.rtncd" class="form-select border-danger-subtle"><option value="">선택</option><option v-for="r in rtnData" :key="r.code" :value="r.code">{{ r.cdnm }}</option></select></td>
                            </tr>
                            <tr>
                                <th class="required bg-light">수주확도</th>
                                <td><div class="input-group input-group-sm"><input v-model="formSales.succrate" type="number" class="form-control text-center" /><span class="input-group-text">%</span></div></td>
                                <th class="bg-light border-start">사용용도</th><td><select v-model="formSales.usecd" class="form-select"><option value="">선택</option><option v-for="u in useData" :key="u.code" :value="u.code">{{ u.cdnm }}</option></select></td>
                                <th class="bg-light border-start">설치예정</th><td><input v-model="formSales.foredelivdt" type="date" class="form-control" /></td>
                                <th class="bg-light border-start">가격조건</th><td><input v-model="formSales.conditions" class="form-control" /></td>
                            </tr>
                            <tr>
                                <th class="bg-light border-start">선정방법</th><td><select v-model="formSales.choice" class="form-select"><option value="">선택</option><option v-for="c in choiceData" :key="c.code" :value="c.code">{{ c.cdnm }}</option></select></td>
                                <th class="bg-light border-start text-muted">최종접촉일</th><td><input v-model="formSales.lastmtdt" type="date" class="form-control" readonly /></td>
                                <th class="bg-light border-start">상세내용</th><td colspan="3"><textarea v-model="formSales.salesremark" class="form-control" rows="5" style="resize: vertical;"></textarea></td>
                            </tr>
                            <tr v-if="formSales.state === '900'" class="bg-success bg-opacity-5">
                                <th class="bg-light text-success fw-bold">성공사유</th><td><select v-model="formSales.wincd" class="form-select"><option value="">선택</option><option v-for="w in winData" :key="w.code" :value="w.code">{{ w.cdnm }}</option></select></td>
                                <th class="bg-light border-start text-success fw-bold">수주일자</th><td><input v-model="formSales.realdt" type="date" class="form-control" /></td>
                                <th class="bg-light border-start text-success fw-bold">실수주액</th><td colspan="3"><input v-model="formSales.realamt" type="number" class="form-control text-end text-success" /></td>
                            </tr>
                            <tr v-if="formSales.state === '910' || formSales.state === '930'" class="bg-danger bg-opacity-5">
                                <th class="bg-light text-danger fw-bold">실패사유</th><td><select v-model="formSales.failcd" class="form-select"><option value="">선택</option><option v-for="f in failData" :key="f.code" :value="f.code">{{ f.cdnm }}</option></select></td>
                                <th class="bg-light border-start text-danger fw-bold">실패일자</th><td colspan="5"><input v-model="formSales.faildt" type="date" class="form-control w-25" /></td>
                            </tr>
                            <tr v-if="formSales.state === '920'" class="bg-warning bg-opacity-5">
                                <th class="bg-light text-warning fw-bold">보류사유</th><td><select v-model="formSales.holdcd" class="form-select"><option value="">선택</option></select></td>
                                <th class="bg-light border-start text-warning fw-bold">보류기한</th><td colspan="5"><input v-model="formSales.holdondt" type="date" class="form-control w-25" /></td>
                            </tr>
                        </tbody>
                    </table>

                    <!-- 2. 상담일지 등록 폼 -->
                    <table v-if="activeTab === 'DIARY'" class="erp-table-dense w-100">
                        <colgroup><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /></colgroup>
                        <tbody>
                            <tr>
                                <th class="required bg-light">상담일시</th>
                                <td><div class="d-flex gap-1"><input type="date" v-model="formDiary.contdt" class="form-control" /><input type="number" v-model="formDiary.conthh" class="form-control w-25" placeholder="H" /><input type="number" v-model="formDiary.contmm" class="form-control w-25" placeholder="M" /></div></td>
                                <th class="required bg-light border-start">상담채널</th>
                                <td><div class="d-flex gap-1"><select v-model="formDiary.channel" class="form-select"><option value="">선택</option><option v-for="c in channelData" :key="c.code" :value="c.code">{{ c.cdnm }}</option></select><select v-show="formDiary.channel === '010'" v-model="formDiary.channelkind" class="form-select"><option value="1">신규</option><option value="2">재방문</option></select></div></td>
                                <th class="required bg-light border-start">상담고객</th><td><select v-model="formDiary.custid" class="form-select"><option v-for="k in keymanData" :key="k.custid" :value="k.custid">{{ k.name }}</option></select></td>
                            </tr>
                            <tr>
                                <th class="bg-light text-muted">연락처</th><td><div class="x-small fw-bold">HP: {{ formDiary.hpno }} | OFF: {{ formDiary.custtel }}</div></td>
                                <th class="bg-light border-start text-primary">단계변동</th><td><select v-model="formDiary.tostate" class="form-select border-primary-subtle"><option value="">변동없음</option><option v-for="s in stateData" :key="s.code" :value="s.code">{{ s.cdnm }}</option></select></td>
                                <th class="bg-light border-start">확인요청</th><td><div class="d-flex align-items-center gap-1 ps-2"><input type="checkbox" v-model="formDiary.reportyn" true-value="Y" false-value="N" class="form-check-input" /><span class="x-small fw-bold text-danger">관리자 보고</span></div></td>
                            </tr>
                            <tr v-if="formDiary.tostate === '900'" class="bg-success bg-opacity-5">
                                <th class="bg-light text-success fw-bold">성공사유</th><td><select v-model="formDiary.wincd" class="form-select"><option value="">선택</option><option v-for="w in winData" :key="w.code" :value="w.code">{{ w.cdnm }}</option></select></td>
                                <th class="bg-light border-start text-success fw-bold">수주일자</th><td><input v-model="formDiary.realdt" type="date" class="form-control" /></td>
                                <th class="bg-light border-start text-success fw-bold">실수주액</th><td><input :value="fComma(formDiary.realamt)" @input="e => formDiary.realamt = pComma(e.target.value)" class="form-control text-end text-success" /></td>
                            </tr>
                            <tr v-if="formDiary.tostate === '910' || formDiary.tostate === '930'" class="bg-danger bg-opacity-5">
                                <th class="bg-light text-danger fw-bold">실패사유</th><td colspan="5"><select v-model="formDiary.failcd" class="form-select w-50"><option value="">선택</option><option v-for="f in failData" :key="f.code" :value="f.code">{{ f.cdnm }}</option></select></td>
                            </tr>
                            <tr v-if="formDiary.tostate === '920'" class="bg-warning bg-opacity-5">
                                <th class="bg-light text-warning fw-bold">보류사유</th><td><select v-model="formDiary.holdcd" class="form-select w-50"><option value="1">예산확확보지연</option></select></td>
                                <th class="bg-light border-start text-warning fw-bold">보류기한</th><td colspan="3"><input v-model="formDiary.holdondt" type="date" class="form-control" /></td>
                            </tr>
                            <tr v-if="formDiary.reportyn === 'Y'">
                                <th class="bg-primary bg-opacity-10 text-primary fw-bold border-top border-bottom">보고내용</th>
                                <td colspan="5" class="p-0 border-top border-bottom">
                                    <textarea v-model="formDiary.reportcontent" class="form-control border-0 rounded-0 bg-primary bg-opacity-5" rows="3" placeholder="관리자에게 보고할 핵심 요지나 요청사항을 입력하세요."></textarea>
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light border-top">후속조치</th>
                                <td colspan="3"><input v-model="formDiary.actionitem" type="text" class="form-control form-control-sm" placeholder="예: 견적서 재발송 및 상세 스펙 전달" /></td>
                                <th class="bg-light border-start border-top">조치완료여부</th>
                                <td class="border-top">
                                    <div class="form-check form-switch m-0">
                                        <input v-model="formDiary.actionstatus" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="hsaa100ActionStatus">
                                        <label class="form-check-label ms-1 small fw-bold" for="hsaa100ActionStatus">{{ formDiary.actionstatus === 'Y' ? '완료' : '미해결' }}</label>
                                    </div>
                                </td>
                            </tr>
                            <tr>
                                <th class="bg-light">다음미팅일자</th>
                                <td><input type="date" v-model="formDiary.nextmeetdt" class="form-control form-control-sm" /></td>
                                <th class="bg-light border-start">다음미팅안건</th>
                                <td colspan="3"><input v-model="formDiary.nextmeetagenda" type="text" class="form-control form-control-sm" placeholder="다음 미팅 안건" /></td>
                            </tr>
                            <tr>
                                <th class="bg-light border-top border-bottom">상담본문</th>
                                <td colspan="5" class="p-0 border-top border-bottom">
                                    <textarea v-model="formDiary.diarycontent" class="form-control border-0 rounded-0" style="min-height: 250px; resize: vertical;" placeholder="상담 내용을 상세히 기록하세요."></textarea>
                                </td>
                            </tr>
                            <tr v-show="formDiary.salescoaching" class="bg-success bg-opacity-10">
                                <th class="bg-success bg-opacity-25 text-success fw-bold border-top">상담코칭</th>
                                <td colspan="5" class="p-3 border-top">
                                    <div class="d-flex justify-content-between align-items-center mb-2">
                                        <div class="fw-bold text-success"><i class="bi bi-chat-left-quote-fill me-1"></i> 관리자 지시 및 코칭사항 ({{ fYmd(formDiary.reportdt) }})</div>
                                        <button v-if="formDiary.coachingreadyn !== 'Y'" class="btn btn-xs btn-success fw-bold" @click="confirmCoachingRead">지시사항 확인</button>
                                        <span v-else class="badge bg-success small"><i class="bi bi-check-circle-fill me-1"></i> 확인완료</span>
                                    </div>
                                    <div class="p-2 bg-white border rounded shadow-sm" style="white-space: pre-wrap; min-height: 80px;">{{ formDiary.salescoaching }}</div>
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <!-- 3. Key맨 등록 폼 (개인정보 전수 복원) -->
                    <table v-if="activeTab === 'KEYMAN'" class="erp-table-dense w-100">
                        <colgroup><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /></colgroup>
                        <tbody>
                            <tr>
                                <th class="required bg-light">고객성명</th><td><input v-model="formKeyman.name" class="form-control" /></td>
                                <th class="bg-light border-start">부서/직위</th><td><div class="d-flex gap-1"><input v-model="formKeyman.custdept" class="form-control" /><input v-model="formKeyman.jikch" class="form-control" /></div></td>
                                <th class="required bg-light border-start text-danger">핸드폰</th><td><input v-model="formKeyman.hpno" class="form-control border-danger-subtle fw-bold" /></td>
                            </tr>
                            <tr>
                                <th class="bg-light border-start">직통전화</th><td><div class="input-group input-group-sm"><input v-model="formKeyman.custtel" class="form-control" /><span class="input-group-text">내선</span><input v-model="formKeyman.innumber" class="form-control" style="width: 50px;" /></div></td>
                                <th class="bg-light border-start fw-bold">개인메일(1)</th><td><input v-model="formKeyman.mail" class="form-control" /></td>
                                <th class="bg-light border-start fw-bold">개인메일(2)</th><td><input v-model="formKeyman.mail2" class="form-control" /></td>
                            </tr>
                            <tr>
                                <th class="bg-light">생일/구분</th><td><div class="d-flex gap-1"><input v-model="formKeyman.birthday" type="date" class="form-control" /><select v-model="formKeyman.birgb" class="form-select w-25"><option value="1">양력</option><option value="2">음력</option></select></div></td>
                                <th class="required bg-light border-start">KEYMAN구분</th><td><select v-model="formKeyman.keyman" class="form-select"><option value="">선택</option><option v-for="k in keymanGbData" :key="k.code" :value="k.code">{{ k.cdnm }}</option></select></td>
                                <th class="bg-light border-start">관리등급</th><td><select v-model="formKeyman.level" class="form-select"><option value="100">상</option><option value="200">중</option></select></td>
                            </tr>
                            <tr>
                                <th class="bg-light">주요주소</th><td colspan="3"><input v-model="formKeyman.haddress" class="form-control" /></td>
                                <th class="bg-light border-start">호감도</th><td><select v-model="formKeyman.favor" class="form-select"><option value="">선택</option><option v-for="f in favorData" :key="f.code" :value="f.code">{{ f.cdnm }}</option></select></td>
                            </tr>
                            <tr>
                                <th class="bg-light border-bottom">특이사항</th><td colspan="5" class="border-bottom"><input v-model="formKeyman.remark" class="form-control" /></td>
                            </tr>
                        </tbody>
                    </table>

                    <!-- 4. 단계변동 등록 폼 -->
                    <table v-if="activeTab === 'STAGES'" class="erp-table-dense w-100">
                        <colgroup><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /><col style="width: 100px;" /><col /></colgroup>
                        <tbody>
                            <tr>
                                <th class="required bg-light">이전상태</th>
                                <td>
                                    <select v-model="formStage.bfstate" class="form-select">
                                        <option value="">선택</option>
                                        <option v-for="s in stateData" :key="s.code" :value="s.code">{{ s.cdnm }}</option>
                                    </select>
                                </td>
                                <th class="required bg-light border-start">변동일자</th><td><input v-model="formStage.chngdt" type="date" class="form-control" /></td>
                                <th class="required bg-light border-start text-primary fw-bold">변동상태</th>
                                <td>
                                    <select v-model="formStage.state" class="form-select border-primary-subtle fw-bold">
                                        <option value="">선택</option>
                                        <option v-for="s in stateData" :key="s.code" :value="s.code">{{ s.cdnm }}</option>
                                    </select>
                                </td>
                            </tr>
                            <tr v-if="formStage.state === '900'" class="bg-success bg-opacity-5">
                                <th class="bg-light text-success fw-bold">성공사유</th><td><select v-model="formStage.wincd" class="form-select"><option value="">선택</option><option v-for="w in winData" :key="w.code" :value="w.code">{{ w.cdnm }}</option></select></td>
                                <th class="bg-light border-start text-success fw-bold">수주일자</th><td><input v-model="formStage.realdt" type="date" class="form-control" /></td>
                                <th class="bg-light border-start text-success fw-bold">실수주액</th><td><input :value="fComma(formStage.realamt)" @input="e => formStage.realamt = pComma(e.target.value)" class="form-control text-end text-success" /></td>
                            </tr>
                            <tr v-if="formStage.state === '910' || formStage.state === '930'" class="bg-danger bg-opacity-5">
                                <th class="bg-light text-danger fw-bold">실패사유</th><td colspan="5"><select v-model="formStage.failcd" class="form-select w-50"><option value="">선택</option><option v-for="f in failData" :key="f.code" :value="f.code">{{ f.cdnm }}</option></select></td>
                            </tr>
                            <tr v-if="formStage.state === '920'" class="bg-warning bg-opacity-5">
                                <th class="bg-light text-warning fw-bold">보류사유</th><td colspan="5"><select v-model="formStage.holdcd" class="form-select w-50"><option value="1">예산확확보지연</option></select></td>
                            </tr>
                            <tr>
                                <th class="required bg-light border-bottom">변동사유</th><td colspan="5" class="border-bottom"><input v-model="formStage.remark" class="form-control" placeholder="상태 변동에 대한 상세 사유 기록" /></td>
                            </tr>
                        </tbody>
                    </table>

                    <!-- 5. 영업문서 등록 폼 -->
                    <table v-if="activeTab === 'DOCS'" class="erp-table-dense w-100">
                        <tbody>
                            <tr>
                                <th class="bg-light" style="width: 90px;">문서번호</th>
                                <td style="width: 220px;">
                                    <div class="d-flex align-items-center gap-1">
                                        <input v-model="formDocs.regdt" type="date" class="form-control form-control-sm" style="width: 130px;" />
                                        <span>-</span>
                                        <input v-model="formDocs.ser" class="form-control form-control-sm text-center" placeholder="001" style="width: 60px;" maxlength="3" />
                                    </div>
                                </td>
                                <th class="bg-light" style="width: 90px;">문서유형</th>
                                <td style="width: 120px;"><select v-model="formDocs.docgb" class="form-select form-select-sm"><option value="">선택</option><option v-for="d in docGbData" :key="d.code" :value="d.code">{{ d.cdnm }}</option></select></td>
                                <th class="bg-light border-start" style="width: 90px;">문서제목</th>
                                <td><input v-model="formDocs.title" class="form-control form-control-sm" /></td>
                                <th class="bg-light border-start" style="width: 90px;">파일업로드</th>
                                <td><input type="file" class="form-control form-control-sm" @change="handleFileChange" /></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- 🛑 하단: 관련 데이터 리스트 (HSOD100U 표준) -->
            <div class="card border shadow-sm flex-grow-1 overflow-hidden d-flex flex-column">
                <div class="card-header bg-white py-1 px-3 border-bottom small fw-bold text-dark d-flex align-items-center justify-content-between flex-shrink-0">
                    <span><i class="bi bi-list-columns me-2 text-primary"></i>관련 내역 히스토리</span>
                    <div v-if="activeTab === 'SALES'" class="btn-group-erp d-flex gap-1">
                        <button class="btn-erp btn-init py-0 px-2" style="height: 22px; font-size: 11px;" @click="addItemRow">행추가</button>
                        <button class="btn-erp btn-delete py-0 px-2" style="height: 22px; font-size: 11px;" @click="deleteItemRow">행삭제</button>
                    </div>
                </div>
                <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                    <div v-show="activeTab === 'SALES'" ref="itemGridRef" class="tabulator-instance flex-grow-1"></div>
                    <div v-show="activeTab === 'DIARY'" ref="diaryGridRef" class="tabulator-instance flex-grow-1"></div>
                    <div v-show="activeTab === 'KEYMAN'" ref="keymanGridRef" class="tabulator-instance flex-grow-1"></div>
                    <div v-show="activeTab === 'STAGES'" ref="stageGridRef" class="tabulator-instance flex-grow-1"></div>
                    <div v-show="activeTab === 'DOCS'" ref="docsGridRef" class="tabulator-instance flex-grow-1"></div>
                </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed, watch } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import DateForm from '@/components/DateForm.vue'
import AddressPopupForm from '@/components/AddressPopupForm.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { API_URL } from '@/config/api'
import { useFormReset } from '@/composables/useFormReset'
import { useCommonHelp } from '@/composables/useCommonHelp'
import { getDate } from '@/composables/useDate'

const authStore = useAuthStore()
const { firstDay, today } = getDate()
const { showAlert, showError, vAlert, vAlertError, alertMessage } = useAlerts()
const { resetForm } = useFormReset()
const { modalVisible, modalProps, openHelp } = useCommonHelp()

// 📋 상태 데이터
const activeTab = ref('SALES')
const masterListCount = ref(0)
const stateData = ref<any[]>([]); const userData = ref<any[]>([]); const deptData = ref<any[]>([])
const channelData = ref<any[]>([]); const favorData = ref<any[]>([]); const rankData = ref<any[]>([]); const choiceData = ref<any[]>([])
const winData = ref<any[]>([]); const failData = ref<any[]>([]); const docGbData = ref<any[]>([])
const keymanGbData = ref<any[]>([]); const rtnData = ref<any[]>([]); const useData = ref<any[]>([])
const keymanData = ref<any[]>([])

const tabs = [
  { id: 'SALES', no: 1, name: '영업건 등록', icon: 'bi-pencil-square' },
  { id: 'DIARY', no: 2, name: '상담일지 등록', icon: 'bi-journal-text' },
  { id: 'KEYMAN', no: 3, name: 'Key맨 등록', icon: 'bi-people' },
  { id: 'STAGES', no: 4, name: '단계변동 등록', icon: 'bi-arrow-left-right' },
  { id: 'DOCS', no: 5, name: '영업문서 등록', icon: 'bi-file-earmark-text' }
]

const currentTabName = computed(() => tabs.find(t => t.id === activeTab.value)?.name || '')
const isDeleteDisabled = computed(() => {
    if (activeTab.value === 'SALES') return !formSales.salesid
    if (activeTab.value === 'DIARY') return !formDiary.ser
    if (activeTab.value === 'KEYMAN') return !formKeyman.custid
    return true
})

// 📝 데이터 모델 (전수 복원)
const filter = reactive({ fromdt: firstDay, todt: today, schcustnm: '', userid: '' })
const formSales = reactive<any>({
    cmpycd: authStore.cmpycd, salesid: '', salestitle: '', state: '100', addtime: today,
    custcd: '', custnm: '', bossnm: '', telno: '', faxno: '', address: '', postno: '', d_address: '', trancd: '',
    userid: authStore.userid, deptnm: authStore.deptnm, deptcd: authStore.deptcd,
    foreamt: 0, realamt: 0, foredt: today, succrate: 30, rtncd: '100', conditions: '', lastmtdt: today,
    salesremark: '', choice: '200', importrank: '200', foredelivdt: today, svcno: '',
    wincd: '', failcd: '', holdcd: '', realdt: today, faildt: today, holdondt: today
})
const formDiary = reactive<any>({ ser: '', contdt: today, conthh: 10, contmm: 0, channel: '020', channelkind: '', custid: '', diarycontent: `[상담 목적]\n- \n\n[상담 내용]\n- \n\n[향후 계획/요청사항]\n- `, content: '', tostate: '', remark: '', reportyn: 'N', reportcontent: '', hpno: '', custtel: '', wincd: '', failcd: '', holdcd: '', realamt: 0, realdt: today, holdondt: today, salescoaching: '', reportdt: '', actionitem: '', actionstatus: 'N', nextmeetdt: '', nextmeetagenda: '' })
const formKeyman = reactive<any>({ custid: '', name: '', hpno: '', custdept: '', jikch: '', mail: '', mail2: '', custtel: '', innumber: '', birthday: today, birgb: '1', keyman: '040', level: '200', favor: '720', haddress: '', remark: '' })
const formStage = reactive<any>({ ser: '', chngdt: today, bfstatenm: '', state: '', remark: '', wincd: '', failcd: '', holdcd: '', realamt: 0, realdt: today, bfstate: '' })
const formDocs = reactive<any>({ regdt: today, ser: '', docgb: '', title: '', filename: '', file: null })

// 영업담당자 변경 시 부서 자동 매핑
watch(() => formSales.userid, (newVal) => {
  if (newVal) {
    const user = userData.value.find(u => u.userid === newVal)
    if (user) {
      formSales.deptnm = user.deptnm
      formSales.deptcd = user.deptcd
    }
  }
})

// 📊 그리드 인스턴스
const masterGridRef = ref(null); let gridMaster: Tabulator | null = null
const diaryGridRef = ref(null); let gridDiary: Tabulator | null = null
const itemGridRef = ref(null); let gridItems: Tabulator | null = null
const keymanGridRef = ref(null); let gridKeyman: Tabulator | null = null
const stageGridRef = ref(null); let gridStages: Tabulator | null = null
const docsGridRef = ref(null); let gridDocs: Tabulator | null = null

const initializeAll = () => {
  resetForm(formSales);
  resetForm(formDiary);
  resetForm(formKeyman);
  resetForm(formStage);
  gridMaster?.clearData();
  gridItems?.clearData();
  gridDiary?.clearData();
  gridKeyman?.clearData();
  gridStages?.clearData();
  gridDocs?.clearData();
  vAlert('전체 초기화 완료');
}

const resetCurrentTab = () => {
  if (activeTab.value === 'SALES') {
    resetForm(formSales)
    formSales.cmpycd = authStore.cmpycd
    formSales.userid = authStore.userid
    formSales.deptnm = authStore.deptnm
    formSales.deptcd = authStore.deptcd
    formSales.addtime = today
    formSales.foredt = today
    formSales.foredelivdt = today
    formSales.lastmtdt = ''
    formSales.realdt = ''
    formSales.faildt = ''
    formSales.holdondt = ''
    formSales.state = '100'
    formSales.rtncd = '100'
    formSales.choice = '200'
    formSales.importrank = '200'
    gridItems?.clearData()
  } else if (activeTab.value === 'DIARY') {
    resetForm(formDiary)
    formDiary.contdt = today
    formDiary.conthh = 10
    formDiary.contmm = 0
    formDiary.content = `[상담 목적]\n- \n\n[상담 내용]\n- \n\n[향후 계획/요청사항]\n- `
  } else if (activeTab.value === 'STAGES') {
    resetForm(formStage)
    formStage.chngdt = today
    formStage.realdt = ''
    formStage.bfstate = formSales.state
  } else if (activeTab.value === 'KEYMAN') {
    resetForm(formKeyman)
  } else if (activeTab.value === 'STAGES') {
    resetForm(formStage)
  } else if (activeTab.value === 'DOCS') {
    resetForm(formDocs)
    formDocs.regdt = today
    formDocs.ser = ''
  }
}

const searchMaster = async () => {
  try {
    const params = {
      fromdt: filter.fromdt.replace(/-/g, ''),
      todt: filter.todt.replace(/-/g, ''),
      schcustnm: filter.schcustnm,
      userid: filter.userid
    }
    const res = await api.get('/hsaa/master', { params })
    const data = res.data || []
    gridMaster?.setData(data);
    masterListCount.value = data.length
  } catch (e: any) {
    vAlertError('조회 중 오류가 발생했습니다.')
  }
}

const fYmd = (d: string) => (d && d.length === 8) ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : d;
const fComma = (val: any) => {
    if (val === undefined || val === null || val === '') return '';
    return val.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}
const pComma = (val: string) => {
    return val.replace(/,/g, "");
}

const fetchSubData = async (row: any) => {
    // UI 표시를 위해 일자 데이터 포맷팅 (YYYYMMDD -> YYYY-MM-DD)
    const formattedRow = { ...row,
        addtime: fYmd(row.addtime),
        foredt: fYmd(row.foredt),
        foredelivdt: fYmd(row.foredelivdt),
        lastmtdt: fYmd(row.lastmtdt),
        realdt: fYmd(row.realdt),
        faildt: fYmd(row.faildt),
        holdondt: fYmd(row.holdondt)
    }
    Object.assign(formSales, formattedRow)
    const salesid = row.salesid
    const custcd = row.custcd

    try {
      // 1. 상세 & 품목
      const resDetail = await api.get('/hsaa/detail', { params: { salesid } })
      if (resDetail.data && resDetail.data.master) {
        const m = resDetail.data.master
        Object.assign(formSales, { ...m,
            addtime: fYmd(m.addtime),
            foredt: fYmd(m.foredt),
            foredelivdt: fYmd(m.foredelivdt),
            lastmtdt: fYmd(m.lastmtdt),
            realdt: fYmd(m.realdt),
            faildt: fYmd(m.faildt),
            holdondt: fYmd(m.holdondt)
        })
        gridItems?.setData(resDetail.data.items || [])
      }

      // 2. 상담일지
      const resDiary = await api.get('/hsaa/diary', { params: { salesid } })
      const formattedDiary = (resDiary.data || []).map((d: any) => ({ ...d,
        contdt: fYmd(d.contdt),
        nextmeetdt: fYmd(d.nextmeetdt)
      }))
      gridDiary?.setData(formattedDiary)

      // 3. Key맨 (거래처 코드 기준)
      if (custcd) {
        const resKeyman = await api.get('/hsaa/keyman', { params: { custcd } })
        keymanData.value = (resKeyman.data || []).map((k: any) => ({ ...k, birthday: fYmd(k.birthday) }))
        gridKeyman?.setData(keymanData.value)
      }

      // 4. 단계변동
      const resStages = await api.get('/hsaa/stages', { params: { salesid, custcd } })
      const formattedStages = (resStages.data || []).map((s: any) => ({ ...s, chngdt: fYmd(s.chngdt) }))
      gridStages?.setData(formattedStages)

      // 5. 문서
      const resDocs = await api.get('/hsaa/docs', { params: { salesid, custcd } })
      gridDocs?.setData(resDocs.data || [])

    } catch (e: any) {
      vAlertError('상세 데이터 로드 중 오류가 발생했습니다.')
    }
}

const switchTab = (tabId: string) => {
  if (tabId !== 'SALES' && !formSales.salesid) {
    vAlertError('영업건(마스터)을 먼저 등록하거나 선택해야 합니다.')
    return
  }
  activeTab.value = tabId;
}

const confirmCoachingRead = async () => {
    if (!formDiary.ser) return
    if (!confirm('관리자의 지시사항을 확인하셨습니까?')) return
    try {
        const payload = {
            salesid: formSales.salesid,
            ser: formDiary.ser,
            contdt: formDiary.contdt?.replace(/-/g, '')
        }
        await api.post('/hsaa/consultations/coaching/confirm', payload)
        vAlert('✅ 지시사항 확인이 완료되었습니다.')
        fetchSubData(formSales)
    } catch (e) { vAlertError('확인 처리 중 오류 발생') }
}

const saveCurrentTab = async () => {
  try {
    if (activeTab.value === 'SALES') {
      if (!formSales.custcd) return vAlertError('거래처를 선택하세요.')
      if (!formSales.salestitle) return vAlertError('영업건명을 입력하세요.')

      const payload = {
        master: { ...formSales,
          addtime: formSales.addtime?.replace(/-/g, ''),
          foredt: formSales.foredt?.replace(/-/g, ''),
          foredelivdt: formSales.foredelivdt?.replace(/-/g, ''),
          lastmtdt: formSales.lastmtdt?.replace(/-/g, ''),
          realdt: formSales.realdt?.replace(/-/g, ''),
          faildt: '',
          holdondt: ''
        },
        items: gridItems?.getData()
      }
      const res = await api.post('/hsaa/master/save', payload)
      const newSalesId = res.data
      formSales.salesid = newSalesId
      vAlert('영업마스터 저장 완료')
      await searchMaster()
    }
    else if (activeTab.value === 'DIARY') {
      if (!formSales.salesid) return vAlertError('영업건(마스터) 정보가 없습니다. 마스터를 먼저 저장하세요.')
      if (!formDiary.channel) return vAlertError('상담채널을 선택하세요.')
      if (!formDiary.custid) return vAlertError('상담고객을 선택하세요.')

      // 상담본문의 첫 줄을 요약내용(content)으로 자동 추출
      const summary = formDiary.diarycontent ? formDiary.diarycontent.split('\n')[0].substring(0, 100) : ''

      const payload = { ...formDiary,
        salesid: formSales.salesid,
        custcd: formSales.custcd,
        content: summary,
        contdt: formDiary.contdt?.replace(/-/g, ''),
        startdate: formDiary.contdt?.replace(/-/g, ''), // 시작일자도 상담일자로 동기화
        realdt: formDiary.realdt?.replace(/-/g, ''),
        nextmeetdt: formDiary.nextmeetdt ? formDiary.nextmeetdt.replace(/-/g, '') : '',
        actionitem: formDiary.actionitem || '',
        actionstatus: formDiary.actionstatus || 'N',
        nextmeetagenda: formDiary.nextmeetagenda || ''
      }
      await api.post('/hsaa/diary/save', payload)
      vAlert('상담일지 저장 완료')
      fetchSubData(formSales)
    }
    else if (activeTab.value === 'STAGES') {
      if (!formSales.salesid) return vAlertError('영업건(마스터) 정보가 없습니다.')
      if (!formStage.bfstate) return vAlertError('이전상태를 선택하세요.')
      if (!formStage.state) return vAlertError('변동상태를 선택하세요.')

      // 영업단계 하향 조정 체크 (경고만 표시)
      if (Number(formStage.bfstate) > Number(formStage.state)) {
        if (!confirm('경고: 영업단계를 이전보다 낮은 단계로 변경하시겠습니까?\n(영업 진행이 지연되거나 후퇴하고 있습니다. 계속하시겠습니까?)')) return
      }

      // 단계변동 시 필수 체크
      if (formStage.state === '900') {
          if (!formStage.wincd) return vAlertError('성공사유를 선택하세요.')
          if (!formStage.realdt) return vAlertError('수주일자를 입력하세요.')
      } else if (formStage.state === '910' || formStage.state === '930') {
          if (!formStage.failcd) return vAlertError('실패사유를 선택하세요.')
      }

      const payload = { ...formStage,
        salesid: formSales.salesid,
        custcd: formSales.custcd,
        chngdt: formStage.chngdt?.replace(/-/g, ''),
        realdt: formStage.realdt?.replace(/-/g, '')
      }
      await api.post('/hsaa/stages/save', payload)
      vAlert('단계변동 저장 완료')
      fetchSubData(formSales)
    }
    else if (activeTab.value === 'KEYMAN') {
      if (!formSales.salesid) return vAlertError('영업건(마스터) 정보가 없습니다.')
      if (!formSales.custcd) return vAlertError('거래처 정보가 없습니다.')

      const payload = { ...formKeyman,
        cmpycd: authStore.cmpycd,
        custcd: formSales.custcd,
        salesid: formSales.salesid,
        birthday: formKeyman.birthday?.replace(/-/g, '')
      }
      await api.post('/hsaa/keyman/save', payload)
      vAlert('Keyman 저장 완료')
      fetchSubData(formSales)
    }
    else if (activeTab.value === 'DOCS') {
      if (!formSales.salesid) return vAlertError('영업건(마스터) 정보가 없습니다.')

      const formData = new FormData()
      const docData = { ...formDocs,
        cmpycd: authStore.cmpycd,
        regdt: (formDocs.regdt || today).replace(/-/g, ''),
        ser: formDocs.ser || '',
        custcd: formSales.custcd,
        salesid: formSales.salesid,
        title: formDocs.title,
        docgb: formDocs.docgb
      }
      console.log(docData)
      formData.append('doc', JSON.stringify(docData))
      if (formDocs.file) {
        formData.append('file', formDocs.file)
      }
      console.log(formData)
      await api.post('/hsaa/docs/save', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      vAlert('문서 저장 및 업로드 완료')
      formDocs.file = null
      fetchSubData(formSales)
    }
  } catch (e: any) {
    vAlertError('저장 중 오류가 발생했습니다.')
  }
}

const deleteCurrentTab = async () => {
  if (!confirm('삭제하시겠습니까?')) return
  try {
    if (activeTab.value === 'SALES') {
      await api.delete('/hsaa/master/delete', { params: { salesid: formSales.salesid } })
      vAlert('영업건 삭제 완료')
      initializeAll()
    }
    else if (activeTab.value === 'DIARY') {
      await api.delete('/hsaa/diary/delete', { params: { salesid: formSales.salesid, ser: formDiary.ser } })
      vAlert('상담일지 삭제 완료')
      fetchSubData(formSales)
    }
    else if (activeTab.value === 'KEYMAN') {
      await api.delete('/hsaa/keyman/delete', { params: { custid: formKeyman.custid } })
      vAlert('Keyman 삭제 완료')
      fetchSubData(formSales)
    }
  } catch (e: any) {
    vAlertError('삭제 중 오류가 발생했습니다.')
  }
}

const calcItemRow = (row: any) => {
    const d = row.getData()
    const qty = Number(d.qty || 0)
    const price = Number(d.unitprice || 0)
    row.update({ amt: qty * price })
}

const addItemRow = () => {
    gridItems?.addRow({ qty: 1, unitprice: 0, amt: 0 })
}

const deleteItemRow = () => {
    const selectedRows = gridItems?.getSelectedRows()
    if (selectedRows && selectedRows.length > 0) {
        selectedRows.forEach(row => row.delete())
    } else {
        vAlertError('삭제할 행을 선택하세요.')
    }
}

const handleFileChange = (e: any) => {
    const file = e.target.files[0]
    if (file) {
        formDocs.file = file
        formDocs.filename = file.name
        if (!formDocs.title) formDocs.title = file.name.split('.')[0]
    }
}

const handleOpenHelp = (type: string, target?: any) => {
  if (type === 'CUST') {
    Object.assign(modalProps, {
      title: '거래처 선택', path: '/erp/customer', defaultField: 'custnm',
      data: { cmpycd: authStore.cmpycd },
      columns: [ { title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 200 } ],
      onConfirm: (d: any) => Object.assign(formSales, d)
    })
    modalVisible.value = true
  } else if (type === 'ADDR') {
    if (!formSales.custcd) return vAlertError('거래처를 먼저 선택하세요.')
    Object.assign(modalProps, {
      title: '배송처 선택',
      path: '/hs00/HS00_000S_STR',
      defaultField: 'trannm',
      data: { gubun: 'T0', cmpycd: authStore.cmpycd, gbncd: '', code: formSales.custcd },
      columns: [
        { title: '코드', field: 'trancd', width: 60, hozAlign: 'center' },
        { title: '배송처명', field: 'custnm', width: 100 },
        { title: '우편번호', field: 'postno', width: 50 },
        { title: '주소', field: 'address', width: 200 },
        { title: '상세주소', field: 'd_address', width: 100 }
      ],
      onConfirm: (d: any) => {
        formSales.trancd = d.trancd;
        formSales.postno = d.postno;
        formSales.address = d.address;
        formSales.d_address = d.d_address || '';
      }
    })
    modalVisible.value = true
  } else if (type === 'ITEM') {
    Object.assign(modalProps, {
      title: '품목 선택',
      path: '/erp/item',
      defaultField: 'itemnm',
      data: { cmpycd: authStore.cmpycd },
      columns: [
        { title: '품목코드', field: 'itemcd', width: 100, hozAlign: 'center' },
        { title: '품목명', field: 'itemnm', width: 200 },
        { title: '단위', field: 'unit', width: 80, hozAlign: 'center' }
      ],
      onConfirm: (d: any) => {
        target.update({
          itemnm: d.itemnm,
          unitprice: d.outcost || 0,
          qty: 1,
          amt: d.outcost || 0
        })
      }
    })
    modalVisible.value = true
  }
}

onMounted(async () => {
  nextTick(() => {
    // ... (grid initialization remains same)
    gridMaster = new Tabulator(masterGridRef.value!, {
      layout: "fitColumns", selectable: 1, height: "100%",
      columns: [
        { title: "No", formatter: "rownum", width: 40, hozAlign: "center", headerSort: false },
        { title: "거래처명", field: "custnm", widthGrow: 1, cssClass: "fw-bold small", tooltip: true },
        { title: "영업건명", field: "salestitle", widthGrow: 2, cssClass: "small text-primary", tooltip: true, formatter: (cell) => {
            const val = cell.getValue() || ''; return val.length > 30 ? val.substring(0, 30) + "..." : val;
        }}
      ]
    })
    gridMaster.on("rowClick", (e, row) => fetchSubData(row.getData()))

    gridDiary = new Tabulator(diaryGridRef.value!, {
      layout: "fitColumns", selectable: 1, height: "100%",
      columns: [
        { title: "상담일자", field: "contdt", width: 100, formatter: (cell: any) => fYmd(cell.getValue()) },
        { title: "내용", field: "diarycontent", widthGrow: 1, tooltip: true, formatter: (cell: any) => {
            const val = cell.getValue() || '';
            return val.length > 50 ? val.substring(0, 50) + '...' : val;
        }},
        { title: "상태", field: "statenm", width: 80 },
        { title: "코칭", field: "salescoaching", visible: false },
        { title: "코칭일", field: "reportdt", visible: false },
        { title: "확인여부", field: "coachingreadyn", visible: false }
      ]
    })
    gridDiary.on("rowClick", (e, row) => {
      const data = row.getData()
      // 🚀 데이터 할당 전 초기화 (리액티비티 유지를 위해 속성값만 변경)
      Object.keys(formDiary).forEach(key => {
          if (typeof formDiary[key] === 'boolean') formDiary[key] = false
          else if (typeof formDiary[key] === 'number') formDiary[key] = 0
          else formDiary[key] = ''
      })
      Object.assign(formDiary, data)

      // 상담 목적/내용 템플릿 처리 (데이터가 없는 신규 상태 방지)
      if (!formDiary.diarycontent) {
          formDiary.diarycontent = `[상담 목적]\n- \n\n[상담 내용]\n- \n\n[향후 계획/요청사항]\n- `
      }
    })

    gridItems = new Tabulator(itemGridRef.value!, {
      layout: "fitColumns", height: "100%", selectable: true,
      columns: [
        { title: "", field: "select", formatter: "rowSelection", titleFormatter: "rowSelection", width: 50, hozAlign: "center", headerSort: false, cellClick: (e, cell) => {
            cell.getRow().toggleSelect();
        }},
        { title: "No", formatter: "rownum", width: 60, hozAlign: "center" },
        { title: "품목명", field: "itemnm", widthGrow: 2, editor: "input", cellClick: (e, cell) => {
            if (e.target.tagName !== 'INPUT') handleOpenHelp('ITEM', cell.getRow())
        }},
        { title: "수량", field: "qty", width: 120, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => calcItemRow(cell.getRow()) },
        { title: "단가", field: "unitprice", width: 150, hozAlign: "right", editor: "number", formatter: "money", formatterParams: { precision: 0 }, cellEdited: (cell) => calcItemRow(cell.getRow()) },
        { title: "금액", field: "amt", hozAlign: "right", formatter: "money", formatterParams: { precision: 0 }, width: 150, cssClass: "fw-bold text-primary" }
      ]
    })

    gridKeyman = new Tabulator(keymanGridRef.value!, {
    layout: "fitColumns", selectable: 1, height: "100%",
    columns: [
        { title: "성명", field: "name", width: 150, cssClass: "fw-bold" },
        { title: "부서", field: "custdept", widthGrow: 1 },
        { title: "핸드폰", field: "hpno", width: 150 },
        { title: "등록일", field: "addtime", width: 150, formatter: (cell: any) => fYmd(cell.getValue()) }
        ]
    })
    gridKeyman.on("rowClick", (e, row) => Object.assign(formKeyman, row.getData()))

    gridStages = new Tabulator(stageGridRef.value!, {
    layout: "fitColumns", height: "100%", selectable: 1,
    columns: [
        { title: "변동일자", field: "chngdt", width: 150, formatter: (cell: any) => fYmd(cell.getValue()) },
        { title: "변동상태", field: "statenm", width: 150, cssClass: "fw-bold text-danger" },
        { title: "변동사유", field: "remark", widthGrow: 1 }
    ]
    })
    gridStages.on("rowClick", (e, row) => Object.assign(formStage, row.getData()))

    gridDocs = new Tabulator(docsGridRef.value!, {
      layout: "fitColumns", height: "100%", selectable: 1,
      columns: [
        { title: "등록일", field: "regdt", width: 110, hozAlign: "center", formatter: (cell: any) => fYmd(cell.getValue()) },
        { title: "일련번호", field: "ser", width: 80, hozAlign: "center" },
        { title: "유형", field: "docgbnm", width: 120, hozAlign: "center" },
        { title: "문서제목", field: "title", widthGrow: 1.5, hozAlign: "left" },
        { title: "파일명", field: "filename", widthGrow: 1, hozAlign: "left", formatter: (cell: any) => {
            const filename = cell.getValue()
            if (!filename) root: {
              const filename = cell.getValue()
            }
            const filenameVal = cell.getValue()
            if (!filenameVal) return ''
            const baseStorage = API_URL ? API_URL.replace(/\/api$/, '') : ''
            return `<a href="${baseStorage}/storage/${authStore.cmpycd}/sfa/${encodeURIComponent(filenameVal)}" target="_blank" class="text-primary text-decoration-underline">${filenameVal}</a>`
        }}
      ]
    })
    gridDocs.on("rowClick", (e, row) => Object.assign(formDocs, row.getData()))

    searchMaster()
  })

  // 🚀 기초 데이터 로드
  try {
    const resUsers = await api.get('/hsaa/users')
    userData.value = resUsers.data || []

    // 공통코드 일괄 로드
    const codeGroups = ['700', '710', '720', '730', '740', '760', '770', '780', '790', '695', '705']
    const responses = await Promise.all(codeGroups.map(group => api.get(`/hsaa/codes/${group}`)))

    const mapCodes = (res: any) => res.data.map((c: any) => ({ code: c.codecd, cdnm: c.codenm }))

    stateData.value = mapCodes(responses[0])
    channelData.value = mapCodes(responses[1])
    favorData.value = mapCodes(responses[2])
    rankData.value = mapCodes(responses[3])
    choiceData.value = mapCodes(responses[4])
    winData.value = mapCodes(responses[5])
    failData.value = mapCodes(responses[6])
    docGbData.value = mapCodes(responses[7])
    keymanGbData.value = mapCodes(responses[8])
    rtnData.value = mapCodes(responses[9])
    useData.value = mapCodes(responses[10])

  } catch (e) {
    console.error('기초 데이터 로드 실패', e)
  }
})
</script>

<style scoped>
.erp-container { font-family: 'Pretendard', sans-serif; letter-spacing: -0.03rem; }
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 12px; border: none; }
.x-small { font-size: 11px; }

/* 💡 핵심: HSOD100U 스타일 2단 가로 배치 엔진 */
.main-content-wrapper {
    display: flex !important;
    flex-direction: column !important;
}

.bottom-layout-container {
    display: flex !important;
    flex-direction: row !important;
    flex-grow: 1 !important;
    overflow: hidden !important;
}

.master-column-fixed { flex: 0 0 500px !important; height: 100% !important; width: 500px !important; margin-right: 0; }
.detail-column-expand { flex: 1 1 0% !important; min-width: 0; height: 100% !important; display: flex; flex-direction: column; }

/* 표준 버튼 및 폼 */
.btn-erp { height: 28px; padding: 0 12px; font-size: 12px; font-weight: 600; border-radius: 4px; border: 1px solid #dee2e6; background-color: #fff; color: #444; }
.btn-search { background-color: #0d6efd !important; border-color: #0d6efd !important; color: #fff !important; }
.btn-save { background-color: #198754 !important; border-color: #198754 !important; color: #fff !important; }
.btn-delete { background-color: #dc3545 !important; border-color: #dc3545 !important; color: #fff !important; }

.erp-table-dense th { padding: 6px 8px; border: 1px solid #dee2e6; font-size: 11px; font-weight: 600; color: #555; background-color: #f8f9fa; }
.erp-table-dense td { padding: 3px 8px; border: 1px solid #dee2e6; background-color: #fff; }

.nav-tabs .nav-link { color: #64748b; border-radius: 0; background: transparent !important; padding: 0.6rem 1.6rem !important; }
.nav-tabs .nav-link.active { color: #0d6efd !important; font-weight: 800 !important; }

:deep(.tabulator-row.tabulator-selected) { background-color: #eef6ff !important; color: #0d6efd !important; font-weight: bold; }
</style>
