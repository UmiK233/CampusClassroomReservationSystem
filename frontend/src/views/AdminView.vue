<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose, Edit, Grid, Plus, Search } from '@element-plus/icons-vue'
import { adminApi, classroomApi } from '../api'
import { buildingOptions } from '../config/buildings'

const loading = ref(false)
const classrooms = ref([])
const selected = ref(null)
const layout = ref(null)
const classroomDialog = ref(false)
const seatDialog = ref(false)
const editingSeat = ref(null)
const filters = ref({
  building: buildingOptions[0]?.value || '',
  min_capacity: 1,
  status: 'ENABLED'
})
const classroomForm = ref({
  roomNumber: '',
  building: '',
  seatRows: 6,
  seatCols: 8,
  status: 'ENABLED',
  remark: ''
})
const seatForm = ref({
  status: 'ENABLED',
  remark: ''
})
const enabledClassrooms = computed(() => classrooms.value.filter(item => item.status === 'ENABLED').length)
const disabledSeats = computed(() => layout.value?.seatVOS?.filter(item => item.status === 'DISABLED').length || 0)

async function loadClassrooms() {
  loading.value = true
  try {
    classrooms.value = await adminApi.classrooms({
      building: filters.value.building,
      min_capacity: filters.value.min_capacity,
      status: filters.value.status
    })
  } finally {
    loading.value = false
  }
}

async function loadSeats(row) {
  selected.value = row
  layout.value = await classroomApi.seats(row.id)
}

function openCreate() {
  classroomForm.value = {
    roomNumber: '',
    building: filters.value.building || buildingOptions[0]?.value || '',
    seatRows: 6,
    seatCols: 8,
    status: 'ENABLED',
    remark: ''
  }
  classroomDialog.value = true
}

function openEdit(row) {
  classroomForm.value = {
    id: row.id,
    roomNumber: row.roomNumber,
    building: row.building,
    seatRows: row.seatRows,
    seatCols: row.seatCols,
    status: row.status,
    remark: row.remark
  }
  classroomDialog.value = true
}

async function saveClassroom() {
  if (classroomForm.value.id) {
    await adminApi.updateClassroom(classroomForm.value.id, {
      status: classroomForm.value.status,
      remark: classroomForm.value.remark
    })
    ElMessage.success('教室已更新')
  } else {
    await adminApi.createClassroom(classroomForm.value)
    ElMessage.success('教室已创建')
  }
  classroomDialog.value = false
  loadClassrooms()
}

async function initSeats(row) {
  await ElMessageBox.confirm(`确认为 ${row.building} ${row.roomNumber} 初始化座位？`, '初始化座位', { type: 'warning' })
  await adminApi.initSeats(row.id)
  ElMessage.success('座位初始化完成')
  loadSeats(row)
}

function openSeat(seat) {
  editingSeat.value = seat
  seatForm.value = {
    status: seat.status,
    remark: seat.remark || ''
  }
  seatDialog.value = true
}

async function saveSeat() {
  await adminApi.updateSeat(editingSeat.value.id, seatForm.value)
  ElMessage.success('座位已更新')
  seatDialog.value = false
  loadSeats(selected.value)
}

async function batchDisable(status) {
  if (!selected.value) return
  await adminApi.batchSeatStatus(selected.value.id, { status })
  ElMessage.success('座位状态已批量更新')
  loadSeats(selected.value)
}

function seatStyle(seat) {
  return {
    gridColumn: seat.colNumber,
    gridRow: seat.rowNumber
  }
}

loadClassrooms()
</script>

<template>
  <div class="metric-row">
    <div class="metric">
      <div class="metric-label">教室数量</div>
      <div class="metric-value">{{ classrooms.length }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">启用教室</div>
      <div class="metric-value">{{ enabledClassrooms }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">当前座位</div>
      <div class="metric-value">{{ layout?.seatVOS?.length || 0 }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">禁用座位</div>
      <div class="metric-value">{{ disabledSeats }}</div>
    </div>
  </div>

  <div class="panel">
    <div class="toolbar">
      <div>
        <strong>教室资源</strong>
        <div class="hint">按教学楼、容量和状态维护教室</div>
      </div>
      <div class="form-row">
        <el-form-item label="教学楼">
          <el-select v-model="filters.building" filterable placeholder="请选择教学楼" style="width: 180px">
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
        <el-form-item label="状态">
          <el-select v-model="filters.status" style="width: 130px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-button type="primary" :icon="Search" @click="loadClassrooms">查询</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增教室</el-button>
    </div>

    <el-table :data="classrooms" v-loading="loading" height="310" @row-click="loadSeats">
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
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Edit" @click.stop="openEdit(row)">编辑</el-button>
          <el-button size="small" :icon="Grid" @click.stop="initSeats(row)">初始化座位</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="panel admin-seat-panel">
    <div class="toolbar">
      <div>
        <strong>{{ selected ? `${selected.building} ${selected.roomNumber}` : '座位管理' }}</strong>
        <div class="hint">点击座位可修改状态和备注</div>
      </div>
      <div class="form-row">
        <el-button :disabled="!selected" :icon="CircleCheck" @click="batchDisable('ENABLED')">全部启用</el-button>
        <el-button :disabled="!selected" :icon="CircleClose" @click="batchDisable('DISABLED')">全部禁用</el-button>
      </div>
    </div>
    <div v-if="!layout" class="empty-block">请选择一间教室</div>
    <div
      v-else
      class="seat-grid"
      :style="{ gridTemplateColumns: `repeat(${layout.seatCols}, 44px)`, gridTemplateRows: `repeat(${layout.seatRows}, 40px)` }"
    >
      <button
        v-for="seat in layout.seatVOS"
        :key="seat.id"
        class="seat-cell"
        :class="{ 'is-disabled': seat.status === 'DISABLED' }"
        :style="seatStyle(seat)"
        @click="openSeat(seat)"
      >
        {{ seat.seatNumber }}
      </button>
    </div>
  </div>

  <el-dialog v-model="classroomDialog" :title="classroomForm.id ? '编辑教室' : '新增教室'" width="560px">
    <el-form :model="classroomForm" label-position="top">
      <div class="dialog-grid">
        <el-form-item label="教学楼">
          <el-select v-model="classroomForm.building" :disabled="!!classroomForm.id" filterable style="width: 100%">
            <el-option
              v-for="building in buildingOptions"
              :key="building.value"
              :label="building.label"
              :value="building.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="教室号">
          <el-input v-model="classroomForm.roomNumber" :disabled="!!classroomForm.id" />
        </el-form-item>
        <el-form-item label="座位行数">
          <el-input-number v-model="classroomForm.seatRows" :min="1" :max="100" :disabled="!!classroomForm.id" />
        </el-form-item>
        <el-form-item label="座位列数">
          <el-input-number v-model="classroomForm.seatCols" :min="1" :max="100" :disabled="!!classroomForm.id" />
        </el-form-item>
      </div>
      <el-form-item label="状态">
        <el-select v-model="classroomForm.status" style="width: 100%">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="classroomForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="classroomDialog = false">取消</el-button>
      <el-button type="primary" @click="saveClassroom">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="seatDialog" title="编辑座位" width="460px">
    <el-form :model="seatForm" label-position="top">
      <el-form-item label="状态">
        <el-select v-model="seatForm.status" style="width: 100%">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="seatForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="seatDialog = false">取消</el-button>
      <el-button type="primary" @click="saveSeat">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.admin-seat-panel {
  margin-top: 16px;
}

.hint {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

@media (max-width: 620px) {
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
