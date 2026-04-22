<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Refresh } from '@element-plus/icons-vue'
import { reservationApi } from '../api'

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
    <el-table :data="active" v-loading="loading">
      <el-table-column prop="resourceType" label="类型" width="110" />
      <el-table-column prop="resourceName" label="资源" min-width="220" />
      <el-table-column prop="startTime" label="开始时间" min-width="180" />
      <el-table-column prop="endTime" label="结束时间" min-width="180" />
      <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" :icon="Close" @click="cancelReservation(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <div class="panel history-panel">
    <div class="toolbar">
      <div>
        <strong>历史预约</strong>
        <div class="hint">取消和过期记录会保留在这里</div>
      </div>
    </div>
    <el-table :data="history" v-loading="loading">
      <el-table-column prop="resourceType" label="类型" width="110" />
      <el-table-column prop="resourceName" label="资源" min-width="220" />
      <el-table-column prop="startTime" label="开始时间" min-width="180" />
      <el-table-column prop="endTime" label="结束时间" min-width="180" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'CANCELLED' ? 'warning' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
    </el-table>
  </div>
</template>

<style scoped>
.history-panel {
  margin-top: 16px;
}

.hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}
</style>
