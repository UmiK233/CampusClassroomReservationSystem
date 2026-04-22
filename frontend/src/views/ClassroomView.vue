<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Grid, Search } from '@element-plus/icons-vue'
import { classroomApi, reservationApi } from '../api'
import { getUser } from '../stores/auth'
import { buildingOptions } from '../config/buildings'

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
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
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
        <strong>教室检索</strong>
        <div class="hint">按教学楼和容量筛选可预约空间</div>
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

    <el-table :data="classrooms" v-loading="loading" height="310" @row-click="openSeats">
      <el-table-column prop="building" label="教学楼" min-width="140" />
      <el-table-column prop="roomNumber" label="教室" width="120" />
      <el-table-column prop="capacity" label="容量" width="100" />
      <el-table-column prop="seatRows" label="行" width="80" />
      <el-table-column prop="seatCols" label="列" width="80" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Grid" @click.stop="openSeats(row)">座位</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="panel seat-panel">
    <div class="toolbar">
      <div>
        <strong>{{ selectedClassroom ? `${selectedClassroom.building} ${selectedClassroom.roomNumber}` : '座位布局' }}</strong>
        <div class="hint">
          {{ selectedSeat ? `已选择座位 ${selectedSeat.seatNumber}` : selectedClassroom ? '已按预约时间标记不可用座位' : '点击教室后先选择预约时间' }}
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
          :disabled="!selectedClassroom || !reserveForm.time?.length"
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

    <div v-if="!layout" class="empty-block">请选择一间教室并确认预约时间</div>
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
      <div class="hint">确认后会加载座位图，并自动标记该时间段已被预约的座位。</div>
    </el-form>
    <template #footer>
      <el-button @click="timeDialog = false">取消</el-button>
      <el-button type="primary" @click="confirmReservationTime">确认并查看座位</el-button>
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
</style>
