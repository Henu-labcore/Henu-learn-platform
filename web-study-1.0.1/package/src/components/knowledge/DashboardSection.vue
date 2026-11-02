<template>
  <div class="dashboard-page">
    <div class="dashboard-container">
      <div v-if="loading" class="status-panel">
        <el-icon class="is-loading" :size="28"><Loading /></el-icon>
        <p>正在加载学习数据...</p>
      </div>

      <div v-else-if="error" class="status-panel">
        <p>{{ error }}</p>
        <el-button type="primary" @click="fetchDashboard">重试</el-button>
      </div>

      <div v-else-if="isEmpty" class="status-panel">
        <p>还没有学习记录，去学习中心开始吧。</p>
        <el-button type="primary" @click="goLearningCenter">去学习中心</el-button>
      </div>

      <div v-else class="dashboard-content">
      <section class="panel">
        <div class="panel-header">
          <h2>总览</h2>
          <span class="panel-sub">本周继续加油</span>
        </div>
        <div class="summary-grid">
          <div class="summary-donut">
            <DonutChart
              :value="summaryRate"
              :empty="summaryTotalCount === 0"
              :completed="summary.completedCount"
              :in-progress="summary.inProgressCount"
              :total="summaryTotalCount"
            />
            <div class="donut-label">
              <span class="rate">{{ summaryRate }}%</span>
              <span>完成率</span>
            </div>
          </div>
          <div class="kpi-grid">
            <div class="kpi-card">
              <span>已完成 / 总数</span>
              <strong>{{ summary.completedCount }}/{{ summaryTotalCount }}</strong>
            </div>
            <div class="kpi-card">
              <span>学习中条目</span>
              <strong>{{ summary.inProgressCount }}</strong>
            </div>
            <div class="kpi-card">
              <span>累计学习时长</span>
              <strong>{{ formatDuration(summary.learnDurationSec) }}</strong>
            </div>
            <div class="kpi-card">
              <span>最近活跃时间</span>
              <strong>{{ formatTime(summary.lastActiveTime) }}</strong>
            </div>
          </div>
          <div class="summary-heatmap">
            <div class="summary-heatmap-head">
              <h3>学习活跃热力图</h3>
              <span>最近 12 周</span>
            </div>
            <div class="heatmap-grid">
              <div v-for="(week, wIndex) in heatmapWeeks" :key="wIndex" class="heatmap-week">
                <div
                  v-for="day in week"
                  :key="day.date"
                  class="heatmap-day"
                  :class="`heatmap-level-${day.level}`"
                  :data-tip="`${day.count}个学习 · ${day.date}`"
                ></div>
              </div>
            </div>
            <div class="heatmap-stats">
              <span>最近一月学习：{{ recentMonthCount }} 次</span>
              <span>最长连续学习：{{ longestStreak }} 日</span>
            </div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h2>分类进度</h2>
          <div class="panel-actions">
            <el-select v-model="categorySort" size="small" style="width: 140px">
              <el-option label="完成率" value="rate" />
              <el-option label="时长" value="duration" />
            </el-select>
            <el-button text type="primary" @click="toggleCategoryExpand">
              {{ categoryExpanded ? '收起' : '展开全部' }}
            </el-button>
          </div>
        </div>
        <div class="category-list">
          <div
            v-for="item in visibleCategories"
            :key="item.category"
            class="category-row"
          >
            <div class="category-name">
              <span>{{ item.category }}</span>
              <small>{{ item.completedCount }}/{{ item.totalCount }}</small>
            </div>
            <div class="category-progress">
              <div class="progress-track">
                <div class="progress-bar" :style="{ width: item.bar + '%' }"></div>
              </div>
              <span>{{ item.metricLabel }}</span>
            </div>
            <div class="category-meta">{{ formatDuration(item.learnDurationSec) }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h2>难度分布</h2>
          <span class="panel-sub">按难度统计学习条目与时长</span>
        </div>
        <div class="difficulty-chart">
          <div ref="difficultyChartRef" class="chart-canvas"></div>
          <div v-if="difficultyEmpty" class="chart-empty">暂无难度分布数据</div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h2>学习完成趋势</h2>
          <div class="panel-actions">
            <el-radio-group v-model="trendDays" size="small">
              <el-radio-button v-for="option in trendOptions" :key="option.value" :label="option.value">
                {{ option.label }}
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="trend-section">
          <TrendChart :labels="trendLabels" :values="trendValues" :empty="trendEmpty" />
          <div v-if="trendEmpty" class="trend-empty-note">
            最近没有完成记录，去学习一个条目吧。
          </div>
        </div>
      </section>


      <section class="panel">
        <div class="panel-header">
          <h2>最近学习</h2>
          <span class="panel-sub">继续保持学习节奏</span>
        </div>
        <div class="recent-list">
          <div v-for="item in recentList" :key="item.itemPk" class="recent-item">
            <div class="recent-main">
              <h3>{{ item.title }}</h3>
              <div class="recent-tags">
                <span>{{ item.category }}</span>
                <span>{{ item.difficulty }}</span>
                <span>{{ formatDuration(item.learnDurationSec) }}</span>
              </div>
              <p class="recent-time">更新时间：{{ formatTime(item.updatedAt) }}</p>
              <p v-if="item.completeTime" class="recent-remark">
                完成备注：{{ item.completeRemark || '无备注' }}
              </p>
            </div>
            <div class="recent-actions">
              <el-tag :type="item.completeTime ? 'success' : 'info'">
                {{ item.completeTime ? '已完成' : '学习中' }}
              </el-tag>
              <el-button type="primary" plain @click="goToLearning(item.itemPk)">
                继续学习
              </el-button>
            </div>
          </div>
        </div>
      </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Loading, HomeFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import DonutChart from '@/components/analytics/DonutChart.vue'
import TrendChart from '@/components/analytics/TrendChart.vue'
import { getLearnDashboard, getLearnCompletionTrend } from '@/api/learnAnalytics'
import { formatDuration, formatTime, withDefaultCategory, withDefaultDifficulty, calcRate } from '@/utils/learnAnalytics'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const dashboard = ref(null)
const categorySort = ref('rate')
const categoryExpanded = ref(false)
const trendDays = ref(30)
const trendOptions = [
  { label: '7天', value: 7 },
  { label: '30天', value: 30 },
  { label: '90天', value: 90 }
]

const difficultyChartRef = ref(null)
let difficultyChart = null

const summary = computed(() => dashboard.value?.summary || {
  completionRate: 0,
  completedCount: 0,
  totalCount: 0,
  totalCourseCount: 0,
  inProgressCount: 0,
  learnDurationSec: 0,
  lastActiveTime: null
})

const summaryTotalCount = computed(() => summary.value.totalCourseCount || summary.value.totalCount)
const summaryRate = computed(() => {
  const rate = summary.value.completionRate
  if (rate) {
    return rate > 0 && rate <= 1 ? Math.round(rate * 100) : Math.round(rate)
  }
  return calcRate(summary.value.completedCount, summaryTotalCount.value)
})

const recentList = computed(() => {
  const list = dashboard.value?.recent || []
  return list.slice(0, 5).map(item => ({
    ...item,
    category: withDefaultCategory(item.category),
    difficulty: withDefaultDifficulty(item.difficulty)
  }))
})

const recommendation = computed(() => {
  const rec = dashboard.value?.recommendation
  if (!rec) return null
  return {
    ...rec,
    category: withDefaultCategory(rec.category),
    difficulty: withDefaultDifficulty(rec.difficulty)
  }
})

const normalizedCategories = computed(() => {
  const list = dashboard.value?.byCategory || []
  return list.map(item => {
    const total = item.totalCount || 0
    const completed = item.completedCount || 0
    return {
      ...item,
      category: withDefaultCategory(item.category),
      totalCount: total,
      completedCount: completed,
      rate: calcRate(completed, total)
    }
  })
})

const sortedCategories = computed(() => {
  const list = [...normalizedCategories.value]
  if (categorySort.value === 'duration') {
    return list.sort((a, b) => b.learnDurationSec - a.learnDurationSec)
  }
  return list.sort((a, b) => b.rate - a.rate)
})

const maxCategoryDuration = computed(() => {
  const values = normalizedCategories.value.map(item => item.learnDurationSec || 0)
  return values.length ? Math.max(...values) : 0
})

const visibleCategories = computed(() => {
  const list = categoryExpanded.value ? sortedCategories.value : sortedCategories.value.slice(0, 6)
  if (categorySort.value === 'duration') {
    const maxDuration = maxCategoryDuration.value || 1
    return list.map(item => ({
      ...item,
      bar: Math.round((item.learnDurationSec / maxDuration) * 100),
      metricLabel: formatDuration(item.learnDurationSec)
    }))
  }
  return list.map(item => ({
    ...item,
    bar: item.rate,
    metricLabel: `${item.rate}%`
  }))
})

const completionTrend = computed(() => dashboard.value?.completionTrend || [])
const trendLabels = computed(() => completionTrend.value.map(item => item.date))
const trendValues = computed(() => completionTrend.value.map(item => item.completedCount))
const trendEmpty = computed(() => trendValues.value.length === 0)

const heatmapDays = computed(() => {
  const map = new Map(completionTrend.value.map(item => [item.date, item.completedCount]))
  const days = []
  for (let i = 83; i >= 0; i -= 1) {
    const date = dayjs().subtract(i, 'day').format('YYYY-MM-DD')
    const count = map.get(date) || 0
    days.push({ date, count })
  }
  const max = Math.max(...days.map(item => item.count), 0)
  return days.map(item => ({
    ...item,
    level: max === 0 ? 0 : Math.min(4, Math.ceil((item.count / max) * 4))
  }))
})

const heatmapWeeks = computed(() => {
  const weeks = []
  const days = heatmapDays.value
  const rows = 7
  const totalWeeks = Math.ceil(days.length / rows)
  for (let col = 0; col < totalWeeks; col += 1) {
    const week = []
    for (let row = 0; row < rows; row += 1) {
      const index = row * totalWeeks + col
      if (index < days.length) week.push(days[index])
    }
    weeks.push(week)
  }
  return weeks
})

const recentMonthCount = computed(() => {
  const recentDays = heatmapDays.value.slice(-30)
  return recentDays.reduce((total, day) => total + day.count, 0)
})

const longestStreak = computed(() => {
  let maxStreak = 0
  let current = 0
  for (const day of heatmapDays.value) {
    if (day.count > 0) {
      current += 1
      if (current > maxStreak) maxStreak = current
    } else {
      current = 0
    }
  }
  return maxStreak
})

const difficultyData = computed(() => (dashboard.value?.byDifficulty || []).map(item => ({
  ...item,
  difficulty: withDefaultDifficulty(item.difficulty)
})))

const difficultyEmpty = computed(() => difficultyData.value.length === 0)

const isEmpty = computed(() => summaryTotalCount.value === 0)

const goHome = () => router.push('/')
const goLearningCenter = () => router.push('/knowledge')
const goToLearning = (itemPk) => router.push(`/learning/item/${itemPk}`)
const toggleCategoryExpand = () => {
  categoryExpanded.value = !categoryExpanded.value
}

const renderDifficultyChart = () => {
  if (!difficultyChartRef.value) return
  if (!difficultyChart) {
    difficultyChart = echarts.init(difficultyChartRef.value)
  }
  const labels = difficultyData.value.map(item => item.difficulty)
  const totals = difficultyData.value.map(item => item.totalCount)
  const completed = difficultyData.value.map(item => item.completedCount)
  const durations = difficultyData.value.map(item => Math.round(item.learnDurationSec / 60))

  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['总数', '已完成', '时长(分)'] },
    grid: { left: 24, right: 24, top: 40, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: labels, axisLine: { lineStyle: { color: '#E5E7EB' } } },
    yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#EEF2F7' } } },
    color: ['#93C5FD', '#1E88E5', '#6EE7B7'],
    series: [
      { type: 'bar', data: totals, barWidth: 16, name: '总数' },
      { type: 'bar', data: completed, barWidth: 16, name: '已完成' },
      { type: 'bar', data: durations, barWidth: 16, name: '时长(分)' }
    ]
  }
  difficultyChart.setOption(option, true)
}

const fetchDashboard = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getLearnDashboard()
    dashboard.value = res?.data ?? res
  } catch (err) {
    error.value = err?.message || '加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

const fetchTrend = async (days) => {
  try {
    const res = await getLearnCompletionTrend(days)
    const data = res?.data ?? res
    dashboard.value = {
      ...dashboard.value,
      completionTrend: data || []
    }
  } catch (err) {
    console.error('趋势数据加载失败:', err)
  }
}

const resizeDifficultyChart = () => {
  if (difficultyChart) difficultyChart.resize()
}

watch(difficultyData, renderDifficultyChart, { deep: true })
watch(trendDays, (val) => fetchTrend(val))

onMounted(async () => {
  await fetchDashboard()
  renderDifficultyChart()
  window.addEventListener('resize', resizeDifficultyChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeDifficultyChart)
  if (difficultyChart) {
    difficultyChart.dispose()
    difficultyChart = null
  }
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  font-family: 'Microsoft YaHei', Arial, sans-serif;
}

.dashboard-container {
  width: 100%;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 24px;
  color: #111827;
}

.dashboard-header p {
  margin: 6px 0 0;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.panel {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  border: 1px solid #eef2f7;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-header h2 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.panel-sub {
  color: #94a3b8;
  font-size: 13px;
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 4fr) minmax(0, 7fr) auto;
  gap: 20px;
}

.summary-donut {
  display: flex;
  align-items: center;
  gap: 20px;
  background: linear-gradient(135deg, #f0f9ff 0%, #eff6ff 100%);
  padding: 16px 20px;
  border-radius: 14px;
}

.donut-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #1f2937;
}

.donut-label .rate {
  font-size: 26px;
  font-weight: 700;
  color: #1E88E5;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 12px;
}

.kpi-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 12px 14px;
  border: 1px solid #eef2f7;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kpi-card span {
  color: #6b7280;
  font-size: 12px;
}

.kpi-card strong {
  font-size: 17px;
  color: #111827;
}

.summary-heatmap {
  background: #f8fafc;
  border-radius: 14px;
  padding: 12px 16px 10px;
  border: 1px solid #eef2f7;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: visible;
  width: max-content;
  justify-self: start;
  align-self: start;
}

.summary-heatmap-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-heatmap-head h3 {
  margin: 0;
  font-size: 15px;
  color: #111827;
}

.summary-heatmap-head span {
  font-size: 12px;
  color: #94a3b8;
}
.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-row {
  display: grid;
  grid-template-columns: 220px 1fr 120px;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.category-row:last-child {
  border-bottom: none;
}

.category-name {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #111827;
}

.category-name small {
  color: #9ca3af;
}

.category-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-track {
  flex: 1;
  height: 8px;
  background: #eef2f7;
  border-radius: 999px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #60a5fa 0%, #1E88E5 100%);
  border-radius: 999px;
}

.category-meta {
  text-align: right;
  color: #6b7280;
  font-size: 13px;
}

.difficulty-chart {
  position: relative;
  height: 320px;
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.trend-section {
  position: relative;
}

.trend-empty-note {
  margin-top: 12px;
  color: #94a3b8;
  font-size: 13px;
}

.heatmap-grid {
  display: flex;
  gap: 6px;
  overflow: visible;
  margin-top: 8px;
  padding: 2px 0;
  width: max-content;
}

.heatmap-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.heatmap-week {
  display: grid;
  grid-template-rows: repeat(7, 14px);
  gap: 6px;
  overflow: visible;
}

.heatmap-day {
  position: relative;
  width: 14px;
  height: 14px;
  border-radius: 4px;
  background: #eef2f7;
  border: 1px solid #e5e7eb;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.heatmap-day:hover {
  transform: translateY(-1px);
  border-color: #1E88E5;
  box-shadow: 0 4px 10px rgba(30, 136, 229, 0.2);
  z-index: 2;
}

.heatmap-day:hover::after {
  content: attr(data-tip);
  position: absolute;
  top: -36px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 8px;
  border-radius: 8px;
  background: #ffffff;
  color: #1f2937;
  font-size: 12px;
  white-space: nowrap;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.15);
  border: 1px solid #e5e7eb;
}

.heatmap-day:hover::before {
  content: '';
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: #ffffff transparent transparent transparent;
}

.heatmap-level-0 {
  background: #eef2f7;
  border-color: #e5e7eb;
}

.heatmap-level-1 {
  background: #dbeafe;
  border-color: #bfdbfe;
}

.heatmap-level-2 {
  background: #93c5fd;
  border-color: #93c5fd;
}

.heatmap-level-3 {
  background: #60a5fa;
  border-color: #60a5fa;
}

.heatmap-level-4 {
  background: #2563eb;
  border-color: #2563eb;
}


.recent-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.recent-main h3 {
  margin: 0 0 8px;
  color: #111827;
}

.recent-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #6b7280;
  font-size: 12px;
}

.recent-time {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.recent-remark {
  margin-top: 6px;
  color: #1f2937;
  font-size: 13px;
}

.recent-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.status-panel {
  background: #ffffff;
  border-radius: 16px;
  padding: 40px;
  border: 1px solid #eef2f7;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
  color: #6b7280;
}

@media (max-width: 1024px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
  .category-row {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }
  .recent-item {
    flex-direction: column;
    align-items: flex-start;
  }
  .recent-actions {
    align-items: flex-start;
  }
  .recommendation-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .header-actions {
    flex-direction: column;
    align-items: flex-end;
  }
}
</style>
