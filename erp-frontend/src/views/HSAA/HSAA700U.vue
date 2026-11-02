<!--
	=============================================================
	프로그램명	: 영업활동 방문계획 자동수립 (HSAA700U)
	작성일자	: 2026.10.28
	설명        : 영업건 중요도(IMPORT), 확도(SUCCRATE), 최종접촉일(LASTMTDT), 공휴일(PD_HOLIDAY), 위·경도 동선 기반 자동 방문 계획 수립 및 캘린더 확정 (그리드 탭 + 월간 캘린더 탭 구조 지원)
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
        <button class="btn-erp btn-alarm" @click="requestWebNotification" title="브라우저 푸시 알림 켜기"><i class="bi bi-bell me-1"></i>알람설정</button>
        <button class="btn-erp btn-save" @click="confirmAndSync">일정확정(S)</button>
      </div>
    </div>

    <!-- 💡 탭 네비게이션 -->
    <ul class="nav nav-tabs px-3 pt-2 bg-white border-bottom flex-shrink-0" role="tablist">
      <li class="nav-item" role="presentation">
        <button class="nav-link py-1 px-3 small fw-bold" :class="{active: activeTab === 'grid'}" @click="activeTab = 'grid'">
          <i class="bi bi-table me-1"></i> 방문계획 수립 및 조정 (그리드)
        </button>
      </li>
      <li class="nav-item" role="presentation">
        <button class="nav-link py-1 px-3 small fw-bold" :class="{active: activeTab === 'calendar'}" @click="activeTab = 'calendar'">
          <i class="bi bi-calendar-month me-1"></i> 월간 캘린더 조회
        </button>
      </li>
    </ul>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-2 d-flex flex-column gap-2 bg-light main-content-wrapper">

      <!-- [상단] 조건 설정 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden">
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 8%;" /><col style="width: 15%;" />
              <col style="width: 8%;" /><col style="width: 15%;" />
              <col style="width: 8%;" /><col style="width: 22%;" />
              <col style="width: 10%;" /><col style="width: 14%;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">계획구분</th>
                <td>
                  <select v-model="filter.viewType" class="form-select form-select-sm">
                    <option value="MONTH">월간 방문계획</option>
                    <option value="WEEK">주간 방문계획</option>
                  </select>
                </td>
                <th class="text-center bg-light border-start">기준년월</th>
                <td>
                  <input v-model="filter.ym" type="text" class="form-control form-control-sm" placeholder="YYYYMM" maxlength="6" @keyup.enter="fetchPlan" />
                </td>
                <th class="text-center bg-light border-start">영업사원</th>
                <td>
                  <select v-model="filter.userid" class="form-select form-select-sm" @change="fetchPlan">
                    <option v-for="user in salesmanList" :key="user.userid" :value="user.userid">{{ user.usernm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light border-start">일일 방문 CAPA</th>
                <td>
                  <div class="d-flex align-items-center gap-1">
                    <input v-model.number="filter.dailyCapa" type="number" class="form-control form-control-sm" min="1" max="10" />
                    <span class="text-muted x-small">건</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [탭 1] 그리드 뷰 -->
      <div v-if="activeTab === 'grid'" class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-dark"><i class="bi bi-table me-2 text-primary"></i>방문계획 수립 및 조정 목록</span>
          <span class="x-small text-muted fst-italic">* 자동 수립된 방문 계획 목록입니다. [월간 캘린더] 탭에서 드래그 앤 드롭으로 일정을 간편하게 이동할 수 있습니다.</span>
        </div>
        <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
          <div ref="gridRef" class="tabulator-instance flex-grow-1"></div>
        </div>
      </div>

      <!-- [탭 2] 월간 캘린더 뷰 (드래그 앤 드롭 지원) -->
      <div v-if="activeTab === 'calendar'" class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden p-3 bg-white">
        <div class="d-flex justify-content-between align-items-center mb-2 flex-shrink-0">
          <h6 class="fw-bold text-dark m-0"><i class="bi bi-calendar3 me-2 text-primary"></i>{{ filter.ym.substring(0,4) }}년 {{ filter.ym.substring(4,6) }}월 방문 일정 캘린더 (드래그로 일정 이동 가능)</h6>
          <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2">총 확정 방문건수: {{ gridData.length }}건</span>
        </div>
        <div class="calendar-wrapper flex-grow-1 overflow-auto border rounded">
          <div class="calendar-header-grid">
            <div class="cal-head text-danger">일</div>
            <div class="cal-head">월</div>
            <div class="cal-head">화</div>
            <div class="cal-head">수</div>
            <div class="cal-head">목</div>
            <div class="cal-head">금</div>
            <div class="cal-head text-primary">토</div>
          </div>
          <div class="calendar-body-grid">
            <div v-for="(day, idx) in calendarDays" :key="idx"
                 class="cal-cell"
                 :class="{'other-month': !day.isCurrentMonth, 'today': day.isToday}"
                 @dragover.prevent
                 @drop="onDropToDay($event, day.fullDate)">
              <div class="cal-day-num">{{ day.dayNum }}</div>
              <div class="cal-events">
                <div v-for="ev in day.events" :key="ev.salesid"
                     class="cal-event-badge"
                     draggable="true"
                     @dragstart="onDragStart($event, ev)"
                     :title="ev.custnm + ' - ' + ev.salestitle">
                  <span class="fw-bold text-truncate">{{ ev.custnm }}</span>
                  <span class="text-muted extra-small d-block text-truncate">{{ ev.salestitle }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 안내 가이드 -->
      <div class="p-1 text-center text-muted x-small opacity-75 flex-shrink-0">
        <i class="bi bi-info-circle me-1"></i> [자동생성] 시 최적 동선으로 스케줄이 수립되며, 그리드 또는 캘린더에서 일정 변경 후 [일정확정] 시 최종 반영됩니다.
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'

const authstore = useAuthStore()
const { showAlert, showError, vAlert, vAlertError, alertMessage } = useAlerts()

const activeTab = ref<'grid' | 'calendar'>('grid')
const filter = reactive({
  viewType: 'MONTH',
  ym: new Date().toISOString().slice(0, 7).replace('-', ''),
  userid: authstore.userid || '',
  dailyCapa: 4
})

const salesmanList = ref<any[]>([])
const gridRef = ref<HTMLDivElement | null>(null)
let grid: Tabulator | null = null
const gridData = ref<any[]>([])
const calendarDays = ref<any[]>([])

let draggedEvent: any = null

const onDragStart = (e: DragEvent, ev: any) => {
  draggedEvent = ev
}

const onDropToDay = (e: DragEvent, targetDateStr: string) => {
  e.preventDefault()
  if (!draggedEvent || !targetDateStr) return

  draggedEvent.plan_dt = targetDateStr
  const targetItem = gridData.value.find(item => item.salesid === draggedEvent.salesid)
  if (targetItem) {
    targetItem.plan_dt = targetDateStr
  }
  grid?.setData(gridData.value)
  buildCalendar()
  vAlert(`일정이 ${targetDateStr.substring(0,4)}-${targetDateStr.substring(4,6)}-${targetDateStr.substring(6,8)}로 변경되었습니다. [일정확정]을 눌러 저장하세요.`)
  draggedEvent = null
}

const initialize = () => {
  grid?.clearData()
  gridData.value = []
  vAlert('초기화되었습니다.')
}

const fetchPlan = async () => {
  try {
    const res = await api.get('/hsaa/visit-plan', {
      params: { ym: filter.ym, userid: filter.userid, viewType: filter.viewType }
    })
    const data = res.data || []
    gridData.value = data
    grid?.setData(data)
    buildCalendar()
    vAlert('방문 계획을 조회했습니다.')
  } catch (e) {
    vAlertError('조회 중 오류 발생')
  }
}

const generateAutoPlan = async () => {
  if (!confirm(`설정된 조건으로 ${filter.viewType === 'MONTH' ? '월간' : '주간'} 방문 계획을 자동 수립하시겠습니까?`)) return
  try {
    const res = await api.post('/hsaa/visit-plan/generate', {
      ym: filter.ym,
      userid: filter.userid,
      dailyCapa: filter.dailyCapa,
      viewType: filter.viewType
    })
    const data = res.data || []
    gridData.value = data
    grid?.setData(data)
    buildCalendar()
    vAlert('스마트 방문 계획이 성공적으로 자동 수립되었습니다.')
  } catch (e) {
    vAlertError('자동 계획 수립 실패')
  }
}

const confirmAndSync = async () => {
  if (!confirm('현재 수립 및 수정된 방문 계획을 개인 일정 및 캘린더에 최종 확정 반영하시겠습니까?')) return
  try {
    const currentData = grid?.getData() || gridData.value || []
    await api.post('/hsaa/visit-plan/confirm', {
      userid: filter.userid,
      plans: currentData
    })
    gridData.value = currentData
    vAlert('방문 계획이 성공적으로 확정 및 캘린더에 연동되었습니다.')
  } catch (e) {
    vAlertError('일정 확정 실패')
  }
}

// 🔔 브라우저 웹 푸시 알림 설정
const requestWebNotification = async () => {
  if (!('Notification' in window)) {
    alert('이 브라우저는 데스크톱 푸시 알림을 지원하지 않습니다.')
    return
  }
  const permission = await Notification.requestPermission()
  if (permission === 'granted') {
    vAlert('브라우저 푸시 알림이 활성화되었습니다. 오늘 방문 예정인 일정에 대해 알림이 전송됩니다.')
    new Notification('스마트 ERP CRM', {
      body: '영업활동 방문계획 알람 기능이 정상 설정되었습니다.',
      icon: '/favicon.ico'
    })
  } else {
    alert('알림 권한이 거부되었습니다. 브라우저 설정에서 알림을 허용해 주세요.')
  }
}

// 🗓️ 월간 캘린더 데이터 빌드
const buildCalendar = () => {
  if (grid) {
    gridData.value = grid.getData() || gridData.value || []
  }
  const ymStr = filter.ym
  if (!ymStr || ymStr.length !== 6) return

  const year = parseInt(ymStr.substring(0, 4))
  const month = parseInt(ymStr.substring(4, 6)) - 1

  const firstDayIndex = new Date(year, month, 1).getDay()
  const lastDay = new Date(year, month + 1, 0).getDate()
  const prevLastDay = new Date(year, month, 0).getDate()

  const days: any[] = []
  const today = new Date()

  // 1. 이전 달 여백 채우기
  for (let i = firstDayIndex - 1; i >= 0; i--) {
    const dNum = prevLastDay - i
    days.push({ dayNum: dNum, fullDate: '', isCurrentMonth: false, isToday: false, events: [] })
  }

  // 2. 이번 달 날짜 채우기
  for (let d = 1; d <= lastDay; d++) {
    const dStr = String(d).padStart(2, '0')
    const mStr = String(month + 1).padStart(2, '0')
    const fullDate = `${year}${mStr}${dStr}`

    const isToday = today.getFullYear() === year && today.getMonth() === month && today.getDate() === d

    const dayEvents = gridData.value.filter(item => {
      const pDt = String(item.plan_dt || '').replace(/[^0-9]/g, '')
      return pDt === fullDate
    })

    days.push({ dayNum: d, fullDate, isCurrentMonth: true, isToday, events: dayEvents })
  }

  // 3. 다음 달 여백 채우기 (총 35일 또는 42일 맞추기)
  const totalCells = days.length <= 35 ? 35 : 42
  const nextDaysCount = totalCells - days.length
  for (let d = 1; d <= nextDaysCount; d++) {
    days.push({ dayNum: d, fullDate: '', isCurrentMonth: false, isToday: false, events: [] })
  }

  calendarDays.value = days
}

watch(activeTab, (val) => {
  if (val === 'grid') {
    nextTick(() => {
      grid?.redraw(true)
    })
  } else if (val === 'calendar') {
    nextTick(() => {
      buildCalendar()
    })
  }
})

onMounted(async () => {
  try {
    const res = await api.get('/hsaa/users')
    salesmanList.value = res.data || []
  } catch (e) {}

  grid = new Tabulator(gridRef.value!, {
    layout: "fitColumns",
    height: "100%",
    headerSort: false,
    columnDefaults: {
      headerHozAlign: 'center',
      vertAlign: "middle"
    },
    columns: [
      {
        title: "방문예정일", field: "plan_dt", width: 200, hozAlign: "center", cssClass: "fw-bold text-primary",
        formatter: (cell) => {
          const v = String(cell.getValue() || '');
          if (v.length === 8) {
            const y = v.substring(0, 4);
            const m = v.substring(4, 6);
            const d = v.substring(6, 8);
            const dateObj = new Date(`${y}-${m}-${d}`);
            if (!isNaN(dateObj.getTime())) {
              const days = ['일', '월', '화', '수', '목', '금', '토'];
              const dayOfWeek = days[dateObj.getDay()];
              return `${y}-${m}-${d} (${dayOfWeek})`;
            }
          }
          return v;
        }
      },
      { title: "거래처명", field: "custnm", width: 200, hozAlign: "left" },
      { title: "영업건명", field: "salestitle", minWidth: 200, hozAlign: "left" },
      { title: "중요도", field: "import_nm", width: 150, hozAlign: "center" },
      { title: "확도(%)", field: "succrate", width: 150, hozAlign: "center" },
      { title: "최종접촉일", field: "lastmtdt", width: 150, hozAlign: "center" },
      { title: "주소", field: "address", minWidth: 180, hozAlign: "left" }
    ]
  })
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
.btn-auto:hover { background-color: #e8590c; border-color: #d9480f; color: #fff; }
.btn-alarm { background-color: #0dcaf0; border-color: #0dcaf0; color: #fff; }
.btn-alarm:hover { background-color: #31d2f2; border-color: #25cff2; color: #fff; }
.btn-save { background-color: #198754; border-color: #198754; color: #fff; }
.btn-save:hover { background-color: #157347; border-color: #146c43; color: #fff; }

.erp-table-dense th { padding: 8px; border: 1px solid #dee2e6; font-size: 12px; font-weight: 600; color: #555; }
.erp-table-dense td { padding: 4px 8px; border: 1px solid #dee2e6; }
:deep(.tabulator-row.tabulator-selectable:hover) { background-color: #f8f9fa !important; }

/* 🗓️ 월간 캘린더 스타일 */
.calendar-wrapper { display: flex; flex-direction: column; height: 100%; background: #f8fafc; }
.calendar-header-grid { display: grid; grid-template-columns: repeat(7, 1fr); background: #f1f5f9; border-bottom: 1px solid #dee2e6; text-align: center; font-weight: bold; font-size: 12px; padding: 6px 0; }
.calendar-header-grid .cal-head { padding: 4px 0; }
.calendar-body-grid { display: grid; grid-template-columns: repeat(7, 1fr); grid-auto-rows: minmax(115px, 1fr); flex-grow: 1; gap: 1px; background: #dee2e6; }
.cal-cell { background: #fff; padding: 6px; display: flex; flex-direction: column; overflow: hidden; }
.cal-cell.other-month { background: #f8fafc; opacity: 0.5; }
.cal-cell.today { background: #eef2ff; border: 2px solid #0d6efd; }
.cal-day-num { font-size: 12px; font-weight: bold; margin-bottom: 4px; color: #333; }
.cal-events { display: flex; flex-direction: column; gap: 3px; overflow-y: auto; max-height: 110px; }
.cal-event-badge { background: #e0f2fe; border-left: 3px solid #0284c7; padding: 3px 6px; border-radius: 4px; font-size: 11px; cursor: grab; transition: all 0.2s; user-select: none; }
.cal-event-badge:active { cursor: grabbing; }
.cal-event-badge:hover { background: #bae6fd; }
.extra-small { font-size: 10px; }
</style>
