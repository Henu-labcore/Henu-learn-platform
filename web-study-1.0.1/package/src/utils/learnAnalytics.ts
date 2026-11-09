import dayjs from 'dayjs'

export const formatDuration = (seconds = 0) => {
  const safe = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(safe / 3600)
  const minutes = Math.floor((safe % 3600) / 60)
  return `${hours}h ${minutes}m`
}

export const formatTime = (value) => {
  if (!value) return '—'
  const time = dayjs(value)
  if (!time.isValid()) return '—'
  return time.format('YYYY-MM-DD HH:mm')
}

export const withDefaultCategory = (value) => {
  return value && String(value).trim() ? value : 'Uncategorized'
}

export const withDefaultDifficulty = (value) => {
  return value && String(value).trim() ? value : 'Unknown'
}

export const calcRate = (completed = 0, total = 0) => {
  if (!total) return 0
  return Math.round((completed / total) * 100)
}
