import request from '@/api/request'

/**
 * 查询全部启用学生的知识点掌握度风险总览。
 *
 * 管理员专用接口。
 *
 * 返回内容包括：
 * 1. 全体学生汇总统计；
 * 2. 每名学生的平均掌握度；
 * 3. 高、中、低风险知识点数量；
 * 4. 是否已经完成掌握度计算。
 */
export function getMasteryStudentOverview() {
    return request.get(
        '/personalized/admin/mastery/students'
    )
}

/**
 * 查询指定学生的知识点掌握度详情。
 *
 * 管理员专用接口。
 *
 * @param {number|string} studentId 学生主键
 */
export function getStudentMasteryDetail(
    studentId
) {
    return request.get(
        `/personalized/admin/mastery/students/${studentId}`
    )
}

/**
 * 重新计算指定学生的全部知识点掌握度。
 *
 * 管理员专用接口。
 *
 * @param {number|string} studentId 学生主键
 */
export function recalculateStudentMastery(
    studentId
) {
    return request.post(
        `/personalized/admin/mastery/students/${studentId}/recalculate`
    )
}

/**
 * 查询当前登录学生自己的知识点掌握度。
 *
 * 学生端不需要传入 studentId，
 * 后端会根据当前登录令牌识别学生身份。
 */
export function getMyKnowledgeMastery() {
    return request.get(
        '/personalized/student/mastery/me'
    )
}