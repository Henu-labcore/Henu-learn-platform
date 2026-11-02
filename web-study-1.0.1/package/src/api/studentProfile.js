import request from '@/api/request'

/**
 * 查询当前登录学生的画像（按天快照，最近 30 天）。
 *
 * 返回五维维度分、画像类型、标签、置信度与建议。
 */
export function getMyProfile(days = 30) {
    return request.get(
        '/personalized/student/profile',
        { params: { days } }
    )
}

/**
 * 查询当前学生画像历史（最近 30 天，按天快照）。
 */
export function getMyProfileHistory(days = 30) {
    return request.get(
        '/personalized/student/profile/history',
        { params: { days } }
    )
}

/**
 * 主动重算当前学生画像。
 */
export function recalculateMyProfile(days = 30) {
    return request.post(
        '/personalized/student/profile/recalculate',
        null,
        { params: { days } }
    )
}
