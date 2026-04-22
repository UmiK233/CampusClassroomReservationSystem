<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Refresh } from '@element-plus/icons-vue'
import { reservationApi } from '../api'
import { formatDateTimeText } from '../utils/date'
import { reservationStatusText, resourceTypeText } from '../utils/dict'

const active = ref([])
const history = ref([])
const loading = ref(false)
const cancelledCount = computed(() => history.value.filter(item => item.status === 'CANCELLED').length)
const expiredCount = computed(() => history.value.filter(item => item.status === 'EXPIRED').length)

async function loadData() {
  loading.value = true
  try {
    const [activeList, historyList] = await Promise.all([
      reservationApi.list(),
      reservationApi.history()
    ])
    active.value = activeList
    history.value = historyList
  } finally {
    loading.value = false
  }
}

async function cancelReservation(row) {
  await ElMessageBox.confirm(`确认取消预约 ${row.resourceName}？`, '取消预约', { type: 'warning' })
  await reservationApi.cancel(row.id)
  window.dispatchEvent(new CustomEvent('reservation-change'))
  ElMessage.success('已取消预约')
  loadData()
}

loadData()
</script>

<template>
  <div class="metric-row">
    <div class="metric">
      <div class="metric-label">有效预约</div>
      <div class="metric-value">{{ active.length }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">历史记录</div>
      <div class="metric-value">{{ history.length }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">已取消</div>
      <div class="metric-value">{{ cancelledCount }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">已过期</div>
      <div class="metric-value">{{ expiredCount }}</div>
    </div>
  </div>

  <div class="panel">
    <div class="toolbar">
      <div>
        <strong>有效预约</strong>
        <div class="hint">正在生效的预约可在此取消</div>
      </div>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>
    <el-empty v-if="active.length === 0 && !loading" description="暂无有效预约" />
    <div v-else class="active-card-grid">
      <article v-for="item in active" :key="item.id" class="reservation-card">
        <div class="reservation-card-head">
          <el-tag :type="item.resourceType === 'SEAT' ? 'primary' : 'success'">{{ resourceTypeText(item.resourceType) }}</el-tag>
          <el-button type="danger" size="small" :icon="Close" @click="cancelReservation(item)">取消</el-button>
        </div>
        <h3>{{ item.resourceName }}</h3>
        <div class="reservation-time">{{ formatDateTimeText(item.startTime) }} - {{ formatDateTimeText(item.endTime) }}</div>
        <p>{{ item.reason || '暂无预约原因' }}</p>
      </article>
    </div>

  </div>

  <div class="panel history-panel">
    <div class="toolbar">
      <div>
        <strong>历史预约</strong>
        <div class="hint">取消和过期记录会保留在这里</div>
      </div>
    </div>
    <el-table :data="history" v-loading="loading">
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ resourceTypeText(row.resourceType) }}</template>
      </el-table-column>
      <el-table-column prop="resourceName" label="资源" min-width="220" />
      <el-table-column label="开始时间" min-width="220">
        <template #default="{ row }">{{ formatDateTimeText(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" min-width="220">
        <template #default="{ row }">{{ formatDateTimeText(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="status" width="120" class-name="status-column" label-class-name="status-column">
        <template #header>
          <div class="status-align">状态</div>
        </template>
        <template #default="{ row }">
          <div class="status-align status-value-align">
            <el-tag class="status-tag" :type="row.status === 'CANCELLED' ? 'warning' : 'info'">{{ reservationStatusText(row.status) }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
    </el-table>
  </div>
</template>

<style scoped>
.metric-row :deep(.metric) {
  display: grid;
  grid-template-columns: minmax(72px, 1fr) auto;
  align-items: center;
  column-gap: 14px;
}

.metric-row :deep(.metric-label) {
  min-height: auto;
  line-height: 1;
  white-space: nowrap;
}

.metric-row :deep(.metric-value) {
  min-height: auto;
  margin-top: 0;
  justify-self: end;
  text-align: right;
  line-height: 1;
}

.history-panel {
  margin-top: 16px;
}

.active-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.reservation-card {
  padding: 16px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  background: #fff;
}

.reservation-card-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.reservation-card h3 {
  margin: 16px 0 8px;
  color: #172033;
  font-size: 18px;
}

.reservation-time {
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.reservation-card p {
  margin: 12px 0 0;
  color: #667085;
  font-size: 13px;
}

.hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.status-align {
  width: 72px;
  margin-left: 12px;
  text-align: left;
}

.status-value-align {
  margin-left: 4px;
}

@media (max-width: 1080px) {
  .active-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .active-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
