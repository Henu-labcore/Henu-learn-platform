import request from './request'

// AI 助学接口模块：统一封装对话和历史记录请求，页面组件不直接拼接 URL。
export const sendAiQuestion = (data) => request({
  url: '/ai-assistant/chat',
  method: 'post',
  data
})

export const getAiHistory = (params) => request({
  url: '/ai-assistant/history',
  method: 'get',
  params
})

// 多会话接口：每个学习内容和使用场景拥有独立的会话列表。
export const getAiConversations = (params) => request({
  url: '/ai-assistant/conversations',
  method: 'get',
  params
})

export const createAiConversation = (data) => request({
  url: '/ai-assistant/conversations',
  method: 'post',
  data
})

export const renameAiConversation = (conversationId, title) => request({
  url: `/ai-assistant/conversations/${conversationId}`,
  method: 'put',
  data: { title }
})

export const deleteAiConversation = (conversationId) => request({
  url: `/ai-assistant/conversations/${conversationId}`,
  method: 'delete'
})

/**
 * 流式问答使用浏览器原生 fetch 读取 SSE。Axios 在浏览器端不会逐块暴露响应体，
 * 因此这里单独实现事件解析，并继续复用项目现有的 Bearer Token。
 */
export const streamAiQuestion = async (data, handlers = {}, signal) => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem('token')
  const response = await fetch(`${baseUrl}/ai-assistant/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data),
    // 页面切换时主动取消读取，避免旧知识点的回答继续写入新页面。
    signal
  })
  if (!response.ok || !response.body) {
    let message = 'AI 助手暂时不可用'
    try {
      const error = await response.json()
      message = error?.message || message
    } catch (ignored) {
      // 非 JSON 错误响应沿用统一提示。
    }
    throw new Error(message)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  try {
    while (true) {
      const { done, value } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() || ''
      blocks.forEach(block => dispatchSseBlock(block, handlers))
      if (done) {
        if (buffer.trim()) dispatchSseBlock(buffer, handlers)
        break
      }
    }
  } finally {
    reader.releaseLock()
  }
}

// Spring SseEmitter 会发送 event/data 两行；data 可能由多行组成，需要先合并再解析。
const dispatchSseBlock = (block, handlers) => {
  let eventName = 'message'
  const dataLines = []
  block.split(/\r?\n/).forEach(line => {
    if (line.startsWith('event:')) eventName = line.slice(6).trim()
    if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  })
  if (dataLines.length === 0) return
  let payload = dataLines.join('\n')
  try { payload = JSON.parse(payload) } catch (ignored) { /* 文本事件保持原值。 */ }
  if (eventName === 'meta') handlers.onMeta?.(payload)
  else if (eventName === 'delta') handlers.onDelta?.(payload)
  else if (eventName === 'done') handlers.onDone?.(payload)
  else if (eventName === 'failure') handlers.onFailure?.(payload)
}

// 回答反馈与消息主键绑定，因此刷新页面后仍能恢复学生之前的选择。
export const updateAiMessageFeedback = (messageId, feedback) => request({
  url: `/ai-assistant/messages/${messageId}/feedback`,
  method: 'put',
  data: { feedback }
})

// 管理端只获取课程级提问计数，不返回学生的具体问题。
export const getAiPopularItems = () => request({
  url: '/admin/ai-assistant/popular-items',
  method: 'get'
})

// AI 用量只向管理员返回汇总数字，不读取任何对话正文。
export const getAiUsageOverview = () => request({
  url: '/admin/ai-assistant/usage-overview',
  method: 'get'
})

