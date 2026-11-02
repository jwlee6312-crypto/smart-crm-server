<!--
	=============================================================
	프로그램명	: 거래 주소록 등록 (HSBA130U)
	작성일자	: 2025.02.27
	설명        : 영업/구매 거래처 주소록 마스터 관리 (Tab 키 순항 및 Alt 단축키 지원)
	=============================================================
-->

<template>
  <AppAlert :show="showalert" :error="showerror" :message="alertmessage" />

  <!-- 💡 [진짜 표준] 최외각 wrapper: h-100 및 overflow-hidden으로 스크롤 고립 -->
  <div class="erp-container">
    <!-- 🚀 1. 상단 액션 바 -->
    <div class="erp-header d-flex justify-content-between align-items-center border-bottom bg-white py-1 px-3 sticky-top shadow-sm flex-shrink-0">
      <div class="fw-bold text-dark d-flex align-items-center" style="font-size: 13px;">
        <i class="bi bi-truck me-2 text-primary"></i>
        기본정보 > <span class="text-primary fw-bolder">거래 주소록 등록 (HSBA130U)</span>
      </div>
      <div class="btn-group-erp d-flex gap-1">
        <button class="btn-erp btn-init" @click="initialize(false)" title="Alt+N: 초기화">초기화(N)</button>
        <button class="btn-erp btn-search" @click="searchcustomers" title="Alt+F: 조회">조회(F)</button>
        <button class="btn-erp btn-geo" @click="batchUpdateGeocode" :disabled="isGeocoding" title="위경도 없는 항목 자동 일괄 지오코딩">
          <span v-if="isGeocoding" class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
          {{ isGeocoding ? '업데이트 중...' : '위경도일괄업데이트' }}
        </button>
        <button class="btn-erp btn-save" @click="save" title="Alt+S: 저장">저장(S)</button>
      </div>
    </div>

    <!-- 💡 2. 메인 컨텐츠 영역 -->
    <div class="flex-grow-1 d-flex flex-column gap-2 p-2 overflow-hidden bg-light">
      <!-- 🅰️ 조회 조건 영역 -->
      <div class="card border shadow-sm flex-shrink-0">
        <div class="card-body p-1 bg-white">
          <div class="d-flex align-items-center gap-3 px-2">
            <div class="input-group input-group-sm flex-nowrap" style="width: 400px;">
              <span class="input-group-text fw-bold border-0 bg-transparent">거래처검색</span>
              <input ref="firstFocusRef" v-model="searchdata.qcustnm" type="text" class="form-control border-0 bg-light" tabindex="1" placeholder="거래처명 또는 코드 입력" @keyup.enter="searchcustomers" />
              <button class="btn btn-dark btn-sm" tabindex="2" @click="searchcustomers"><i class="bi bi-search"></i></button>
            </div>
          </div>
        </div>
      </div>

      <!-- 🅱️ 배송처 정보 입력 영역 (HSOD100U 스타일 및 탭키 순차 순항) -->
      <div class="card border shadow-sm overflow-hidden flex-shrink-0">
        <div class="card-header py-1 px-3 border-bottom d-flex align-items-center justify-content-between" style="background-color: #f0f7ff !important;">
          <span class="fw-bold small text-primary"><i class="bi bi-pencil-square me-1"></i> 주소 정보 입력</span>
          <div class="d-flex gap-2 align-items-center">
            <span v-if="masterdata.actkind === 'U0'" class="badge bg-warning text-dark" style="font-size: 10px;">수정 모드 ({{ masterdata.custcd }} - 순번: {{ masterdata.trancd }})</span>
            <span v-else class="badge bg-primary" style="font-size: 10px;">신규 등록</span>
          </div>
        </div>
        <div class="card-body p-0 bg-white">
          <table class="erp-table-dense w-100">
            <colgroup>
              <col style="width: 90px;" /><col style="width: 130px;"/>
              <col style="width: 90px;" /><col style="width: 130px;" />
              <col style="width: 90px;" /><col style="width: 130px;"/>
              <col style="width: 90px;" /><col style="width: 130px;"/>
              <col style="width: 90px;" /><col style="width: 80px;"/>
            </colgroup>
            <tbody>
              <tr>
                <th class="required bg-light text-center">거 래 처</th>
                <td colspan="3">
                  <div class="input-group input-group-sm">
                    <input v-model="masterdata.custcd" type="text" class="form-control text-center bg-light" style="max-width: 80px;" tabindex="3" readonly />
                    <input v-model="masterdata.custnm" type="text" class="form-control bg-light" placeholder="거래처 선택" tabindex="4" readonly />
                    <button class="btn btn-outline-secondary" tabindex="5" @click="openhelp('cust')" :disabled="masterdata.actkind === 'U0'"><i class="bi bi-search"></i></button>
                  </div>
                </td>
                <th class="required bg-light text-center">주&nbsp;&nbsp;&nbsp;&nbsp;소</th>
                <td colspan="5">
                  <AddressPopupForm v-model:postno="masterdata.postno" v-model:address="masterdata.address" v-model:d_address="masterdata.address_detail" tabindex="6" active />
                </td>
              </tr>
              <tr>
                <th class="bg-light text-center">위&nbsp;&nbsp;&nbsp;&nbsp;도</th>
                <td>
                  <input v-model="masterdata.latitude" type="text" class="form-control form-control-sm" tabindex="7" placeholder="자동계산" />
                </td>
                <th class="bg-light text-center">경&nbsp;&nbsp;&nbsp;&nbsp;도</th>
                <td>
                  <input v-model="masterdata.longitude" type="text" class="form-control form-control-sm" tabindex="8" placeholder="자동계산" />
                </td>
                <th class="bg-light text-center">담 당 자</th>
                <td>
                  <input v-model="masterdata.damdang" type="text" class="form-control form-control-sm" tabindex="9" maxlength="30" />
                </td>
                <th class="bg-light text-center">전화번호</th>
                <td>
                  <input v-model="masterdata.telno" type="text" class="form-control form-control-sm" tabindex="10" maxlength="30" />
                </td>
                <th class="bg-light text-center">사용여부</th>
                <td>
                  <div class="form-check form-switch m-0 d-flex justify-content-center">
                    <input v-model="masterdata.useyn" class="form-check-input" type="checkbox" id="useyn130" true-value="Y" false-value="N" tabindex="11">
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Ⓒ 하단 목록 영역 -->
      <div class="d-flex flex-grow-1 gap-2 overflow-hidden" style="min-height: 0;">
        <!-- 좌측: 거래처 목록 -->
        <div class="card border shadow-sm d-flex flex-column overflow-hidden flex-shrink-0" style="width: 320px;">
          <div class="card-header bg-white py-1 px-3 border-bottom fw-bold small text-dark">
            <i class="bi bi-building me-1"></i> 거래처 목록
          </div>
          <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
              <div ref="custgridelement" class="tabulator-instance flex-grow-1" tabindex="11"></div>
          </div>
        </div>

        <!-- 우측: 주소록 내역 목록 -->
        <div class="card border shadow-sm flex-grow-1 d-flex flex-column overflow-hidden">
          <div class="card-header bg-white py-1 px-3 border-bottom d-flex align-items-center justify-content-between flex-shrink-0">
            <span class="fw-bold small text-dark d-flex align-items-center">
              <i class="bi bi-grid-3x3-gap-fill me-2 text-primary"></i> 주소록 설정 내역
              <span v-if="selectedcustname" class="badge bg-primary-subtle text-primary border border-primary-subtle ms-2">{{ selectedcustname }}</span>
            </span>
          </div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="trangridelement" class="tabulator-instance flex-grow-1" tabindex="12"></div>
            </div>
        </div>
      </div>
    </div>
    <Modal v-model:visible="modalvisible" :modalProps="modalprops" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import AppAlert from '@/components/AppAlert.vue'
import Modal from '@/components/Modal.vue'
import AddressPopupForm from '@/components/AddressPopupForm.vue'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { useAuthStore } from '@/stores/authStore'
import { useFormReset } from '@/composables/useFormReset'
import { useManualStore } from '@/stores/manualStore'
import type { ModalProps } from '@/types/modal'

const authstore = useAuthStore()
const { showAlert: showalert, showError: showerror, alertMessage: alertmessage, vAlert: valert, vAlertError: valerterror } = useAlerts()
const { resetForm: resetform } = useFormReset()
const manualStore = useManualStore()

const firstFocusRef = ref<HTMLInputElement | null>(null)

// 1. 상태 관리
const searchdata = reactive({ qcustnm: '' })
const masterdata = reactive<any>({
  actkind: 'A0', cmpycd: authstore.cmpycd, custcd: '', custnm: '', trancd: '',
  postno: '', address: '', address_detail: '', damdang: '', telno: '', email: '', useyn: 'Y',
  latitude: '', longitude: ''
})

const selectedcustname = ref('')
const custgridelement = ref<HTMLElement | null>(null)
const trangridelement = ref<HTMLElement | null>(null)
let custgrid: Tabulator | null = null
let trangrid: Tabulator | null = null

// 2. 그리드 초기화
const initgrids = () => {
  if (custgridelement.value) {
    custgrid = new Tabulator(custgridelement.value, {
      layout: "fitColumns", height: "100%", placeholder: "데이터 없음",
      pagination: "local", paginationSize: 20, paginationButtonCount: 3,
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        {
          title: "거래처 상호", field: "custnm", hozAlign: "left",
          formatter: (cell) => {
            const v = cell.getValue() || '';
            return cell.getData().useyn === 'Y' ? `<span class="text-primary fw-bold">${v}</span>` : v;
          }
        }
      ]
    })
    custgrid.on("rowClick", (e, row) => {
      const data = row.getData()
      masterdata.custcd = data.custcd;
      masterdata.custnm = data.custnm;
      selectedcustname.value = data.custnm;
      initialize(true)
      fetchdeliverylist()
    })
  }

  if (trangridelement.value) {
    trangrid = new Tabulator(trangridelement.value, {
      layout: "fitColumns", height: "100%", placeholder: "내역 없음",
      columnDefaults: { headerSort: false, headerHozAlign: "center", hozAlign: "center", vertAlign: "middle" },
      columns: [
        { title: "순번", field: "trancd", width: 60 },
        { title: "담당자", field: "damdang", width: 120 },
        { title: "전화번호", field: "telno", width: 130 },
        { title: "위도", field: "latitude", width: 110 },
        { title: "경도", field: "longitude", width: 110 },
        { title: "주소", field: "address", minWidth: 250, hozAlign: "left" },
        { title: "상세 주소", field: "d_address", minWidth: 150, hozAlign: "left" },
        { title: "사용", field: "useyn", width: 80, hozAlign: "center",  editor: true,
          formatter: (cell) => {
            const val = String(cell.getValue() || '').trim().toUpperCase();
            return val === 'Y' ? '<b class="text-primary">사용</b>' : '';
          }
        }
      ]
    })
    trangrid.on("rowClick", (e, row) => {
      const data = row.getData();
      Object.assign(masterdata, data);
      masterdata.useyn = String(data.useyn || 'Y').toUpperCase();
      if (data.address && data.address.includes('  ')) {
        const parts = data.address.split('  '); masterdata.address = parts[0]; masterdata.address_detail = parts[1];
      } else {
        masterdata.address = data.address; masterdata.address_detail = '';
      }
      masterdata.actkind = 'U0';
    })
  }
}

// 3. 기능 구현
async function searchcustomers() {
  try {
    const res = await api.post('/hsba/HSBA_130U_STR', {
      actkind: 'S1', cmpycd: authstore.cmpycd, custcd: '', custnm: searchdata.qcustnm || '',
      trancd: '', area: '', postno: '', address: '', d_address: '', damdang: '', telno: '', email: '', useyn: '', userid: authstore.userid
    })
    custgrid?.setData(res.data || []); trangrid?.clearData(); selectedcustname.value = ''
  } catch (e) { valerterror('조회 실패') }
}

async function fetchdeliverylist() {
  try {
    const res = await api.post('/hsba/HSBA_130U_STR', {
      actkind: 'S2', cmpycd: authstore.cmpycd, custcd: masterdata.custcd,
      custnm: '', trancd: '', area: '', postno: '', address: '', d_address: '', damdang: '', telno: '', email: '', useyn: '', userid: authstore.userid
    })
    trangrid?.setData(res.data || [])
  } catch (e) { valerterror('내역 조회 실패') }
}

const isGeocoding = ref(false)

async function batchUpdateGeocode() {
  if (!confirm('위경도 좌표가 없는 모든 배송처 주소를 대상으로 자동 일괄 업데이트를 수행하시겠습니까? (다소 시간이 걸릴 수 있습니다)')) return
  isGeocoding.value = true
  valert('⏳ 위경도 일괄 업데이트를 진행 중입니다... 잠시만 기다려주세요.')
  try {
    const res = await api.post('/hsba/batch-geocode', { cmpycd: authstore.cmpycd })
    valert('✅ ' + (res.data?.msg || '위경도 일괄 업데이트가 성공적으로 완료되었습니다.'))
    if (masterdata.custcd) fetchdeliverylist()
  } catch (e) {
    valerterror('❌ 위경도 일괄 업데이트 중 오류가 발생했습니다.')
  } finally {
    isGeocoding.value = false
  }
}

async function save() {
  if (!masterdata.custcd) return valerterror('거래처를 먼저 선택하세요.')
  if (!masterdata.address) return valerterror('주소를 입력해 주십시요.')
  if (!confirm('저장하시겠습니까?')) return
  try {
    const payload = { ...masterdata, address: `${masterdata.address}  ${masterdata.address_detail}`.trim(), userid: authstore.userid }
    const res = await api.post('/hsba/HSBA_130U_STR', payload)
    const resdata = res.data?.[0];
    if (resdata.result === 'N' || resdata.erryn === 'Y') valerterror(resdata.msg || '저장 실패')
    else {
      valert('성공적으로 저장되었습니다.');
      fetchdeliverylist();
      if (masterdata.actkind === 'A0') initialize(true)
    }
  } catch (e) { valerterror('저장 중 오류 발생') }
}

function initialize(keepcust = false) {
  const cd = masterdata.custcd; const nm = masterdata.custnm; resetform(masterdata)
  Object.assign(masterdata, { actkind: 'A0', cmpycd: authstore.cmpycd, useyn: 'Y', latitude: '', longitude: '' })
  if (keepcust) { masterdata.custcd = cd; masterdata.custnm = nm } else { selectedcustname.value = ''; trangrid?.clearData() }
  nextTick(() => firstFocusRef.value?.focus())
}

const modalvisible = ref(false); const modalprops = reactive<ModalProps>({ title: '', path: '', defaultField: '', columns: [], data: {}, onConfirm: () => {}, type: 'table' })
function openhelp(type: string) {
  if (type === 'cust') {
    Object.assign(modalprops, { title: '거래처 선택', path: '/ha00/HA00_00P_STR', defaultField: 'custnm', large: true, data: { gubun: 'C4', cmpycd: authstore.cmpycd }, columns: [{ title: '코드', field: 'custcd', width: 80 }, { title: '거래처명', field: 'custnm', width: 200 }, { title: '사업자번호', field: 'custno', width: 120 }, { title: '주소', field: 'address', minWidth: 300 }], onConfirm: (d: any) => {
      const n = d;
      masterdata.custcd = n.custcd; masterdata.custnm = n.custnm
      selectedcustname.value = n.custnm;
      initialize(true);
      fetchdeliverylist();
    } })
    modalvisible.value = true
  }
}

/** 🚀 [HSOD100U 표준 키보드 단축키 핸들러 연동] */
function handleGlobalShortcuts(e: KeyboardEvent) {
  if (e.altKey) {
    const key = e.key.toLowerCase()
    if (key === 'n') { e.preventDefault(); initialize(false) }
    else if (key === 'f') { e.preventDefault(); searchcustomers() }
    else if (key === 's') { e.preventDefault(); save() }
    else if (key === 'h') { e.preventDefault(); manualStore.open('HSBA130U') }
  }
}

onMounted(async () => {
  window.addEventListener('keydown', handleGlobalShortcuts)
  nextTick(() => { initgrids(); searchcustomers(); firstFocusRef.value?.focus() })
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalShortcuts)
})
</script>

<style scoped>
.tabulator-instance { width:!important; background-color: #fff; }
.tabulator-instance { width: 100% !important; background-color: #fff; }
.btn-geo { background-color: #6f42c1; border-color: #6f42c1; color: #fff; height: 28px; padding: 0 12px; font-size: 12px; font-weight: 600; border-radius: 4px; transition: all 0.2s; }
.btn-geo:hover { background-color: #59339d; border-color: #512da8; color: #fff; }
input:focus, select:focus, button:focus {
  border-color: #005a9f !important;
  box-shadow: 0 0 0 0.2rem rgba(0, 90, 159, 0.25) !important;
  outline: none;
}
</style>