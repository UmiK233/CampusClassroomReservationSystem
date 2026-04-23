<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Close, Refresh } from '@element-plus/icons-vue'
import { notificationApi, reservationApi } from '../api'
import { useReservationStore } from '../stores/reservation'
import { formatDateTimeText } from '../utils/date'
import { notificationTypeText, reservationStatusText, resourceTypeText } from '../utils/dict'

const reservationStore = useReservationStore()
const active = ref([])
const history = ref([])
const notifications = ref([])
const unreadCount = ref(0)
const loading = ref(false)

const sortedActive = computed(() => [...active.value].sort((a, b) => getTime(a.startTime) - getTime(b.startTime)))
const currentReservations = computed(() => sortedActive.value.filter(item => getTime(item.startTime) <= Date.now() && getTime(item.endTime) >= Date.now()))
const upcomingReservations = computed(() => sortedActive.value.filter(item => getTime(item.startTime) > Date.now()))
const nextReservation = computed(() => upcomingReservations.value[0] || sortedActive.value[0])
const cancelledCount = computed(() => history.value.filter(item => item.status === 'CANCELLED').length)
const expiredCount = computed(() => history.value.filter(item => item.status === 'EXPIRED').length)

async function loadData() {
  loading.value = true
  try {
    const [activeList, historyList, notificationList, unread] = await Promise.all([
      reservationApi.list(),
      reservationApi.history(),
      notificationApi.list({ limit: 6 }),
      notificationApi.unreadCount()
    ])
    active.value = activeList || []
    history.value = historyList || []
    notifications.value = notificationList || []
    unreadCount.value = unread?.count || 0
  } finally {
    loading.value = false
  }
}

async function cancelReservation(row) {
  await ElMessageBox.confirm(`确认取消预约 ${row.resourceName}？`, '取消预约', { type: 'warning' })
  await reservationApi.cancel(row.id)
  reservationStore.markChanged()
  ElMessage.success('预约已取消')
  await loadData()
}

async function markNotificationsRead() {
  await notificationApi.markAllRead()
  unreadCount.value = 0
  notifications.value = notifications.value.map(item => ({ ...item, isRead: 1 }))
  ElMessage.success('通知已全部标记为已读')
}

function getTime(value) {
  if (!value) return 0
  return new Date(String(value).replace('T', ' ')).getTime()
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
      <div class="metric-label">进行中</div>
      <div class="metric-value">{{ currentReservations.length }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">已取消</div>
      <div class="metric-value">{{ cancelledCount }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">未读通知</div>
      <div class="metric-value">{{ unreadCount }}</div>
    </div>
  </div>

  <div class="reservation-layout">
    <section class="panel next-panel">
      <div class="toolbar">
        <div>
          <strong>下一条预约</strong>
          <div class="hint">优先显示最需要关注的当前或即将开始的安排</div>
        </div>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
      <div v-if="nextReservation" class="next-card">
        <el-tag :type="currentReservations.some(item => item.id === nextReservation.id) ? 'success' : 'primary'">
          {{ currentReservations.some(item => item.id === nextReservation.id) ? '进行中' : '即将开始' }}
        </el-tag>
        <h3>{{ nextReservation.resourceName }}</h3>
        <p>{{ resourceTypeText(nextReservation.resourceType) }} | {{ formatDateTimeText(nextReservation.startTime) }} - {{ formatDateTimeText(nextReservation.endTime) }}</p>
        <div class="next-actions">
          <el-button type="danger" plain :icon="Close" @click="cancelReservation(nextReservation)">取消预约</el-button>
        </div>
      </div>
      <el-empty v-else description="暂无有效预约" />
    </section>

    <section class="panel">
      <div class="toolbar">
        <div>
          <strong>通知中心</strong>
          <div class="hint">管理员封禁账号或取消预约后，会在这里收到站内通知</div>
        </div>
        <el-button :disabled="unreadCount === 0" :icon="Bell" @click="markNotificationsRead">全部已读</el-button>
      </div>
      <el-empty v-if="notifications.length === 0" description="暂无通知" />
      <div v-else class="notification-list">
        <article v-for="item in notifications" :key="item.id" class="notification-card" :class="{ unread: item.isRead === 0 }">
          <div class="notification-head">
            <el-tag :type="item.isRead === 0 ? 'danger' : 'info'">{{ notificationTypeText(item.type) }}</el-tag>
            <span>{{ formatDateTimeText(item.createTime) }}</span>
          </div>
          <h4>{{ item.title }}</h4>
          <p>{{ item.content }}</p>
        </article>
      </div>
    </section>
  </div>

  <div class="panel history-panel">
    <div class="toolbar">
      <div>
        <strong>有效预约</strong>
        <div class="hint">当前生效中的预约可以在这里直接取消</div>
      </div>
    </div>
    <el-empty v-if="active.length === 0 && !loading" description="暂无有效预约" />
    <div v-else class="active-card-grid">
      <article v-for="item in sortedActive" :key="item.id" class="reservation-card">
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
      <el-table-column prop="resourceName" label="资源" min-width="200" />
      <el-table-column label="开始时间" min-width="190">
        <template #default="{ row }">{{ formatDateTimeText(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" min-width="190">
        <template #default="{ row }">{{ formatDateTimeText(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'CANCELLED' ? 'warning' : 'info'">{{ reservationStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
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

.reservation-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 16px;
  margin-bottom: 16px;
}

.history-panel {
  margin-top: 16px;
}

.next-card {
  padding: 18px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
}

.next-card h3 {
  margin: 14px 0 8px;
  color: #172033;
  font-size: 24px;
}

.next-card p {
  margin: 0;
  color: #475467;
}

.next-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.notification-list,
.active-card-grid {
  display: grid;
  gap: 12px;
}

.active-card-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 16px;
}

.notification-card,
.reservation-card {
  padding: 16px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  background: #fff;
}

.notification-card.unread {
  border-color: #fecaca;
  background: #fff7f7;
}

.notification-head,
.reservation-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.notification-head span {
  color: #667085;
  font-size: 12px;
}

.notification-card h4,
.reservation-card h3 {
  margin: 14px 0 8px;
  color: #172033;
}

.notification-card p,
.reservation-card p {
  margin: 0;
  color: #667085;
  line-height: 1.6;
  white-space: pre-line;
}

.reservation-time {
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 1080px) {
  .reservation-layout,
  .active-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
