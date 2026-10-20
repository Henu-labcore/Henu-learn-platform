import request from '@/api/request'

/**
 * 分页查询知识点。
 *
 * 支持：
 * 1. 编码、名称和说明关键词搜索；
 * 2. ACTIVE、INACTIVE 状态筛选；
 * 3. 分类筛选；
 * 4. 分页查询。
 *
 * @param {Object} params 查询参数
 * @returns {Promise<Object>} 分页数据
 */
export function getAdminKnowledgePoints(
    params = {}
) {
    return request.get(
        '/personalized/admin/knowledge-points',
        {
            params
        }
    )
}

/**
 * 查询知识点管理统计数据。
 *
 * 返回知识点总数、启用数、停用数，以及
 * 配置了资源、考核和前置关系的知识点数量。
 *
 * @returns {Promise<Object>} 统计数据
 */
export function getAdminKnowledgePointStats() {
    return request.get(
        '/personalized/admin/knowledge-points/stats'
    )
}

/**
 * 查询指定知识点的完整详情。
 *
 * 详情包括：
 * 1. 知识点基础信息；
 * 2. 前置知识点；
 * 3. 学习资源绑定；
 * 4. 能力考核绑定。
 *
 * @param {number|string} knowledgePointId 知识点主键
 * @returns {Promise<Object>} 知识点详情
 */
export function getAdminKnowledgePointDetail(
    knowledgePointId
) {
    return request.get(
        `/personalized/admin/knowledge-points/${knowledgePointId}`
    )
}

/**
 * 新增知识点。
 *
 * 可同时保存前置知识点、学习资源绑定和能力考核绑定。
 *
 * @param {Object} data 知识点表单数据
 * @returns {Promise<Object>} 创建后的知识点详情
 */
export function createAdminKnowledgePoint(
    data
) {
    return request.post(
        '/personalized/admin/knowledge-points',
        data
    )
}

/**
 * 编辑知识点。
 *
 * 后端会整体替换当前知识点的三类关系数据。
 *
 * @param {number|string} knowledgePointId 知识点主键
 * @param {Object} data 知识点表单数据
 * @returns {Promise<Object>} 更新后的知识点详情
 */
export function updateAdminKnowledgePoint(
    knowledgePointId,
    data
) {
    return request.put(
        `/personalized/admin/knowledge-points/${knowledgePointId}`,
        data
    )
}

/**
 * 启用或停用知识点。
 *
 * @param {number|string} knowledgePointId 知识点主键
 * @param {'ACTIVE'|'INACTIVE'} status 目标状态
 * @returns {Promise<Object>} 更新后的知识点详情
 */
export function updateAdminKnowledgePointStatus(
    knowledgePointId,
    status
) {
    return request.put(
        `/personalized/admin/knowledge-points/${knowledgePointId}/status`,
        {
            status
        }
    )
}

/**
 * 查询可选择的前置知识点。
 *
 * 编辑知识点时传入当前知识点 ID，
 * 后端会从候选项中排除它自身。
 *
 * @param {number|string|null} excludeKnowledgePointId 当前知识点主键
 * @returns {Promise<Array>} 知识点选项
 */
export function getKnowledgePointOptions(
    excludeKnowledgePointId = null
) {
    return request.get(
        '/personalized/admin/knowledge-points/options/knowledge-points',
        {
            params: {
                excludeKnowledgePointId:
                    excludeKnowledgePointId
                    || undefined
            }
        }
    )
}

/**
 * 查询可绑定的学习资源。
 *
 * @returns {Promise<Array>} 学习资源选项
 */
export function getKnowledgePointLearningItemOptions() {
    return request.get(
        '/personalized/admin/knowledge-points/options/learning-items'
    )
}

/**
 * 查询可绑定的能力考核。
 *
 * @returns {Promise<Array>} 能力考核选项
 */
export function getKnowledgePointAssignmentOptions() {
    return request.get(
        '/personalized/admin/knowledge-points/options/assignments'
    )
}