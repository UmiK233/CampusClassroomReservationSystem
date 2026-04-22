import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUser } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import ClassroomView from '../views/ClassroomView.vue'
import ReservationsView from '../views/ReservationsView.vue'
import AdminView from '../views/AdminView.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/dashboard', component: DashboardView },
  { path: '/classrooms', component: ClassroomView, meta: { keepAlive: true, roles: ['STUDENT', 'TEACHER'] } },
  { path: '/reservations', component: ReservationsView, meta: { roles: ['STUDENT', 'TEACHER'] } },
  { path: '/admin', component: AdminView, meta: { role: 'ADMIN' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(to => {
  if (to.meta.public) {
    return true
  }
  if (!getToken()) {
    return '/login'
  }
  const role = getUser()?.role
  if (to.meta.role && role !== to.meta.role) {
    return '/classrooms'
  }
  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return role === 'ADMIN' ? '/admin' : '/dashboard'
  }
  return true
})

export default router
