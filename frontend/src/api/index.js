import http from './http'

export const authApi = {
  login: data => http.post('/auth/login', data),
  register: data => http.post('/auth/register', data),
  me: () => http.get('/auth/me')
}

export const classroomApi = {
  available: params => http.get('/classrooms/available_list', { params }),
  detail: id => http.get(`/classrooms/${id}`),
  seats: id => http.get(`/classrooms/${id}/seats`)
}

export const reservationApi = {
  list: () => http.get('/reservations'),
  history: () => http.get('/reservations/history'),
  reservedSeats: (classroomId, params) => http.get(`/reservations/classrooms/${classroomId}/reserved_seats`, { params }),
  reserveSeat: data => http.post('/reservations/seats', data),
  reserveClassroom: data => http.post('/reservations/classrooms', data),
  cancel: id => http.delete(`/reservations/${id}`)
}

export const adminApi = {
  classrooms: params => http.get('/admin/classrooms/list', { params }),
  createClassroom: data => http.post('/admin/classrooms', data),
  updateClassroom: (id, data) => http.put(`/admin/classrooms/${id}`, data),
  initSeats: id => http.post(`/admin/classrooms/${id}/seats/init`),
  batchSeatStatus: (id, data) => http.put(`/admin/classrooms/${id}/seats/status`, data),
  updateSeat: (id, data) => http.put(`/seats/${id}`, data)
}
