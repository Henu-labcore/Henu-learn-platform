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
                <h2>学生平均掌握度</h2>
                <p>已计算学生按平均掌握度展示</p>
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
                <h2>知识点风险分布</h2>
                <p>全部已计算知识点的风险等级分布</p>
              </div>
            </div>

            <div
              ref="riskChartRef"
              class="chart"
            />
          </article>
        </section>

        <section class="panel">
          <div class="panel-header table-header">
            <div>
              <h2>学生风险列表</h2>

              <p>
                优先展示高风险数量更多、
                平均掌握度更低的学生
              </p>
            </div>

            <div class="filter-bar">
              <el-input
                v-model="keyword"
                clearable
                placeholder="搜索用户名或姓名"
                class="keyword-input"
              >
                <template #prefix>
                  <el-icon>
                    <Search />
                  </el-icon>
                </template>
              </el-input>

              <el-select
                v-model="masteryStatusFilter"
                class="filter-select"
                placeholder="计算状态"
              >
                <el-option
                  label="全部计算状态"
                  value="ALL"
                />

                <el-option
                  label="已计算"
                  value="CALCULATED"
                />

                <el-option
                  label="未计算"
                  value="NOT_CALCULATED"
                />
              </el-select>

              <el-select
                v-model="riskFilter"
                class="filter-select"
                placeholder="最高风险"
              >
                <el-option
                  label="全部风险"
                  value="ALL"
                />

                <el-option
                  label="高风险"
                  value="HIGH"
                />

                <el-option
                  label="中风险"
                  value="MEDIUM"
                />

                <el-option
                  label="低风险"
                  value="LOW"
                />

                <el-option
                  label="未计算"
                  value="NOT_CALCULATED"
                />
              </el-select>
            </div>
          </div>

          <el-table
            v-loading="loading"
            :data="filteredStudents"
            stripe
            border
            style="width: 100%"
            :header-cell-style="tableHeaderStyle"
          >
            <el-table-column
              label="学生"
              min-width="190"
              fixed="left"
            >
              <template #default="{ row }">
                <div class="student-cell">
                  <div class="student-avatar">
                    {{ getAvatarText(row) }}
                  </div>

                  <div>
                    <div class="student-name">
                      {{ getStudentName(row) }}
                    </div>

                    <div class="student-username">
                      @{{ row.username }}
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column
              label="计算状态"
              width="115"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  :type="
                    getMasteryStatusTagType(
                      row.masteryStatus
                    )
                  "
                  effect="light"
                >
                  {{
                    getMasteryStatusText(
                      row.masteryStatus
                    )
                  }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
              label="平均掌握度"
              min-width="165"
            >
              <template #default="{ row }">
                <div
                  v-if="
                    row.masteryStatus
                    === 'CALCULATED'
                  "
                  class="score-cell"
                >
                  <el-progress
                    :percentage="
                      normalizeScore(
                        row.averageMasteryScore
                      )
                    "
                    :stroke-width="10"
                    :color="
                      getScoreColor(
                        row.averageMasteryScore
                      )
                    "
                  />
                </div>

                <span
                  v-else
                  class="muted-text"
                >
                  尚未计算
                </span>
              </template>
            </el-table-column>

            <el-table-column
              label="知识点数"
              prop="knowledgePointCount"
              width="95"
              align="center"
            />

            <el-table-column
              label="高风险"
              width="90"
              align="center"
            >
              <template #default="{ row }">
                <span class="risk-number high-number">
                  {{ row.highRiskCount }}
                </span>
              </template>
            </el-table-column>

            <el-table-column
              label="中风险"
              width="90"
              align="center"
            >
              <template #default="{ row }">
                <span class="risk-number medium-number">
                  {{ row.mediumRiskCount }}
                </span>
              </template>
            </el-table-column>

            <el-table-column
              label="低风险"
              width="90"
              align="center"
            >
              <template #default="{ row }">
                <span class="risk-number low-number">
                  {{ row.lowRiskCount }}
                </span>
              </template>
            </el-table-column>

            <el-table-column
              label="最高风险"
              width="110"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  :type="
                    getRiskTagType(
                      row.highestRiskLevel
                    )
                  "
                  effect="dark"
                >
                  {{
                    getRiskText(
                      row.highestRiskLevel
                    )
                  }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
              label="首次改密"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  :type="
                    row.mustChangePassword
                      ? 'warning'
                      : 'success'
                  "
                  effect="plain"
                >
                  {{
                    row.mustChangePassword
                      ? '待完成'
                      : '已完成'
                  }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column
              label="最近计算时间"
              min-width="170"
            >
              <template #default="{ row }">
                {{
                  formatDateTime(
                    row.lastCalculatedAt
                  )
                }}
              </template>
            </el-table-column>

            <el-table-column
              label="操作"
              width="285"
              fixed="right"
              align="center"
            >
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  :disabled="
                    row.masteryStatus
                    !== 'CALCULATED'
                  "
                  @click="openDetail(row)"
                >
                  查看详情
                </el-button>

                <el-button
                  type="success"
                  link
                  :loading="
                    generatingStudentId
                    === row.studentId
                  "
                  :disabled="
                    row.masteryStatus
                    !== 'CALCULATED'
                  "
                  @click="handleGeneratePlan(row)"
                >
                  生成计划
                </el-button>

                <el-button
                  type="warning"
                  link
                  :loading="
                    recalculatingStudentId
                    === row.studentId
                  "
                  @click="handleRecalculate(row)"
                >
                  重新计算
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty
            v-if="
              !loading
              && filteredStudents.length === 0
            "
            description="没有符合筛选条件的学生"
          />
        </section>
      </div>
    </div>

    <el-dialog
      v-model="detailVisible"
      width="88%"
      top="5vh"
      destroy-on-close
      title="学生知识点掌握度详情"
    >
      <div
        v-loading="detailLoading"
        class="detail-content"
      >
        <template v-if="detailData">
          <section class="detail-student">
            <div>
              <h3>
                {{
                  getStudentName(
                    detailData.student
                  )
                }}
              </h3>

              <p>
                用户名：
                {{ detailData.student.username }}
              </p>
            </div>

            <div class="detail-summary">
              <el-statistic
                title="平均掌握度"
                :value="
                  detailData.summary
                    .averageMasteryScore
                "
                :precision="2"
                suffix="%"
              />

              <el-statistic
                title="高风险"
                :value="
                  detailData.summary.highRiskCount
                "
              />

              <el-statistic
                title="中风险"
                :value="
                  detailData.summary.mediumRiskCount
                "
              />

              <el-statistic
                title="低风险"
                :value="
                  detailData.summary.lowRiskCount
                "
              />
            </div>
          </section>

          <el-alert
            v-if="
              detailWeakKnowledgePointNames.length > 0
            "
            type="warning"
            :closable="false"
            show-icon
            class="weak-alert"
          >
            <template #title>
              薄弱知识点：
              {{
                detailWeakKnowledgePointNames
                  .join('、')
              }}
            </template>
          </el-alert>

          <el-table
            :data="detailData.data"
            stripe
            border
            max-height="470"
          >
            <el-table-column
              prop="code"
              label="知识点编码"
              width="120"
            />

            <el-table-column
              prop="knowledgePointName"
              label="知识点名称"
              min-width="180"
            />

            <el-table-column
              label="学习得分"
              width="115"
              align="center"
            >
              <template #default="{ row }">
                {{
                  formatPoints(
                    row.learningScore
                  )
                }}
              </template>
            </el-table-column>

            <el-table-column
              label="考核得分"
              width="115"
              align="center"
            >
              <template #default="{ row }">
                {{
                  formatPoints(
                    row.assessmentScore
                  )
                }}
              </template>
            </el-table-column>

            <el-table-column
              label="综合掌握度"
              width="125"
              align="center"
            >
              <template #default="{ row }">
                <strong>
                  {{
                    formatPercent(
                      row.masteryScore
                    )
                  }}
                </strong>
              </template>
            </el-table-column>

            <el-table-column
              label="预警阈值"
              width="115"
              align="center"
            >
              <template #default="{ row }">
                {{
                  formatPercent(
                    row.warningThreshold
                  )
                }}
              </template>
            </el-table-column>

            <el-table-column
              label="风险等级"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  :type="
                    getRiskTagType(
                      row.riskLevel
                    )
                  "
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
              label="学习完成"
              width="105"
              align="center"
            >
              <template #default="{ row }">
                {{ row.completedLearningCount }}
                /
                {{ row.totalLearningCount }}
              </template>
            </el-table-column>

            <el-table-column
              label="已批改考核"
              width="115"
              align="center"
            >
              <template #default="{ row }">
                {{ row.gradedAssignmentCount }}
                /
                {{ row.totalAssignmentCount }}
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>

      <template #footer>
        <el-button
          @click="detailVisible = false"
        >
          关闭
        </el-button>

        <el-button
          type="success"
          :loading="
            generatingStudentId
            === detailStudentId
          "
          :disabled="!detailStudentId"
          @click="
            handleGeneratePlan(
              detailData?.student
            )
          "
        >
          生成个性化学习计划
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="planResultVisible"
      width="82%"
      top="5vh"
      destroy-on-close
      title="个性化学习计划生成结果"
    >
      <div
        v-loading="planResultLoading"
        class="plan-result-content"
      >
        <template v-if="planResult">
          <section class="plan-result-hero">
            <div class="plan-result-title">
              <div>
                <div class="plan-title-line">
                  <h2>
                    {{
                      planResult.title
                      || '个性化学习计划'
                    }}
                  </h2>

                  <el-tag
                    :type="
                      getPlanStatusType(
                        planResult.status
                      )
                    "
                    effect="dark"
                  >
                    {{
                      getPlanStatusText(
                        planResult.status
                      )
                    }}
                  </el-tag>
                </div>

                <p>
                  {{
                    planResult.description
                    || '暂无计划说明'
                  }}
                </p>
              </div>

              <div class="plan-id-box">
                <span>计划编号</span>
                <strong>
                  #{{ planResult.planId }}
                </strong>
              </div>
            </div>
          </section>

          <el-alert
            v-if="planResult.maintenanceMode"
            type="warning"
            show-icon
            :closable="false"
            class="plan-maintenance-alert"
            title="当前计划处于维护模式，请检查计划任务和资源绑定情况。"
          />

          <el-descriptions
            :column="3"
            border
            class="plan-descriptions"
          >
            <el-descriptions-item label="学生">
              {{
                getPlanStudentName(
                  planResult
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="用户名">
              {{
                planResult.username
                || planResult.student?.username
                || '暂无'
              }}
            </el-descriptions-item>

            <el-descriptions-item label="生成方式">
              {{
                getGenerationTypeText(
                  planResult.generationType
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="目标知识点">
              {{
                getPlanTargetCount(
                  planResult
                )
              }}
              个
            </el-descriptions-item>

            <el-descriptions-item label="计划任务">
              {{ planTaskCount }}
              项
            </el-descriptions-item>

            <el-descriptions-item label="创建管理员">
              {{
                planResult.creatorUsername
                || planResult.createdByUsername
                || planResult.createdBy
                || '系统'
              }}
            </el-descriptions-item>

            <el-descriptions-item label="开始日期">
              {{
                formatDate(
                  planResult.startDate
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="目标日期">
              {{
                formatDate(
                  planResult.targetDate
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="生成时间">
              {{
                formatDateTime(
                  planResult.createdAt
                )
              }}
            </el-descriptions-item>
          </el-descriptions>

          <section class="plan-task-section">
            <div class="plan-task-header">
              <div>
                <h3>计划任务</h3>

                <p>
                  系统根据风险等级、知识点前置关系
                  和资源绑定关系自动安排任务顺序。
                </p>
              </div>

              <el-tag
                type="info"
                effect="plain"
              >
                共 {{ planTaskCount }} 项
              </el-tag>
            </div>

            <el-table
              :data="planResultTasks"
              stripe
              border
              max-height="440"
              empty-text="当前计划没有生成任务"
            >
              <el-table-column
                label="顺序"
                width="75"
                align="center"
              >
                <template #default="{ row, $index }">
                  {{
                    row.sortOrder
                    || $index + 1
                  }}
                </template>
              </el-table-column>

              <el-table-column
                label="任务类型"
                width="115"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="
                      getTaskTypeTagType(
                        row.taskType
                      )
                    "
                    effect="plain"
                  >
                    {{
                      getTaskTypeText(
                        row.taskType
                      )
                    }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column
                label="目标知识点"
                min-width="190"
              >
                <template #default="{ row }">
                  <div class="plan-knowledge-cell">
                    <span>
                      {{
                        row.knowledgePointCode
                        || row.code
                        || '暂无编码'
                      }}
                    </span>

                    <strong>
                      {{
                        row.knowledgePointName
                        || row.name
                        || '暂无名称'
                      }}
                    </strong>
                  </div>
                </template>
              </el-table-column>

              <el-table-column
                label="任务标题"
                prop="title"
                min-width="260"
              />

              <el-table-column
                label="优先级"
                width="105"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="
                      getPriorityTagType(
                        row.priority
                      )
                    "
                  >
                    {{
                      getPriorityText(
                        row.priority
                      )
                    }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column
                label="任务状态"
                width="105"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="
                      getTaskStatusTagType(
                        row.status
                      )
                    "
                    effect="dark"
                  >
                    {{
                      getTaskStatusText(
                        row.status
                      )
                    }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column
                label="截止时间"
                min-width="170"
              >
                <template #default="{ row }">
                  {{
                    formatDateTime(
                      row.dueAt
                    )
                  }}
                </template>
              </el-table-column>
            </el-table>
          </section>
        </template>
      </div>

      <template #footer>
        <el-button
          @click="planResultVisible = false"
        >
          关闭
        </el-button>

        <el-button
          type="primary"
          :loading="planResultLoading"
          :disabled="!planResult?.planId"
          @click="refreshPlanResult"
        >
          刷新计划详情
        </el-button>
      </template>
    </el-dialog>
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
  Search,
  TrendCharts,
  User,
  Warning,
  WarningFilled
} from '@element-plus/icons-vue'

import {
  getMasteryStudentOverview,
  getStudentMasteryDetail,
  recalculateStudentMastery
} from '@/api/masteryRisk'

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

const keyword = ref('')
const masteryStatusFilter = ref('ALL')
const riskFilter = ref('ALL')

const recalculatingStudentId = ref(null)
const generatingStudentId = ref(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null)

const planResultVisible = ref(false)
const planResultLoading = ref(false)
const planResult = ref(null)

const masteryChartRef = ref(null)
const riskChartRef = ref(null)

let masteryChart = null
let riskChart = null

const tableHeaderStyle = {
  background: '#f8fafc',
  color: '#334155',
  fontWeight: '600'
}

const filteredStudents = computed(() => {
  const normalizedKeyword =
    keyword.value.trim().toLowerCase()

  return students.value.filter(student => {
    const matchesKeyword =
      !normalizedKeyword
      || String(student.username || '')
        .toLowerCase()
        .includes(normalizedKeyword)
      || String(student.displayName || '')
        .toLowerCase()
        .includes(normalizedKeyword)
      || String(student.realName || '')
        .toLowerCase()
        .includes(normalizedKeyword)

    const matchesMasteryStatus =
      masteryStatusFilter.value === 'ALL'
      || student.masteryStatus
        === masteryStatusFilter.value

    const matchesRisk =
      riskFilter.value === 'ALL'
      || student.highestRiskLevel
        === riskFilter.value

    return matchesKeyword
      && matchesMasteryStatus
      && matchesRisk
  })
})

const detailWeakKnowledgePointNames =
  computed(() => {
    const names =
      detailData.value
        ?.summary
        ?.weakKnowledgePointNames

    return Array.isArray(names)
      ? names
      : []
  })

const detailStudentId = computed(() => {
  return detailData.value
    ?.student
    ?.studentId
    || detailData.value
      ?.student
      ?.userId
    || detailData.value
      ?.student
      ?.id
    || null
})

const planResultTasks = computed(() => {
  return Array.isArray(
    planResult.value?.tasks
  )
    ? planResult.value.tasks
    : []
})

const planTaskCount = computed(() => {
  const explicitCount =
    Number(
      planResult.value?.taskCount
    )

  if (
    Number.isFinite(explicitCount)
    && explicitCount > 0
  ) {
    return explicitCount
  }

  return planResultTasks.value.length
})

const unwrapPayload = response => {
  return response
}

const normalizePlanResponse = response => {
  const payload =
    unwrapPayload(response) || {}

  if (
    payload.plan
    && typeof payload.plan === 'object'
  ) {
    const tasks =
      Array.isArray(payload.tasks)
        ? payload.tasks
        : (
          Array.isArray(payload.plan.tasks)
            ? payload.plan.tasks
            : []
        )

    const {
      plan,
      ...payloadWithoutPlan
    } = payload

    return {
      ...payloadWithoutPlan,
      ...plan,
      student:
        payload.student
        || plan.student,
      tasks,
      targetKnowledgePointCount:
        payload.targetKnowledgePointCount
        ?? plan.targetKnowledgePointCount,
      taskCount:
        payload.taskCount
        ?? plan.taskCount
        ?? tasks.length,
      maintenanceMode:
        payload.maintenanceMode
        ?? plan.maintenanceMode
        ?? false
    }
  }

  const tasks =
    Array.isArray(payload.tasks)
      ? payload.tasks
      : []

  return {
    ...payload,
    tasks,
    taskCount:
      payload.taskCount
      ?? tasks.length
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

    await nextTick()
    renderCharts()
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

  const calculatedStudents =
    students.value.filter(
      student =>
        student.masteryStatus
        === 'CALCULATED'
    )

  masteryChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter(params) {
        const item = params[0]

        return `${item.name}<br/>
          平均掌握度：${formatPercent(
            item.value
          )}`
      }
    },
    grid: {
      top: 25,
      right: 25,
      bottom: 45,
      left: 60
    },
    xAxis: {
      type: 'category',
      data: calculatedStudents.map(
        student =>
          getStudentName(student)
      ),
      axisLabel: {
        interval: 0,
        rotate:
          calculatedStudents.length > 5
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
        data: calculatedStudents.map(
          student => ({
            value: Number(
              student.averageMasteryScore
              || 0
            ),
            itemStyle: {
              color: getScoreColor(
                student.averageMasteryScore
              ),
              borderRadius: [6, 6, 0, 0]
            }
          })
        ),
        label: {
          show: true,
          position: 'top',
          formatter(params) {
            return formatPercent(params.value)
          }
        }
      }
    ]
  })
}

const renderRiskChart = () => {
  if (!riskChartRef.value) {
    return
  }

  if (!riskChart) {
    riskChart = echarts.init(
      riskChartRef.value
    )
  }

  riskChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter:
        '{b}：{c} 项（{d}%）'
    },
    legend: {
      bottom: 5,
      left: 'center'
    },
    series: [
      {
        name: '风险分布',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: {
          formatter: '{b}\n{c} 项'
        },
        data: [
          {
            name: '高风险',
            value:
              summary.value
                .totalHighRiskCount,
            itemStyle: {
              color: '#ef4444'
            }
          },
          {
            name: '中风险',
            value:
              summary.value
                .totalMediumRiskCount,
            itemStyle: {
              color: '#f59e0b'
            }
          },
          {
            name: '低风险',
            value:
              summary.value
                .totalLowRiskCount,
            itemStyle: {
              color: '#22c55e'
            }
          },
          {
            name: '未知风险',
            value:
              summary.value
                .totalUnknownRiskCount,
            itemStyle: {
              color: '#94a3b8'
            }
          }
        ]
      }
    ]
  })
}

const openDetail = async student => {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null

  try {
    const response =
      await getStudentMasteryDetail(
        student.studentId
      )

    detailData.value =
      unwrapPayload(response)
  } catch (requestError) {
    console.error(
      '加载学生掌握度详情失败',
      requestError
    )

    ElMessage.error(
      requestError.message
      || '加载学生掌握度详情失败'
    )

    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const handleRecalculate = async student => {
  try {
    await ElMessageBox.confirm(
      `确定重新计算“${getStudentName(student)}”的知识点掌握度吗？`,
      '重新计算确认',
      {
        confirmButtonText: '确定计算',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (cancelError) {
    return
  }

  recalculatingStudentId.value =
    student.studentId

  try {
    const response =
      await recalculateStudentMastery(
        student.studentId
      )

    const payload =
      unwrapPayload(response) || {}

    ElMessage.success(
      `重新计算成功，共生成 ${payload.count || 0} 条掌握度记录`
    )

    await fetchOverview()
  } catch (requestError) {
    console.error(
      '重新计算掌握度失败',
      requestError
    )

    ElMessage.error(
      requestError.message
      || '重新计算掌握度失败'
    )
  } finally {
    recalculatingStudentId.value = null
  }
}

const handleGeneratePlan = async student => {
  ElMessage.info(
    '个性化学习计划模块将在后续版本提供，当前版本聚焦掌握度计算与薄弱知识点识别'
  )
}

const refreshPlanResult = async () => {
  ElMessage.info(
    '个性化学习计划模块将在后续版本提供'
  )
}

const getStudentName = student => {
  if (!student) {
    return '未知学生'
  }

  return student.displayName
    || student.realName
    || student.username
    || '未知学生'
}

const getPlanStudentName = plan => {
  if (!plan) {
    return '未知学生'
  }

  return plan.displayName
    || plan.realName
    || plan.student?.displayName
    || plan.student?.realName
    || plan.username
    || plan.student?.username
    || '未知学生'
}

const getPlanTargetCount = plan => {
  const explicitCount =
    Number(
      plan?.targetKnowledgePointCount
    )

  if (
    Number.isFinite(explicitCount)
    && explicitCount > 0
  ) {
    return explicitCount
  }

  const tasks =
    Array.isArray(plan?.tasks)
      ? plan.tasks
      : []

  const knowledgePointKeys =
    new Set()

  tasks.forEach(task => {
    const key =
      task.knowledgePointId
      ?? task.knowledgePointCode
      ?? task.code
      ?? task.knowledgePointName
      ?? task.name
      ?? null

    if (
      key !== null
      && key !== undefined
      && String(key).trim() !== ''
    ) {
      knowledgePointKeys.add(
        String(key)
      )
    }
  })

  return knowledgePointKeys.size
}

const getAvatarText = student => {
  const name =
    getStudentName(student)

  return name
    .charAt(0)
    .toUpperCase()
}

const getMasteryStatusText = status => {
  return status === 'CALCULATED'
    ? '已计算'
    : '未计算'
}

const getMasteryStatusTagType = status => {
  return status === 'CALCULATED'
    ? 'success'
    : 'info'
}

const getRiskText = riskLevel => {
  const textMap = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险',
    UNKNOWN: '未知',
    NOT_CALCULATED: '未计算'
  }

  return textMap[riskLevel]
    || '未知'
}

const getRiskTagType = riskLevel => {
  const typeMap = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success',
    UNKNOWN: 'info',
    NOT_CALCULATED: 'info'
  }

  return typeMap[riskLevel]
    || 'info'
}

const getPlanStatusText = status => {
  const textMap = {
    DRAFT: '草稿',
    ACTIVE: '进行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }

  return textMap[status]
    || status
    || '未知'
}

const getPlanStatusType = status => {
  const typeMap = {
    DRAFT: 'info',
    ACTIVE: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }

  return typeMap[status]
    || 'info'
}

const getGenerationTypeText = type => {
  const textMap = {
    AUTO: '系统自动生成',
    MANUAL: '管理员手动创建'
  }

  return textMap[type]
    || type
    || '未知'
}

const getTaskTypeText = type => {
  const textMap = {
    LEARNING_ITEM: '学习资源',
    ASSIGNMENT: '能力考核',
    CUSTOM: '自定义任务'
  }

  return textMap[type]
    || type
    || '未知任务'
}

const getTaskTypeTagType = type => {
  const typeMap = {
    LEARNING_ITEM: 'primary',
    ASSIGNMENT: 'warning',
    CUSTOM: 'info'
  }

  return typeMap[type]
    || 'info'
}

const getPriorityText = priority => {
  const textMap = {
    HIGH: '高优先级',
    MEDIUM: '中优先级',
    LOW: '低优先级'
  }

  return textMap[priority]
    || priority
    || '未设置'
}

const getPriorityTagType = priority => {
  const typeMap = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success'
  }

  return typeMap[priority]
    || 'info'
}

const getTaskStatusText = status => {
  const textMap = {
    PENDING: '待完成',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成',
    SKIPPED: '已跳过'
  }

  return textMap[status]
    || status
    || '未知'
}

const getTaskStatusTagType = status => {
  const typeMap = {
    PENDING: 'info',
    IN_PROGRESS: 'warning',
    COMPLETED: 'success',
    SKIPPED: 'info'
  }

  return typeMap[status]
    || 'info'
}

const normalizeScore = score => {
  const value =
    Number(score || 0)

  return Math.min(
    100,
    Math.max(0, value)
  )
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

const formatPoints = score => {
  return `${formatNumber(score)} 分`
}

const formatPercent = value => {
  return `${formatNumber(value)}%`
}

const formatDate = value => {
  if (!value) {
    return '暂无'
  }

  return String(value)
    .slice(0, 10)
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
  height: 350px;
}

.table-header {
  align-items: center;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.keyword-input {
  width: 230px;
}

.filter-select {
  width: 145px;
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.student-avatar {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 700;
}

.student-name {
  color: #0f172a;
  font-weight: 600;
}

.student-username {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 12px;
}

.score-cell {
  width: 100%;
}

.risk-number {
  font-weight: 700;
}

.high-number {
  color: #dc2626;
}

.medium-number {
  color: #d97706;
}

.low-number {
  color: #16a34a;
}

.muted-text {
  color: #94a3b8;
}

.detail-content {
  min-height: 240px;
}

.detail-student {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 25px;
  margin-bottom: 18px;
  padding: 18px;
  border-radius: 12px;
  background: #f8fafc;
}

.detail-student h3 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.detail-student p {
  margin: 7px 0 0;
  color: #64748b;
}

.detail-summary {
  display: grid;
  grid-template-columns:
    repeat(4, minmax(100px, 1fr));
  gap: 25px;
}

.weak-alert {
  margin-bottom: 18px;
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

  .table-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-bar {
    justify-content: flex-start;
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

  .filter-bar {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
  }

  .keyword-input,
  .filter-select {
    width: 100%;
  }

  .detail-student {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-summary {
    width: 100%;
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
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
</style>