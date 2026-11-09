<template>
  <transition name="experiment-overlay">
    <div v-show="modelValue" class="experiment-overlay">
      <div class="experiment-toolbar">
        <div class="experiment-brand">
          <span class="experiment-badge">实验模式</span>
          <span class="experiment-system">相关基础知识学习实验环境</span>
        </div>
        <div v-if="experimentTitle" class="experiment-title">
          <span class="experiment-title-label">当前实验</span>
          <span class="experiment-title-text">{{ experimentTitle }}</span>
        </div>
        <div class="experiment-actions">
          <button
            class="experiment-reset-btn"
            type="button"
            :disabled="!experimentId || loading || isResetting"
            @click="handleResetTemplate"
          >
            {{ isResetting ? '恢复中...' : '恢复默认模板' }}
          </button>
          <button
            class="experiment-complete-btn"
            type="button"
            :disabled="!experimentId || isCompleting"
            @click="openCompleteDialog"
          >
            完成学习
          </button>
          <button class="experiment-exit-btn" type="button" @click="closeOverlay">
            退出实验
          </button>
        </div>
      </div>
      <div
        v-if="completeDialogVisible"
        class="experiment-complete-overlay"
        @click.self="closeCompleteDialog"
      >
        <div class="experiment-complete-card" role="dialog" aria-modal="true">
          <div class="experiment-complete-header">
            <span class="experiment-complete-title">完成学习</span>
            <button
              class="experiment-complete-close"
              type="button"
              aria-label="关闭"
              @click="closeCompleteDialog"
            >
              &times;
            </button>
          </div>
          <div class="experiment-complete-body">
            <label class="experiment-complete-label" for="experiment-complete-remark">心得与备注</label>
            <textarea
              id="experiment-complete-remark"
              class="experiment-complete-textarea"
              rows="4"
              placeholder="记录本次学习的心得与备注..."
              v-model="completeRemark"
            ></textarea>
          </div>
          <div class="experiment-complete-footer">
            <button
              class="experiment-complete-submit"
              type="button"
              :disabled="isCompleting"
              @click="handleCompleteLearning"
            >
              {{ isCompleting ? '提交中...' : '完成学习' }}
            </button>
          </div>
        </div>
      </div>
      <div class="experiment-frame">
        <div v-if="loading" class="experiment-loading">
          <div class="experiment-spinner"></div>
          <div class="experiment-loading-text">加载实验中...</div>
        </div>
        <div v-if="error" class="experiment-error">
          {{ error }}
        </div>
        <iframe
          ref="experimentIframeRef"
          v-show="iframeSrc"
          class="experiment-iframe"
          :src="iframeSrc"
          @load="handleLoad"
        ></iframe>
      </div>
      <!-- 实验环境保持全屏，AI 助手以网页浮层覆盖，不修改 JupyterLite 内部结构。 -->
      <AiStudyAssistant
        v-if="experimentId"
        :item-pk="experimentId"
        :item-title="experimentTitle"
        scene="EXPERIMENT"
        :read-active-cell="readActiveCell"
        :execute-active-cell="executeActiveCell"
      />
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { completeLearningProgress } from '@/api/knowledge'
import AiStudyAssistant from '@/components/AiStudyAssistant.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  experimentId: {
    type: [String, Number],
    default: null
  },
  experimentTitle: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const iframeSrc = ref('')
const loading = ref(false)
const error = ref('')
const scrollY = ref(0)
const completeDialogVisible = ref(false)
const completeRemark = ref('')
const isCompleting = ref(false)
const isResetting = ref(false)
const experimentIframeRef = ref(null)

/**
 * JupyterLite 与主站同源部署，因此可以安全读取当前激活单元的可见编辑器文本。
 * 这里只读取 DOM；执行动作由 executeActiveCell 单独完成，跨域部署时会给出明确错误。
 */
const readActiveCell = () => {
  const iframe = experimentIframeRef.value
  if (!iframe?.contentDocument) throw new Error('实验环境尚未加载完成')
  try {
    const doc = iframe.contentDocument
    const activeCell = doc.querySelector('.jp-Notebook-cell.jp-mod-active, .jp-Cell.jp-mod-active')
    if (!activeCell) return null
    const editor = activeCell.querySelector('.cm-content')
    const code = editor?.innerText?.trim() || ''
    const outputNodes = activeCell.querySelectorAll('.jp-OutputArea-output')
    const output = Array.from(outputNodes).map(node => node.innerText?.trim()).filter(Boolean).join('\n')
    return { code, output }
  } catch (error) {
    if (error?.name === 'SecurityError') throw new Error('实验环境与主站不同源，无法读取当前代码单元')
    throw error
  }
}

/**
 * 触发 JupyterLite 当前代码单元的 Shift+Enter，并等待输出趋于稳定。
 * 返回“代码 + 实际输出”后由 AI 助手作为附加上下文发送，从而形成执行与分析闭环。
 */
const executeActiveCell = async () => {
  const iframe = experimentIframeRef.value
  if (!iframe?.contentDocument) throw new Error('实验环境尚未加载完成')
  try {
    const doc = iframe.contentDocument
    const activeCell = doc.querySelector('.jp-Notebook-cell.jp-mod-active, .jp-Cell.jp-mod-active')
    const editor = activeCell?.querySelector('.cm-content')
    const code = editor?.innerText?.trim() || ''
    if (!activeCell || !editor || !code) throw new Error('请先选中一个包含代码的单元')

    const initialOutput = Array.from(activeCell.querySelectorAll('.jp-OutputArea-output'))
      .map(node => node.innerText?.trim()).filter(Boolean).join('\n')
    const runButton = doc.querySelector(
      '[data-command="notebook:run-cell-and-select-next"], button[title*="Run the selected"]'
    )
    if (runButton) {
      runButton.click()
    } else {
      // 不同 JupyterLite 版本的工具栏结构不同，找不到运行按钮时回退到标准快捷键。
      editor.focus()
      editor.dispatchEvent(new KeyboardEvent('keydown', {
        key: 'Enter', code: 'Enter', shiftKey: true, bubbles: true, cancelable: true,
        view: iframe.contentWindow
      }))
    }

    // 无输出代码也可能执行成功；轮询到输出连续稳定或达到 15 秒上限后返回。
    let previousOutput = ''
    let stableRounds = 0
    for (let round = 0; round < 50; round += 1) {
      await new Promise(resolve => window.setTimeout(resolve, 300))
      const output = Array.from(activeCell.querySelectorAll('.jp-OutputArea-output'))
        .map(node => node.innerText?.trim()).filter(Boolean).join('\n')
      if (output && output === previousOutput) stableRounds += 1
      else stableRounds = 0
      previousOutput = output
      const running = activeCell.classList.contains('jp-mod-running')
      const outputChanged = output !== initialOutput
      if ((outputChanged && stableRounds >= 2) || (!running && !initialOutput && !output && round >= 9)) {
        return { code, output }
      }
    }
    return { code, output: previousOutput }
  } catch (error) {
    if (error?.name === 'SecurityError') throw new Error('实验环境与主站不同源，无法执行当前代码单元')
    throw error
  }
}

const lockBody = () => {
  scrollY.value = window.scrollY || 0
  document.body.style.top = `-${scrollY.value}px`
  document.body.classList.add('experiment-mode')
}

const unlockBody = () => {
  document.body.classList.remove('experiment-mode')
  document.body.style.top = ''
  window.scrollTo(0, scrollY.value)
}

const resolveLaunchUrl = async (experimentId) => {
  if (!experimentId) {
    return ''
  }
  try {
    const payload = await request({
      url: `/experiment/${experimentId}/launch`,
      method: 'get'
    })
    if (!payload) {
      return ''
    }
    if (payload.launchUrl) {
      return payload.launchUrl
    }
    if (payload.data && payload.data.launchUrl) {
      return payload.data.launchUrl
    }
  } catch {
    // ignore
  }
  return ''
}

const encodeBase64 = (value) => {
  try {
    return window.btoa(unescape(encodeURIComponent(value)))
  } catch (error) {
    return ''
  }
}

const appendAuthParams = (launchUrl, options = {}) => {
  if (!launchUrl) return ''
  try {
    const url = new URL(launchUrl, window.location.origin)
    if (options.reset) {
      url.searchParams.set('reset', '1')
    }
    const token = localStorage.getItem('token')
    if (token) {
      url.searchParams.set('token', token)
    }
    const user = localStorage.getItem('user')
    if (user) {
      const encodedUser = encodeBase64(user)
      if (encodedUser) {
        url.searchParams.set('user_b64', encodedUser)
      }
    }
    return url.toString()
  } catch (error) {
    return launchUrl
  }
}

const loadExperiment = async (options = {}) => {
  loading.value = true
  error.value = ''
  iframeSrc.value = ''
  const launchUrl = await resolveLaunchUrl(props.experimentId)
  if (!launchUrl) {
    loading.value = false
    error.value = '实验启动失败，请稍后重试'
    return
  }
  iframeSrc.value = appendAuthParams(launchUrl, options)
}

const handleLoad = () => {
  loading.value = false
}

const closeOverlay = () => {
  emit('update:modelValue', false)
}

const handleResetTemplate = async () => {
  if (!props.experimentId || isResetting.value) return
  try {
    await ElMessageBox.confirm(
      '恢复默认模板会覆盖当前实验工作区里的同名 notebook，已经写入的内容将被替换。确定继续吗？',
      '恢复默认模板',
      {
        confirmButtonText: '恢复默认模板',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    isResetting.value = true
    await loadExperiment({ reset: true })
    ElMessage.success('已重新加载默认模板')
  } catch (error) {
    // user cancelled
  } finally {
    isResetting.value = false
  }
}

const openCompleteDialog = () => {
  if (!props.experimentId) {
    ElMessage.error('学习项无效')
    return
  }
  completeDialogVisible.value = true
}

const closeCompleteDialog = () => {
  completeDialogVisible.value = false
  completeRemark.value = ''
}

const handleCompleteLearning = async () => {
  const itemPk = Number(props.experimentId)
  if (!itemPk || itemPk <= 0) {
    ElMessage.error('学习项无效')
    return
  }
  if (isCompleting.value) return
  isCompleting.value = true
  try {
    await completeLearningProgress({
      itemPk,
      completeRemark: completeRemark.value?.trim() || undefined
    })
    ElMessage.success('学习完成')
    closeCompleteDialog()
  } catch (error) {
    console.error('完成学习请求失败:', error)
  } finally {
    isCompleting.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      lockBody()
      loadExperiment()
    } else {
      iframeSrc.value = ''
      loading.value = false
      error.value = ''
      closeCompleteDialog()
      unlockBody()
    }
  }
)

watch(
  () => props.experimentId,
  (newId, oldId) => {
    if (props.modelValue && newId && newId !== oldId) {
      loadExperiment()
    }
  }
)

onBeforeUnmount(() => {
  if (props.modelValue) {
    unlockBody()
  }
})
</script>

<style scoped>
.experiment-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(6px);
  display: flex;
  flex-direction: column;
}

.experiment-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 20px;
  min-height: 56px;
  background: rgba(248, 250, 252, 0.94);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(30, 136, 229, 0.2);
  box-shadow: 0 6px 12px rgba(15, 23, 42, 0.12);
  z-index: 2100;
}

.experiment-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.experiment-complete-btn {
  padding: 10px 18px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #1E88E5 0%, #1565C0 100%);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(30, 136, 229, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.experiment-complete-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(21, 101, 192, 0.3);
  filter: brightness(0.98);
}

.experiment-complete-btn:disabled {
  cursor: not-allowed;
  filter: grayscale(0.2);
  opacity: 0.7;
  box-shadow: none;
  transform: none;
}

.experiment-reset-btn {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #fed7aa;
  background: #fff7ed;
  color: #c2410c;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(234, 88, 12, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.experiment-reset-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(234, 88, 12, 0.18);
  filter: brightness(0.99);
}

.experiment-reset-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
  box-shadow: none;
  transform: none;
}

.experiment-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.experiment-badge {
  padding: 6px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #1E88E5 0%, #1565C0 100%);
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 6px 14px rgba(30, 136, 229, 0.35);
}

.experiment-system {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.experiment-title {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 10px;
  background: rgba(30, 136, 229, 0.08);
  border: 1px solid rgba(30, 136, 229, 0.22);
  color: #1e3a8a;
  min-width: 0;
}

.experiment-title-label {
  font-size: 12px;
  font-weight: 600;
  color: #1E88E5;
  white-space: nowrap;
}

.experiment-title-text {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.experiment-frame {
  position: relative;
  flex: 1;
  background: #f8fafc;
  overflow: hidden;
}

.experiment-iframe {
  width: 100%;
  height: calc(100% + 28px);
  border: none;
  background: #f8fafc;
  transform: translateY(-28px);
}

.experiment-exit-btn {
  padding: 10px 18px;
  border-radius: 8px;
  border: 1px solid #bfdbfe;
  background: #ffffff;
  color: #1E88E5;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(30, 136, 229, 0.16);
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.experiment-exit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(30, 136, 229, 0.22);
  filter: brightness(0.98);
}

.experiment-complete-overlay {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
}

.experiment-complete-card {
  width: min(520px, 92vw);
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.25);
  padding: 20px 22px 22px;
}

.experiment-complete-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.experiment-complete-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.experiment-complete-close {
  border: none;
  background: rgba(148, 163, 184, 0.2);
  color: #334155;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  font-size: 18px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease;
}

.experiment-complete-close:hover {
  background: rgba(30, 136, 229, 0.15);
  color: #1e3a8a;
}

.experiment-complete-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.experiment-complete-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e3a8a;
}

.experiment-complete-textarea {
  width: 100%;
  resize: vertical;
  min-height: 110px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #bfdbfe;
  background: #f8fafc;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.6;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.08);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.experiment-complete-textarea::placeholder {
  color: #9ca3af;
}

.experiment-complete-textarea:focus {
  outline: none;
  border-color: #1E88E5;
  box-shadow: 0 0 0 3px rgba(30, 136, 229, 0.15);
  background: #ffffff;
}

.experiment-complete-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.experiment-complete-submit {
  padding: 10px 22px;
  border-radius: 999px;
  border: none;
  background: #1E88E5;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 10px 20px rgba(30, 136, 229, 0.24);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.experiment-complete-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(21, 101, 192, 0.3);
}

.experiment-complete-submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
  box-shadow: none;
  transform: none;
}

@media (max-width: 960px) {
  .experiment-toolbar {
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .experiment-title {
    order: 3;
    width: 100%;
    justify-content: flex-start;
  }

  .experiment-system {
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.experiment-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #e2e8f0;
  background: rgba(15, 23, 42, 0.85);
  z-index: 1;
}

.experiment-spinner {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 4px solid rgba(226, 232, 240, 0.25);
  border-top-color: #38bdf8;
  animation: experiment-spin 0.9s linear infinite;
}

.experiment-loading-text {
  margin-top: 14px;
  font-size: 14px;
}

.experiment-error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fca5a5;
  background: rgba(15, 23, 42, 0.9);
  z-index: 2;
  font-size: 14px;
}

.experiment-overlay-enter-active,
.experiment-overlay-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.experiment-overlay-enter-from {
  opacity: 0;
  transform: scale(0.98);
}

.experiment-overlay-leave-to {
  opacity: 0;
}

@keyframes experiment-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
