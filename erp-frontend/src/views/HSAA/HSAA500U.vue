<!--
	=============================================================
	프로그램명	: 자동 주간 영업활동 보고서 (HSAA500U)
	작성일자	: 2026.10.30
	설명        : 지난주 주요 실적(좌) 및 다음주 예정 사항(우) 좌우 분할 배치, 하단 특이사항 영역 지정, 세련된 기업형 대시보드 디자인 적용
	=============================================================
-->

<template>
  <AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

  <div class="erp-container d-flex flex-column h-100 bg-white">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom">
      <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
        <i class="bi bi-file-earmark-text-fill me-2 text-primary" style="font-size: 18px;"></i>
        영업활동관리 <i class="bi bi-chevron-right mx-1 small opacity-50"></i>
        <span class="text-primary fw-bolder">자동 주간 영업활동 보고서 (HSAA500U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1 pe-3">
        <button class="btn-erp btn-init" @click="initialize">초기화</button>
        <button class="btn-erp btn-search" @click="fetchWeeklyReport">조회</button>
        <button class="btn-erp btn-auto" @click="autoGenerateReport"><i class="bi bi-magic me-1"></i>주간보고 자동생성(A)</button>
        <button class="btn-erp btn-save" @click="saveReport">저장(S)</button>
        <button class="btn-erp btn-print" @click="exportExcel"><i class="bi bi-file-earmark-excel-fill me-1"></i>엑셀다운</button>
        <button class="btn-erp btn-print" @click="sendEmailReport"><i class="bi bi-envelope-fill me-1"></i>메일발송</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 overflow-hidden p-3 d-flex flex-column gap-3 bg-light main-content-wrapper">

      <!-- [상단] 조건 설정 영역 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-2 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 8%;" /><col style="width: 20%;" />
              <col style="width: 8%;" /><col style="width: 20%;" />
              <col style="width: 12%;" /><col style="width: 32%;" />
            </colgroup>
            <tbody>
              <tr>
                <th class="text-center bg-light">주차 식별자</th>
                <td>
                  <input v-model="filter.weekId" type="text" class="form-control form-control-sm" placeholder="예: 2026-W43" />
                </td>
                <th class="text-center bg-light border-start">영업사원</th>
                <td>
                  <select v-model="filter.userid" class="form-select form-select-sm">
                    <option v-for="user in salesmanList" :key="user.userid" :value="user.userid">{{ user.usernm }}</option>
                  </select>
                </td>
                <th class="text-center bg-light border-start">기간(지난주~다음주)</th>
                <td class="d-flex align-items-center gap-1">
                  <input v-model="filter.sdate" type="text" class="form-control form-control-sm" placeholder="지난주시작(YYYYMMDD)" maxlength="8" />
                  <span>~</span>
                  <input v-model="filter.edate" type="text" class="form-control form-control-sm" placeholder="지난주종료(YYYYMMDD)" maxlength="8" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- [중앙] 좌우 분할 편집 영역 (좌: 실적 / 우: 예정) -->
      <div class="d-flex flex-row gap-3 flex-grow-1 overflow-hidden" style="min-height: 280px;">
        <!-- 좌측: 지난주 주요 실적 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden bg-white flex-grow-1" style="width: 50%;">
          <div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-primary"><i class="bi bi-check2-square me-2"></i>1. 지난주 주요 실적 및 상담 이력</span>
            <span class="badge bg-primary-subtle text-primary x-small">Performance</span>
          </div>
          <div class="card-body p-2 flex-grow-1 d-flex flex-column bg-white">
            <textarea v-model="reportForm.last_week_perf" class="form-control font-monospace flex-grow-1 border-0 bg-light bg-opacity-25 p-2 small" style="resize: none; line-height: 1.5;" placeholder="지난주 주요 실적 내용..."></textarea>
          </div>
        </div>

        <!-- 우측: 금주(다음주) 예정 사항 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden bg-white flex-grow-1" style="width: 50%;">
          <div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-success"><i class="bi bi-calendar-event me-2"></i>2. 금주(다음주) 방문 및 미팅 예정 계획</span>
            <span class="badge bg-success-subtle text-success border-0 x-small">Schedule Plan</span>
          </div>
          <div class="card-body p-2 flex-grow-1 d-flex flex-column bg-white">
            <textarea v-model="reportForm.next_week_plan" class="form-control font-monospace flex-grow-1 border-0 bg-light bg-opacity-25 p-2 small" style="resize: none; line-height: 1.5;" placeholder="다음주 예정 사항 내용..."></textarea>
          </div>
        </div>
      </div>

      <!-- [하단] 특이사항 및 건의사항 영역 -->
      <div class="card border shadow-sm flex-shrink-0 overflow-hidden bg-white" style="height: 140px;">
        <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
          <span class="fw-bold small text-danger"><i class="bi bi-exclamation-triangle me-2"></i>3. 특이사항 및 건의사항</span>
          <span class="badge bg-danger-subtle text-danger border-0 x-small">Issues & Comments</span>
        </div>
        <div class="card-body p-2 d-flex flex-column bg-white">
          <textarea v-model="reportForm.issues_comments" class="form-control font-monospace flex-grow-1 border-0 bg-light bg-opacity-25 p-2 small" style="resize: none;" placeholder="특이사항 및 건의사항 입력..."></textarea>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import * as XLSX from 'xlsx'

const authstore = useAuthStore()
const { showAlert, showError, vAlert, vAlertError, alertMessage } = useAlerts()

const filter = reactive({
  weekId: '2026-W43',
  userid: authstore.userid || '',
  sdate: '20261019',
  edate: '20261025'
})

const updateDatesFromWeekId = (weekIdStr: string) => {
  const match = weekIdStr.match(/^(\d{4})-?W(\d{1,2})$/i)
  if (!match) return

  const year = parseInt(match[1])
  const week = parseInt(match[2])

  const jan4 = new Date(year, 0, 4)
  const dayOfWeek = jan4.getDay() || 7
  const week1Monday = new Date(jan4.getTime())
  week1Monday.setDate(jan4.getDate() - dayOfWeek + 1)

  const targetMonday = new Date(week1Monday.getTime())
  targetMonday.setDate(week1Monday.getDate() + (week - 1) * 7)

  const targetSunday = new Date(targetMonday.getTime())
  targetSunday.setDate(targetMonday.getDate() + 6)

  const formatDateYmd = (d: Date) => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}${m}${day}`
  }

  filter.sdate = formatDateYmd(targetMonday)
  filter.edate = formatDateYmd(targetSunday)
  reportForm.sdate = filter.sdate
  reportForm.edate = filter.edate
}

watch(() => filter.weekId, (newVal) => {
  if (newVal) {
    updateDatesFromWeekId(newVal)
  }
})

const salesmanList = ref<any[]>([])
const reportForm = reactive({
  cmpycd: authstore.cmpycd,
  userid: authstore.userid || '',
  week_id: '2026-W43',
  sdate: '20261019',
  edate: '20261025',
  last_week_perf: '',
  next_week_plan: '',
  issues_comments: '',
  status: '1'
})

const initialize = () => {
  reportForm.last_week_perf = ''
  reportForm.next_week_plan = ''
  reportForm.issues_comments = ''
  reportForm.status = '1'
  vAlert('초기화되었습니다.')
}

const fetchWeeklyReport = async () => {
  try {
    const res = await api.get('/hsaa/weekly-report', {
      params: { weekId: filter.weekId, userid: filter.userid }
    })
    if (res.data && res.data.data) {
      Object.assign(reportForm, res.data.data)
      vAlert('주간보고서를 조회했습니다.')
    }
  } catch (e) {
    vAlertError('주간보고서 조회 중 오류 발생')
  }
}

const autoGenerateReport = async () => {
  if (!confirm('지난주 상담일지와 다음주 방문 계획을 기반으로 주간 영업보고서 초안을 자동으로 생성하시겠습니까?')) return
  try {
    const getNextWeekDates = (sdateStr: string) => {
      if (!sdateStr || sdateStr.length !== 8) return { s: '20261026', e: '20261101' }
      const y = parseInt(sdateStr.substring(0, 4))
      const m = parseInt(sdateStr.substring(4, 6)) - 1
      const d = parseInt(sdateStr.substring(6, 8))

      const monday = new Date(y, m, d)
      const nextMonday = new Date(monday.getTime())
      nextMonday.setDate(monday.getDate() + 7)
      const nextSunday = new Date(nextMonday.getTime())
      nextSunday.setDate(nextMonday.getDate() + 6)

      const format = (dt: Date) => {
        const yy = dt.getFullYear()
        const mm = String(dt.getMonth() + 1).padStart(2, '0')
        const dd = String(dt.getDate()).padStart(2, '0')
        return `${yy}${mm}${dd}`
      }
      return { s: format(nextMonday), e: format(nextSunday) }
    }

    const nextDates = getNextWeekDates(filter.sdate)

    const res = await api.post('/hsaa/weekly-report/auto-generate', {
      weekId: filter.weekId,
      userid: filter.userid,
      sdate: filter.sdate,
      edate: filter.edate,
      nextSdate: nextDates.s,
      nextEdate: nextDates.e
    })
    if (res.data && res.data.data) {
      Object.assign(reportForm, res.data.data)
      vAlert('주간 영업보고서 초안이 성공적으로 자동 생성되었습니다. 내용을 검토 후 수정 및 저장하세요.')
    }
  } catch (e) {
    vAlertError('주간 영업보고서 자동 생성 실패')
  }
}

const saveReport = async () => {
  if (!confirm('현재 작성된 주간 영업보고서를 저장하시겠습니까?')) return
  try {
    reportForm.week_id = filter.weekId
    reportForm.sdate = filter.sdate
    reportForm.edate = filter.edate
    reportForm.userid = filter.userid
    await api.post('/hsaa/weekly-report/save', reportForm)
    vAlert('주간 영업보고서가 성공적으로 저장되었습니다.')
    fetchWeeklyReport()
  } catch (e) {
    vAlertError('저장 중 오류 발생')
  }
}

const exportExcel = () => {
  const wb = XLSX.utils.book_new()
  const wsData = [
    ['[자동 주간 영업활동 보고서]'],
    ['주차:', filter.weekId, '작성자:', filter.userid],
    ['대상 기간:', `${filter.sdate} ~ ${filter.edate}`],
    [],
    ['1. 지난주 주요 실적'],
    [reportForm.last_week_perf],
    [],
    ['2. 금주(다음주) 예정 사항'],
    [reportForm.next_week_plan],
    [],
    ['3. 특이사항 및 건의사항'],
    [reportForm.issues_comments]
  ]
  const ws = XLSX.utils.aoa_to_sheet(wsData)
  XLSX.utils.book_append_sheet(wb, ws, '주간보고서')
  XLSX.writeFile(wb, `Weekly_Report_${filter.weekId}_${filter.userid}.xlsx`)
  vAlert('주간보고서가 엑셀 파일로 다운로드되었습니다.')
}

const sendEmailReport = () => {
  vAlert(`[${filter.userid}]님의 ${filter.weekId} 주간 영업활동 보고서 메일 발송이 요청되었습니다.`)
}

onMounted(async () => {
  try {
    const res = await api.get('/hsaa/users')
    salesmanList.value = res.data || []
  } catch (e) {}
  fetchWeeklyReport()
})
</script>

<style scoped>
.erp-container { font-family: 'Pretendard', sans-serif; letter-spacing: -0.03rem; }
.x-small { font-size: 11px; }

.erp-header { height: 48px; padding: 0 10px; background-color: #fff; }
.btn-erp { height: 28px; padding: 0 12px; font-size: 12px; font-weight: 600; border-radius: 4px; transition: all 0.2s; border: 1px solid #dee2e6; background-color: #fff; color: #444; }
.btn-erp:hover { background-color: #f8f9fa; border-color: #ced4da; }
.btn-init { color: #6c757d; }
.btn-search { background-color: #0d6efd; border-color: #0d6efd; color: #fff; }
.btn-search:hover { background-color: #0b5ed7; border-color: #0a58ca; color: #fff; }
.btn-auto { background-color: #fd7e14; border-color: #fd7e14; color: #fff; }
.btn-auto:hover { background-color: #e8590c; border-color: #d9480f; color: #fff; }
.btn-save { background-color: #198754; border-color: #198754; color: #fff; }
.btn-save:hover { background-color: #157347; border-color: #146c43; color: #fff; }
.btn-print { background-color: #6c757d; border-color: #6c757d; color: #fff; }
.btn-print:hover { background-color: #5c636a; border-color: #565e64; color: #fff; }

.erp-table-dense th { padding: 8px; border: 1px solid #dee2e6; font-size: 12px; font-weight: 600; color: #555; }
.erp-table-dense td { padding: 4px 8px; border: 1px solid #dee2e6; }
</style>
