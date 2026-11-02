/**
 * 知识学习目录配置：栏目 → 类别 → 卡片。
 *
 * 规则（按当前需求）：
 * - 「智能体」归入「人工智能」；
 * - 其余所有分类归入「安全与密码」；
 * - 「软件开发」暂无类别，展示为空文件夹。
 *
 * 新增分类时无需改页面：非智能体的分类会自动进入「安全与密码」；
 * 若以后要给「软件开发」放内容，把分类名加入 DEV_CATEGORIES 即可。
 */
export const AI_CATEGORIES = ['智能体']

export const DEV_CATEGORIES = []

export const KNOWLEDGE_GROUP_META = [
  {
    key: 'security',
    name: '安全与密码',
    description: '古典密码、现代密码、密码协议、区块链安全与数字水印'
  },
  {
    key: 'ai',
    name: '人工智能',
    description: '智能体原理、工具调用与多智能体协作'
  },
  {
    key: 'dev',
    name: '软件开发',
    description: '栏目建设中，敬请期待'
  }
]

/**
 * 根据实际存在的分类构建栏目列表。
 *
 * @param {string[]} categories 学习卡片中出现过的分类
 * @returns {Array<{key:string,name:string,description:string,categories:string[]}>}
 */
export function buildKnowledgeGroups(categories = []) {
  const all = Array.isArray(categories) ? categories : []

  const lists = {
    security: all.filter(
      category =>
        !AI_CATEGORIES.includes(category)
        && !DEV_CATEGORIES.includes(category)
    ),

    ai: all.filter(category => AI_CATEGORIES.includes(category)),

    dev: all.filter(category => DEV_CATEGORIES.includes(category))
  }

  return KNOWLEDGE_GROUP_META.map(meta => ({
    ...meta,
    categories: lists[meta.key] || []
  }))
}

/**
 * 按分类反查所属栏目 key，找不到返回空字符串。
 */
export function findGroupKeyOfCategory(categories = [], category = '') {
  if (!category) {
    return ''
  }

  const group = buildKnowledgeGroups(categories).find(
    item => item.categories.includes(category)
  )

  return group ? group.key : ''
}
