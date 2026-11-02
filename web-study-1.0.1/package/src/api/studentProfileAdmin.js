import request from '@/api/request'

/**
 * 管理员：学生画像列表（每页最多 8 人）。
 *
 * @param {object} params { keyword, persona, page, pageSize }
 */
export function getAdminProfiles(params = {}) {
    return request.get(
        '/personalized/admin/profiles',
        { params }
    )
}

/**
 * 管理员：画像概览（类型分布 / 维度均值 / 置信度分布）。
 */
export function getAdminProfileOverview() {
    return request.get(
        '/personalized/admin/profiles/overview'
    )
}

/**
 * 管理员：单个学生完整画像（教师端完整标签与依据）。
 */
export function getAdminProfileDetail(studentId) {
    return request.get(
        `/personalized/admin/profiles/${studentId}`
    )
}

/**
 * 管理员：单个学生画像历史（最近 30 天）。
 */
export function getAdminProfileHistory(studentId, days = 30) {
    return request.get(
        `/personalized/admin/profiles/${studentId}/history`,
        { params: { days } }
    )
}

/**
 * 管理员：导出学生画像 CSV。
 */
export function exportAdminProfiles(params = {}) {
    return request.get(
        '/personalized/admin/profiles/export',
        {
            params,
            responseType: 'blob'
        }
    )
}
