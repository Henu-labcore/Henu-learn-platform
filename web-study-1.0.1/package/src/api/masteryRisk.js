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
 * 查询每个启用知识点的学生掌握度聚合。
 *
 * 管理员专用接口，用于预警页"知识点平均掌握度"图：
 * 返回每个知识点的已接触学生数、平均掌握度与风险分布。
 */
export function getKnowledgePointsSummary() {
    return request.get(
        '/personalized/admin/mastery/knowledge-points-summary'
    )
}

/**
 * 重新计算全部启用学生的全部知识点掌握度。
 *
 * 管理员专用接口，新建知识点或调整绑定关系后，
 * 一键让所有学生的最新掌握度保持一致。
 */
export function recalculateAllStudents() {
    return request.post(
        '/personalized/admin/mastery/recalculate-all'
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

/**
 * 重新计算当前登录学生自己的全部知识点掌握度。
 *
 * 学生端页面打开时调用，确保刚学完的
 * 学习记录与最新考核成绩能立即反映到掌握度上。
 */
export function recalculateMyMastery() {
    return request.post(
        '/personalized/student/mastery/recalculate'
    )
}

/**
 * 查询当前学生的掌握度历史（热力图 + 趋势）。
 *
 * 返回按天聚合的掌握度快照：
 * 1. dates：日期轴；
 * 2. knowledgePoints：知识点轴；
 * 3. cells：知识点 × 日期 的掌握度格子；
 * 4. trend：每天的平均掌握度与风险数量。
 *
 * @param {number} days 最近天数（默认 30，范围 1~180）
 */
export function getMyMasteryHistory(days = 30) {
    return request.get(
        '/personalized/student/mastery/history',
        { params: { days } }
    )
}

/**
 * 查询学生 × 知识点 当前掌握度矩阵。
 *
 * 管理员专用，用于"学生 × 知识点"热力图。
 *
 * @param {object} params { keyword, page, pageSize }
 *   keyword 支持姓名/学号/用户名模糊搜索；
 *   pageSize 每页人数，后端上限 8。
 */
export function getMasteryMatrix(params = {}) {
    return request.get(
        '/personalized/admin/mastery/matrix',
        { params }
    )
}

/**
 * 查询班级掌握度历史趋势。
 *
 * 管理员专用：返回每天的整体平均掌握度与风险数量，
 * 用于班级掌握度趋势折线图（不含逐学生对比）。
 *
 * @param {number} days 最近天数（默认 30，范围 1~180）
 */
export function getMasteryTrend(days = 30) {
    return request.get(
        '/personalized/admin/mastery/trend',
        { params: { days } }
    )
}

/**
 * 查询某个知识点下指定风险等级的学生名单。
 *
 * @param {number} knowledgePointId 知识点 ID
 * @param {string} risk HIGH / MEDIUM / LOW / UNKNOWN / ALL
 */
export function getKnowledgePointStudents(knowledgePointId, risk = 'HIGH') {
    return request.get(
        `/personalized/admin/mastery/knowledge-points/${knowledgePointId}/students`,
        { params: { risk } }
    )
}