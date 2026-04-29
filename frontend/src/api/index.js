import http from './http'

export const authApi = {
  login: data => http.post('/auth/login', data),
  register: data => http.post('/auth/register', data),
  me: () => http.get('/auth/me'),
  changePassword: data => http.put('/auth/password', data)
}

export const notificationApi = {
  list: params => http.get('/notifications', { params }),
  unreadCount: () => http.get('/notifications/unread-count'),
  markAllRead: () => http.patch('/notifications/read-all')
}

export const classroomApi = {
  available: params => http.get('/classrooms/available_list', { params }),
  preferredBuildings: () => http.get('/classrooms/preferred_buildings'),
  detail: id => http.get(`/classrooms/${id}`),
  seats: id => http.get(`/classrooms/${id}/seats`)
}

export const reservationApi = {
  list: () => http.get('/reservations'),
  history: () => http.get('/reservations/history'),
  reservedSeats: (id, params, config = {}) => http.get(`/classrooms/${id}/reserved_seats`, { params, ...config }),
  reserveSeat: data => http.post('/reservations/seats', data),
  reserveClassroom: data => http.post('/reservations/classrooms', data),
  checkIn: id => http.post(`/reservations/${id}/check-in`),
  cancel: id => http.delete(`/reservations/${id}`)
}

export const adminApi = {
  analytics: params => http.get('/admin/analytics', { params }),
  classrooms: params => http.get('/admin/classrooms/list', { params }),
  createClassroom: data => http.post('/admin/classrooms', data),
  updateClassroom: (id, data) => http.put(`/admin/classrooms/${id}`, data),
  initSeats: id => http.post(`/admin/classrooms/${id}/seat_layout`),
  createSeat: (id, data) => http.post(`/admin/classrooms/${id}/seats`, data),
  batchSeatStatus: (id, data) => http.patch(`/admin/classrooms/${id}/seat_layout`, data),
  deleteSeatLayout: id => http.delete(`/admin/classrooms/${id}/seat_layout`),
  updateSeat: (id, data) => http.put(`/admin/seats/${id}`, data),
  deleteSeat: id => http.delete(`/admin/seats/${id}`),
  users: params => http.get('/admin/users', { params }),
  updateUserStatus: (id, data) => http.patch(`/admin/users/${id}/status`, data),
  reservations: params => http.get('/admin/reservations', { params }),
  cancelReservation: (id, data) => http.delete(`/admin/reservations/${id}`, { data }),
  configs: params => http.get('/admin/configs', { params }),
  updateConfig: (key, data) => http.put(`/admin/configs/${key}`, data)
}
