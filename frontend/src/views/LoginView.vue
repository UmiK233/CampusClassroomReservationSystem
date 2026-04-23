<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, School, User } from '@element-plus/icons-vue'
import { authApi } from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const mode = ref('login')
const loading = ref(false)
const formRef = ref()
const form = ref({
  username: '',
  password: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (mode.value === 'login') {
      const data = await authApi.login({
        username: form.value.username,
        password: form.value.password
      })
      authStore.setAuth(data.token, data.userInfo)
      router.replace(data.userInfo?.role === 'ADMIN' ? '/admin' : '/dashboard')
      ElMessage.success('登录成功')
    } else {
      await authApi.register({
        username: form.value.username,
        password: form.value.password,
        email: form.value.email
      })
      ElMessage.success('注册成功，请登录')
      mode.value = 'login'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-shell">
      <div class="login-intro">
        <div class="login-brand">
          <div class="brand-mark">
            <el-icon><School /></el-icon>
          </div>
          <div>
            <strong>教室预约</strong>
            <span>Campus Reserve</span>
          </div>
        </div>
        <h1>校园教室预约系统</h1>
        <p>统一管理教室、座位和预约记录，支持按角色进入不同预约与管理流程。</p>
        <div class="login-metrics">
          <div class="metric">
            <div class="metric-label">账号安全</div>
            <div class="metric-value">JWT</div>
          </div>
          <div class="metric">
            <div class="metric-label">预约规则</div>
            <div class="metric-value">冲突检测</div>
          </div>
          <div class="metric">
            <div class="metric-label">权限模型</div>
            <div class="metric-value">3 角色</div>
          </div>
        </div>
      </div>

      <section class="panel login-panel">
        <div class="login-heading">
          <span>欢迎使用</span>
          <strong>{{ mode === 'login' ? '登录账号' : '创建学生账号' }}</strong>
        </div>
        <el-segmented v-model="mode" :options="[{ label: '登录', value: 'login' }, { label: '学生注册', value: 'register' }]" />
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" :prefix-icon="Lock" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-form-item v-if="mode === 'register'" label="邮箱" prop="email">
            <el-input v-model="form.email" :prefix-icon="Message" placeholder="请输入邮箱" />
          </el-form-item>
          <el-button type="primary" :loading="loading" class="login-button" @click="submit">
            {{ mode === 'login' ? '登录' : '注册' }}
          </el-button>
        </el-form>
      </section>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 42px;
  background:
    radial-gradient(circle at 12% 18%, rgba(37, 99, 235, 0.08), transparent 28%),
    radial-gradient(circle at 92% 8%, rgba(15, 118, 110, 0.08), transparent 24%),
    #f5f7fb;
}

.login-shell {
  min-height: calc(100vh - 84px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  align-items: center;
  gap: 32px;
  max-width: 1180px;
  margin: 0 auto;
}

.login-intro {
  max-width: 720px;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 36px;
}

.login-brand strong {
  display: block;
  color: #172033;
  font-size: 18px;
}

.login-brand span {
  display: block;
  margin-top: 2px;
  color: #667085;
  font-size: 12px;
  font-weight: 600;
}

.login-intro h1 {
  margin: 0;
  color: #172033;
  font-size: 48px;
  line-height: 1.15;
  letter-spacing: 0;
}

.login-intro p {
  max-width: 620px;
  margin: 18px 0 0;
  color: #667085;
  font-size: 17px;
  line-height: 1.8;
}

.login-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 34px;
}

.login-panel {
  width: 100%;
  padding: 28px;
}

.login-heading {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 26px;
}

.login-heading span {
  color: #667085;
  font-size: 14px;
  font-weight: 700;
}

.login-heading strong {
  color: #172033;
  font-size: 30px;
  line-height: 1.2;
}

.login-form {
  margin-top: 28px;
}

.login-button {
  width: 100%;
  margin-top: 8px;
  height: 42px;
}

@media (max-width: 820px) {
  .login-page {
    padding: 16px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: calc(100vh - 32px);
  }

  .login-intro h1 {
    font-size: 36px;
  }

  .login-metrics {
    grid-template-columns: 1fr;
  }

  .login-panel {
    padding: 22px;
  }
}
</style>
