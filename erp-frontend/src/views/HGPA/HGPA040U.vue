<!--
	=============================================================
	프로그램명  : ARS 안내멘트 관리 (TTS 연동)
    프로그램 ID	: HGPA040U
	작성일자	    : 2025.03.14
	작성자      : AI Assistant
    설명        : ARS 대본 수정 및 TTS 음원 실시간 생성 관리
	=============================================================
-->

<template>
	<AppAlert :show="showAlert" :error="showError" :message="alertMessage" />

    <div class="erp-container d-flex flex-column h-100 bg-white">
        <!-- 🚀 1. 상단 액션 바 -->
        <div class="erp-header d-flex justify-content-between align-items-center flex-shrink-0 border-bottom bg-white py-2 px-3 sticky-top shadow-sm">
            <div class="fw-bold ps-1 text-dark d-flex align-items-center" style="font-size: 14px;">
                <i class="bi bi-mic-fill me-2 text-primary" style="font-size: 18px;"></i>
                시스템관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
                통신관리 <i class="bi bi-chevron-right mx-2 small opacity-50"></i>
                <span class="text-primary fw-bolder">ARS 안내멘트 관리 (HGPA040U)</span>
            </div>
            <div class="btn-group-erp d-flex gap-1 pe-3">
                <button class="btn-erp btn-init" @click="initialize">초기화</button>
                <button class="btn-erp btn-search" @click="search">조회</button>
                <button class="btn-erp btn-save" @click="save">음원 적용</button>
            </div>
        </div>

        <div class="row g-1 flex-grow-1 overflow-hidden p-2 bg-light main-content-wrapper">
            <!-- ⬅️ 2. 좌측: 대본 목록 -->
            <div class="col-md-4 h-100 d-flex flex-column">
                <div class="card border shadow-sm h-100 overflow-hidden d-flex flex-column">
                    <div class="card-header bg-white py-2 px-3 border-bottom d-flex justify-content-between align-items-center">
                        <span class="fw-bold small text-dark"><i class="bi bi-list-columns-reverse me-1"></i> ARS 대본 리스트</span>
                    </div>
                    <div class="card-body p-0 flex-grow-1 bg-white position-relative overflow-hidden d-flex flex-column">
                        <div ref="tableRef" class="tabulator-instance flex-grow-1" />
                    </div>
                </div>
            </div>

            <!-- 🔄 3. 우측: 상세 편집기 -->
            <div class="col h-100 d-flex flex-column">
                <div class="card border shadow-sm h-100 overflow-hidden d-flex flex-column bg-white">
                    <div class="card-header bg-white py-2 px-3 border-bottom d-flex align-items-center">
                        <span class="fw-bold small text-dark"><i class="bi bi-pencil-square me-2 text-primary"></i> 대본 상세 수정 및 미리보기</span>
                    </div>
                    <div class="card-body p-4 bg-white overflow-auto">
                        <div v-if="selectedScript" class="d-flex flex-column gap-4 h-100">
                            <!-- 스크립트 정보 -->
                            <div class="d-flex align-items-center gap-3">
                                <div class="bg-primary bg-opacity-10 p-3 rounded-3 border border-primary border-opacity-25">
                                    <div class="text-muted small fw-bold mb-1">스크립트 ID</div>
                                    <div class="h5 mb-0 fw-bold text-primary">{{ selectedScript.id }}</div>
                                </div>
                                <div class="flex-grow-1">
                                    <div class="text-muted small fw-bold mb-1">용도</div>
                                    <div class="h6 mb-0 fw-bold text-dark">{{ selectedScript.description }}</div>
                                </div>
                                <!-- 💡 음원 상태 표시 추가 -->
                                <div class="text-end">
                                    <div class="text-muted small fw-bold mb-1">음원 적용 상태</div>
                                    <span v-if="selectedScript.file_exists" class="badge bg-success shadow-sm p-2 px-3">
                                        <i class="bi bi-check-circle-fill me-1"></i> 서버 음원 존재
                                    </span>
                                    <span v-else class="badge bg-danger shadow-sm p-2 px-3">
                                        <i class="bi bi-x-circle-fill me-1"></i> 음원 미생성
                                    </span>
                                </div>
                            </div>

                            <!-- 텍스트 편집기 -->
                            <div class="flex-grow-1 d-flex flex-column">
                                <label class="form-label fw-bold text-secondary small"><i class="bi bi-chat-left-dots-fill me-1"></i>안내 방송 내용</label>
                                <textarea v-model="selectedScript.script_text" class="form-control flex-grow-1 border-2 border-primary-subtle p-3 fs-5"
                                          style="line-height: 1.6; resize: none;"
                                          placeholder="방송할 내용을 입력하세요."></textarea>
                                <div class="mt-2 text-muted italic small">
                                    <i class="bi bi-info-circle me-1"></i> [음원 적용] 버튼을 누르면 인공지능 목소리로 변환되어 즉시 반영됩니다.
                                </div>
                            </div>

                            <!-- 미리듣기 영역 -->
                            <div class="mt-auto pt-3 border-top d-flex gap-2">
                                <button class="btn btn-dark px-4 py-2 fw-bold d-flex align-items-center gap-2" @click="playCurrentVoice">
                                    <i class="bi bi-play-circle-fill"></i> 현재 서버 음원 듣기
                                </button>
                                <button class="btn btn-primary flex-grow-1 py-2 fw-bold d-flex align-items-center justify-content-center gap-2 shadow-sm" @click="save">
                                    <i class="bi bi-check-all fs-5"></i> 새로운 대본으로 음원 생성 및 서버 적용
                                </button>
                            </div>
                        </div>
                        <div v-else class="h-100 d-flex flex-column align-items-center justify-content-center text-muted">
                            <i class="bi bi-cursor fs-1 opacity-25 mb-3"></i>
                            <p>좌측 목록에서 수정할 대본을 선택하세요.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 🎙️ 오디오 플레이어 -->
        <audio ref="audioPlayer" hidden></audio>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator_bootstrap5.min.css'
import { useAlerts } from '@/composables/useAlerts'
import AppAlert from '@/components/AppAlert.vue'
import { api } from '@/utils/axios'

const { showAlert, showError, vAlert, vAlertError, alertMessage } = useAlerts()

const tableRef = ref<HTMLDivElement | null>(null)
const audioPlayer = ref<HTMLAudioElement | null>(null)
const selectedScript = ref<any>(null)
let tableInstance: Tabulator | null = null

const initTable = () => {
	if (!tableRef.value) return
	tableInstance = new Tabulator(tableRef.value, {
		placeholder: '데이터 없음',
        layout: 'fitColumns',
        selectable: 1,
        height: '100%',
        columnDefaults: { headerSort: false, headerHozAlign: 'center', vertAlign: 'middle' },
		columns: [
			{ title: 'ID', field: 'id', width: 100, cssClass: 'fw-bold text-primary' },
			{ title: '설명', field: 'description', hozAlign: 'left' },
            {
                title: '상태',
                field: 'file_exists',
                width: 80,
                formatter: (cell) => {
                    return cell.getValue()
                        ? '<span class="text-success"><i class="bi bi-check-circle-fill"></i></span>'
                        : '<span class="text-danger"><i class="bi bi-x-circle-fill"></i></span>'
                }
            }
		],
	})

    tableInstance.on("rowClick", (e, row) => {
        selectedScript.value = { ...row.getData() }
    })
}

async function search() {
	try {
        // 💡 [최종 수정] 백엔드 경로(/script/search)와 공백 문제 해결
		const { data } = await api.get('/crm/asterisk/script/search');
        tableInstance?.setData(data || []);
        vAlert('조회되었습니다.');
	} catch (error) {
        console.error('ARS 조회 에러:', error);
        vAlertError('조회 중 오류가 발생했습니다.');
    }
}

async function save() {
    if (!selectedScript.value) return
    if (!confirm('새로운 대본을 AI 음성으로 변환하여 서버에 즉시 적용하시겠습니까?')) return

	try {
		await api.post('/crm/asterisk/script/save', [selectedScript.value])
		vAlert('✅ 성공적으로 적용되었습니다. 이제 고객 전화 시 바뀐 목소리가 송출됩니다.')
        search()
	} catch (error) { vAlertError('적용 중 오류가 발생했습니다.') }
}

function playCurrentVoice() {
    if (!selectedScript.value || !audioPlayer.value) return
    const filename = `${selectedScript.value.id}.wav`
    // 🚀 ARS 음원은 Nginx 직접 지름길(/sounds/)을 통해 서빙합니다.
    audioPlayer.value.src = `/sounds/${filename}?t=${new Date().getTime()}`
    audioPlayer.value.play().catch(() => vAlertError('음원 파일을 찾을 수 없거나 재생할 수 없습니다.'))
}

function initialize() {
	selectedScript.value = null
	search()
}

onMounted(() => {
    nextTick(() => {
        initTable()
        search()
    })
})

onUnmounted(() => { if (tableInstance) tableInstance.destroy(); })
</script>

<style scoped>
.erp-container { font-family: 'Pretendard', sans-serif; letter-spacing: -0.02rem; }
.tabulator-instance { width: 100% !important; background-color: #fff; font-size: 13px; }
:deep(.tabulator-row.tabulator-selected) { background-color: #e7f1ff !important; color: #0d6efd !important; }
.italic { font-style: italic; }
</style>
