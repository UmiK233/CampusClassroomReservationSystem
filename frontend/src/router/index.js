import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUser } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import ClassroomView from '../views/ClassroomView.vue'
import ReservationsView from '../views/ReservationsView.vue'
import AdminView from '../views/AdminView.vue'

const routes = [
  { path: '/', redirect: '/classrooms' },
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/classrooms', component: ClassroomView },
  { path: '/reservations', component: ReservationsView },
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
  return true
})

export default router
