<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Grid, Search } from '@element-plus/icons-vue'
import { classroomApi, reservationApi } from '../api'
import { getUser } from '../stores/auth'
import { buildingOptions } from '../config/buildings'
import { formatDateTimeText } from '../utils/date'
import { enabledStatusText } from '../utils/dict'

const RESERVATION_TIME_KEY = 'campus_reservation_time'

function getSavedReservationTime() {
  try {
    return JSON.parse(sessionStorage.getItem(RESERVATION_TIME_KEY) || '[]')
  } catch {
    return []
  }
}

const user = computed(() => getUser())
const loading = ref(false)
const seatLoading = ref(false)
const classrooms = ref([])
const selectedClassroom = ref(null)
const layout = ref(null)
const selectedSeat = ref(null)
const reserveDialog = ref(false)
const timeDialog = ref(false)
const pendingClassroom = ref(null)
const reserveType = ref('seat')
const reservedSeatIds = ref(new Set())
const reserveForm = ref({
  time: getSavedReservationTime(),
  reason: ''
})
const filters = ref({
  building: '',
  min_capacity: 1
})
const enabledClassrooms = computed(() => classrooms.value.filter(item => item.status === 'ENABLED').length)
const totalCapacity = computed(() => classrooms.value.reduce((sum, item) => sum + (item.capacity || 0), 0))
const pageActionLabel = computed(() => user.value?.role === 'TEACHER' ? '查看并预约教室' : '查看座位')
const timeConfirmLabel = computed(() => user.value?.role === 'TEACHER' ? '确认并查看教室状态' : '确认并查看座位')
const reservedSeatCount = computed(() => reservedSeatIds.value.size)
const isClassroomUnavailableForTeacher = computed(() => user.value?.role === 'TEACHER' && reservedSeatCount.value > 0)
const selectedTimeLabel = computed(() => {
  if (!hasReservationTime()) return ''
  const [start, end] = reserveForm.value.time
  return `${formatDisplayDateTime(start)} 至 ${formatDisplayDateTime(end)}`
})

async function loadClassrooms() {
  loading.value = true
  try {
    classrooms.value = await classroomApi.available({
      building: filters.value.building || undefined,
      min_capacity: filters.value.min_capacity || 1
    })
  } finally {
    loading.value = false
  }
}

async function openSeats(row) {
  if (user.value?.role !== 'ADMIN') {
    pendingClassroom.value = row
    timeDialog.value = true
    return
  }
  await loadSeatsForClassroom(row)
}

async function loadSeatsForClassroom(row) {
  selectedClassroom.value = row
  selectedSeat.value = null
  seatLoading.value = true
  try {
    layout.value = await classroomApi.seats(row.id)
    if (hasReservationTime()) {
      await loadReservedSeats()
    } else {
      reservedSeatIds.value = new Set()
    }
  } finally {
    seatLoading.value = false
  }
}

async function confirmReservationTime() {
  if (!hasReservationTime()) {
    ElMessage.warning('请选择预约时间')
    return
  }
  sessionStorage.setItem(RESERVATION_TIME_KEY, JSON.stringify(reserveForm.value.time))
  timeDialog.value = false
  if (pendingClassroom.value) {
    await loadSeatsForClassroom(pendingClassroom.value)
  }
}

function seatStyle(seat) {
  return {
    gridColumn: seat.colNumber,
    gridRow: seat.rowNumber
  }
}

function chooseSeat(seat) {
  if (user.value?.role !== 'STUDENT') return
  if (isSeatUnavailable(seat)) return
  selectedSeat.value = seat
}

function openReserve(type) {
  reserveType.value = type
  reserveForm.value = { ...reserveForm.value, reason: '' }
  reserveDialog.value = true
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function formatDisplayDateTime(value) {
  return formatDateTimeText(value)
}

async function submitReservation() {
  if (!reserveForm.value.time?.length) {
    ElMessage.warning('请选择预约时间')
    return
  }
  const [start, end] = reserveForm.value.time
  const payload = {
    start_time: formatDateTime(start),
    end_time: formatDateTime(end),
    reason: reserveForm.value.reason
  }
  if (reserveType.value === 'seat') {
    if (!selectedSeat.value) {
      ElMessage.warning('请选择座位')
      return
    }
    await reservationApi.reserveSeat({ ...payload, seat_id: selectedSeat.value.id })
  } else {
    await reservationApi.reserveClassroom({ ...payload, classroom_id: selectedClassroom.value.id })
  }
  ElMessage.success('预约成功')
  reserveDialog.value = false
  selectedSeat.value = null
  await loadReservedSeats()
}

function isSeatReserved(seat) {
  return reservedSeatIds.value.has(seat.id)
}

function hasReservationTime() {
  return Array.isArray(reserveForm.value.time) && reserveForm.value.time.length === 2
}

function isSeatWaitingForTime() {
  return user.value?.role === 'STUDENT' && !hasReservationTime()
}

function isSeatUnavailable(seat) {
  return seat.status === 'DISABLED' || isSeatReserved(seat) || isSeatWaitingForTime()
}

function handleReservationTimeChange(value) {
  selectedSeat.value = null
  if (Array.isArray(value) && value.length === 2) {
    sessionStorage.setItem(RESERVATION_TIME_KEY, JSON.stringify(value))
  } else {
    sessionStorage.removeItem(RESERVATION_TIME_KEY)
  }
}

async function loadReservedSeats() {
  if (!selectedClassroom.value || !hasReservationTime()) {
    reservedSeatIds.value = new Set()
    return
  }

  const [start, end] = reserveForm.value.time
  const data = await reservationApi.reservedSeats(selectedClassroom.value.id, {
    start_time: formatDateTime(start),
    end_time: formatDateTime(end)
  })
  reservedSeatIds.value = new Set(data || [])
  if (selectedSeat.value && reservedSeatIds.value.has(selectedSeat.value.id)) {
    selectedSeat.value = null
    ElMessage.warning('原座位在该时间段已被预约，请重新选择座位')
  }
}

async function refreshReservationState() {
  if (!selectedClassroom.value || !hasReservationTime()) return
  selectedSeat.value = null
  await loadReservedSeats()
}

onMounted(() => {
  window.addEventListener('reservation-change', refreshReservationState)
})

onUnmounted(() => {
  window.removeEventListener('reservation-change', refreshReservationState)
})

loadClassrooms()
</script>

<template>
  <div class="metric-row">
    <div class="metric">
      <div class="metric-label">可见教室</div>
      <div class="metric-value">{{ classrooms.length }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">启用教室</div>
      <div class="metric-value">{{ enabledClassrooms }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">总容量</div>
      <div class="metric-value">{{ totalCapacity }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">当前选择</div>
      <div class="metric-value compact">{{ selectedSeat?.seatNumber || selectedClassroom?.roomNumber || '-' }}</div>
    </div>
  </div>

  <div class="panel">
    <div class="toolbar">
      <div>
        <strong>{{ user?.role === 'TEACHER' ? '按时间查找空教室' : '按时间查找座位' }}</strong>
        <div class="hint">先筛选教学楼和容量，再选择教室确认预约时间</div>
      </div>
      <div class="form-row">
        <el-form-item label="教学楼">
          <el-select v-model="filters.building" clearable filterable placeholder="全部教学楼" style="width: 180px">
            <el-option
              v-for="building in buildingOptions"
              :key="building.value"
              :label="building.label"
              :value="building.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="最低容量">
          <el-input-number v-model="filters.min_capacity" :min="1" />
        </el-form-item>
        <el-button type="primary" :icon="Search" :loading="loading" @click="loadClassrooms">查询</el-button>
      </div>
    </div>

    <div class="classroom-card-grid">
      <article v-for="room in classrooms" :key="room.id" class="classroom-card" @click="openSeats(room)">
        <div class="classroom-card-head">
          <div>
            <strong>{{ room.building }}</strong>
            <span>{{ room.roomNumber }}</span>
          </div>
          <el-tag :type="room.status === 'ENABLED' ? 'success' : 'danger'">{{ enabledStatusText(room.status) }}</el-tag>
        </div>
        <div class="classroom-card-meta">
          <span>容量 {{ room.capacity }}</span>
          <span>{{ room.seatRows }} 行 x {{ room.seatCols }} 列</span>
        </div>
        <p>{{ room.remark || '暂无备注' }}</p>
        <el-button type="primary" plain :icon="Grid" @click.stop="openSeats(room)">{{ pageActionLabel }}</el-button>
      </article>
    </div>

    <el-table :data="classrooms" v-loading="loading" height="310" @row-click="openSeats">
      <el-table-column prop="building" label="教学楼" min-width="140" />
      <el-table-column prop="roomNumber" label="教室" width="120" />
      <el-table-column prop="capacity" label="容量" width="100" />
      <el-table-column prop="seatRows" label="行" width="80" />
      <el-table-column prop="seatCols" label="列" width="80" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">{{ enabledStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Grid" @click.stop="openSeats(row)">{{ pageActionLabel }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="panel seat-panel">
    <div class="toolbar">
      <div>
        <strong>{{ selectedClassroom ? `${selectedClassroom.building} ${selectedClassroom.roomNumber}` : user?.role === 'TEACHER' ? '教室预约状态' : '座位布局' }}</strong>
        <div class="hint">
          {{
            user?.role === 'TEACHER'
              ? selectedClassroom ? '按预约时间判断整间教室是否可预约' : '点击教室后先选择预约时间'
              : selectedSeat ? `已选择座位 ${selectedSeat.seatNumber}` : selectedClassroom ? '已按预约时间标记不可用座位' : '点击教室后先选择预约时间'
          }}
        </div>
      </div>
      <div class="form-row">
        <el-button
          v-if="user?.role === 'STUDENT'"
          type="primary"
          :icon="Calendar"
          :disabled="!selectedSeat || !reserveForm.time?.length"
          @click="openReserve('seat')"
        >
          预约座位
        </el-button>
        <el-button
          v-if="user?.role === 'TEACHER'"
          type="primary"
          :icon="Calendar"
          :disabled="!selectedClassroom || !reserveForm.time?.length || isClassroomUnavailableForTeacher"
          @click="openReserve('classroom')"
        >
          预约教室
        </el-button>
      </div>
    </div>

    <div v-if="selectedClassroom && selectedTimeLabel" class="time-summary">
      <div>
        <span>当前预约时间</span>
        <strong>{{ selectedTimeLabel }}</strong>
      </div>
      <el-button text type="primary" :icon="Calendar" @click="openSeats(selectedClassroom)">修改时间</el-button>
    </div>

    <div v-if="layout && user?.role !== 'TEACHER'" class="seat-legend">
      <span><i class="legend-dot available"></i>可选</span>
      <span><i class="legend-dot reserved"></i>已预约</span>
      <span><i class="legend-dot disabled"></i>禁用</span>
      <span><i class="legend-dot selected"></i>已选择</span>
    </div>

    <div v-if="!layout" class="empty-block">请选择一间教室并确认预约时间</div>
    <div v-else-if="user?.role === 'TEACHER'" class="teacher-status-card" :class="{ blocked: isClassroomUnavailableForTeacher }">
      <div class="teacher-status-main">
        <el-tag :type="isClassroomUnavailableForTeacher ? 'danger' : 'success'">
          {{ isClassroomUnavailableForTeacher ? '当前时间不可预约' : '当前时间可预约' }}
        </el-tag>
        <h3>{{ selectedClassroom.building }} {{ selectedClassroom.roomNumber }}</h3>
        <p>
          {{
            isClassroomUnavailableForTeacher
              ? `该时间段已有 ${reservedSeatCount} 个座位被预约，或整间教室已被占用，不能预约整间教室。`
              : '该时间段未检测到座位或整间教室冲突，可以提交整间教室预约。'
          }}
        </p>
      </div>
      <div class="teacher-status-meta">
        <span>容量 {{ selectedClassroom.capacity }}</span>
        <span>{{ selectedClassroom.seatRows }} 行 x {{ selectedClassroom.seatCols }} 列</span>
      </div>
    </div>
    <div
      v-else
      v-loading="seatLoading"
      class="seat-grid"
      :style="{ gridTemplateColumns: `repeat(${layout.seatCols}, 44px)`, gridTemplateRows: `repeat(${layout.seatRows}, 40px)` }"
    >
      <button
        v-for="seat in layout.seatVOS"
        :key="seat.id"
        class="seat-cell"
        :class="{ 'is-disabled': seat.status === 'DISABLED', 'is-reserved': isSeatReserved(seat), 'is-pending-time': isSeatWaitingForTime(), 'is-selected': selectedSeat?.id === seat.id }"
        :style="seatStyle(seat)"
        :title="isSeatWaitingForTime() ? '请先选择预约时间' : isSeatReserved(seat) ? '该时间段已被预约' : (seat.remark || seat.seatNumber)"
        @click="chooseSeat(seat)"
      >
        {{ seat.seatNumber }}
      </button>
    </div>
  </div>

  <el-dialog v-model="reserveDialog" :title="reserveType === 'seat' ? '预约座位' : '预约教室'" width="520px">
    <el-form label-position="top">
      <el-alert
        :title="`预约时间：${selectedTimeLabel}`"
        description="如需调整请关闭弹窗后点击“修改时间”。"
        type="info"
        :closable="false"
        class="dialog-alert"
      />
      <el-form-item label="预约原因">
        <el-input v-model="reserveForm.reason" type="textarea" :rows="3" maxlength="255" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reserveDialog = false">取消</el-button>
      <el-button type="primary" @click="submitReservation">提交</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="timeDialog" title="选择预约时间" width="520px" :close-on-click-modal="false">
    <el-form label-position="top">
      <el-form-item :label="pendingClassroom ? `${pendingClassroom.building} ${pendingClassroom.roomNumber}` : '预约教室'">
        <el-date-picker
          v-model="reserveForm.time"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
          @change="handleReservationTimeChange"
        />
      </el-form-item>
      <div class="hint">{{ user?.role === 'TEACHER' ? '确认后会判断该教室在当前时间段是否可整间预约。' : '确认后会加载座位图，并自动标记该时间段已被预约的座位。' }}</div>
    </el-form>
    <template #footer>
      <el-button @click="timeDialog = false">取消</el-button>
      <el-button type="primary" @click="confirmReservationTime">{{ timeConfirmLabel }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.seat-panel {
  margin-top: 16px;
}

.hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.metric-value.compact {
  max-width: 100%;
  overflow: hidden;
  font-size: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-alert {
  margin-bottom: 16px;
}

.classroom-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.classroom-card {
  padding: 16px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.classroom-card:hover {
  transform: translateY(-1px);
  border-color: #bfdbfe;
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.08);
}

.classroom-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.classroom-card-head strong,
.classroom-card-head span {
  display: block;
}

.classroom-card-head span {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.classroom-card-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.classroom-card-meta span {
  padding: 5px 8px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475467;
  font-size: 12px;
  font-weight: 700;
}

.classroom-card p {
  min-height: 36px;
  margin: 12px 0;
  color: #667085;
  font-size: 13px;
}

.seat-legend {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  color: #475467;
  font-size: 13px;
  font-weight: 700;
}

.seat-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 4px;
  border: 1px solid #d0d5dd;
  background: #fff;
}

.legend-dot.reserved {
  border-color: #fde68a;
  background: #fffbeb;
}

.legend-dot.disabled {
  border-color: #fecdd3;
  background: #fff1f2;
}

.legend-dot.selected {
  border-color: #2563eb;
  background: #2563eb;
}

.time-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
}

.time-summary span {
  display: block;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.time-summary strong {
  display: block;
  margin-top: 4px;
  color: #172033;
  font-size: 15px;
}

.teacher-status-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 180px;
  padding: 22px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  background: #f0fdf4;
}

.teacher-status-card.blocked {
  border-color: #fecaca;
  background: #fff1f2;
}

.teacher-status-main h3 {
  margin: 14px 0 8px;
  color: #172033;
  font-size: 24px;
}

.teacher-status-main p {
  max-width: 680px;
  margin: 0;
  color: #475467;
  line-height: 1.7;
}

.teacher-status-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.teacher-status-meta span {
  padding: 8px 10px;
  border-radius: 999px;
  background: #fff;
  color: #475467;
  font-size: 13px;
  font-weight: 800;
}

:deep(.seat-cell.is-reserved) {
  background: #fffbeb;
  color: #b45309;
  border-color: #fde68a;
  cursor: not-allowed;
}

:deep(.seat-cell.is-reserved:hover) {
  transform: none;
  box-shadow: none;
}

:deep(.seat-cell.is-pending-time) {
  background: #f8fafc;
  color: #98a2b3;
  border-color: #e4e8f0;
  cursor: not-allowed;
}

:deep(.seat-cell.is-pending-time:hover) {
  transform: none;
  box-shadow: none;
}

@media (max-width: 1100px) {
  .classroom-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .classroom-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
