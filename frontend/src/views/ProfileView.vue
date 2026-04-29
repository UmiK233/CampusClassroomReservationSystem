<script setup>
import { computed, onMounted, ref } from 'vue'
import { UserFilled, Clock, Histogram, Timer } from '@element-plus/icons-vue'
import { authApi, reservationApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { userRoleText } from '../utils/dict'

const authStore = useAuthStore()
const loading = ref(false)
const activeReservations = ref([])
const historyReservations = ref([])

const user = computed(() => authStore.user)

const behaviorLevel = computed(() => {
  const map = { A: '优秀', B: '良好', C: '一般' }
  return map[user.value?.creditLevel] || '优秀'
})

const behaviorTagType = computed(() => {
  const map = { A: 'success', B: 'warning', C: 'danger' }
  return map[user.value?.creditLevel] || 'success'
})

const totalReservations = computed(() => activeReservations.value.length + historyReservations.value.length)
const checkedInCount = computed(() => historyReservations.value.filter(item => item.attendanceStatus === 'CHECKED_IN').length)
const cancelledCount = computed(() => historyReservations.value.filter(item => item.status === 'CANCELLED').length)

async function refreshUser() {
  try {
    const data = await authApi.me()
    authStore.setUser(data)
  } catch {
    // keep existing user data on failure
  }
}

async function loadData() {
  loading.value = true
  try {
    await refreshUser()
    const [active, history] = await Promise.all([
      reservationApi.list(),
      reservationApi.history()
    ])
    activeReservations.value = active || []
    historyReservations.value = history || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="profile-shell">
    <section class="panel profile-hero">
      <div class="profile-avatar">
        <el-icon :size="42"><UserFilled /></el-icon>
      </div>
      <div class="profile-main">
        <h2>{{ user?.nickname || user?.username }}</h2>
        <div class="profile-meta">
          <el-tag>{{ userRoleText(user?.role) }}</el-tag>
          <span>@{{ user?.username }}</span>
          <span>{{ user?.email }}</span>
        </div>
      </div>
      <div class="profile-behavior">
        <div class="behavior-badge">
          <span class="behavior-label">使用行为</span>
          <el-tag :type="behaviorTagType" size="large">{{ behaviorLevel }}</el-tag>
        </div>
      </div>
    </section>

    <div class="metric-row">
      <div class="metric">
        <div class="metric-label"><el-icon><Histogram /></el-icon> 累计预约</div>
        <div class="metric-value">{{ totalReservations }}</div>
      </div>
      <div class="metric">
        <div class="metric-label"><el-icon><Clock /></el-icon> 进行中</div>
        <div class="metric-value">{{ activeReservations.length }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">已签到</div>
        <div class="metric-value">{{ checkedInCount }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">已取消</div>
        <div class="metric-value">{{ cancelledCount }}</div>
      </div>
    </div>

    <div class="profile-grid">
      <section class="panel">
        <div class="toolbar">
          <div>
            <strong><el-icon><Timer /></el-icon> 预约参数</strong>
            <div class="hint">当前可预约的时间范围和时长限制</div>
          </div>
        </div>
        <div class="limit-list">
          <div class="limit-item">
            <span>可提前预约</span>
            <strong>{{ user?.seatReservationAdvanceHours || 24 }} 小时</strong>
          </div>
          <div class="limit-item">
            <span>单次预约最长</span>
            <strong>{{ Math.floor((user?.maxSingleReservationMinutes || 180) / 60) }} 小时</strong>
          </div>
          <div class="limit-item">
            <span>每日累计上限</span>
            <strong>{{ Math.floor((user?.dailyReservationLimitMinutes || 360) / 60) }} 小时</strong>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="toolbar">
          <div>
            <strong>账户信息</strong>
            <div class="hint">基本信息与角色</div>
          </div>
        </div>
        <div class="info-list">
          <div class="info-item">
            <span>用户名</span>
            <strong>{{ user?.username }}</strong>
          </div>
          <div class="info-item">
            <span>昵称</span>
            <strong>{{ user?.nickname || '-' }}</strong>
          </div>
          <div class="info-item">
            <span>邮箱</span>
            <strong>{{ user?.email || '-' }}</strong>
          </div>
          <div class="info-item">
            <span>角色</span>
            <el-tag>{{ userRoleText(user?.role) }}</el-tag>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.profile-shell {
  display: grid;
  gap: 16px;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #eff6ff;
  color: #2563eb;
  flex-shrink: 0;
}

.profile-main {
  min-width: 0;
  flex: 1;
}

.profile-main h2 {
  margin: 0 0 10px;
  color: #172033;
  font-size: 26px;
}

.profile-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: #667085;
  font-size: 13px;
}

.profile-behavior {
  flex-shrink: 0;
}

.behavior-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 28px;
  border: 1px solid #e4e8f0;
  border-radius: 10px;
  background: #fbfcff;
}

.behavior-label {
  color: #667085;
  font-size: 12px;
  font-weight: 600;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.limit-list,
.info-list {
  display: grid;
  gap: 12px;
}

.limit-item,
.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid #e4e8f0;
  border-radius: 8px;
  background: #fff;
}

.limit-item span,
.info-item span {
  color: #667085;
  font-size: 13px;
}

.limit-item strong {
  color: #2563eb;
  font-size: 16px;
}

.info-item strong {
  color: #172033;
  font-size: 14px;
}

.hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}

.toolbar .el-icon {
  margin-right: 6px;
  vertical-align: -2px;
  color: #2563eb;
}

@media (max-width: 860px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-hero {
    flex-direction: column;
    text-align: center;
  }
}
</style>
