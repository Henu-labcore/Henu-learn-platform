import request from './request'

/**
 * 贡献者展示（所有登录用户可访问）
 */
export function getContributors() {
  return request({ url: '/contributors', method: 'get' })
}

/**
 * 维护页：贡献者列表，含不可见记录（仅 ADMIN）
 */
export function getAdminContributors() {
  return request({ url: '/admin/contributors', method: 'get' })
}

export function createAdminContributor(data) {
  return request({ url: '/admin/contributors', method: 'post', data })
}

export function updateAdminContributor(contributorId, data) {
  return request({ url: `/admin/contributors/${contributorId}`, method: 'put', data })
}

export function deleteAdminContributor(contributorId) {
  return request({ url: `/admin/contributors/${contributorId}`, method: 'delete' })
}
