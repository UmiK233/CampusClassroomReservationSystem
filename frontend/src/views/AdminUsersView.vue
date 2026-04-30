<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Search } from '@element-plus/icons-vue'
import { adminApi } from '../api'
import { userRoleText, userStatusText } from '../utils/dict'
import { formatDateTimeText } from '../utils/date'
import { dateStamp, downloadBlob } from '../utils/download'

const loading = ref(false)
const exportLoading = ref(false)
const users = ref([])
const userDialog = ref(false)
const editingUser = ref(null)

const userFilters = ref({
  keyword: '',
  role: '',
  status: null
})

const userForm = ref({
  status: 1,
  reason: ''
})

const activeUsers = computed(() => users.value.filter(item => item.status === 1).length)
const bannedUsers = computed(() => users.value.filter(item => item.status === 0).length)
const studentCount = computed(() => users.value.filter(item => item.role === 'STUDENT').length)
const teacherCount = computed(() => users.value.filter(item => item.role === 'TEACHER').length)

function userQueryParams() {
  return {
    keyword: userFilters.value.keyword || undefined,
    role: userFilters.value.role || undefined,
    status: userFilters.value.status
  }
}

async function loadUsers() {
  loading.value = true
  try {
    users.value = await adminApi.users(userQueryParams())
  } finally {
    loading.value = false
  }
}

async function exportUsers() {
  exportLoading.value = true
  try {
    const blob = await adminApi.exportUsers(userQueryParams())
    downloadBlob(blob, `users-${dateStamp()}.csv`)
    ElMessage.success('用户数据已导出')
  } finally {
    exportLoading.value = false
  }
}

function openUserDialog(row) {
  editingUser.value = row
  userForm.value = { status: row.status, reason: '' }
  userDialog.value = true
}

async function saveUserStatus() {
  await adminApi.updateUserStatus(editingUser.value.id, userForm.value)
  ElMessage.success(userForm.value.status === 0 ? '用户已封禁并已通知' : '用户已恢复并已通知')
  userDialog.value = false
  await loadUsers()
}

loadUsers()
</script>

<template>
  <div class="metric-row">
    <div class="metric">
      <div class="metric-label">用户总数</div>
      <div class="metric-value">{{ users.length }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">正常用户</div>
      <div class="metric-value">{{ activeUsers }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">已封禁</div>
      <div class="metric-value">{{ bannedUsers }}</div>
    </div>
    <div class="metric">
      <div class="metric-label">学生 / 教师</div>
      <div class="metric-value" style="font-size:22px">{{ studentCount }} / {{ teacherCount }}</div>
    </div>
  </div>

  <div class="panel">
    <div class="toolbar">
      <div>
        <strong>用户管理</strong>
        <div class="hint">按角色和状态筛选用户，并执行封禁、恢复或导出。</div>
      </div>
      <div class="form-row">
        <el-input v-model="userFilters.keyword" placeholder="用户名 / 昵称 / 邮箱" clearable style="width: 220px" />
        <el-select v-model="userFilters.role" clearable placeholder="角色" style="width: 120px">
          <el-option label="学生" value="STUDENT" />
          <el-option label="教师" value="TEACHER" />
        </el-select>
        <el-select v-model="userFilters.status" clearable placeholder="状态" style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="已封禁" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
        <el-button :icon="Refresh" @click="loadUsers">刷新</el-button>
        <el-button :icon="Download" :loading="exportLoading" @click="exportUsers">导出</el-button>
      </div>
    </div>

    <el-table :data="users" v-loading="loading">
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="role" label="角色" width="110">
        <template #default="{ row }">{{ userRoleText(row.role) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ userStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="180">
        <template #default="{ row }">{{ formatDateTimeText(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openUserDialog(row)">
            {{ row.status === 1 ? '封禁/恢复' : '恢复/封禁' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <el-dialog v-model="userDialog" :title="editingUser ? `管理用户 ${editingUser.username}` : '管理用户'" width="520px">
    <el-form :model="userForm" label-position="top">
      <el-form-item label="状态">
        <el-select v-model="userForm.status" style="width: 100%">
          <el-option label="正常" :value="1" />
          <el-option label="已封禁" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="通知原因">
        <el-input
          v-model="userForm.reason"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          placeholder="可选，将发送给该用户作为通知说明"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="userDialog = false">取消</el-button>
      <el-button type="primary" @click="saveUserStatus">保存并通知</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hint {
  margin-top: 4px;
  color: #667085;
  font-size: 13px;
}
</style>
