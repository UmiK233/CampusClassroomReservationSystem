<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataBoard,
  OfficeBuilding,
  School,
  Setting,
  SwitchButton,
  Tickets,
  UserFilled
} from '@element-plus/icons-vue'
import { authApi } from './api'
import { useAuthStore } from './stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const user = computed(() => authStore.user)

async function refreshUser() {
  if (!user.value) return
  try {
    const data = await authApi.me()
    authStore.setUser(data)
  } catch {
    authStore.clearAuth()
  }
}

function logout() {
  authStore.clearAuth()
  router.replace('/login')
}

const menuIndex = computed(() => route.path)
const pageMeta = computed(() => {
  if (route.path === '/dashboard') {
    return { title: '工作台', subtitle: '汇总今日任务、通知与常用操作入口' }
  }
  if (route.path === '/admin') {
    return { title: '后台管理', subtitle: '维护教室、用户和预约，并向用户发送站内通知' }
  }
  if (route.path === '/reservations') {
    return { title: '我的预约', subtitle: '查看当前预约、历史记录和管理员通知' }
  }
  return { title: '教室与座位', subtitle: '按时间检索可用资源并创建预约' }
})

onMounted(() => {
  refreshUser()
})
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
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item v-if="user?.role !== 'ADMIN'" index="/classrooms">
          <el-icon><OfficeBuilding /></el-icon>
          <span>{{ user?.role === 'TEACHER' ? '查找空教室' : '查找座位' }}</span>
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
        <router-view v-slot="{ Component, route }">
          <keep-alive>
            <component :is="Component" v-if="route.meta.keepAlive" />
          </keep-alive>
          <component :is="Component" v-if="!route.meta.keepAlive" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>
