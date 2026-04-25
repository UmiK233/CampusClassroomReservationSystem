import { createRouter, createWebHistory } from 'vue-router'
import pinia from '../stores'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/dashboard', component: () => import('../views/DashboardView.vue') },
  { path: '/classrooms', component: () => import('../views/ClassroomView.vue'), meta: { keepAlive: true, roles: ['STUDENT', 'TEACHER'] } },
  { path: '/reservations', component: () => import('../views/ReservationsView.vue'), meta: { roles: ['STUDENT', 'TEACHER'] } },
  { path: '/admin', component: () => import('../views/AdminView.vue'), meta: { role: 'ADMIN' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(to => {
  const authStore = useAuthStore(pinia)
  if (to.meta.public) {
    return true
  }
  if (!authStore.token) {
    return '/login'
  }
  const role = authStore.role
  if (to.meta.role && role !== to.meta.role) {
    return '/classrooms'
  }
  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return role === 'ADMIN' ? '/admin' : '/dashboard'
  }
  return true
})

export default router
