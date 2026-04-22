export function formatDateTimeText(value) {
  if (!value) return ''
  const normalized = typeof value === 'string' ? value.replace('T', ' ') : value
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  const pad = number => String(number).padStart(2, '0')
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
