<template>
  <div class="mastery-page">
    <div class="page-container">
      <div
        v-if="loading && !masteryData"
        class="status-panel"
      >
        <el-icon
          class="is-loading"
          :size="32"
        >
          <Loading />
        </el-icon>

        <p>正在加载知识点掌握度...</p>
      </div>

      <div
        v-else-if="error && !masteryData"
        class="status-panel error-panel"
      >
        <el-icon :size="32">
          <WarningFilled />
        </el-icon>

        <p>{{ error }}</p>

        <el-button
          type="primary"
          @click="fetchMastery"
        >
          重新加载
        </el-button>
      </div>

      <template v-else-if="masteryData">
        <section class="student-banner">
          <div class="student-profile">
            <div class="student-avatar">
              {{ avatarText }}
            </div>

            <div>
              <h2>{{ studentName }}</h2>

              <p>
                用户名：{{ masteryData.student.username }}
              </p>
            </div>
          </div>

          <el-tag
            type="success"
            effect="plain"
            size="large"
          >
            当前登录学生
          </el-tag>
        </section>

        <div
          v-if="masteryData.count === 0"
          class="empty-panel"
        >
          <el-empty
            description="暂未生成知识点掌握度数据"
          >
            <template #image>
              <el-icon
                class="empty-icon"
                :size="74"
              >
                <DataAnalysis />
              </el-icon>
            </template>

            <div class="empty-description">
              <p>
                当前账号还没有掌握度计算结果。
              </p>

              <p>
                完成学习卡片和能力考核后，
                管理员可以为你生成知识点风险分析。
              </p>
            </div>
          </el-empty>
        </div>

        <template v-else>
          <section class="profile-overview">
            <article class="panel profile-radar-panel">
              <div class="panel-header">
                <div>
                  <h2>能力画像</h2>

                  <p>
                    五个维度均为 0~100 分
                  </p>
                </div>

                <div class="radar-header-actions">
                  <el-button
                    text
                    :loading="loading"
                    @click="fetchMastery"
                  >
                    刷新数据
                  </el-button>

                  <el-tag
                    v-if="profileData"
                    type="info"
                    effect="plain"
                  >
                    置信度：{{ profileData.confidence.label }}
                  </el-tag>
                </div>
              </div>

              <ProfileRadar
                v-loading="profileLoading"
                :dimensions="profileDimensions"
                :height="420"
              />
            </article>

            <article class="panel profile-summary-panel">
              <div class="panel-header profile-summary-header">
                <div>
                  <h2>画像结论</h2>

                  <p>基于最近 30 天的学习与考核数据</p>
                </div>

                <span
                  v-if="profileData"
                  class="profile-updated"
                >
                  更新于 {{ formatProfileTime(profileData.updatedAt) }}
                </span>
              </div>

              <template v-if="profileData">
                <div
                  class="persona-block"
                  :class="`persona-${String(profileData.persona.code).toLowerCase()}`"
                >
                  <span class="persona-label">
                    {{ profileData.persona.label }}
                  </span>

                  <p>{{ profileData.persona.description }}</p>
                </div>

                <ProfileTagList :tags="profileTags" />

                <ProfileSuggestion
                  :recommendation="profileRecommendation"
                />
              </template>

              <div v-else class="profile-empty">
                画像生成中，完成一次学习后刷新即可看到
              </div>
            </article>
          </section>

          <section class="panel profile-trend-panel">
            <div class="panel-header">
              <div>
                <h2>画像趋势</h2>

                <p>
                  近 30 天五维分数变化；没有记录的日期不连线
                </p>
              </div>
            </div>

            <ProfileTrendChart
              v-loading="profileHistoryLoading"
              :dates="profileHistoryDates"
              :series="profileHistorySeries"
              :height="300"
            />
          </section>

          <section class="summary-grid">
            <article class="summary-card">
              <div class="summary-icon score-icon">
                <el-icon>
                  <TrendCharts />
                </el-icon>
              </div>

              <div>
                <span class="summary-label">
                  平均掌握度
                </span>

                <strong>
                  {{
                    formatPercentage(
                      masteryData.summary
                        .averageMasteryScore
                    )
                  }}
                </strong>

                <small>
                  共 {{ masteryData.count }} 个知识点
                </small>
              </div>
            </article>

            <article class="summary-card">
              <div class="summary-icon high-icon">
                <el-icon>
                  <WarningFilled />
                </el-icon>
              </div>

              <div>
                <span class="summary-label">
                  高风险知识点
                </span>

                <strong>
                  {{
                    masteryData.summary
                      .highRiskCount
                  }}
                </strong>

                <small>
                  建议优先补强
                </small>
              </div>
            </article>

            <article class="summary-card">
              <div class="summary-icon medium-icon">
                <el-icon>
                  <Warning />
                </el-icon>
              </div>

              <div>
                <span class="summary-label">
                  中风险知识点
                </span>

                <strong>
                  {{
                    masteryData.summary
                      .mediumRiskCount
                  }}
                </strong>

                <small>
                  建议继续巩固
                </small>
              </div>
            </article>

            <article class="summary-card">
              <div class="summary-icon low-icon">
                <el-icon>
                  <CircleCheck />
                </el-icon>
              </div>

              <div>
                <span class="summary-label">
                  低风险知识点
                </span>

                <strong>
                  {{
                    masteryData.summary
                      .lowRiskCount
                  }}
                </strong>

                <small>
                  当前掌握较稳定
                </small>
              </div>
            </article>
          </section>

          <section class="chart-section">
            <article class="panel chart-panel">
              <div class="panel-header chart-header">
                <div>
                  <h2>知识点掌握度分布</h2>

                  <p>
                    对比各知识点的综合掌握度和预警阈值
                  </p>
                </div>

                <el-button
                  class="detail-entry"
                  text
                  title="查看掌握度明细"
                  @click="detailVisible = true"
                >
                  详情
                </el-button>
              </div>

              <div
                ref="chartRef"
                class="mastery-chart"
              />
            </article>
          </section>

          <section class="panel history-panel">
            <div class="panel-header history-header">
              <div>
                <h2>掌握度趋势</h2>

                <p>
                  按天统计已接触知识点的平均掌握度，
                  并叠加高风险数量；30 天视图每次显示 14 天，
                  可拖动底部滑块查看更早数据
                </p>
              </div>

              <el-radio-group
                v-model="historyDays"
                size="small"
                :disabled="historyLoading"
                @change="fetchHistory"
              >
                <el-radio-button :label="7">
                  近 7 天
                </el-radio-button>

                <el-radio-button :label="30">
                  近 30 天
                </el-radio-button>
              </el-radio-group>
            </div>

            <MasteryTrendChart
              v-loading="historyLoading"
              :points="historyTrend"
              :window-size="historyDays === 30 ? 14 : 0"
              :height="330"
            />
          </section>

          <el-drawer
            v-model="detailVisible"
            title="掌握度明细"
            size="86%"
            destroy-on-close
          >
            <p class="detail-hint">
              查看学习表现、考核表现和综合风险等级
            </p>

            <el-table
              v-loading="loading"
              :data="masteryData.data"
              stripe
              border
              style="width: 100%"
              :header-cell-style="tableHeaderStyle"
            >
              <el-table-column
                prop="code"
                label="知识点编码"
                width="125"
                fixed="left"
              />

              <el-table-column
                prop="knowledgePointName"
                label="知识点名称"
                min-width="190"
                fixed="left"
              />

              <el-table-column
                prop="category"
                label="分类"
                width="120"
              />

              <el-table-column
                label="难度"
                width="105"
                align="center"
              >
                <template #default="{ row }">
                  {{
                    getDifficultyText(
                      row.difficulty
                    )
                  }}
                </template>
              </el-table-column>

              <el-table-column
                label="学习得分"
                width="125"
                align="center"
              >
                <template #default="{ row }">
                  {{
                    formatPointScore(
                      row.learningScore
                    )
                  }}
                </template>
              </el-table-column>

              <el-table-column
                label="考核得分"
                width="125"
                align="center"
              >
                <template #default="{ row }">
                  {{
                    formatPointScore(
                      row.assessmentScore
                    )
                  }}
                </template>
              </el-table-column>

              <el-table-column
                label="综合掌握度"
                min-width="190"
              >
                <template #default="{ row }">
                  <div class="score-cell">
                    <el-progress
                      :percentage="
                        normalizeScore(
                          row.masteryScore
                        )
                      "
                      :stroke-width="10"
                      :show-text="false"
                      :color="
                        getScoreColor(
                          row.masteryScore
                        )
                      "
                    />

                    <span>
                      {{
                        formatPercentage(
                          row.masteryScore
                        )
                      }}
                    </span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column
                label="预警阈值"
                width="125"
                align="center"
              >
                <template #default="{ row }">
                  {{
                    formatPercentage(
                      row.warningThreshold
                    )
                  }}
                </template>
              </el-table-column>

              <el-table-column
                label="风险等级"
                width="110"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="
                      getRiskTagType(
                        row.riskLevel
                      )
                    "
                    effect="dark"
                  >
                    {{
                      getRiskText(
                        row.riskLevel
                      )
                    }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column
                label="学习进度"
                width="115"
                align="center"
              >
                <template #default="{ row }">
                  {{ row.completedLearningCount }}
                  /
                  {{ row.totalLearningCount }}
                </template>
              </el-table-column>

              <el-table-column
                label="考核进度"
                width="115"
                align="center"
              >
                <template #default="{ row }">
                  {{ row.gradedAssignmentCount }}
                  /
                  {{ row.totalAssignmentCount }}
                </template>
              </el-table-column>

              <el-table-column
                label="计算时间"
                min-width="170"
              >
                <template #default="{ row }">
                  {{
                    formatDateTime(
                      row.calculatedAt
                    )
                  }}
                </template>
              </el-table-column>
            </el-table>
          </el-drawer>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref
} from 'vue'

import * as echarts from 'echarts'

import {
  CircleCheck,
  DataAnalysis,
  Loading,
  TrendCharts,
  Warning,
  WarningFilled
} from '@element-plus/icons-vue'

import {
  getMyKnowledgeMastery,
  getMyMasteryHistory,
  recalculateMyMastery
} from '@/api/masteryRisk'

import MasteryTrendChart from '@/components/mastery/MasteryTrendChart.vue'

import ProfileRadar from '@/components/profile/ProfileRadar.vue'

import ProfileSuggestion from '@/components/profile/ProfileSuggestion.vue'

import ProfileTagList from '@/components/profile/ProfileTagList.vue'

import ProfileTrendChart from '@/components/profile/ProfileTrendChart.vue'

import { getMyProfile, getMyProfileHistory } from '@/api/studentProfile'

const loading = ref(false)
const error = ref('')
const masteryData = ref(null)

const profileData = ref(null)

const profileLoading = ref(false)

const profileHistory = ref(null)

const profileHistoryLoading = ref(false)

const historyDays = ref(30)

const historyData = ref(null)

const historyLoading = ref(false)

const chartRef = ref(null)

let masteryChart = null

const detailVisible = ref(false)

const tableHeaderStyle = {
  background: '#f8fafc',
  color: '#334155',
  fontWeight: '600'
}

const studentName = computed(() => {
  const student = masteryData.value?.student

  if (!student) {
    return '当前学生'
  }

  return student.displayName
    || student.realName
    || student.username
    || '当前学生'
})

const avatarText = computed(() => {
  return studentName.value
    .charAt(0)
    .toUpperCase()
})

const profileDimensions = computed(() => {
  return profileData.value?.dimensions || []
})

const profileTags = computed(() => {
  return profileData.value?.tags || []
})

const profileRecommendation = computed(() => {
  return profileData.value?.recommendation || {
    weakPoints: [],
    continueItem: null,
    nextAction: null
  }
})

const profileHistoryDates = computed(() => {
  return profileHistory.value?.dates || []
})

const profileHistorySeries = computed(() => {
  return profileHistory.value?.series || []
})

const formatProfileTime = value => {
  return String(value || '')
    .replace('T', ' ')
    .slice(0, 16)
}

const fetchProfile = async () => {
  profileLoading.value = true

  try {
    profileData.value = await getMyProfile(30)
  } catch (profileError) {
    console.warn('加载学生画像失败', profileError)
  } finally {
    profileLoading.value = false
  }
}

const fetchProfileHistory = async () => {
  profileHistoryLoading.value = true

  try {
    profileHistory.value = await getMyProfileHistory(30)
  } catch (historyError) {
    console.warn('加载画像历史失败', historyError)
  } finally {
    profileHistoryLoading.value = false
  }
}

const weakKnowledgePointNames = computed(() => {
  const names =
    masteryData.value
      ?.summary
      ?.weakKnowledgePointNames

  return Array.isArray(names)
    ? names
    : []
})

const sortedMasteryList = computed(() => {
  const list = masteryData.value?.data

  if (!Array.isArray(list)) {
    return []
  }

  const riskOrder = {
    HIGH: 0,
    MEDIUM: 1,
    LOW: 2,
    UNKNOWN: 3
  }

  return [...list].sort((left, right) => {
    const leftRisk =
      riskOrder[left.riskLevel] ?? 4

    const rightRisk =
      riskOrder[right.riskLevel] ?? 4

    if (leftRisk !== rightRisk) {
      return leftRisk - rightRisk
    }

    return Number(left.masteryScore || 0)
      - Number(right.masteryScore || 0)
  })
})

const topWeakPoints = computed(() => {
  return sortedMasteryList.value
    .filter(
      item =>
        item.riskLevel === 'HIGH'
        || item.riskLevel === 'MEDIUM'
    )
    .slice(0, 3)
})

const historyTrend = computed(() => {
  return historyData.value?.trend || []
})

const fetchHistory = async () => {
  historyLoading.value = true

  try {
    historyData.value =
      await getMyMasteryHistory(
        historyDays.value
      )
  } catch (historyError) {
    console.warn(
      '加载掌握度历史失败',
      historyError
    )
  } finally {
    historyLoading.value = false
  }
}

const fetchMastery = async () => {
  loading.value = true
  error.value = ''

  try {
    // 先按最新学习记录与考核成绩重算自己的掌握度，
    // 再拉取详情，保证刚学完的内容能立即反映到页面上。
    // 重算失败不阻断数据加载，直接展示已有结果。
    try {
      await recalculateMyMastery()
    } catch (recalculateError) {
      console.warn(
        '个人掌握度重算失败，继续加载已有数据',
        recalculateError
      )
    }

    masteryData.value =
      await getMyKnowledgeMastery()

    await nextTick()
    renderChart()

    await fetchHistory()

    await fetchProfile()

    await fetchProfileHistory()
  } catch (requestError) {
    console.error(
      '加载个人知识点掌握度失败',
      requestError
    )

    error.value =
      requestError.message
      || '加载个人知识点掌握度失败'
  } finally {
    loading.value = false
  }
}


const renderChart = () => {
  if (
    !chartRef.value
    || !masteryData.value
    || masteryData.value.count === 0
  ) {
    masteryChart?.dispose()
    masteryChart = null
    return
  }

  if (!masteryChart) {
    masteryChart = echarts.init(
      chartRef.value
    )
  }

  const list = masteryData.value.data

  masteryChart.setOption(
    {
      tooltip: {
        trigger: 'axis',

        axisPointer: {
          type: 'shadow'
        },

        formatter(params) {
          const masteryItem =
            params.find(
              item =>
                item.seriesName
                === '综合掌握度'
            )

          const thresholdItem =
            params.find(
              item =>
                item.seriesName
                === '预警阈值'
            )

          return [
            params[0]?.name || '',

            `综合掌握度：${
              formatPercentage(
                masteryItem?.value
              )
            }`,

            `预警阈值：${
              formatPercentage(
                thresholdItem?.value
              )
            }`
          ].join('<br/>')
        }
      },

      legend: {
        top: 0,
        right: 10
      },

      grid: {
        top: 52,
        right: 28,
        bottom: 60,
        left: 60
      },

      xAxis: {
        type: 'category',

        data: list.map(
          item =>
            item.knowledgePointName
        ),

        axisLabel: {
          interval: 0,

          rotate:
            list.length > 4
              ? 20
              : 0
        }
      },

      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        name: '掌握度（%）',

        axisLabel: {
          formatter: '{value}%'
        }
      },

      series: [
        {
          name: '综合掌握度',
          type: 'bar',
          barMaxWidth: 46,

          data: list.map(item => ({
            value:
              Number(
                item.masteryScore || 0
              ),

            itemStyle: {
              color:
                getScoreColor(
                  item.masteryScore
                ),

              borderRadius:
                [6, 6, 0, 0]
            }
          })),

          label: {
            show: true,
            position: 'top',

            formatter(params) {
              return formatNumber(
                params.value
              )
            }
          }
        },

        {
          name: '预警阈值',
          type: 'line',
          symbol: 'circle',
          symbolSize: 8,

          data: list.map(
            item =>
              Number(
                item.warningThreshold || 0
              )
          )
        }
      ]
    },
    true
  )
}

const getSuggestionText = item => {
  if (item.riskLevel === 'LOW') {
    return '当前掌握较好，可进行阶段性复习并继续学习后续内容。'
  }

  if (item.riskLevel === 'UNKNOWN') {
    return '当前有效数据不足，请继续完成相关学习和考核任务。'
  }

  const reasons = item.riskLevel === 'HIGH'
    ? ['掌握度低于预警阈值']
    : ['接近预警阈值，稳定性不足']

  const actions = []

  if (
    Number(item.totalLearningCount || 0) > 0
    && Number(item.completedLearningCount || 0)
      < Number(item.totalLearningCount || 0)
  ) {
    actions.push(
      `完成剩余 ${Number(item.totalLearningCount) - Number(item.completedLearningCount)} 个学习资源`
    )
  }

  if (
    Number(item.totalAssignmentCount || 0) > 0
  ) {
    if (
      Number(item.gradedAssignmentCount || 0) === 0
    ) {
      actions.push('提交对应能力考核')
    } else {
      actions.push('复习并重做对应考核')
    }
  }

  if (actions.length === 0) {
    actions.push('重新学习相关学习卡片')
  }

  return `${reasons.join('，')}，建议：${actions.join('；')}。`
}

const getRiskText = riskLevel => {
  const textMap = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险',
    UNKNOWN: '未学'
  }

  return textMap[riskLevel]
    || '未学'
}

const getRiskTagType = riskLevel => {
  const typeMap = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success',
    UNKNOWN: 'info'
  }

  return typeMap[riskLevel]
    || 'info'
}

const getDifficultyText = difficulty => {
  const difficultyMap = {
    BASIC: '基础',
    INTERMEDIATE: '中等',
    ADVANCED: '进阶'
  }

  return difficultyMap[difficulty]
    || difficulty
    || '未设置'
}

const normalizeScore = score => {
  const value = Number(score || 0)

  return Math.min(
    100,
    Math.max(0, value)
  )
}

const getScoreColor = score => {
  const value = Number(score || 0)

  if (value < 60) {
    return '#ef4444'
  }

  if (value < 75) {
    return '#f59e0b'
  }

  return '#22c55e'
}

const formatNumber = value => {
  const number = Number(value)

  if (!Number.isFinite(number)) {
    return '0.00'
  }

  return number.toFixed(2)
}

const formatPointScore = score => {
  return `${formatNumber(score)} 分`
}

const formatPercentage = score => {
  return `${formatNumber(score)}%`
}

const formatDateTime = value => {
  if (!value) {
    return '暂无'
  }

  return String(value)
    .replace('T', ' ')
    .slice(0, 19)
}

const handleResize = () => {
  masteryChart?.resize()
}

onMounted(() => {
  window.addEventListener(
    'resize',
    handleResize
  )

  fetchMastery()
})

onBeforeUnmount(() => {
  window.removeEventListener(
    'resize',
    handleResize
  )

  masteryChart?.dispose()
  masteryChart = null
})
</script>

<style scoped>
.mastery-page {
  min-height: 100%;
}

.page-container {
  width: 100%;
}

.radar-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-panel {
  min-height: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  color: #64748b;
}

.error-panel {
  color: #dc2626;
}

.student-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  padding: 20px 24px;
  border: 1px solid #bfdbfe;
  border-radius: 14px;
  background:
    linear-gradient(
      135deg,
      #eff6ff 0%,
      #ffffff 100%
    );
}

.student-profile {
  display: flex;
  align-items: center;
  gap: 15px;
}

.student-avatar {
  width: 54px;
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background:
    linear-gradient(
      135deg,
      #3b82f6 0%,
      #2563eb 100%
    );
  color: #ffffff;
  font-size: 22px;
  font-weight: 700;
}

.student-profile h2 {
  margin: 0;
  color: #0f172a;
  font-size: 21px;
}

.student-profile p {
  margin: 7px 0 0;
  color: #64748b;
}

.empty-panel {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
}

.empty-icon {
  color: #94a3b8;
}

.empty-description {
  color: #64748b;
  line-height: 1.8;
}

.empty-description p {
  margin: 4px 0;
}

.summary-grid {
  display: grid;
  grid-template-columns:
    repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

.summary-card {
  min-height: 126px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  box-shadow:
    0 6px 20px
    rgba(15, 23, 42, 0.05);
}

.summary-icon {
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 13px;
  font-size: 25px;
}

.score-icon {
  color: #2563eb;
  background: #dbeafe;
}

.high-icon {
  color: #dc2626;
  background: #fee2e2;
}

.medium-icon {
  color: #d97706;
  background: #fef3c7;
}

.low-icon {
  color: #16a34a;
  background: #dcfce7;
}

.summary-label {
  display: block;
  color: #64748b;
  font-size: 14px;
}

.summary-card strong {
  display: block;
  margin: 5px 0;
  color: #0f172a;
  font-size: 27px;
}

.summary-card small {
  color: #94a3b8;
}

.weak-alert {
  margin-bottom: 20px;
}

.focus-section {
  margin-bottom: 20px;
  padding: 18px;
  border: 1px solid #fde68a;
  border-radius: 14px;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
}

.focus-header h4 {
  margin: 0 0 4px;
  color: #92400e;
  font-size: 16px;
}

.focus-header p {
  margin: 0 0 14px;
  color: #a16207;
  font-size: 13px;
}

.focus-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.focus-card {
  padding: 14px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #fde68a;
  border-left: 4px solid #f59e0b;
}

.focus-card.risk-high {
  border-left-color: #ef4444;
}

.focus-card.risk-medium {
  border-left-color: #f59e0b;
}

.focus-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.focus-head strong {
  color: #1f2937;
  font-size: 14px;
}

.focus-score {
  margin-bottom: 6px;
  color: #b45309;
  font-size: 13px;
  font-weight: 700;
}

.focus-card p {
  margin: 0;
  color: #57534e;
  font-size: 13px;
  line-height: 1.6;
}

.content-grid {
  display: grid;
  grid-template-columns:
    minmax(0, 1.45fr)
    minmax(330px, 0.75fr);
  gap: 20px;
  margin-bottom: 20px;
}

.history-panel {
  margin-bottom: 20px;
}

.history-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.panel {
  padding: 22px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  box-shadow:
    0 6px 20px
    rgba(15, 23, 42, 0.05);
}

.panel-header {
  margin-bottom: 18px;
}

.panel-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 19px;
}

.panel-header p {
  margin: 7px 0 0;
  color: #64748b;
  font-size: 13px;
}

.suggestion-list {
  max-height: 380px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 3px;
}

.suggestion-item {
  padding: 14px 15px;
  border: 1px solid #e2e8f0;
  border-left-width: 4px;
  border-radius: 10px;
  background: #f8fafc;
}

.suggestion-item.risk-high {
  border-left-color: #ef4444;
}

.suggestion-item.risk-medium {
  border-left-color: #f59e0b;
}

.suggestion-item.risk-low {
  border-left-color: #22c55e;
}

.suggestion-item.risk-unknown {
  border-left-color: #94a3b8;
}

.suggestion-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.suggestion-main > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.suggestion-main strong {
  color: #0f172a;
  line-height: 1.5;
}

.suggestion-code {
  color: #64748b;
  font-size: 12px;
}

.suggestion-item p {
  margin: 9px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.chart-header {
  align-items: center;
}

.detail-entry {
  flex: none;
  padding: 4px 12px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.detail-entry:hover {
  border-color: #93c5fd;
  background: #dbeafe;
  color: #1e40af;
}

.detail-hint {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 13px;
}

.score-cell {
  display: grid;
  grid-template-columns:
    minmax(85px, 1fr)
    64px;
  align-items: center;
  gap: 8px;
}

.score-cell span {
  color: #334155;
  font-weight: 600;
  text-align: right;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .mastery-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
  }

  .student-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}

.profile-overview {
  display: grid;
  grid-template-columns:
    minmax(0, 1.05fr)
    minmax(340px, 0.95fr);
  gap: 20px;
  margin-bottom: 20px;
}

.profile-summary-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.profile-updated {
  color: #94a3b8;
  font-size: 12px;
}

.persona-block {
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fbff 100%);
  margin-bottom: 14px;
}

.persona-label {
  display: inline-block;
  color: #1d4ed8;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
}

.persona-block p {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.persona-attention,
.persona-imbalanced,
.persona-needs_support,
.persona-execution,
.persona-insufficient,
.persona-silent {
  border-color: #fde68a;
  background: linear-gradient(135deg, #fffbeb 0%, #fffdf5 100%);
}

.persona-attention .persona-label,
.persona-imbalanced .persona-label,
.persona-needs_support .persona-label,
.persona-execution .persona-label,
.persona-insufficient .persona-label,
.persona-silent .persona-label {
  color: #b45309;
}

.profile-empty {
  padding: 30px 16px;
  color: #94a3b8;
  text-align: center;
  font-size: 13px;
}

.chart-section {
  margin-bottom: 20px;
}

.mastery-chart {
  width: 100%;
  height: 380px;
}

.profile-trend-panel {
  margin-bottom: 20px;
}

@media (max-width: 1100px) {
  .profile-overview {
    grid-template-columns: 1fr;
  }
}
</style>
