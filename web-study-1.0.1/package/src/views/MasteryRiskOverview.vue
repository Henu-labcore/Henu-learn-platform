<template>
  <div class="risk-page">
    <div class="page-container">
      <header class="page-header">
        <div>
          <h1>知识点掌握度与风险预警</h1>

          <p>
            根据学习行为和考核成绩分析学生掌握度，
            识别薄弱知识点，并为学生生成个性化学习计划。
          </p>
        </div>

        <div class="header-actions">
          <el-button
            type="warning"
            plain
            :loading="recalculatingAll"
            @click="handleRecalculateAll"
          >
            <el-icon>
              <TrendCharts />
            </el-icon>

            全部重算
          </el-button>

          <el-button
            type="primary"
            :loading="loading"
            @click="fetchOverview"
          >
            <el-icon>
              <Refresh />
            </el-icon>

            刷新数据
          </el-button>
        </div>
      </header>

      <div
        v-if="loading && students.length === 0"
        class="status-panel"
      >
        <el-icon
          class="is-loading"
          :size="30"
        >
          <Loading />
        </el-icon>

        <p>正在加载风险分析数据...</p>
      </div>

      <div
        v-else-if="error && students.length === 0"
        class="status-panel error-panel"
      >
        <el-icon :size="30">
          <WarningFilled />
        </el-icon>

        <p>{{ error }}</p>

        <el-button
          type="primary"
          @click="fetchOverview"
        >
          重新加载
        </el-button>
      </div>

      <div
        v-else
        class="page-content"
      >
        <section class="summary-grid">
          <article class="summary-card">
            <div class="summary-icon student-icon">
              <el-icon>
                <User />
              </el-icon>
            </div>

            <div>
              <span class="summary-label">
                启用学生
              </span>

              <strong>
                {{ summary.activeStudentCount }}
              </strong>

              <small>
                当前 ACTIVE 普通用户
              </small>
            </div>
          </article>

          <article class="summary-card">
            <div class="summary-icon calculated-icon">
              <el-icon>
                <DataAnalysis />
              </el-icon>
            </div>

            <div>
              <span class="summary-label">
                已完成计算
              </span>

              <strong>
                {{ summary.calculatedStudentCount }}
              </strong>

              <small>
                未计算
                {{ summary.uncalculatedStudentCount }}
                人
              </small>
            </div>
          </article>

          <article class="summary-card">
            <div class="summary-icon warning-icon">
              <el-icon>
                <Warning />
              </el-icon>
            </div>

            <div>
              <span class="summary-label">
                高风险学生
              </span>

              <strong>
                {{ summary.highRiskStudentCount }}
              </strong>

              <small>
                存在至少一个高风险知识点
              </small>
            </div>
          </article>

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
                  formatPercent(
                    summary.averageMasteryScore
                  )
                }}
              </strong>

              <small>
                仅统计已计算学生
              </small>
            </div>
          </article>
        </section>

        <section class="chart-grid">
          <article class="panel">
            <div class="panel-header">
              <div>
                <h2>知识点平均掌握度</h2>
                <p>已接触学生（非未学）在各知识点上的平均掌握度</p>
              </div>
            </div>

            <div
              ref="masteryChartRef"
              class="chart"
            />
          </article>

          <article class="panel">
            <div class="panel-header">
              <div>
                <h2>知识点风险分布（按学生数）</h2>
                <p>
                  按高风险率降序，最多显示 10 个知识点；
                  已接触不足 10 人标注“样本不足”并排在后面
                </p>
              </div>
            </div>

            <div
              v-if="riskRows.length > 0"
              ref="riskChartRef"
              class="chart"
            />

            <div
              v-else
              class="chart-empty"
            >
              暂无已接触的知识点数据
            </div>
          </article>
        </section>

        <section class="panel trend-panel">
          <div class="panel-header history-header">
            <div>
              <h2>班级掌握度趋势</h2>

              <p>
                按天统计已接触记录的整体平均掌握度
              </p>
            </div>

            <div class="trend-actions">
              <el-radio-group
                v-model="trendDays"
                size="small"
                :disabled="trendLoading"
                @change="fetchTrend"
              >
                <el-radio-button :label="7">
                  近 7 天
                </el-radio-button>

                <el-radio-button :label="30">
                  近 30 天
                </el-radio-button>

                <el-radio-button :label="90">
                  近 90 天
                </el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <MasteryTrendChart
            v-loading="trendLoading"
            :points="adminTrendPoints"
            :show-risk-bars="false"
            :height="330"
          />
        </section>

        <AdminProfileSection />

      </div>
    </div>

    <el-drawer
      v-model="riskDetailVisible"
      :title="riskDetailTitle"
      size="640px"
      destroy-on-close
    >
      <div v-loading="riskDetailLoading">
        <p
          v-if="riskDetailData"
          class="risk-detail-summary"
        >
          共 {{ riskDetailData.count }} 名学生
        </p>

        <el-table
          v-if="
            riskDetailData
            && riskDetailData.students.length > 0
          "
          :data="riskDetailData.students"
          stripe
          border
          style="width: 100%"
        >
          <el-table-column
            label="学生"
            min-width="140"
          >
            <template #default="{ row }">
              <div class="risk-student">
                <strong>
                  {{ row.displayName || row.username }}
                </strong>
                <small>{{ row.username }}</small>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            label="掌握度"
            width="90"
            align="center"
          >
            <template #default="{ row }">
              {{ formatScore(row.masteryScore) }}
            </template>
          </el-table-column>

          <el-table-column
            label="学习分"
            width="90"
            align="center"
          >
            <template #default="{ row }">
              {{ formatScore(row.learningScore) }}
            </template>
          </el-table-column>

          <el-table-column
            label="考核分"
            width="90"
            align="center"
          >
            <template #default="{ row }">
              {{ formatScore(row.assessmentScore) }}
            </template>
          </el-table-column>

          <el-table-column
            label="风险"
            width="90"
            align="center"
          >
            <template #default="{ row }">
              <el-tag
                :type="riskTagType(row.riskLevel)"
                effect="light"
              >
                {{ riskLabel(row.riskLevel) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-else-if="!riskDetailLoading"
          description="暂无该风险等级的学生"
        />

        <p
          v-if="riskDetailError"
          class="risk-detail-error"
        >
          {{ riskDetailError }}
        </p>
      </div>
    </el-drawer>

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
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  DataAnalysis,
  Loading,
  Refresh,
  TrendCharts,
  User,
  Warning,
  WarningFilled
} from '@element-plus/icons-vue'

import {
  getKnowledgePointsSummary,
  getKnowledgePointStudents,
  getMasteryStudentOverview,
  getMasteryTrend,
  recalculateAllStudents
} from '@/api/masteryRisk'

import MasteryTrendChart from '@/components/mastery/MasteryTrendChart.vue'

import AdminProfileSection from '@/components/profile/AdminProfileSection.vue'

const loading = ref(false)
const error = ref('')

const students = ref([])

const summary = ref({
  activeStudentCount: 0,
  calculatedStudentCount: 0,
  uncalculatedStudentCount: 0,
  highRiskStudentCount: 0,
  totalKnowledgePointCount: 0,
  totalHighRiskCount: 0,
  totalMediumRiskCount: 0,
  totalLowRiskCount: 0,
  totalUnknownRiskCount: 0,
  averageMasteryScore: 0,
  highRiskStudentNames: []
})

const recalculatingAll = ref(false)

const riskDetailVisible = ref(false)
const riskDetailLoading = ref(false)
const riskDetailData = ref(null)
const riskDetailError = ref('')
const riskDetailTitle = ref('学生名单')

const masteryChartRef = ref(null)
const riskChartRef = ref(null)

const knowledgePoints = ref([])

const trendData = ref(null)

const trendLoading = ref(false)

const trendDays = ref(30)

let masteryChart = null
let riskChart = null



const adminTrendPoints = computed(() => {
  return trendData.value?.trend || []
})

// 按知识点统计各风险等级的学生数，供风险堆叠条形图使用
const riskRows = computed(() => {
  const activeStudentCount = Number(
    summary.value.activeStudentCount || 0
  )

  // 学生较少时不启用最小样本门槛，避免图表被清空
  const enforceSample = activeStudentCount >= 10

  return (knowledgePoints.value || [])
    .map(item => {
      const high = Number(item.highCount || 0)
      const medium = Number(item.mediumCount || 0)
      const low = Number(item.lowCount || 0)
      const unknown = Number(item.unknownCount || 0)
      const contact = Number(item.contactCount || 0)

      return {
        id: item.knowledgePointId,
        name: item.name,
        high,
        medium,
        low,
        unknown,
        contact,
        rate: contact > 0
          ? high / contact * 100
          : 0,
        eligible: !enforceSample || contact >= 10
      }
    })
    .filter(item => {
      return item.high + item.medium + item.low > 0
    })
    .sort((left, right) => {
      // 样本不足的排在后面
      if (left.eligible !== right.eligible) {
        return left.eligible ? -1 : 1
      }

      return (
        right.rate - left.rate
        || right.high - left.high
        || right.medium - left.medium
        || right.low - left.low
        || left.name.localeCompare(right.name, 'zh')
      )
    })
    .slice(0, 10)
})




const unwrapPayload = response => {
  return response
}

const fetchTrend = async () => {
  trendLoading.value = true

  try {
    const response =
      await getMasteryTrend(
        trendDays.value
      )

    trendData.value =
      unwrapPayload(response) || null
  } catch (trendError) {
    console.warn(
      '加载掌握度趋势失败',
      trendError
    )

    trendData.value = null
  } finally {
    trendLoading.value = false
  }
}

const fetchOverview = async () => {
  loading.value = true
  error.value = ''

  try {
    const response =
      await getMasteryStudentOverview()

    const payload =
      unwrapPayload(response) || {}

    summary.value = {
      ...summary.value,
      ...(payload.summary || {})
    }

    students.value =
      Array.isArray(payload.data)
        ? payload.data
        : []

    // 拉取知识点维度聚合，供"知识点平均掌握度"图使用
    try {
      const knowledgeResponse =
        await getKnowledgePointsSummary()

      knowledgePoints.value =
        unwrapPayload(knowledgeResponse)
        || []
    } catch (knowledgeError) {
      console.warn(
        '加载知识点聚合失败',
        knowledgeError
      )

      knowledgePoints.value = []
    }

    await nextTick()
    renderCharts()

    // 趋势独立加载，失败不影响主列表
    fetchTrend()
  } catch (requestError) {
    console.error(
      '加载掌握度风险总览失败',
      requestError
    )

    error.value =
      requestError.message
      || '加载掌握度风险总览失败'
  } finally {
    loading.value = false
  }
}

const renderCharts = () => {
  renderMasteryChart()
  renderRiskChart()
}

const renderMasteryChart = () => {
  if (!masteryChartRef.value) {
    return
  }

  if (!masteryChart) {
    masteryChart = echarts.init(
      masteryChartRef.value
    )
  }

  const items =
    Array.isArray(knowledgePoints.value)
      ? knowledgePoints.value
      : []

  masteryChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter(params) {
        const item = params[0]

        const raw =
          items[item.dataIndex] || {}

        const score =
          raw.avgMasteryScore

        return `${raw.name || ''}<br/>` +
          `已接触学生：${raw.contactCount ?? 0} 人<br/>` +
          `平均掌握度：${
            score == null
              ? '暂无数据'
              : formatPercent(score)
          }`
      }
    },
    grid: {
      top: 25,
      right: 25,
      bottom: 55,
      left: 60
    },
    xAxis: {
      type: 'category',
      data: items.map(
        item => item.name
      ),
      axisLabel: {
        interval: 0,
        rotate: items.length > 6
          ? 25
          : 0
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      name: '掌握度（%）'
    },
    series: [
      {
        name: '平均掌握度',
        type: 'bar',
        barMaxWidth: 52,
        data: items.map(item => {
          const score =
            item.avgMasteryScore

          const hasData =
            score != null

          return {
            value: hasData
              ? Number(score)
              : 0,
            itemStyle: {
              color: hasData
                ? getScoreColor(score)
                : '#e2e8f0',
              borderRadius: [6, 6, 0, 0]
            },
            label: {
              show: true,
              position: 'top',
              formatter() {
                return hasData
                  ? formatPercent(score)
                  : '暂无'
              }
            }
          }
        })
      }
    ]
  })
}

const renderRiskChart = () => {
  if (!riskChartRef.value) {
    return
  }

  const rows = riskRows.value

  if (rows.length === 0) {
    riskChart?.dispose()
    riskChart = null
    return
  }

  if (!riskChart) {
    riskChart = echarts.init(
      riskChartRef.value
    )
  }

  riskChart.off('click')

  riskChart.on('click', params => {
    const row = rows[params.dataIndex]

    if (!row) {
      return
    }

    const riskMap = {
      '高风险': 'HIGH',
      '中风险': 'MEDIUM',
      '低风险': 'LOW'
    }

    openRiskStudents(
      row,
      riskMap[params.seriesName] || 'HIGH'
    )
  })

  riskChart.setOption({
    tooltip: {
      trigger: 'axis',

      axisPointer: {
        type: 'shadow'
      },

      formatter(params) {
        const row = rows[params[0]?.dataIndex]

        if (!row) {
          return ''
        }

        return [
          `<strong>${row.name}</strong>`,
          `高风险：${row.high} 人（${row.rate.toFixed(0)}%）`,
          `中风险：${row.medium} 人`,
          `低风险：${row.low} 人`,
          `已接触：${row.contact} 人 · 未学：${row.unknown} 人`,
          row.eligible
            ? '<span style="color:#94a3b8">点击柱子查看学生名单</span>'
            : '<span style="color:#b45309">样本不足，不参与优先排序</span>'
        ].join('<br/>')
      }
    },

    legend: {
      top: 0,
      itemWidth: 12,
      itemHeight: 10
    },

    grid: {
      top: 38,
      left: 220,
      right: 28,
      bottom: 24
    },

    xAxis: {
      type: 'value',
      minInterval: 1,
      name: '学生数',

      nameTextStyle: {
        fontSize: 11,
        color: '#64748b'
      },

      axisLabel: {
        fontSize: 11
      },

      splitLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      }
    },

    yAxis: {
      type: 'category',
      inverse: true,
      data: rows.map(row => row.name),

      axisLabel: {
        rich: {
          name: {
            color: '#0f172a',
            fontSize: 12,
            fontWeight: 600,
            lineHeight: 18
          },

          meta: {
            color: '#94a3b8',
            fontSize: 10,
            lineHeight: 14
          },

          warn: {
            color: '#b45309',
            fontSize: 10,
            lineHeight: 14
          }
        },

        formatter(name, index) {
          const row = rows[index]

          if (!row) {
            return name
          }

          const meta = row.eligible
            ? `接触 ${row.contact} · 未学 ${row.unknown} · 高 ${row.rate.toFixed(0)}%`
            : `样本不足 · 接触 ${row.contact}`

          return (
            `{name|${name}}\n`
            + `{${row.eligible ? 'meta' : 'warn'}|${meta}}`
          )
        }
      },

      axisTick: {
        show: false
      }
    },

    series: [
      {
        name: '高风险',
        type: 'bar',
        stack: 'risk',
        barMaxWidth: 18,

        data: rows.map(row => ({
          value: row.high,

          itemStyle: {
            opacity: row.eligible ? 1 : 0.45
          }
        })),

        itemStyle: {
          color: '#ef4444'
        }
      },
      {
        name: '中风险',
        type: 'bar',
        stack: 'risk',
        barMaxWidth: 18,

        data: rows.map(row => ({
          value: row.medium,

          itemStyle: {
            opacity: row.eligible ? 1 : 0.45
          }
        })),

        itemStyle: {
          color: '#f59e0b'
        }
      },
      {
        name: '低风险',
        type: 'bar',
        stack: 'risk',
        barMaxWidth: 18,

        data: rows.map(row => ({
          value: row.low,

          itemStyle: {
            opacity: row.eligible ? 1 : 0.45
          }
        })),

        itemStyle: {
          color: '#22c55e'
        }
      }
    ]
  }, true)

  riskChart.resize()
}

const riskLabel = risk => {
  const labels = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险',
    UNKNOWN: '未学'
  }

  return labels[risk] || risk || '未知'
}

const riskTagType = risk => {
  if (risk === 'HIGH') {
    return 'danger'
  }

  if (risk === 'MEDIUM') {
    return 'warning'
  }

  if (risk === 'LOW') {
    return 'success'
  }

  return 'info'
}

const formatScore = value => {
  if (value === null || value === undefined) {
    return '—'
  }

  return Number(value).toFixed(1)
}

const openRiskStudents = async (row, risk) => {
  riskDetailTitle.value =
    `${row.name} · ${riskLabel(risk)}学生`

  riskDetailVisible.value = true
  riskDetailLoading.value = true
  riskDetailData.value = null
  riskDetailError.value = ''

  try {
    riskDetailData.value =
      await getKnowledgePointStudents(
        row.id,
        risk
      )
  } catch (detailError) {
    console.warn(
      '加载知识点学生名单失败',
      detailError
    )

    riskDetailError.value =
      detailError.message
      || '加载学生名单失败'
  } finally {
    riskDetailLoading.value = false
  }
}

const handleRecalculateAll = async () => {
  try {
    await ElMessageBox.confirm(
      '将对全部启用学生重新计算知识点掌握度，' +
      '新建知识点或调整绑定后，所有学生的最新掌握度将保持一致。是否继续？',
      '全部重算确认',
      {
        confirmButtonText: '开始全部重算',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (cancelError) {
    return
  }

  recalculatingAll.value = true

  try {
    const response =
      await recalculateAllStudents()

    const payload =
      unwrapPayload(response) || {}

    ElMessage.success(
      `全部重算完成：${payload.studentCount || 0} 名学生，` +
      `共 ${payload.recordCount || 0} 条掌握度记录`
    )

    await fetchOverview()
  } catch (requestError) {
    console.error(
      '全部重算失败',
      requestError
    )

    ElMessage.error(
      requestError.message
      || '全部重算失败'
    )
  } finally {
    recalculatingAll.value = false
  }
}












const getScoreColor = score => {
  const value =
    Number(score || 0)

  if (value < 60) {
    return '#ef4444'
  }

  if (value < 75) {
    return '#f59e0b'
  }

  return '#22c55e'
}

const formatNumber = value => {
  return Number(value || 0)
    .toFixed(2)
}


const formatPercent = value => {
  return `${formatNumber(value)}%`
}



const handleResize = () => {
  masteryChart?.resize()
  riskChart?.resize()
}

onMounted(() => {
  window.addEventListener(
    'resize',
    handleResize
  )

  fetchOverview()
})

onBeforeUnmount(() => {
  window.removeEventListener(
    'resize',
    handleResize
  )

  masteryChart?.dispose()
  riskChart?.dispose()

  masteryChart = null
  riskChart = null
})
</script>

<style scoped>
.risk-page {
  min-height: 100vh;
  padding: 24px;
  background: #f1f5f9;
}

.page-container {
  width: 100%;
  max-width: 1500px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.header-actions .el-button + .el-button {
  margin-left: 0;
}

.page-header h1 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
}

.page-header p {
  margin: 9px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.status-panel {
  min-height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  border-radius: 14px;
  background: #ffffff;
  color: #64748b;
}

.error-panel {
  color: #dc2626;
}

.page-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-grid {
  display: grid;
  grid-template-columns:
    repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 128px;
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

.student-icon {
  color: #2563eb;
  background: #dbeafe;
}

.calculated-icon {
  color: #059669;
  background: #d1fae5;
}

.warning-icon {
  color: #dc2626;
  background: #fee2e2;
}

.score-icon {
  color: #7c3aed;
  background: #ede9fe;
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

.chart-grid {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 20px;
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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
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

.chart {
  width: 100%;
  height: 420px;
}

.chart-grid .panel-header {
  min-height: 64px;
}

.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 13px;
}

.risk-detail-summary {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 13px;
}

.risk-student {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.risk-student small {
  color: #94a3b8;
  font-size: 11px;
}

.risk-detail-error {
  margin: 12px 0 0;
  color: #dc2626;
  font-size: 13px;
}


.plan-result-content {
  min-height: 280px;
}

.plan-result-hero {
  margin-bottom: 18px;
  padding: 22px;
  border: 1px solid #bfdbfe;
  border-radius: 14px;
  background:
    linear-gradient(
      135deg,
      #eff6ff 0%,
      #ffffff 60%,
      #ecfdf5 100%
    );
}

.plan-result-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.plan-title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.plan-title-line h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.plan-result-title p {
  margin: 12px 0 0;
  color: #475569;
  line-height: 1.8;
}

.plan-id-box {
  min-width: 110px;
  padding: 13px 17px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.85);
  text-align: center;
}

.plan-id-box span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.plan-id-box strong {
  display: block;
  margin-top: 5px;
  color: #2563eb;
  font-size: 20px;
}

.plan-maintenance-alert {
  margin-bottom: 18px;
}

.plan-descriptions {
  margin-bottom: 22px;
}

.plan-task-section {
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 13px;
  background: #f8fafc;
}

.plan-task-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.plan-task-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 19px;
}

.plan-task-header p {
  margin: 7px 0 0;
  color: #64748b;
  font-size: 13px;
}

.plan-knowledge-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.plan-knowledge-cell span {
  color: #64748b;
  font-family: Consolas, monospace;
  font-size: 12px;
}

.plan-knowledge-cell strong {
  color: #334155;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }

}

@media (max-width: 720px) {
  .risk-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }


  .plan-result-title,
  .plan-task-header {
    flex-direction: column;
  }

  .plan-id-box {
    width: 100%;
    box-sizing: border-box;
  }
}

.trend-panel {
  margin-bottom: 20px;
}

.history-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.trend-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
</style>
