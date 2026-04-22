export function resourceTypeText(type) {
  const map = {
    SEAT: '座位',
    CLASSROOM: '教室'
  }
  return map[type] || type || '-'
}

export function reservationStatusText(status) {
  const map = {
    ACTIVE: '正在使用',
    CANCELLED: '已取消',
    EXPIRED: '已过期'
  }
  return map[status] || status || '-'
}

export function enabledStatusText(status) {
  const map = {
    ENABLED: '启用中',
    DISABLED: '已禁用'
  }
  return map[status] || status || '-'
}
