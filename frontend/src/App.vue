<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { clearAuth, getUser, setUser } from './stores/auth'
import { authApi } from './api'

const router = useRouter()
const route = useRoute()
const user = ref(getUser())

function syncUser() {
  user.value = getUser()
}

async function refreshUser() {
  if (!user.value) return
  try {
    const data = await authApi.me()
    setUser(data)
    user.value = data
  } catch {
    clearAuth()
  }
}

function logout() {
  clearAuth()
  user.value = null
  router.replace('/login')
}

const menuIndex = computed(() => route.path)
const pageMeta = computed(() => {
  if (route.path === '/admin') {
    return { title: '后台管理', subtitle: '维护教室、座位和资源可用状态' }
  }
  if (route.path === '/reservations') {
    return { title: '我的预约', subtitle: '查看当前预约、历史记录并处理取消' }
  }
  return { title: '教室与座位', subtitle: '查询可用教室，选择座位或整间教室发起预约' }
})

onMounted(() => {
  window.addEventListener('auth-change', syncUser)
  refreshUser()
})

onUnmounted(() => window.removeEventListener('auth-change', syncUser))
</script>

<template>
  <router-view v-if="$route.path === '/login'" />
  <el-container v-else class="app-shell">
    <el-aside width="232px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">
          <el-icon><School /></el-icon>
        </div>
        <div>
          <span>教室预约</span>
          <small>Campus Reserve</small>
        </div>
      </div>
      <el-menu :default-active="menuIndex" router class="side-menu">
        <el-menu-item index="/classrooms">
          <el-icon><OfficeBuilding /></el-icon>
          <span>教室与座位</span>
        </el-menu-item>
        <el-menu-item v-if="user?.role !== 'ADMIN'" index="/reservations">
          <el-icon><Tickets /></el-icon>
          <span>我的预约</span>
        </el-menu-item>
        <el-menu-item v-if="user?.role === 'ADMIN'" index="/admin">
          <el-icon><Setting /></el-icon>
          <span>后台管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div>
          <div class="page-title">{{ pageMeta.title }}</div>
          <div class="page-subtitle">{{ pageMeta.subtitle }}</div>
        </div>
        <div class="user-zone">
          <el-tag class="role-tag" effect="plain">{{ user?.role || 'USER' }}</el-tag>
          <div class="user-pill">
            <el-icon><UserFilled /></el-icon>
            <span>{{ user?.nickname || user?.username }}</span>
          </div>
          <el-button :icon="SwitchButton" plain @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
