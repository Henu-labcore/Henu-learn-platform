import request from './request'

export function getAssignments(params = {}) {
  return request({ url: '/assignments', method: 'get', params })
}

export function getAssignmentStats(params = {}) {
  return request({ url: '/assignments/stats', method: 'get', params })
}

export function getAssignment(id) {
  return request({ url: `/assignments/${id}`, method: 'get' })
}

export function createSubmission(id) {
  return request({ url: `/assignments/${id}/submissions`, method: 'post' })
}

export function uploadSubmissionFile(id, fileType, file, options = {}) {
  const formData = new FormData()
  formData.append('fileType', fileType)
  formData.append('file', file)
  return request({
    url: `/assignments/${id}/files`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: options.onUploadProgress
  })
}

export function deleteSubmissionFile(fileId) {
  return request({ url: `/assignments/files/${fileId}`, method: 'delete' })
}

export function saveSubmissionAnswer(id, answerText) {
  return request({ url: `/assignments/${id}/answer`, method: 'put', data: { answerText } })
}

export function submitAssignment(id, answerText = '') {
  return request({ url: `/assignments/${id}/submit`, method: 'post', data: { answerText } })
}

export function getAdminAssignments(params = {}) {
  return request({ url: '/admin/assignments', method: 'get', params })
}

export function getAdminAssignmentStats(params = {}) {
  return request({ url: '/admin/assignments/stats', method: 'get', params })
}

export function getAdminAssignment(id) {
  return request({ url: `/admin/assignments/${id}`, method: 'get' })
}

export function createAdminAssignment(data) {
  return request({ url: '/admin/assignments', method: 'post', data })
}

export function updateAdminAssignment(id, data) {
  return request({ url: `/admin/assignments/${id}`, method: 'put', data })
}

export function updateAdminAssignmentStatus(id, status) {
  return request({ url: `/admin/assignments/${id}/status`, method: 'put', data: { status } })
}

export function uploadAdminAssignmentMaterial(id, materialType, file, title = '', options = {}) {
  const formData = new FormData()
  formData.append('materialType', materialType)
  formData.append('file', file)
  if (title) {
    formData.append('title', title)
  }
  return request({
    url: `/admin/assignments/${id}/materials`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: options.onUploadProgress
  })
}

export function deleteAdminAssignmentMaterial(materialId) {
  return request({ url: `/admin/assignments/materials/${materialId}`, method: 'delete' })
}

export function getAdminAssignmentSubmissions(id, params = {}) {
  return request({ url: `/admin/assignments/${id}/submissions`, method: 'get', params })
}

export function getAdminSubmissionBatchDownloadInfo(id, params = {}) {
  return request({ url: `/admin/assignments/${id}/submissions/files/download-info`, method: 'get', params })
}

export function getAdminSubmission(id) {
  return request({ url: `/admin/assignments/submissions/${id}`, method: 'get' })
}

export function gradeSubmission(id, data) {
  return request({ url: `/admin/assignments/submissions/${id}/grade`, method: 'post', data })
}

export function returnSubmission(id, data) {
  return request({ url: `/admin/assignments/submissions/${id}/return`, method: 'post', data })
}
