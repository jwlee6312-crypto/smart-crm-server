<!--
	=============================================================
	프로그램명	  : 인바운드 상담현황
    프로그램 ID	: HGIA020S
	작성일자	    : 2026.05.19
	작성자	      : AI Assistant
	Description	: 인바운드 상담 내역을 조회하고 녹취 파일을 청취하는 현황 화면
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

	<div class="hgia020s-wrapper bg-light text-start d-flex flex-column h-100 overflow-hidden p-2 gap-2">
		<!-- 1. 상단 표준 버튼 바 -->
		<div class="mb-1 btn-line flex-shrink-0">
			<button class="btn btn-sm btn-outline-secondary px-3" @click="initialize">초기화</button>
			<button class="btn btn-sm btn-dark px-3" @click="handleSearch">조회</button>
			<button class="btn btn-sm btn-outline-success px-3" @click="exportExcel">Excel</button>
		</div>

		<!-- 2. 조회 조건 영역 -->
		<div class="card shadow-sm border-0 mb-1 flex-shrink-0">
			<div class="card-body p-2 px-3">
				<div class="row g-3 align-items-center">
					<div class="col-auto">
						<label class="small fw-bold me-2"><i class="bi bi-calendar-check me-1 text-primary"></i>상담기간:</label>
						<div class="d-inline-flex align-items-center gap-1">
							<DateForm v-model:fromdt="searchForm.fromdt" v-model:todt="searchForm.todt" />
						</div>
					</div>
					<div class="col-auto">
						<label class="small fw-bold me-2 ms-3">고객명:</label>
						<input type="text" v-model="searchForm.custnm" class="form-control form-control-sm d-inline-block" style="width: 150px;" placeholder="검색어..." @keyup.enter="handleSearch">
					</div>
				</div>
			</div>
		</div>

		<!-- 3. 메인 그리드 영역 -->
		<div class="card shadow-sm border-0 flex-grow-1 overflow-hidden border-top border-3 border-primary">
			<div class="card-header bg-white py-1 px-3 fw-bold small border-bottom d-flex justify-content-between align-items-center">
				<span><i class="bi bi-list-columns-reverse me-2 text-primary"></i>상담 내역 리스트</span>
				<span class="text-muted extra-small">※ 행을 선택하면 상세 메모를 확인할 수 있습니다.</span>
			</div>
            <div class="card-body p-0 flex-grow-1 bg-white overflow-hidden d-flex flex-column">
                <div ref="mainGridRef" class="tabulator-instance flex-grow-1"></div>
            </div>
		</div>

		<!-- 4. 녹취 플레이어 모달 -->
		<div v-if="showAudioPlayer" class="audio-player-overlay d-flex justify-content-center align-items-center" @click.self="closePlayer">
			<div class="card shadow-lg border-0" style="width: 400px; border-radius: 15px; overflow: hidden;">
				<div class="card-header bg-dark text-white d-flex justify-content-between align-items-center py-2">
					<span class="small fw-bold"><i class="bi bi-play-circle-fill me-2 text-info"></i>녹취 파일 재생</span>
					<button type="button" class="btn-close btn-close-white" @click="closePlayer"></button>
				</div>
				<div class="card-body text-center p-4">
					<div class="mb-3 small fw-bold text-primary text-truncate">{{ currentFile }}</div>
					<audio ref="audioRef" :src="audioUrl" controls autoplay class="w-100">
						브라우저가 오디오 재생을 지원하지 않습니다.
					</audio>
				</div>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator, type CellComponent } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import { api } from '@/utils/axios'
import { getDate } from '@/composables/useDate'
import AppAlert from '@/components/AppAlert.vue'
import DateForm from '@/components/DateForm.vue'

const { showAlert, showError, vAlert, vAlertError, alertMessage } = useAlerts()
const { firstDay, today } = getDate()

// 1. 조회 조건
const searchForm = reactive({
	fromdt: firstDay,
	todt: today,
	custnm: ''
})

const mainGridRef = ref<HTMLElement | null>(null)
let mainGrid: Tabulator | null = null

// 오디오 플레이어 관련
const showAudioPlayer = ref(false)
const audioUrl = ref('')
const currentFile = ref('')
const audioRef = ref<HTMLAudioElement | null>(null)

onMounted(() => {
	if (mainGridRef.value) {
        if (mainGrid) mainGrid.destroy();
		mainGrid = new Tabulator(mainGridRef.value, {
			layout: 'fitColumns',
			height: '100%',
			placeholder: "조회된 상담 내역이 없습니다.",
			columnDefaults: { headerHozAlign: 'center', headerSort: false, resizable: false },
			columns: [
				{ title: "상담일시", field: "start_time", width: 140, hozAlign: 'center', formatter: (c:any) => c.getValue()?.substring(2, 16) || '-' },
				{ title: "고객사명", field: "custnm", width: 150, hozAlign: 'left', cssClass: 'fw-bold' },
				{ title: "발신번호", field: "hpno", width: 120, hozAlign: 'center', cssClass: 'text-primary fw-bold' },
				{ title: "발신자명", field: "call_usernm", width: 100, hozAlign: 'center' },
				{ title: "발신이메일", field: "call_email", width: 150, hozAlign: 'left' },
				{ title: "접수번호", field: "svcno", width: 120, hozAlign: 'center' },
				{ title: "상담요약", field: "ai_summary", minWidth: 200, hozAlign: 'left' },
				{ title: "상담원", field: "consultnm", width: 100, hozAlign: 'center' },
				{
					title: "녹취", field: "rec_file", width: 60, hozAlign: 'center',
					formatter: (cell: CellComponent) => {
						return cell.getValue() ? `<button class="btn btn-xs btn-primary py-0 px-2 shadow-none"><i class="bi bi-play-fill"></i></button>` : '';
					},
					cellClick: (e, cell) => {
						const file = cell.getValue();
						if (file) playRecording(file);
					}
				}
			]
		})
	}
	handleSearch()
})

const initialize = () => {
	searchForm.fromdt = firstDay; searchForm.todt = today; searchForm.custnm = '';
	mainGrid?.clearData();
}

const handleSearch = async () => {
	try {
		const params = {
			fromdt: searchForm.fromdt.replaceAll('-', ''),
			todt: searchForm.todt.replaceAll('-', ''),
			custnm: searchForm.custnm
		}
		const res = await api.get('/crm/inbound/status-list', { params });
		mainGrid?.setData(res.data || []);
	} catch (e) { vAlertError('조회 실패'); }
}

const playRecording = (file: string) => {
	currentFile.value = file;
	audioUrl.value = `/crm/inbound/play-recording?file=${encodeURIComponent(file)}&t=${new Date().getTime()}`;
	showAudioPlayer.value = true;
	nextTick(() => {
		if (audioRef.value) {
			audioRef.value.load();
			audioRef.value.play().catch(() => {});
		}
	});
}

const closePlayer = () => {
	if (audioRef.value) audioRef.value.pause();
	showAudioPlayer.value = false;
}

const exportExcel = () => {
	mainGrid?.download("xlsx", "인바운드상담현황.xlsx");
}

onUnmounted(() => {
    mainGrid?.destroy();
})
</script>

<style scoped>
.hgia020s-wrapper { height: calc(100vh - 110px); font-family: 'Pretendard', sans-serif; }
.tabulator-full-height { height: 100% !important; border: 1px solid #dee2e6; font-size: 0.85rem; }
.btn-line button { background: #fff; border: 1px solid #dee2e6; padding: 2px 15px; font-size: 0.85rem; border-radius: 4px; margin-right: 4px; transition: all 0.2s; }
.audio-player-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.6); z-index: 12000; }
.btn-xs { padding: 1px 5px; font-size: 0.75rem; }
:deep(.tabulator-header) { background-color: #f8f9fa !important; font-weight: bold; }
:deep(.tabulator-row:hover) { background-color: #f0f7ff !important; cursor: pointer; }
</style>
