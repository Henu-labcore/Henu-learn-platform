<template>
  <div class="knowledge-details-container">
    <div class="layui-fluid">
      <!-- 顶部标题区域 -->
      <div class="page-header" :class="{ 'is-compact': isHeaderCompact }">
        <div>
          <h1 class="page-title">{{ knowledge ? `${knowledge.title} - 知识学习` : '知识学习' }}</h1>
          <p class="page-description" v-if="knowledge">{{ objectivesText }}</p>
        </div>
        <div class="header-actions">
          <button class="practice-btn" @click="startPractice" :disabled="!knowledge">
            在线练习
          </button>
          <button class="back-btn" @click="goBackToMain">
            <i class="layui-icon layui-icon-return"></i>
            返回知识中心
          </button>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="detail-section" v-if="!loading && knowledge">
        <!-- 面包屑导航 -->
        <div class="breadcrumb">
          <a href="javascript:void(0);" @click="goBackToMain" class="breadcrumb-link">知识首页</a>
          <i class="layui-icon layui-icon-right"></i>
          <span class="breadcrumb-title">{{ breadcrumbTitle }}</span>
        </div>

        <!-- 详情内容容器 -->
        <div class="detail-container">
          <!-- 知识标题和基本信息 -->
          <div class="knowledge-overview">
            <div class="overview-card">
              <div class="overview-left">
                <h2 class="knowledge-title" v-html="processedTitle"></h2>
                <!-- 三个标签同一行排列 -->
                <div class="tags-row">
                  <span class="tag">分类：{{ knowledge.category }}</span>
                  <span class="tag">难度：{{ knowledge.difficulty }}</span>
                  <span class="tag">时长：{{ knowledge.duration }}</span>
                </div>
                <div class="prerequisites-section">
                  <p class="prerequisite-text">前置要求：{{ knowledge.prerequisites }}</p>
                  <p class="prerequisite-text">学习目标：{{ objectivesText }}</p>
                </div>
              </div>
              <div class="overview-right">
                <h3 class="section-title">知识简介</h3>
                <p class="summary-text">{{ knowledge.summary }}</p>
                <div class="features-grid">
                  <div v-for="(feature, index) in featuresList" :key="index" class="feature-card">
                    <h4 class="feature-title">
                      <i class="layui-icon layui-icon-chart"></i>
                      <span>{{ feature.title }}</span>
                    </h4>
                    <p class="feature-description">{{ feature.description }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 学习步骤 -->
          <div class="steps-section">
            <h3 class="steps-title">学习步骤</h3>
            <div class="steps-list">
              <div v-for="(step, index) in knowledge.steps" :key="index" class="step-item">
                <div class="step-number">{{ index + 1 }}</div>
                <div class="step-content">
                  <h4 class="step-title">{{ step.title }}</h4>
                  <p class="step-description" v-html="formatDescription(step.description)"></p>
                  <pre v-if="step.code" class="code-block">{{ step.code }}</pre>
                  <div v-if="step.tip" class="tip-box">
                    <div class="tip-content">
                      <i class="layui-icon layui-icon-tips"></i>
                      <p class="tip-text">{{ step.tip }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="completion-section">
            <div v-if="isCompleted" class="completion-card completion-card--done">
              <div class="completion-text">
                <h4>非常棒</h4>
                <p>你已经完成本内容的学习了！</p>
              </div>
              <div class="completion-input completion-input--readonly">
                <span class="completion-label">学习备注</span>
                <div class="completion-remark">
                  {{ completedRemark }}
                </div>
              </div>
              <button class="complete-btn complete-btn--ghost" @click="goBackToMain">
                返回知识中心
              </button>
            </div>
            <div v-else class="completion-card">
              <div class="completion-text">
                <h4>学习完成了吗？</h4>
                <p>回顾重点内容，巩固知识结构。</p>
              </div>
              <div class="completion-input">
                <label class="completion-label" for="completion-notes">心得与备注</label>
                <textarea
                  id="completion-notes"
                  class="completion-textarea"
                  rows="3"
                  placeholder="记录你本次学习的心得与待补充内容..."
                  v-model="completeRemark"
                ></textarea>
              </div>
              <button class="complete-btn" @click="handleCompleteLearning">
                完成学习
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40"><Loading /></el-icon>
        <p>加载知识详情中...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="!knowledge" class="error-container">
        <div class="error-content">
          <i class="layui-icon layui-icon-close-fill" style="font-size: 48px; color: #ef4444;"></i>
          <h3 class="error-title">知识不存在</h3>
          <p class="error-message">未找到对应的知识内容</p>
          <button class="back-btn" @click="goBackToMain">返回知识中心</button>
        </div>
      </div>
    </div>

    <ExperimentOverlay
      v-model="experimentVisible"
      :experiment-id="activeExperimentId"
      :experiment-title="activeExperimentTitle"
    />
    <!-- 独立悬浮入口，不占用平台原有虚拟形象和学习内容区域。 -->
    <AiStudyAssistant
      v-if="knowledge"
      :item-pk="knowledge.id"
      :item-title="knowledge.title"
      :current-step="currentStepContext"
      scene="LEARNING"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getLearningItemDetail, completeLearningProgress, getLearningProgress, enterLearningProgress, addLearningTime } from '@/api/knowledge'
import ExperimentOverlay from '@/components/ExperimentOverlay.vue'
import AiStudyAssistant from '@/components/AiStudyAssistant.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const knowledge = ref(null)
const completeRemark = ref('')
const learningProgress = ref(null)
const learningTimeQueueKeyPrefix = 'learning-time-queue'
const idleTimeoutMs = 60000
const heartbeatMs = 20000
const isHeaderCompact = ref(false)
const experimentVisible = ref(false)
const activeExperimentId = ref(null)
const activeExperimentTitle = ref('')
// 当前知识点由页面滚动位置实时计算，供“解释当前知识点”等快捷操作使用。
const currentStepIndex = ref(0)
let activityTimer = null
let heartbeatTimer = null
let activeStart = 0
let pendingSec = 0
let lastInteractionAt = 0
let isActive = false
let lastMouseMoveAt = 0

const unwrapData = (payload) => {
  if (payload && payload.data !== undefined) {
    return payload.data
  }
  return payload
}

const formatObjectives = (objectives) => {
  if (Array.isArray(objectives)) {
    return objectives.join('；')
  }
  return objectives || ''
}

const objectivesText = computed(() => {
  if (!knowledge.value) return ''
  return formatObjectives(knowledge.value.objectives)
})

const featuresList = computed(() => {
  if (!knowledge.value || knowledge.value.features == null) {
    return []
  }
  const { features } = knowledge.value
  if (Array.isArray(features)) {
    return features
  }
  if (typeof features === 'string') {
    try {
      const parsed = JSON.parse(features)
      return Array.isArray(parsed) ? parsed : []
    } catch (error) {
      console.error('features 解析失败:', error)
      return []
    }
  }
  return []
})

const isCompleted = computed(() => {
  return Boolean(learningProgress.value?.completeTime)
})

const completedRemark = computed(() => {
  const remark = learningProgress.value?.completeRemark
  return remark && remark.trim() ? remark : '暂无备注'
})

const currentStepContext = computed(() => {
  const step = knowledge.value?.steps?.[currentStepIndex.value]
  if (!step) return ''
  return `第 ${currentStepIndex.value + 1} 个知识点：${step.title}`
})

// 处理标题，避免MathJax解析括号内容
const processedTitle = computed(() => {
  if (!knowledge.value) return ''
  return knowledge.value.title.replace(/\(([^)]*)\)/g, '(<span class="no-mathjax">$1</span>)')
})

// 面包屑标题（只显示中文部分）
const breadcrumbTitle = computed(() => {
  if (!knowledge.value) return '加载中...'
  return knowledge.value.title.replace(/\([^)]*\)/g, '').trim()
})

// 格式化描述文本，将换行符转换为<br>
const formatDescription = (text) => {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
}

// 返回知识中心
const goBackToMain = () => {
  router.push('/knowledge')
}

const getCurrentKnowledgeId = () => {
  return (
    knowledge.value?.id ??
    knowledge.value?.itemPk ??
    knowledge.value?.item_id ??
    route.query.id ??
    route.params.itemPk ??
    null
  )
}

const startPractice = () => {
  const currentId = getCurrentKnowledgeId()
  if (!currentId) return
  activeExperimentId.value = currentId
  activeExperimentTitle.value = knowledge.value?.title || ''
  experimentVisible.value = true
}

const handleHeaderScroll = () => {
  // 主布局的 .main-content 才是真正滚动容器，不能只读取 window.scrollY。
  const contentScrollTop = document.querySelector('.main-content')?.scrollTop || window.scrollY || 0
  isHeaderCompact.value = contentScrollTop >= 60
  updateCurrentStep()
}

// 以视口上部约三分之一处作为阅读焦点，选择最后一个越过焦点线的知识步骤。
const updateCurrentStep = () => {
  const nodes = Array.from(document.querySelectorAll('.steps-list .step-item'))
  if (!nodes.length) return
  const focusLine = Math.max(160, window.innerHeight * 0.32)
  let activeIndex = 0
  nodes.forEach((node, index) => {
    if (node.getBoundingClientRect().top <= focusLine) activeIndex = index
  })
  currentStepIndex.value = activeIndex
}

const getApiBaseUrl = () => {
  return import.meta.env.VITE_API_BASE_URL || '/api'
}

const getCurrentUserId = () => {
  const raw = localStorage.getItem('user')
  if (!raw) return null
  try {
    const user = JSON.parse(raw)
    return user?.userId || user?.id || null
  } catch (error) {
    return null
  }
}

const getQueueKey = () => {
  const userId = getCurrentUserId()
  return userId ? `${learningTimeQueueKeyPrefix}:${userId}` : `${learningTimeQueueKeyPrefix}:anonymous`
}

const clearLegacyQueue = () => {
  if (getCurrentUserId()) {
    localStorage.removeItem(learningTimeQueueKeyPrefix)
  }
}

const loadQueue = () => {
  try {
    const raw = localStorage.getItem(getQueueKey())
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    console.error('学习时长队列解析失败:', error)
    return []
  } finally {
    clearLegacyQueue()
  }
}

const saveQueue = (queue) => {
  localStorage.setItem(getQueueKey(), JSON.stringify(queue))
}

const shouldDropQueueEntry = (status) => {
  return status === 400 || status === 404
}

const enqueueDelta = (itemPk, deltaSec) => {
  if (!itemPk || deltaSec <= 0) return
  const queue = loadQueue()
  const id = `${itemPk}-${Date.now()}-${deltaSec}`
  queue.push({ id, itemPk, deltaSec })
  saveQueue(queue)
}

const flushQueue = async () => {
  const queue = loadQueue()
  if (!queue.length) return
  const remaining = []
  for (const entry of queue) {
    try {
      await addLearningTime({
        item_pk: entry.itemPk,
        delta_sec: entry.deltaSec
      })
    } catch (error) {
      const status = error?.response?.status
      if (!shouldDropQueueEntry(status)) {
        remaining.push(entry)
      }
    }
  }
  saveQueue(remaining)
}

const sendQueueWithKeepAlive = (queue) => {
  if (!queue.length) return
  const token = localStorage.getItem('token')
  const apiBase = getApiBaseUrl()
  const requests = queue.map((entry) => {
    return fetch(`${apiBase}/learning-progress/time`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({
        item_pk: entry.itemPk,
        delta_sec: entry.deltaSec
      }),
      keepalive: true
    })
  })
  Promise.allSettled(requests).then((results) => {
    const failed = []
    results.forEach((result, index) => {
      if (result.status === 'rejected') {
        failed.push(queue[index])
        return
      }
      const response = result.value
      if (!response?.ok && !shouldDropQueueEntry(response?.status)) {
        failed.push(queue[index])
      }
    })
    saveQueue(failed)
  })
}

const stopActive = () => {
  if (!isActive) return
  const now = Date.now()
  const delta = Math.floor((now - activeStart) / 1000)
  if (delta > 0) {
    pendingSec += delta
  }
  isActive = false
  activeStart = 0
}

const startActive = () => {
  if (isActive) return
  isActive = true
  activeStart = Date.now()
}

const syncPending = () => {
  if (!isActive) return
  const now = Date.now()
  const delta = Math.floor((now - activeStart) / 1000)
  if (delta > 0) {
    pendingSec += delta
    activeStart = now
  }
}

const reportPending = async () => {
  if (!knowledge.value?.id) return
  syncPending()
  const deltaSec = Math.floor(pendingSec)
  if (deltaSec <= 0) return
  pendingSec -= deltaSec
  enqueueDelta(knowledge.value.id, deltaSec)
  await flushQueue()
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    stopActive()
    reportPending()
  } else {
    lastInteractionAt = Date.now()
    startActive()
  }
}

const handlePageHide = () => {
  stopActive()
  const deltaSec = Math.floor(pendingSec)
  if (deltaSec > 0 && knowledge.value?.id) {
    pendingSec -= deltaSec
    const queue = loadQueue()
    const id = `${knowledge.value.id}-${Date.now()}-${deltaSec}`
    queue.push({ id, itemPk: knowledge.value.id, deltaSec })
    saveQueue(queue)
    sendQueueWithKeepAlive(queue)
  }
}

const handleActivity = () => {
  lastInteractionAt = Date.now()
  if (!document.hidden) {
    startActive()
  }
}

const handleMouseMove = () => {
  const now = Date.now()
  if (now - lastMouseMoveAt < 1000) return
  lastMouseMoveAt = now
  handleActivity()
}

const startTimers = () => {
  lastInteractionAt = Date.now()
  startActive()
  activityTimer = setInterval(() => {
    if (isActive && Date.now() - lastInteractionAt > idleTimeoutMs) {
      stopActive()
    }
  }, 5000)
  heartbeatTimer = setInterval(() => {
    reportPending()
  }, heartbeatMs)
}

const bindActivityListeners = () => {
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('pagehide', handlePageHide)
  window.addEventListener('online', flushQueue)
  // scroll 不冒泡，使用捕获阶段才能监听 .main-content 等嵌套滚动容器。
  window.addEventListener('scroll', handleActivity, { passive: true, capture: true })
  window.addEventListener('click', handleActivity, { passive: true })
  window.addEventListener('keydown', handleActivity)
  window.addEventListener('touchstart', handleActivity, { passive: true })
  window.addEventListener('mousemove', handleMouseMove, { passive: true })
}

const unbindActivityListeners = () => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('pagehide', handlePageHide)
  window.removeEventListener('online', flushQueue)
  window.removeEventListener('scroll', handleActivity, true)
  window.removeEventListener('click', handleActivity)
  window.removeEventListener('keydown', handleActivity)
  window.removeEventListener('touchstart', handleActivity)
  window.removeEventListener('mousemove', handleMouseMove)
}

const stopTimers = async () => {
  if (activityTimer) clearInterval(activityTimer)
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  activityTimer = null
  heartbeatTimer = null
  stopActive()
  await reportPending()
}

const handleCompleteLearning = async () => {
  const itemPk = knowledge.value?.id
  if (!itemPk || itemPk <= 0) {
    ElMessage.error('学习项无效')
    return
  }

  try {
    await completeLearningProgress({
      itemPk,
      completeRemark: completeRemark.value?.trim() || undefined
    })
    ElMessage.success('学习完成')
    await fetchLearningProgress(itemPk)
  } catch (error) {
    console.error('完成学习请求失败:', error)
  }
}

const fetchLearningProgress = async (itemPk) => {
  try {
    const res = await getLearningProgress({ itemPk })
    const data = unwrapData(res)
    if (Array.isArray(data) && data.length > 0) {
      learningProgress.value = data[0]
    } else {
      learningProgress.value = null
    }
  } catch (error) {
    learningProgress.value = null
    console.error('学习进度加载失败:', error)
  }
}

const enterLearning = async (itemPk) => {
  try {
    await enterLearningProgress({ itemPk })
  } catch (error) {
    console.error('进入学习请求失败:', error)
  }
}

onMounted(async () => {
  handleHeaderScroll()
  window.addEventListener('scroll', handleHeaderScroll, { passive: true, capture: true })
  const queryId = parseInt(route.query.id)
  const paramId = parseInt(route.params.itemPk)
  const id = queryId || paramId
  
  if (!id) {
    loading.value = false
    return
  }

  try {
    const res = await getLearningItemDetail(id)
    const data = unwrapData(res)
    knowledge.value = data || null
    const itemPk = knowledge.value?.id
    if (itemPk) {
      await enterLearning(itemPk)
      await fetchLearningProgress(itemPk)
    }
  } catch (error) {
    knowledge.value = null
    console.error('知识详情加载失败:', error)
  }
  
  loading.value = false

  if (knowledge.value?.id) {
    await flushQueue()
    startTimers()
    bindActivityListeners()
  }

  // 等待DOM更新后初始化MathJax（如果不存在或版本不支持，则尝试动态加载 MathJax v3）
  await nextTick()
  updateCurrentStep()

  const loadMathJaxV3 = () => {
    return new Promise((resolve, reject) => {
      try {
        // 如果已经加载了 v3 并且 typesetPromise 可用，直接返回
        if (window.MathJax && typeof window.MathJax.typesetPromise === 'function') {
          resolve()
          return
        }

        // 创建脚本标签加载 MathJax v3
        const script = document.createElement('script')
        script.src = 'https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js'
        script.async = true
        script.onload = () => {
          // 等待 MathJax 初始化
          setTimeout(() => resolve(), 200)
        }
        script.onerror = (e) => reject(e)
        document.head.appendChild(script)
      } catch (err) {
        reject(err)
      }
    })
  }

  const ensureAndTypesetMathJax = async () => {
    try {
      // 如果 MathJax 未定义或不包含 typesetPromise，尝试加载 v3
      if (typeof window.MathJax === 'undefined' || typeof window.MathJax.typesetPromise !== 'function') {
        try {
          await loadMathJaxV3()
        } catch (loadErr) {
          console.warn('加载 MathJax v3 失败，继续尝试使用现有 MathJax（若有）:', loadErr)
        }
      }

      // 到这里，优先使用 v3 的 typesetPromise，如果不存在再回落到 v2 的 Hub.Queue
      if (window.MathJax && typeof window.MathJax.typesetPromise === 'function') {
        try {
          await window.MathJax.typesetPromise()
          console.log('MathJax(v3) 渲染成功')
        } catch (err) {
          console.error('MathJax(v3) 渲染失败:', err)
          // 重试一次
          try {
            await window.MathJax.typesetPromise()
            console.log('MathJax(v3) 重试渲染成功')
          } catch (retryErr) {
            console.error('MathJax(v3) 重试渲染失败:', retryErr)
          }
        }
      } else if (window.MathJax && window.MathJax.Hub && typeof window.MathJax.Hub.Queue === 'function') {
        try {
          window.MathJax.Hub.Queue(['Typeset', window.MathJax])
          console.log('MathJax(v2) 渲染触发成功')
        } catch (err2) {
          console.error('MathJax(v2) 渲染失败:', err2)
        }
      } else {
        console.warn('MathJax 未找到可用的渲染接口（v2/v3 均不可用）')
      }
    } catch (e) {
      console.error('ensureAndTypesetMathJax 出错:', e)
    }
  }

  // 延迟执行以避免与页面初始渲染冲突
  setTimeout(() => {
    ensureAndTypesetMathJax()
  }, 300)
})

onBeforeRouteLeave(async () => {
  await stopTimers()
})

onBeforeUnmount(async () => {
  await stopTimers()
  unbindActivityListeners()
  window.removeEventListener('scroll', handleHeaderScroll, true)
})
</script>

<style scoped>
.knowledge-details-container {
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  background-color: #ffffff;
  min-height: 100%;
}

.layui-fluid {
  width: 100%;
  max-width: 100%;
  margin: 0;
  background: #ffffff;
  padding: 24px 28px 32px;
  box-sizing: border-box;
}

/* 顶部标题区域 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f2f5;
  position: sticky;
  top: 0;
  background: #ffffff;
  z-index: 12;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  transition: padding 0.25s ease, box-shadow 0.25s ease;
  width: calc(100% + 56px);
  margin-left: -28px;
  margin-right: -28px;
  padding-left: 28px;
  padding-right: 28px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #111827;
  letter-spacing: 0.5px;
  margin: 0;
  transition: font-size 0.25s ease;
}

.page-description {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
  transition: opacity 0.25s ease, max-height 0.25s ease, margin 0.25s ease;
  max-height: 48px;
  overflow: hidden;
}

.page-header.is-compact {
  padding: 8px 28px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
}

.page-header.is-compact .page-title {
  font-size: 18px;
}

.page-header.is-compact .page-description {
  opacity: 0;
  max-height: 0;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.practice-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #ffffff;
  color: #1E88E5;
  border: 1px solid #93c5fd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.practice-btn:hover {
  background: #1E88E5;
  color: #ffffff;
}

.practice-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #1E88E5;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.back-btn:hover {
  background: #1565C0;
}

.back-btn .layui-icon {
  font-size: 16px;
}

/* 面包屑导航 */
.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 24px;
}

.breadcrumb-link {
  color: #6b7280;
  cursor: pointer;
  transition: color 0.3s ease;
}

.breadcrumb-link:hover {
  color: #1E88E5;
}

.breadcrumb .layui-icon-right {
  margin: 0 8px;
  font-size: 12px;
}

.breadcrumb-title {
  color: #111827;
}

/* 详情内容容器 */
.detail-section {
  margin-bottom: 32px;
}

.detail-container {
  width: 100%;
}

/* 知识概览卡片 */
.knowledge-overview {
  margin-bottom: 32px;
}

.overview-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #e5e7eb;
  overflow: hidden;
  display: flex;
}

.overview-left {
  width: 33.333%;
  background: linear-gradient(135deg, #1E88E5 0%, #1565C0 100%);
  padding: 32px;
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.knowledge-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 24px;
  line-height: 1.4;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tag {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 13px;
}

.prerequisites-section {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.prerequisite-text {
  font-size: 13px;
  opacity: 0.9;
  margin: 8px 0;
  line-height: 1.6;
}

.overview-right {
  flex: 1;
  padding: 32px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

.summary-text {
  color: #4b5563;
  font-size: 15px;
  line-height: 1.7;
  margin-bottom: 24px;
}

.features-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
}

.feature-card {
  flex: 1;
  min-width: 200px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
}

.feature-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #1E88E5;
}

.feature-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.feature-title .layui-icon {
  font-size: 16px;
  color: #1E88E5;
}

.feature-description {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}

/* 学习步骤 */
.steps-section {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 24px;
  border: 1px solid #e5e7eb;
}

.steps-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 24px;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.step-item {
  display: flex;
  gap: 16px;
}

.step-number {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  background: #1E88E5;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
}

.step-content {
  flex: 1;
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  color: #1E88E5;
  margin-bottom: 10px;
}

.step-description {
  font-size: 15px;
  color: #4b5563;
  line-height: 1.8;
  margin-bottom: 12px;
  text-align: justify;
}

.code-block {
  background: #1e293b;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.6;
  margin: 16px 0;
}

.tip-box {
  background: #eff6ff;
  border-left: 4px solid #1E88E5;
  padding: 16px;
  border-radius: 0 8px 8px 0;
  margin-top: 12px;
}

.tip-content {
  display: flex;
  gap: 12px;
}

.tip-content .layui-icon {
  font-size: 18px;
  color: #1E88E5;
  flex-shrink: 0;
}

.tip-text {
  font-size: 14px;
  color: #1e40af;
  margin: 0;
  line-height: 1.6;
}

/* 完成学习区域 */
.completion-section {
  margin-top: 36px;
}

.completion-card {
  display: grid;
  grid-template-columns: minmax(200px, 240px) minmax(320px, 1fr) auto;
  align-items: center;
  gap: 20px;
  padding: 22px 26px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #eef2ff 100%);
  border: 1px solid #dbeafe;
  box-shadow: 0 10px 24px rgba(30, 136, 229, 0.1);
  max-width: 980px;
  margin: 0 auto;
}

.completion-card--done {
  background: linear-gradient(135deg, #ecfeff 0%, #e0f2fe 100%);
  border-color: #bae6fd;
}

.completion-text h4 {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 600;
  color: #1e3a8a;
}

.completion-text p {
  margin: 0;
  color: #4b5563;
  font-size: 13px;
}

.completion-input {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 480px;
  width: 100%;
}

.completion-input--readonly {
  gap: 10px;
}

.completion-label {
  font-size: 12px;
  color: #1e3a8a;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.completion-textarea {
  width: 100%;
  resize: vertical;
  min-height: 88px;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid #bfdbfe;
  background: #ffffff;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.7;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.08);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.completion-textarea::placeholder {
  color: #9ca3af;
}

.completion-textarea:focus {
  outline: none;
  border-color: #1E88E5;
  box-shadow: 0 0 0 3px rgba(30, 136, 229, 0.15);
}

.completion-remark {
  min-height: 88px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px dashed #93c5fd;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.7;
}

.complete-btn {
  padding: 11px 24px;
  background: #1E88E5;
  color: #ffffff;
  border: none;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 10px 20px rgba(30, 136, 229, 0.25);
  white-space: nowrap;
}

.complete-btn:hover {
  background: #1565C0;
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(21, 101, 192, 0.3);
}

.complete-btn:active {
  transform: translateY(0);
  box-shadow: 0 8px 16px rgba(30, 136, 229, 0.2);
}

.complete-btn--ghost {
  background: #ffffff;
  color: #1E88E5;
  border: 1px solid #93c5fd;
  box-shadow: none;
}

.complete-btn--ghost:hover {
  background: #1E88E5;
  color: #ffffff;
}

@media (max-width: 640px) {
  .completion-card {
    grid-template-columns: 1fr;
    align-items: stretch;
    max-width: 100%;
  }

  .completion-input {
    width: 100%;
    max-width: 100%;
  }
}

/* 加载和错误状态 */
.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  color: #1E88E5;
}

.error-content {
  text-align: center;
  padding: 32px;
}

.error-title {
  font-size: 20px;
  font-weight: 600;
  color: #ef4444;
  margin: 16px 0 8px;
}

.error-message {
  color: #6b7280;
  margin-bottom: 24px;
}

/* MathJax样式 */
:deep(.no-mathjax) {
  /* 阻止MathJax解析 - 通过MathJax配置中的ignoreHtmlClass实现 */
  display: inline;
}

:deep(.mjx-chtml) {
  margin: 0 0.2em !important;
}

:deep(.MathJax) {
  margin: 0 0.2em !important;
}
</style>
