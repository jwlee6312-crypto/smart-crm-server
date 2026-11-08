<!--
	=============================================================
	프로그램명	: 영업활동 방문계획 자동수립 (HSAA700U)
	작성일자	: 2026.10.28
	설명        : 영업건 중요도(IMPORT), 확도(SUCCRATE), 최종접촉일(LASTMTDT), 방문주기, 공휴일(PD_HOLIDAY), 위·경도 동선 최적화 기반 자동 방문 계획 수립 및 캘린더 확정
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-map me-2 text-primary" style="font-size: 18px;"></i>
        영업활동관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">영업활동 방문계획 자동수립 (HSAA700U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchPlan">계획조회</button>
        <button class="btn-erp btn-auto" @click="generateAutoPlan">자동생성(A)</button>
        <button class="btn-erp btn-save" @click="confirmAndSync">일정확정(S)</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조건 설정 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 10%;" /><col style="width: 20%;" />
              <col style="width: 10%;" /><col style="width: 20%;" />
              <col style="width: 10%;" /><col style="width: 30%;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">기준년월</th>
                <td>
                  <input v-model="filter.ym" type="text" class="form-control form-control-sm" placeholder="YYYYMM" maxlength="6" @keyup.enter="fetchPlan" />
                </td>
                <th class="text-center bg-light border-start">영업사원</th>
                <td>
                  <select v-model="filter.userid" class="form-select form-select-sm">
                    <option v-for="user in salesmanList" :key="user.userid" :value="user.userid">{{ user.usernm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light border-start">일일 방문 CAPA</th>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <input v-model.number="filter.dailyCapa" type="number" class="form-control form-control-sm" style="width: 80px;" min="1" max="10" />
                    <span class="text-muted x-small">(일일 최대 방문 건수)</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중앙] 탭 구조 컨테이너 (목록 뷰 vs 캘린더 뷰) -->
      <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
        <!-- 탭 헤더 -->
        <ul class="nav nav-tabs px-3 pt-2 bg-white border-bottom flex-shrink-0">
          <li class="nav-item">
            <button class="nav-link py-1 px-3 small" :class="{ 'active fw-bold': activeTab === 'TABLE' }" @click="activeTab = 'TABLE'">
              <i class="bi bi-table me-1 text-primary"></i>방문계획 목록 (Table View)
            </button>
          </li>
          <li class="nav-item">
            <button class="nav-link py-1 px-3 small" :class="{ 'active fw-bold': activeTab === 'CALENDAR' }" @click="activeTab = 'CALENDAR'">
              <i class="bi bi-calendar3 me-1 text-primary"></i>월간 캘린더 (Calendar View)
            </button>
          </li>
          <li class="nav-item ms-auto align-self-center pb-1">
            <span class="x-small text-muted fst-italic">
              <i class="bi bi-info-circle me-1"></i>방문 주기의 시급성, 수주 성공 확도, 주말·공휴일 제외 평일 및 위·경도 동선 기반 최적 자동 산출
            </span>
          </li>
        </ul>

        <!-- 탭 컨텐츠 영역 -->
        <div class="tab-content flex-grow-1 d-flex flex-column overflow-hidden bg-white">
          <!-- 탭 1) 목록 뷰 (Table) -->
          <div class="tab-pane flex-grow-1 d-flex flex-column overflow-hidden" :class="{ 'show active d-flex': activeTab === 'TABLE', 'd-none': activeTab !== 'TABLE' }">
            <div ref="gridRef" class="tabulator-instance flex-grow-1"></div>
          </div>

          <!-- 탭 2) 캘린더 뷰 (Calendar) -->
          <div class="tab-pane flex-grow-1 overflow-auto p-3" :class="{ 'show active': activeTab === 'CALENDAR', 'd-none': activeTab !== 'CALENDAR' }">
            <div class="calendar-grid">
              <div class="calendar-day-head" v-for="d in ['일','월','화','수','목','금','토']" :key="d">{{ d }}</div>
              <!-- 빈 칸 패딩 -->
              <div class="calendar-day empty" v-for="n in startDayOfWeek" :key="'e'+n"></div>
              <!-- 날짜별 칸 -->
              <div class="calendar-day" v-for="day in daysInMonth" :key="day" :class="{'today': isToday(day)}">
                <span class="day-num">{{ day }}</span>
                <div class="events-container mt-1">
                  <div v-for="ev in getEventsForDay(day)" :key="ev.salesid + ev.plan_dt" class="event-badge text-truncate mb-1" :title="ev.custnm + ' - ' + ev.deal_title">
                    <span class="badge bg-primary text-white x-small w-100 text-truncate text-start px-1 py-1">
                      <i class="bi bi-geo-alt-fill me-1"></i>{{ ev.custnm }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 안내 가이드 -->
      <div class="p-1 text-center text-muted x-small opacity-75">
        <i class="bi bi-info-circle me-1"></i> [자동생성] 버튼을 누르면 조건에 맞는 미팅 일정이 최적의 동선과 주기로 자동 배치되며, 목록 탭에서 체크박스로 행을 선택한 뒤 [일정확정] 시 DB에 최종 저장됩니다. 각 행 우측의 [상담작성]을 통해 팝업창에서 바로 상담일지를 등록할 수 있습니다.
      </div>
    </div>
  </div>

  <!-- 🚀 상담일지 작성 팝업 모달 -->
  <div v-if="diaryModalVisible" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content border-0 shadow-lg rounded-4">
        <div class="modal-header bg-primary text-white py-2 px-3">
          <h6 class="modal-title fw-bold"><i class="bi bi-journal-text me-2"></i>상담일지 작성</h6>
          <button type="button" class="btn-close btn-close-white" @click="diaryModalVisible = false"></button>
        </div>
        <div class="modal-body p-3 bg-light">
          <div class="card border-0 shadow-sm p-3 bg-white rounded-3">
            <!-- 폼 내에서 거래처명, 연락처, 영업건 ID, 영업건명 조회 -->
            <div class="row g-2 mb-2">
              <div class="col-md-3">
                <label class="form-label x-small fw-bold text-muted">거래처명</label>
                <input v-model="formDiaryModal.custnm" type="text" class="form-control form-control-sm bg-light" readonly />
              </div>
              <div class="col-md-3">
                <label class="form-label x-small fw-bold text-muted">고객 연락처</label>
                <input v-model="formDiaryModal.hpno" type="text" class="form-control form-control-sm bg-light text-primary fw-bold" readonly />
              </div>
              <div class="col-md-3">
                <label class="form-label x-small fw-bold text-muted">영업건 ID</label>
                <input v-model="formDiaryModal.salesid" type="text" class="form-control form-control-sm bg-light" readonly />
              </div>
              <div class="col-md-3">
                <label class="form-label x-small fw-bold text-muted">영업건명</label>
                <input v-model="formDiaryModal.deal_title" type="text" class="form-control form-control-sm bg-light" readonly />
              </div>
            </div>

            <!-- 상담일자, 상담시간, 상담채널, 상담고객(Keyman - 필수) -->
            <div class="row g-2 mb-2">
              <div class="col-md-3">
                <label class="form-label x-small fw-bold">상담일자</label>
                <input v-model="formDiaryModal.contdt" type="date" class="form-control form-control-sm" />
              </div>
              <div class="col-md-3">
                <label class="form-label x-small fw-bold">상담시간</label>
                <div class="input-group input-group-sm">
                  <input v-model.number="formDiaryModal.conthh" type="number" class="form-control" min="0" max="23" style="max-width: 45px;" />
                  <span class="input-group-text px-1">시</span>
                  <input v-model.number="formDiaryModal.contmm" type="number" class="form-control" min="0" max="59" style="max-width: 45px;" />
                  <span class="input-group-text px-1">분</span>
                </div>
              </div>
              <div class="col-md-3">
                <label class="form-label x-small fw-bold">상담채널</label>
                <select v-model="formDiaryModal.channel" class="form-select form-select-sm">
                  <option value="010">방문</option>
                  <option value="020">전화</option>
                  <option value="030">이메일</option>
                  <option value="040">기타</option>
                </select>
              </div>
              <div class="col-md-3">
                <label class="form-label x-small fw-bold text-danger">상담고객 (필수)</label>
                <select v-model="formDiaryModal.custid" class="form-select form-select-sm border-danger">
                  <option value="" disabled>상담고객 선택</option>
                  <option v-for="k in modalKeymanList" :key="k.custid" :value="k.custid">{{ k.name }} ({{ k.jikch || '담당' }})</option>
                </select>
              </div>
            </div>

            <div class="row g-2 mb-2">
              <div class="col-md-8">
                <label class="form-label x-small fw-bold">후속조치 (Action Item)</label>
                <input v-model="formDiaryModal.actionitem" type="text" class="form-control form-control-sm" placeholder="예: 견적서 발송" />
              </div>
              <div class="col-md-4">
                <label class="form-label x-small fw-bold">조치완료여부</label>
                <div class="form-check form-switch mt-1">
                  <input v-model="formDiaryModal.actionstatus" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="modalActionStatus">
                  <label class="form-check-label small fw-bold" for="modalActionStatus">{{ formDiaryModal.actionstatus === 'Y' ? '완료' : '미해결' }}</label>
                </div>
              </div>
            </div>

            <div class="row g-2 mb-2">
              <div class="col-md-4">
                <label class="form-label x-small fw-bold">다음 미팅일</label>
                <input v-model="formDiaryModal.nextmeetdt" type="date" class="form-control form-control-sm" />
              </div>
              <div class="col-md-8">
                <label class="form-label x-small fw-bold">다음 미팅 안건</label>
                <input v-model="formDiaryModal.nextmeetagenda" type="text" class="form-control form-control-sm" placeholder="다음 미팅 안건" />
              </div>
            </div>

            <!-- HSAA100U와 동일한 표준 용어: 확인요청 (관리자 보고) 및 보고내용 -->
            <div class="row g-2 mb-2">
              <div class="col-md-4">
                <label class="form-label x-small fw-bold">확인요청</label>
                <div class="d-flex align-items-center gap-1 mt-1">
                  <input v-model="formDiaryModal.reportyn" class="form-check-input" type="checkbox" true-value="Y" false-value="N" id="modalReportYn">
                  <label class="form-check-label x-small fw-bold text-danger" for="modalReportYn">관리자 보고</label>
                </div>
              </div>
              <div class="col-md-8" v-if="formDiaryModal.reportyn === 'Y'">
                <label class="form-label x-small fw-bold text-primary">보고내용</label>
                <input v-model="formDiaryModal.reportcontent" type="text" class="form-control form-control-sm bg-primary bg-opacity-10" placeholder="관리자에게 보고할 내용을 입력하세요" />
              </div>
            </div>

            <div class="mb-2">
              <label class="form-label x-small fw-bold">상담 내용</label>
              <textarea v-model="formDiaryModal.diarycontent" class="form-control form-control-sm" rows="5"></textarea>
            </div>
          </div>
        </div>
        <div class="modal-footer bg-white py-2 px-3">
          <button type="button" class="btn btn-secondary btn-sm px-3" @click="diaryModalVisible = false">취소</button>
          <button type="button" class="btn btn-primary btn-sm px-3" @click="saveDiaryModal">저장하기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authstore = useAuthStore()
const { showAlert, showError, vAlert, vAlertError, alertMessage } = useAlerts()

const activeTab = ref<'TABLE' | 'CALENDAR'>('TABLE')
const planData = ref<any[]>([])

const filter = reactive({
  ym: new Date().toISOString().slice(0, 7).replace('-', ''),
  userid: authstore.userid || '',
  dailyCapa: 4
})

// 상담일지 모달 상태
const diaryModalVisible = ref(false)
const diaryModalData = ref<any>({})
const modalKeymanList = ref<any[]>([])
const formDiaryModal = reactive<any>({
  ser: '',
  salesid: '',
  custcd: '',
  custid: '',
  custnm: '',
  deal_title: '',
  hpno: '',
  contdt: new Date().toISOString().slice(0, 10),
  conthh: 10,
  contmm: 0,
  channel: '010', // 🚀 기본값 '방문' (010)
  diarycontent: '[상담 목적]\n- \n\n[상담 내용]\n- \n\n[향후 계획/요청사항]\n- ',
  actionitem: '',
  actionstatus: 'N',
  nextmeetdt: '',
  nextmeetagenda: '',
  reportyn: 'N',
  reportcontent: ''
})

watch(() => formDiaryModal.custid, (newId) => {
  if (newId && modalKeymanList.value.length > 0) {
    const found = modalKeymanList.value.find((k: any) => String(k.custid) === String(newId))
    if (found) {
      formDiaryModal.hpno = found.hpno || found.custtel || found.telno || ''
    }
  }
})

const openDiaryModal = async (row: any) => {
  diaryModalData.value = row
  formDiaryModal.ser = ''
  formDiaryModal.salesid = row.salesid || ''
  formDiaryModal.custcd = row.custcd || ''
  formDiaryModal.custid = ''
  formDiaryModal.custnm = row.custnm || ''
  formDiaryModal.deal_title = row.deal_title || row.salestitle || ''
  formDiaryModal.hpno = ''
  modalKeymanList.value = []

  try {
    const resDetail = await api.get('/hsaa/detail', { params: { salesid: row.salesid } })
    if (resDetail.data && resDetail.data.master) {
      const m = resDetail.data.master
      formDiaryModal.custnm = m.custnm || formDiaryModal.custnm
      formDiaryModal.deal_title = m.salestitle || formDiaryModal.deal_title
    }
    if (row.custcd) {
      const resKeyman = await api.get('/hsaa/keyman', { params: { custcd: row.custcd } })
      modalKeymanList.value = resKeyman.data || []
      if (modalKeymanList.value.length > 0) {
        formDiaryModal.custid = modalKeymanList.value[0].custid || ''
        formDiaryModal.hpno = modalKeymanList.value[0].hpno || modalKeymanList.value[0].custtel || modalKeymanList.value[0].telno || ''
      }
    }
  } catch (e) {}

  formDiaryModal.contdt = row.plan_dt ? row.plan_dt.replace(/-/g, '').replace(/^(\d{4})(\d{2})(\d{2})$/, '$1-$2-$3') : new Date().toISOString().slice(0, 10)
  formDiaryModal.conthh = 10
  formDiaryModal.contmm = 0
  formDiaryModal.channel = '010' // 방문 기본값
  formDiaryModal.diarycontent = `[상담 목적]\n- (${formDiaryModal.custnm || ''}) 미팅\n\n[상담 내용]\n- \n\n[향후 계획/요청사항]\n- `
  formDiaryModal.actionitem = ''
  formDiaryModal.actionstatus = 'N'
  formDiaryModal.nextmeetdt = ''
  formDiaryModal.nextmeetagenda = ''
  formDiaryModal.reportyn = 'N'
  formDiaryModal.reportcontent = ''
  diaryModalVisible.value = true
}

const saveDiaryModal = async () => {
  if (!formDiaryModal.custid) {
    return vAlertError('상담고객(Keyman)을 반드시 선택하세요. 방문 시 상담 상대방은 필수입니다.')
  }
  if (!formDiaryModal.diarycontent) {
    return vAlertError('상담 내용을 입력하세요.')
  }
  try {
    const payload = {
      ...formDiaryModal,
      salesid: formDiaryModal.salesid,
      custcd: formDiaryModal.custcd,
      custid: formDiaryModal.custid,
      cmpycd: authstore.cmpycd,
      userid: filter.userid || authstore.userid,
      deptcd: authstore.deptcd || '',
      updemp: authstore.userid,
      contdt: formDiaryModal.contdt.replace(/-/g, ''),
      nextmeetdt: formDiaryModal.nextmeetdt ? formDiaryModal.nextmeetdt.replace(/-/g, '') : '',
      content: formDiaryModal.diarycontent.split('\n')[0].substring(0, 100),
      reportyn: formDiaryModal.reportyn || 'N'
    }
    await api.post('/hsaa/diary/save', payload)
    vAlert('상담일지가 성공적으로 등록되었습니다.')
    diaryModalVisible.value = false
    fetchPlan()
  } catch (e: any) {
    vAlertError('상담일지 저장 실패: ' + (e.message || ''))
  }
}

// 캘린더 계산용 Computed
const currentYear = computed(() => {
  const ym = filter.ym || '202611'
  return parseInt(ym.substring(0, 4)) || new Date().getFullYear()
})

const currentMonth = computed(() => {
  const ym = filter.ym || '202611'
  return parseInt(ym.substring(4, 6)) || (new Date().getMonth() + 1)
})

const daysInMonth = computed(() => {
  return new Date(currentYear.value, currentMonth.value, 0).getDate()
})

const startDayOfWeek = computed(() => {
  return new Date(currentYear.value, currentMonth.value - 1, 1).getDay()
})

const isToday = (day: number) => {
  const now = new Date()
  return now.getFullYear() === currentYear.value && (now.getMonth() + 1) === currentMonth.value && now.getDate() === day
}

const getEventsForDay = (day: number) => {
  const mStr = String(currentMonth.value).padStart(2, '0')
  const dStr = String(day).padStart(2, '0')
  const targetYmd = `${currentYear.value}${mStr}${dStr}`
  return planData.value.filter(ev => String(ev.plan_dt || '').replace(/-/g, '') === targetYmd)
}

const salesmanList = ref<any[]>([])
const gridRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null

const initialize = () => {
  grid?.clearData()
  planData.value = []
  vAlert('초기화되었습니다.')
}

const fetchPlan = async () => {
  try {
    const res = await api.get('/hsaa/visit-plan', {
      params: { ym: filter.ym, userid: filter.userid || authstore.userid }
    })
    const list = res.data || []
    planData.value = list
    if (grid) {
      grid.setData(list)
      grid.selectRow()
    }
    if (list.length === 0) {
      vAlert('해당 월에 확정된 방문 계획이 없습니다. [자동생성] 후 [일정확정]을 진행하세요.')
    } else {
      vAlert(`방문 계획 조회 완료 (${list.length}건)`)
    }
  } catch (e) {
    vAlertError('조회 중 오류 발생')
  }
}

const generateAutoPlan = async () => {
  if (!confirm('설정된 조건(중요도, 확도, 최종접촉일, 공휴일, 위경도 동선)을 기반으로 방문 계획을 자동 수립하시겠습니까?')) return
  try {
    const res = await api.post('/hsaa/visit-plan/generate', {
      ym: filter.ym,
      userid: filter.userid,
      dailyCapa: filter.dailyCapa
    })
    const list = res.data || []
    planData.value = list
    if (grid) {
      grid.setData(list)
      grid.selectRow()
    }
    vAlert('스마트 방문 계획이 성공적으로 자동 수립되었습니다.')
  } catch (e) {
    vAlertError('자동 계획 수립 실패')
  }
}

const confirmAndSync = async () => {
  const selectedRows = grid?.getSelectedData() || []
  if (selectedRows.length === 0) {
    return vAlertError('확정할 방문 계획 행을 선택하세요.')
  }
  if (!confirm(`선택된 ${selectedRows.length}건의 방문 계획을 최종 확정하여 데이터베이스에 저장하시겠습니까?`)) return
  try {
    await api.post('/hsaa/visit-plan/confirm', {
      userid: filter.userid,
      plans: selectedRows
    })
    vAlert('선택된 방문 계획이 성공적으로 확정 및 저장되었습니다.')
    fetchPlan()
  } catch (e) {
    vAlertError('일정 확정 실패')
  }
}

const initTabulator = () => {
  if (!gridRef.value) return
  grid = new Tabulator(gridRef.value, {
    layout: "fitColumns",
    height: "100%",
    selectable: true,
    headerSort: false,
    columnDefaults: {
      headerHozAlign: 'center',
      vertAlign: "middle"
    },
    columns: [
      { title: "", formatter: "rowSelection", titleFormatter: "rowSelection", width: 50, hozAlign: "center", headerSort: false, cellClick: (e, cell) => {
          cell.getRow().toggleSelect();
      }},
      { title: "방문예정일", field: "plan_dt", width: 110, hozAlign: "center", cssClass: "fw-bold text-primary", editor: "input" },
      { title: "거래처명", field: "custnm", width: 160, hozAlign: "left" },
      { title: "영업건명", field: "deal_title", minWidth: 200, hozAlign: "left" },
      { title: "중요도(등급)", field: "import_nm", width: 100, hozAlign: "center" },
      { title: "확도(%)", field: "succrate", width: 80, hozAlign: "center" },
      { title: "최종접촉일", field: "lastmtdt", width: 110, hozAlign: "center" },
      { title: "주소", field: "address", minWidth: 250, hozAlign: "left" },
      { title: "상담작성", width: 90, hozAlign: "center", headerSort: false, formatter: () => "<button class='btn btn-xs btn-primary py-0 px-1' style='font-size:11px;'>상담작성</button>", cellClick: (e, cell) => {
          const row = cell.getRow().getData()
          if (!row.salesid) {
            vAlertError('영업건 정보가 없습니다.')
            return
          }
          openDiaryModal(row)
      }}
    ]
  })
  if (planData.value.length > 0) {
    grid.setData(planData.value)
    grid.selectRow()
  }
}

watch(activeTab, async (newTab) => {
  if (newTab === 'TABLE') {
    await nextTick()
    initTabulator()
    fetchPlan()
  }
})

onMounted(async () => {
  try {
    const res = await api.get('/hsaa/users')
    salesmanList.value = res.data || []
  } catch (e) {}

  initTabulator()
  fetchPlan()
})
</script>

<style scoped>
.erp-container { font-family: 'Pretendard', sans-serif; letter-spacing: -0.03rem; }
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 13px; }
.x-small { font-size: 11px; }

.erp-header { height: 48px; padding: 0 10px; background-color: #fff; }
.btn-erp { height: 28px; padding: 0 12px; font-size: 12px; font-weight: 600; border-radius: 4px; transition: all 0.2s; border: 1px solid #dee2e6; background-color: #fff; color: #444; }
.btn-erp:hover { background-color: #f8f9fa; border-color: #ced4da; }
.btn-init { color: #6c757d; }
.btn-search { background-color: #0d6efd; border-color: #0d6efd; color: #fff; }
.btn-search:hover { background-color: #0b5ed7; border-color: #0a58ca; color: #fff; }
.btn-auto { background-color: #fd7e14; border-color: #fd7e14; color: #fff; }
.btn-auto:hover { background-color: #e8590c; border-color: #d9480f; color: #d9480f; }
.btn-save { background-color: #198754; border-color: #198754; color: #fff; }
.btn-save:hover { background-color: #157347; border-color: #146c43; color: #fff; }

.erp-table-dense th { padding: 8px; border: 1px solid #dee2e6; font-size: 12px; font-weight: 600; color: #555; }
.erp-table-dense td { padding: 4px 8px; border: 1px solid #dee2e6; }
:deep(.tabulator-row.tabulator-selectable:hover) { background-color: #f8f9fa !important; }

/* 캘린더 그리드 스타일 */
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 1px; background-color: #e2e8f0; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }
.calendar-day-head { background-color: #f8fafc; padding: 8px; text-align: center; font-size: 12px; font-weight: bold; color: #475569; }
.calendar-day { background-color: #fff; min-height: 110px; padding: 6px; position: relative; }
.calendar-day.empty { background-color: #f8fafc; }
.calendar-day.today { background-color: #eef2ff; }
.day-num { font-size: 12px; font-weight: bold; color: #334155; }
.event-badge { font-size: 11px; cursor: pointer; }
</style>
