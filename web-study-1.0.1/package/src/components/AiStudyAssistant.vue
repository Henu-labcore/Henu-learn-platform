<template>
  <div class="ai-study-assistant" :class="{ 'is-experiment': scene === 'EXPERIMENT' }">
    <button
      v-if="!panelVisible"
      class="ai-float-button"
      type="button"
      :aria-expanded="panelVisible"
      aria-label="打开 AI 助学助手"
      @click="togglePanel"
    >
      <span class="ai-mark">AI</span>
      <span class="ai-button-text">助学</span>
      <span v-if="!panelVisible" class="ai-status-dot"></span>
    </button>

    <transition name="assistant-panel">
      <section v-if="panelVisible" class="assistant-panel" aria-label="AI 助学助手">
        <header class="assistant-header">
          <div class="assistant-heading">
            <span class="assistant-logo">AI</span>
            <div>
              <strong>学习助手</strong>
              <p>{{ sceneLabel }} · {{ conversationTitle }}</p>
            </div>
          </div>
          <div class="assistant-header-actions">
            <button class="assistant-history-button" type="button" @click="openHistory">记录</button>
            <button class="assistant-close" type="button" aria-label="关闭助手" @click="panelVisible = false">×</button>
          </div>
        </header>

        <div class="assistant-context-bar">
          <span class="context-caption">{{ contextCaption }}</span>
          <strong>{{ activeContextLabel }}</strong>
          <span v-if="demoMode" class="demo-badge">演示模式</span>
        </div>

        <div ref="messageListRef" class="assistant-messages">
          <div v-if="loadingHistory" class="assistant-loading">正在恢复本次学习记录...</div>
          <div v-else-if="messages.length === 0" class="assistant-welcome">
            <span class="welcome-kicker">CONTEXT READY</span>
            <h3>围绕当前内容提问</h3>
            <p>我会结合当前学习卡片回答，并优先给出思路和调试步骤，不参与考核评分。</p>
          </div>

          <article v-for="message in messages" :key="message.localId" class="message-row" :class="message.role.toLowerCase()">
            <div class="message-bubble">
              <pre>{{ message.content }}<i v-if="message.streaming" class="stream-cursor" aria-hidden="true"></i></pre>
              <small v-if="message.sourceLabel">依据：{{ message.sourceLabel }}</small>
              <div v-if="message.role === 'ASSISTANT'" class="message-tools">
                <button type="button" @click="copyAnswer(message.content)">复制</button>
                <button
                  type="button"
                  :class="{ active: message.feedback === 'HELPFUL' }"
                  :disabled="!message.messageId || message.feedbackSaving"
                  @click="setFeedback(message, 'HELPFUL')"
                >有帮助</button>
                <button
                  type="button"
                  :class="{ active: message.feedback === 'NOT_HELPFUL' }"
                  :disabled="!message.messageId || message.feedbackSaving"
                  @click="setFeedback(message, 'NOT_HELPFUL')"
                >没帮助</button>
              </div>
            </div>
          </article>
          <div v-if="sending" class="assistant-thinking"><i></i><i></i><i></i><span>正在梳理上下文</span></div>
        </div>

        <div class="assistant-quick-actions">
          <button
            v-if="scene === 'EXPERIMENT' && readActiveCell"
            class="cell-capture-button"
            :class="{ captured: capturedCell }"
            type="button"
            @click="readCurrentCell"
          >{{ capturedCell ? `已读取 ${capturedCell.code.length} 字` : '读取当前代码单元' }}</button>
          <button
            v-if="scene === 'EXPERIMENT' && executeActiveCell"
            class="cell-execute-button"
            type="button"
            :disabled="sending || executingCell"
            @click="runAndAnalyzeCell"
          >{{ executingCell ? '正在运行...' : '运行并分析' }}</button>
          <button v-for="prompt in quickPrompts" :key="prompt" type="button" @click="applyPrompt(prompt)">{{ prompt }}</button>
        </div>

        <footer class="assistant-composer">
          <textarea
            v-model="question"
            rows="2"
            maxlength="4000"
            :placeholder="composerPlaceholder"
            @keydown.ctrl.enter.prevent="submitQuestion"
          ></textarea>
          <div class="composer-meta">
            <span>Ctrl + Enter 发送</span>
            <button type="button" :disabled="sending || !question.trim()" @click="submitQuestion">发送</button>
          </div>
        </footer>

        <transition name="history-layer">
          <div v-if="historyVisible" class="assistant-history-layer">
            <div class="history-title-row">
              <div>
                <span>HISTORY</span>
                <h3>历史会话</h3>
              </div>
              <button type="button" @click="historyVisible = false">返回对话</button>
            </div>
            <p class="history-context">{{ sceneLabel }} · {{ compactTitle }}</p>
            <button class="history-create" type="button" @click="createConversation">＋ 新建会话</button>
            <div v-if="loadingConversations" class="assistant-loading">正在读取会话列表...</div>
            <div v-else-if="conversations.length === 0" class="history-empty">还没有会话，发送问题后会自动创建。</div>
            <div v-else class="history-list">
              <article
                v-for="conversation in conversations"
                :key="conversation.conversationId"
                :class="{ active: conversation.conversationId === conversationId }"
                @click="selectConversation(conversation)"
              >
                <div class="history-meta">
                  <strong>{{ conversation.title }}</strong>
                  <time>{{ formatMessageTime(conversation.updatedAt) }}</time>
                </div>
                <div class="history-summary">
                  <span>{{ conversation.messageCount }} 条消息</span>
                  <div>
                    <button type="button" @click.stop="renameConversation(conversation)">重命名</button>
                    <button type="button" @click.stop="removeConversation(conversation)">删除</button>
                  </div>
                </div>
              </article>
            </div>
          </div>
        </transition>
      </section>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAiConversation,
  deleteAiConversation,
  getAiConversations,
  getAiHistory,
  renameAiConversation,
  streamAiQuestion,
  updateAiMessageFeedback
} from '@/api/aiAssistant'

// 该组件同时服务知识学习页与实验覆盖层，通过 scene 区分提示语和快捷操作。
const props = defineProps({
  itemPk: { type: [String, Number], default: null },
  itemTitle: { type: String, default: '' },
  currentStep: { type: String, default: '' },
  scene: { type: String, default: 'LEARNING' },
  /** 实验覆盖层注入读取函数，助手本身不直接依赖 iframe DOM。 */
  readActiveCell: { type: Function, default: null },
  /** 执行函数返回当前单元代码和真实输出，助手只负责把结果提交给模型分析。 */
  executeActiveCell: { type: Function, default: null }
})

const panelVisible = ref(false)
const loadingHistory = ref(false)
const loadingConversations = ref(false)
const sending = ref(false)
const executingCell = ref(false)
const historyVisible = ref(false)
const demoMode = ref(false)
const question = ref('')
const messages = ref([])
const conversations = ref([])
const conversationId = ref(null)
const conversationTitle = ref('默认会话')
const capturedCell = ref(null)
const messageListRef = ref(null)
let localSequence = 0
let activeStreamController = null
let activeTextRenderer = null

const sceneLabel = computed(() => props.scene === 'EXPERIMENT' ? '在线实验' : '知识学习')
const contextCaption = computed(() => props.scene === 'EXPERIMENT' ? '当前实验' : '当前知识点')
const compactTitle = computed(() => props.itemTitle || '当前内容')
// 学习页直接展示滚动识别出的知识点，让学生知道助手当前依据哪一步回答。
const activeContextLabel = computed(() => props.currentStep || compactTitle.value)
const composerPlaceholder = computed(() => props.scene === 'EXPERIMENT'
  ? '粘贴代码、报错信息，或描述你卡住的步骤...'
  : '输入你对当前知识点的疑问...')
const quickPrompts = computed(() => props.scene === 'EXPERIMENT'
  ? ['分析运行报错', '只给我提示', '解释这段代码']
  : ['解释当前知识点', '给我一个提示', '举一个简单例子'])

const readCurrentCell = () => {
  if (!props.readActiveCell) return
  try {
    const result = props.readActiveCell()
    if (!result?.code) {
      ElMessage.warning('请先在 JupyterLite 中选中一个代码单元')
      return
    }
    capturedCell.value = result
    ElMessage.success(`已读取当前代码单元（${result.code.length} 字）`)
  } catch (error) {
    ElMessage.error(error?.message || '当前代码单元读取失败')
  }
}

// 一次点击完成运行、采集结果和提问，模型能够同时看到代码与实际输出。
const runAndAnalyzeCell = async () => {
  if (!props.executeActiveCell || sending.value || executingCell.value) return
  executingCell.value = true
  try {
    const result = await props.executeActiveCell()
    if (!result?.code) throw new Error('当前代码单元没有可分析的代码')
    capturedCell.value = result
    question.value = result.output
      ? '请结合这次真实运行结果分析当前代码，说明现象、原因和下一步修改建议。'
      : '这段代码已经运行但没有产生输出，请分析执行逻辑，并说明如何验证结果。'
    await submitQuestion()
  } catch (error) {
    ElMessage.error(error?.message || '代码运行失败')
  } finally {
    executingCell.value = false
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight
}

const normalizeMessage = (message) => ({
  localId: message.messageId || `local-${++localSequence}`,
  messageId: message.messageId || null,
  role: message.role || 'ASSISTANT',
  content: message.content || '',
  sourceLabel: message.sourceLabel || '',
  feedback: message.feedback || '',
  feedbackSaving: false,
  streaming: Boolean(message.streaming),
  createdAt: message.createdAt || null
})

/**
 * 将网络收到的文本放进显示队列，再按较短片段写入响应式消息对象。
 * 这样既能展示模型的真实SSE增量，也能处理开发代理把多个网络包合并成一个大包的情况。
 */
const createIncrementalTextRenderer = (message) => {
  let pendingText = ''
  let timer = null
  let cancelled = false
  const drainResolvers = []

  const resolveDrain = () => {
    if (pendingText || timer) return
    drainResolvers.splice(0).forEach(resolve => resolve())
  }

  const renderNext = () => {
    timer = null
    if (cancelled || !pendingText) {
      resolveDrain()
      return
    }

    // 积压较多时适当加大每段长度，长回答不会因逐字动画拖延过久。
    const characters = Array.from(pendingText)
    const segmentSize = characters.length > 600 ? 24
      : characters.length > 240 ? 12
        : characters.length > 80 ? 6 : 2
    message.content += characters.slice(0, segmentSize).join('')
    pendingText = characters.slice(segmentSize).join('')
    scrollToBottom()
    timer = window.setTimeout(renderNext, 24)
  }

  return {
    enqueue(text) {
      if (cancelled || !text) return
      pendingText += text
      if (timer === null) timer = window.setTimeout(renderNext, 0)
    },
    waitForDrain() {
      if (!pendingText && timer === null) return Promise.resolve()
      return new Promise(resolve => drainResolvers.push(resolve))
    },
    cancel() {
      cancelled = true
      pendingText = ''
      if (timer !== null) window.clearTimeout(timer)
      timer = null
      resolveDrain()
    }
  }
}

const loadHistory = async (targetConversationId = conversationId.value) => {
  if (!props.itemPk) return
  loadingHistory.value = true
  try {
    const data = await getAiHistory({
      itemPk: props.itemPk,
      scene: props.scene,
      ...(targetConversationId ? { conversationId: targetConversationId } : {})
    })
    demoMode.value = Boolean(data?.demoMode)
    conversationId.value = data?.conversationId || null
    conversationTitle.value = data?.conversationTitle || '默认会话'
    messages.value = (data?.messages || []).map(normalizeMessage)
    await scrollToBottom()
  } catch (error) {
    console.error('AI 历史记录加载失败:', error)
  } finally {
    loadingHistory.value = false
  }
}

// 会话列表只读取摘要；具体消息在学生选择会话后再加载。
const loadConversations = async () => {
  if (!props.itemPk) return
  loadingConversations.value = true
  try {
    conversations.value = await getAiConversations({ itemPk: props.itemPk, scene: props.scene }) || []
    const active = conversations.value.find(item => item.conversationId === conversationId.value)
    if (active) conversationTitle.value = active.title
  } catch (error) {
    console.error('AI 会话列表加载失败:', error)
  } finally {
    loadingConversations.value = false
  }
}

const openHistory = async () => {
  historyVisible.value = true
  await loadConversations()
}

const createConversation = async () => {
  try {
    const created = await createAiConversation({
      itemPk: Number(props.itemPk),
      scene: props.scene,
      title: `新会话 ${conversations.value.length + 1}`
    })
    conversationId.value = created.conversationId
    conversationTitle.value = created.title
    messages.value = []
    historyVisible.value = false
    await loadConversations()
  } catch (error) {
    ElMessage.error(error?.message || '新建会话失败')
  }
}

const selectConversation = async (conversation) => {
  conversationId.value = conversation.conversationId
  conversationTitle.value = conversation.title
  historyVisible.value = false
  await loadHistory(conversation.conversationId)
}

const renameConversation = async (conversation) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的会话名称', '重命名会话', {
      inputValue: conversation.title,
      inputPattern: /\S/,
      inputErrorMessage: '会话名称不能为空'
    })
    const updated = await renameAiConversation(conversation.conversationId, value.trim())
    if (conversationId.value === updated.conversationId) conversationTitle.value = updated.title
    await loadConversations()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '重命名失败')
  }
}

const removeConversation = async (conversation) => {
  try {
    await ElMessageBox.confirm(`确定删除“${conversation.title}”及其全部消息吗？`, '删除会话', { type: 'warning' })
    await deleteAiConversation(conversation.conversationId)
    const removedCurrent = conversationId.value === conversation.conversationId
    if (removedCurrent) {
      conversationId.value = null
      conversationTitle.value = '默认会话'
      messages.value = []
    }
    await loadConversations()
    // 删除当前会话后明确切换到剩余的最近会话，避免下一次发送时上下文悄悄变化。
    if (removedCurrent && conversations.value.length > 0) {
      await selectConversation(conversations.value[0])
    }
    ElMessage.success('会话已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '删除失败')
  }
}

const formatMessageTime = (value) => {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const togglePanel = () => {
  panelVisible.value = !panelVisible.value
  if (panelVisible.value) loadHistory()
}

const applyPrompt = (prompt) => {
  question.value = prompt
}

// 复制只在浏览器本地完成，不产生新的对话消息。
const copyAnswer = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('回答已复制')
  } catch (error) {
    ElMessage.error('复制失败，请手动选择文本')
  }
}

// 再次点击相同选项会撤销反馈；接口失败时恢复原来的选中状态。
const setFeedback = async (message, value) => {
  if (!message.messageId || message.feedbackSaving) return
  const previous = message.feedback
  const nextValue = previous === value ? '' : value
  message.feedback = nextValue
  message.feedbackSaving = true
  try {
    await updateAiMessageFeedback(message.messageId, nextValue || 'NONE')
    ElMessage.success(nextValue ? '感谢你的反馈' : '已撤销反馈')
  } catch (error) {
    message.feedback = previous
    ElMessage.error(error?.message || '反馈提交失败')
  } finally {
    message.feedbackSaving = false
  }
}

const submitQuestion = async () => {
  const content = question.value.trim()
  if (!content || sending.value || !props.itemPk) return
  messages.value.push(normalizeMessage({ role: 'USER', content }))
  const assistantIndex = messages.value.push(normalizeMessage({ role: 'ASSISTANT', content: '', streaming: true })) - 1
  // 必须从响应式数组中重新取出代理对象；直接修改push前的原始对象不会触发Vue逐段重绘。
  const assistantMessage = messages.value[assistantIndex]
  const textRenderer = createIncrementalTextRenderer(assistantMessage)
  activeTextRenderer = textRenderer
  question.value = ''
  sending.value = true
  activeStreamController = new AbortController()
  await scrollToBottom()
  try {
    await streamAiQuestion({
      conversationId: conversationId.value,
      itemPk: Number(props.itemPk),
      scene: props.scene,
      question: content,
      currentStep: props.currentStep,
      // 一键读取的代码和输出只随本次提问发送，不额外生成文件或修改 notebook。
      additionalContext: capturedCell.value
        ? `当前代码单元：\n${capturedCell.value.code}\n\n当前输出：\n${capturedCell.value.output || '（无输出）'}`
        : ''
    }, {
      onMeta: meta => {
        demoMode.value = Boolean(meta?.demoMode)
        assistantMessage.sourceLabel = meta?.sourceLabel || ''
      },
      onDelta: delta => {
        textRenderer.enqueue(delta?.content || '')
      },
      onDone: done => {
        conversationId.value = done?.conversationId || conversationId.value
        assistantMessage.messageId = done?.messageId || null
        assistantMessage.localId = done?.messageId || assistantMessage.localId
      },
      onFailure: failure => {
        throw new Error(failure?.message || 'AI 服务暂时不可用')
      }
    }, activeStreamController.signal)
    // 网络结束不代表显示队列已清空，等待最后一段文字真正显示后再结束生成状态。
    await textRenderer.waitForDrain()
    capturedCell.value = null
    await loadConversations()
  } catch (error) {
    // 连接中断前已经收到的有效片段仍应展示，先清空显示队列再判断是否删除空消息。
    await textRenderer.waitForDrain()
    if (!assistantMessage.content) {
      messages.value = messages.value.filter(message => message !== assistantMessage)
    }
    if (error?.name !== 'AbortError') ElMessage.error(error?.message || 'AI 助手暂时不可用')
  } finally {
    await textRenderer.waitForDrain()
    assistantMessage.streaming = false
    if (activeTextRenderer === textRenderer) activeTextRenderer = null
    activeStreamController = null
    sending.value = false
    await scrollToBottom()
  }
}

watch(() => props.itemPk, () => {
  activeStreamController?.abort()
  activeTextRenderer?.cancel()
  messages.value = []
  conversations.value = []
  conversationId.value = null
  conversationTitle.value = '默认会话'
  capturedCell.value = null
  if (panelVisible.value) loadHistory()
})

onBeforeUnmount(() => {
  activeStreamController?.abort()
  activeTextRenderer?.cancel()
})
</script>

<style scoped>
.ai-float-button {
  position: fixed;
  right: 26px;
  bottom: 28px;
  z-index: 1450;
  height: 52px;
  padding: 0 18px 0 8px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  background: linear-gradient(135deg, #1677d2, #0b5fb7);
  color: #fff;
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  box-shadow: 0 14px 34px rgba(13, 99, 184, 0.34);
  transition: transform .2s ease, box-shadow .2s ease;
}
.is-experiment .ai-float-button { z-index: 2550; bottom: 24px; }
.ai-float-button:hover { transform: translateY(-3px); box-shadow: 0 18px 38px rgba(13, 99, 184, .42); }
.ai-mark, .assistant-logo { display: grid; place-items: center; background: #fff; color: #126cc6; font-weight: 800; letter-spacing: -.5px; }
.ai-mark { width: 36px; height: 36px; border-radius: 11px; }
.ai-button-text { font-size: 15px; font-weight: 700; }
.ai-status-dot { position: absolute; right: 7px; top: 7px; width: 8px; height: 8px; border-radius: 50%; background: #6ee7b7; border: 2px solid #0b5fb7; }
.assistant-panel {
  position: fixed;
  right: 24px;
  bottom: 92px;
  z-index: 1440;
  width: min(390px, calc(100vw - 32px));
  height: min(620px, calc(100vh - 120px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #d5e6f7;
  border-radius: 22px;
  background: #f7fbff;
  box-shadow: 0 24px 70px rgba(15, 57, 101, .26);
}
.is-experiment .assistant-panel { z-index: 2540; bottom: 88px; }
.assistant-header { min-height: 58px; padding: 9px 13px; display: flex; align-items: center; justify-content: space-between; color: #fff; background: linear-gradient(120deg, #0d69bf, #258de5); }
.assistant-heading { display: flex; align-items: center; gap: 11px; min-width: 0; }
.assistant-logo { width: 36px; height: 36px; border-radius: 11px; box-shadow: 0 7px 16px rgba(0,0,0,.13); }
.assistant-heading strong { font-size: 16px; }
.assistant-heading p { max-width: 280px; margin: 3px 0 0; opacity: .84; font-size: 12px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.assistant-header-actions { display: flex; align-items: center; gap: 7px; }
.assistant-history-button { height: 34px; padding: 0 11px; border: 1px solid rgba(255,255,255,.28); border-radius: 10px; background: rgba(255,255,255,.12); color: #fff; font-size: 12px; font-weight: 700; cursor: pointer; }
.assistant-close { width: 34px; height: 34px; border: 0; border-radius: 10px; background: rgba(255,255,255,.14); color: #fff; font-size: 24px; cursor: pointer; }
.assistant-context-bar { min-height: 38px; padding: 6px 12px; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 7px; border-bottom: 1px solid #dbe8f3; background: #edf7ff; }
.context-caption { padding: 4px 7px; border-radius: 7px; background: #d8edff; color: #1671c4; font-size: 10px; font-weight: 800; white-space: nowrap; }
.assistant-context-bar strong { color: #294a67; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.demo-badge { padding: 4px 7px; border: 1px solid #f5c88e; border-radius: 999px; background: #fff7e8; color: #b45b05; font-size: 10px; font-weight: 800; white-space: nowrap; }
.assistant-messages { flex: 1; padding: 13px 14px; overflow-y: auto; }
.assistant-loading { color: #64748b; text-align: center; padding-top: 40px; }
.assistant-welcome { padding: 20px; border: 1px solid #dceaf7; border-radius: 16px; background: #fff; }
.welcome-kicker { color: #1e88e5; font-size: 10px; font-weight: 800; letter-spacing: 1.4px; }
.assistant-welcome h3 { margin: 8px 0; color: #17324d; font-size: 18px; }
.assistant-welcome p { margin: 0; color: #60758b; font-size: 13px; line-height: 1.7; }
.message-row { display: flex; margin: 12px 0; }
.message-row.user { justify-content: flex-end; }
.message-bubble { max-width: 88%; padding: 11px 13px; border-radius: 15px 15px 15px 5px; background: #fff; border: 1px solid #dbe8f3; color: #263d54; box-shadow: 0 5px 14px rgba(30, 75, 112, .06); }
.user .message-bubble { border: 0; border-radius: 15px 15px 5px 15px; background: #1e82dc; color: #fff; }
.message-bubble pre { margin: 0; white-space: pre-wrap; word-break: break-word; font: inherit; line-height: 1.65; }
.stream-cursor { display: inline-block; width: 2px; height: 1em; margin-left: 2px; vertical-align: -.12em; background: #258de5; animation: stream-cursor-blink .8s steps(1) infinite; }
.message-bubble small { display: block; margin-top: 9px; padding-top: 8px; border-top: 1px solid #e7eef5; color: #71869a; }
.message-tools { display: flex; gap: 5px; margin-top: 7px; }
.message-tools button { padding: 4px 7px; border: 0; border-radius: 7px; background: #f1f6fa; color: #6c8194; font-size: 10px; cursor: pointer; }
.message-tools button:hover,.message-tools button.active { background: #dceeff; color: #126cc6; }
.message-tools button:disabled { opacity: .45; cursor: not-allowed; }
.assistant-thinking { display: flex; align-items: center; gap: 5px; color: #6a8095; font-size: 12px; }
.assistant-thinking i { width: 6px; height: 6px; border-radius: 50%; background: #258de5; animation: ai-pulse 1s infinite alternate; }
.assistant-thinking i:nth-child(2) { animation-delay: .2s; }.assistant-thinking i:nth-child(3) { animation-delay: .4s; }.assistant-thinking span { margin-left: 5px; }
.assistant-quick-actions { padding: 7px 12px; display: flex; gap: 6px; overflow-x: auto; border-top: 1px solid #e3edf6; background: #fff; }
.assistant-quick-actions button { flex: 0 0 auto; padding: 7px 10px; border: 1px solid #bfdbf5; border-radius: 999px; background: #f4f9ff; color: #1766ad; font-size: 12px; cursor: pointer; }
.assistant-quick-actions .cell-capture-button { border-color: #e5bd7c; background: #fff8eb; color: #9b590d; }
.assistant-quick-actions .cell-capture-button.captured { border-color: #7dcdb8; background: #ecfbf6; color: #11745d; }
.assistant-quick-actions .cell-execute-button { border-color: #86c7a8; background: #effbf5; color: #157653; }
.assistant-quick-actions button:disabled { opacity: .5; cursor: not-allowed; }
.assistant-composer { padding: 9px 12px 11px; background: #fff; }
.assistant-composer textarea { width: 100%; resize: none; box-sizing: border-box; padding: 11px 12px; border: 1px solid #cbddeb; border-radius: 13px; outline: none; color: #263d54; line-height: 1.5; }
.assistant-composer textarea:focus { border-color: #278be3; box-shadow: 0 0 0 3px rgba(39,139,227,.12); }
.composer-meta { margin-top: 8px; display: flex; align-items: center; justify-content: space-between; color: #91a1b0; font-size: 11px; }
.composer-meta button { padding: 7px 18px; border: 0; border-radius: 10px; background: #1879d2; color: #fff; font-weight: 700; cursor: pointer; }
.composer-meta button:disabled { opacity: .45; cursor: not-allowed; }
.assistant-history-layer { position: absolute; inset: 58px 0 0; z-index: 4; display: flex; flex-direction: column; padding: 15px 14px; background: #f7fbff; }
.history-title-row { display: flex; align-items: center; justify-content: space-between; }
.history-title-row span { color: #1e88e5; font-size: 10px; font-weight: 800; letter-spacing: 1.5px; }
.history-title-row h3 { margin: 3px 0 0; color: #17324d; }
.history-title-row button { padding: 7px 11px; border: 1px solid #bfdbf5; border-radius: 9px; background: #fff; color: #1766ad; cursor: pointer; }
.history-context { margin: 10px 0 14px; padding-bottom: 12px; border-bottom: 1px solid #dce8f2; color: #71869a; font-size: 12px; }
.history-create { margin-bottom: 10px; padding: 9px 12px; border: 1px dashed #79b8ef; border-radius: 10px; background: #eef7ff; color: #1766ad; font-weight: 700; cursor: pointer; }
.history-list { flex: 1; overflow-y: auto; padding-right: 3px; }
.history-list article { margin-bottom: 9px; padding: 11px; border: 1px solid #dce8f2; border-radius: 12px; background: #fff; cursor: pointer; }
.history-list article:hover,.history-list article.active { border-color: #6eb2ec; background: #f1f8ff; }
.history-meta { display: flex; justify-content: space-between; color: #234a6c; font-size: 12px; }
.history-meta strong { max-width: 210px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-meta time { color: #93a4b3; font-weight: 400; }
.history-summary { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; color: #7c90a2; font-size: 11px; }
.history-summary div { display: flex; gap: 5px; }
.history-summary button { padding: 3px 6px; border: 0; border-radius: 6px; background: #e5f1fc; color: #2871b2; font-size: 10px; cursor: pointer; }
.history-empty { margin-top: 44px; color: #71869a; text-align: center; }
.history-layer-enter-active,.history-layer-leave-active { transition: opacity .18s ease, transform .18s ease; }
.history-layer-enter-from,.history-layer-leave-to { opacity: 0; transform: translateX(18px); }
.assistant-panel-enter-active,.assistant-panel-leave-active { transition: opacity .2s ease, transform .2s ease; }
.assistant-panel-enter-from,.assistant-panel-leave-to { opacity: 0; transform: translateY(12px) scale(.97); transform-origin: bottom right; }
@keyframes ai-pulse { to { opacity: .2; transform: translateY(-2px); } }
@keyframes stream-cursor-blink { 50% { opacity: 0; } }
@media (max-width: 600px) { .assistant-panel { right: 10px; bottom: 82px; height: calc(100vh - 100px); }.ai-float-button { right: 16px; bottom: 18px; } }
</style>
