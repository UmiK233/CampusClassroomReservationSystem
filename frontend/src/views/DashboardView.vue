<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, DataBoard, OfficeBuilding, Plus, Tickets } from '@element-plus/icons-vue'
import { classroomApi, reservationApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { formatDateTimeText } from '../utils/date'
import { reservationStatusText, resourceTypeText } from '../utils/dict'

const router = useRouter()
const authStore = useAuthStore()
const user = computed(() => authStore.user)
const loading = ref(false)
const classrooms = ref([])
const activeReservations = ref([])
const historyReservations = ref([])

const today = new Date().toISOString().slice(0, 10)
const todayReservations = computed(() => activeReservations.value.filter(item => item.reserveDate === today || item.startTime?.startsWith(today)))
const enabledClassrooms = computed(() => classrooms.value.filter(item => item.status === 'ENABLED').length)
const totalCapacity = computed(() => classrooms.value.reduce((sum, item) => sum + (item.capacity || 0), 0))
const recentReservations = computed(() => [...activeReservations.value, ...historyReservations.value].slice(0, 5))

async function loadData() {
  loading.value = true
  try {
    const tasks = [
      classroomApi.available({ min_capacity: 1 }),
      user.value?.role !== 'ADMIN' ? reservationApi.list() : Promise.resolve([]),
      user.value?.role !== 'ADMIN' ? reservationApi.history() : Promise.resolve([])
    ]
    const [classroomList, activeList, historyList] = await Promise.all(tasks)
    classrooms.value = classroomList || []
    activeReservations.value = activeList || []
    historyReservations.value = historyList || []
  } finally {
    loading.value = false
  }
}

function goPrimaryAction() {
  router.push(user.value?.role === 'ADMIN' ? '/admin' : '/classrooms')
}

loadData()
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <section class="workbench-hero">
      <div>
        <div class="hero-kicker">{{ user?.role || 'USER' }}</div>
        <h2>{{ user?.role === 'ADMIN' ? '资源管理工作台' : user?.role === 'TEACHER' ? '教室预约工作台' : '座位预约工作台' }}</h2>
        <p>{{ user?.role === 'ADMIN' ? '集中维护教室、座位和启用状态。' : '先选择时间和教学楼，再进入可预约空间完成预约。' }}</p>
      </div>
      <el-button type="primary" size="large" :icon="Plus" @click="goPrimaryAction">
        {{ user?.role === 'ADMIN' ? '进入后台管理' : user?.role === 'TEACHER' ? '查找空教室' : '查找座位' }}
      </el-button>
    </section>

    <div class="metric-row">
      <div class="metric">
        <div class="metric-label">启用教室</div>
        <div class="metric-value">{{ enabledClassrooms }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">总容量</div>
        <div class="metric-value">{{ totalCapacity }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">有效预约</div>
        <div class="metric-value">{{ activeReservations.length }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">今日预约</div>
        <div class="metric-value">{{ todayReservations.length }}</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="toolbar">
          <div>
            <strong>快捷操作</strong>
            <div class="hint">按当前角色进入最常用流程</div>
          </div>
        </div>
        <div class="quick-actions">
          <button class="quick-action" @click="router.push('/classrooms')">
            <el-icon><OfficeBuilding /></el-icon>
            <span>{{ user?.role === 'TEACHER' ? '按时间找空教室' : '按时间找座位' }}</span>
          </button>
          <button v-if="user?.role !== 'ADMIN'" class="quick-action" @click="router.push('/reservations')">
            <el-icon><Tickets /></el-icon>
            <span>查看我的预约</span>
          </button>
          <button v-if="user?.role === 'ADMIN'" class="quick-action" @click="router.push('/admin')">
            <el-icon><DataBoard /></el-icon>
            <span>维护教室座位</span>
          </button>
        </div>
      </section>

      <section class="panel">
        <div class="toolbar">
          <div>
            <strong>最近预约</strong>
            <div class="hint">有效和历史记录的最近 5 条</div>
          </div>
          <el-button v-if="user?.role !== 'ADMIN'" text type="primary" @click="router.push('/reservations')">全部</el-button>
        </div>
        <el-empty v-if="recentReservations.length === 0" description="暂无预约记录" />
        <div v-else class="reservation-list">
          <div v-for="item in recentReservations" :key="item.id" class="reservation-item">
            <el-icon><Calendar /></el-icon>
            <div>
              <strong>{{ item.resourceName || '预约资源' }}</strong>
              <span>{{ resourceTypeText(item.resourceType) }} · {{ formatDateTimeText(item.startTime) }} - {{ formatDateTimeText(item.endTime) }}</span>
            </div>
            <el-tag :type="item.status === 'ACTIVE' ? 'success' : 'info'">{{ reservationStatusText(item.status) }}</el-tag>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workbench-hero {
  min-height: 190px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 28px;
  border: 1px solid rgba(228, 232, 240, 0.92);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(15, 118, 110, 0.08)),
    #fff;
  box-shadow: 0 18px 42px rgba(16, 24, 40, 0.06);
}

.hero-kicker {
  width: fit-content;
  margin-bottom: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.workbench-hero h2 {
  margin: 0;
  color: #172033;
  font-size: 32px;
  letter-spacing: 0;
}

.workbench-hero p {
  margin: 10px 0 0;
  color: #667085;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 16px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  min-height: 104px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 12px;
  padding: 18px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  background: #fff;
  color: #172033;
  cursor: pointer;
  font-weight: 800;
}

.quick-action .el-icon {
  color: #2563eb;
  font-size: 24px;
}

.reservation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reservation-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  background: #fff;
}

.reservation-item span {
  display: block;
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 900px) {
  .workbench-hero,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .workbench-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
