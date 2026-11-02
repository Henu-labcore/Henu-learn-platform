<template>
  <section class="panel admin-profile-section">
    <div class="panel-header profile-header">
      <div>
        <h2>学生画像</h2>

        <p>
          五维画像与标签；教师端展示完整判定文案与依据
        </p>
      </div>

      <div class="profile-tools">
        <el-input
          v-model="keyword"
          clearable
          placeholder="姓名 / 学号"
          class="profile-search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="persona"
          class="profile-persona-select"
          placeholder="画像类型"
          @change="handleSearch"
        >
          <el-option
            label="全部类型"
            value=""
          />

          <el-option
            v-for="item in personaOptions"
            :key="item.code"
            :label="item.label"
            :value="item.code"
          />
        </el-select>

        <el-button
          :loading="exporting"
          @click="handleExport"
        >
          导出 CSV
        </el-button>
      </div>
    </div>

    <div
      v-if="overview.personas.length > 0"
      class="persona-chips"
    >
      <button
        v-for="item in overview.personas"
        :key="item.code"
        type="button"
        class="persona-chip"
        :class="{ active: persona === item.code }"
        @click="handlePersonaChip(item.code)"
      >
        {{ item.label }}
        <strong>{{ item.count }}</strong>
      </button>
    </div>

    <el-table
      v-loading="loading"
      :data="profiles"
      class="profile-table"
      row-key="studentId"
      @row-click="openDetail"
    >
      <el-table-column
        label="学生"
        min-width="150"
      >
        <template #default="{ row }">
          <div class="student-cell">
            <strong>{{ row.displayName || row.username }}</strong>
            <small>{{ row.username }}</small>
          </div>
        </template>
      </el-table-column>

      <el-table-column
        label="画像类型"
        min-width="130"
      >
        <template #default="{ row }">
          <el-tag
            :type="personaTagType(row.persona?.code)"
            effect="light"
          >
            {{ row.persona?.label || '—' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="平均掌握度"
        width="110"
      >
        <template #default="{ row }">
          {{ formatScore(row.averageMasteryScore) }}
        </template>
      </el-table-column>

      <el-table-column
        label="高风险"
        width="90"
      >
        <template #default="{ row }">
          <span :class="{ 'risk-high-text': row.highRiskCount > 0 }">
            {{ row.highRiskCount }}
          </span>
        </template>
      </el-table-column>

      <el-table-column
        label="五维"
        min-width="210"
      >
        <template #default="{ row }">
          <div class="dimension-bars">
            <div
              v-for="dimension in row.dimensions"
              :key="dimension.key"
              class="dimension-bar"
            >
              <span class="bar-label">{{ dimension.label }}</span>

              <span class="bar-track">
                <span
                  class="bar-fill"
                  :style="{ width: `${dimension.score || 0}%` }"
                />
              </span>

              <span class="bar-value">
                {{ formatScore(dimension.score) }}
              </span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column
        label="置信度"
        width="100"
      >
        <template #default="{ row }">
          <span class="confidence-text">
            {{ row.confidence?.label || '—' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column
        label="标签"
        min-width="180"
      >
        <template #default="{ row }">
          <div class="tag-cell">
            <el-tag
              v-for="tag in row.tags"
              :key="tag.code"
              size="small"
              :type="tagType(tag.polarity)"
              effect="plain"
            >
              {{ tag.label }}
            </el-tag>

            <span
              v-if="!row.tags || row.tags.length === 0"
              class="muted-text"
            >
              暂无
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column
        label="操作"
        width="90"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click.stop="openDetail(row)"
          >
            查看画像
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="profile-pagination">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="handlePageChange"
      />
    </div>

    <el-drawer
      v-model="drawerVisible"
      size="560px"
      :title="drawerTitle"
      destroy-on-close
    >
      <div
        v-loading="detailLoading"
        class="profile-drawer"
      >
        <template v-if="detail">
          <div class="drawer-persona">
            <div>
              <span class="drawer-persona-label">
                {{ detail.persona?.label || '数据积累中' }}
              </span>

              <p>{{ detail.persona?.description }}</p>
            </div>

            <el-tag type="info" effect="plain">
              置信度：{{ detail.confidence?.label }}
            </el-tag>
          </div>

          <ProfileRadar
            :dimensions="detail.dimensions || []"
            :height="300"
          />

          <div class="drawer-block">
            <div class="drawer-block-head">
              <h3>画像趋势（近 30 天）</h3>

              <el-button
                link
                type="primary"
                :loading="recalcLoading"
                @click="handleRecalculate"
              >
                重算掌握度
              </el-button>
            </div>

            <ProfileTrendChart
              :dates="detailHistory?.dates || []"
              :series="detailHistory?.series || []"
              :height="260"
            />
          </div>

          <div class="drawer-block">
            <h3>知识点掌握</h3>

            <div
              v-for="row in masteryRows"
              :key="row.knowledgePointId"
              class="kp-row"
            >
              <span class="kp-name">
                {{ row.knowledgePointName }}
              </span>

              <el-tag
                :type="riskTagType(row.riskLevel)"
                size="small"
                effect="light"
              >
                {{ riskText(row.riskLevel) }}
              </el-tag>

              <strong class="kp-score">
                {{ formatScore(row.masteryScore) }}
              </strong>
            </div>

            <p
              v-if="masteryRows.length === 0"
              class="muted-text"
            >
              暂无掌握度记录
            </p>
          </div>

          <div class="drawer-block">
            <h3>维度依据</h3>

            <div
              v-for="dimension in detail.dimensions || []"
              :key="dimension.key"
              class="dimension-row"
            >
              <div class="dimension-row-head">
                <strong>{{ dimension.label }}</strong>
                <span>{{ formatScore(dimension.score) }}</span>
              </div>

              <p>{{ dimension.evidence }}</p>
            </div>
          </div>

          <div class="drawer-block">
            <h3>标签与依据（教师端）</h3>

            <div
              v-for="tag in detail.tags || []"
              :key="tag.code"
              class="drawer-tag-row"
            >
              <el-tag
                :type="tagType(tag.polarity)"
                effect="light"
              >
                {{ tag.label }}
              </el-tag>

              <span>{{ tag.evidence }}</span>
            </div>

            <p
              v-if="!detail.tags || detail.tags.length === 0"
              class="muted-text"
            >
              暂无标签
            </p>
          </div>

          <div class="drawer-block">
            <h3>下一步建议</h3>

            <p class="drawer-next">
              {{ detail.recommendation?.nextAction?.label || '暂无建议' }}
            </p>

            <div
              v-for="point in detail.recommendation?.weakPoints || []"
              :key="point.knowledgePointId"
              class="drawer-weak-row"
            >
              <strong>{{ point.knowledgePointName }}</strong>
              <span>{{ point.masteryScore }} 分</span>
              <em>{{ point.action }}</em>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

import { ElMessage } from 'element-plus'

import { Search } from '@element-plus/icons-vue'

import {
  exportAdminProfiles,
  getAdminProfiles,
  getAdminProfileDetail,
  getAdminProfileHistory,
  getAdminProfileOverview
} from '@/api/studentProfileAdmin'

import {
  getStudentMasteryDetail,
  recalculateStudentMastery
} from '@/api/masteryRisk'

import ProfileRadar from '@/components/profile/ProfileRadar.vue'

import ProfileTrendChart from '@/components/profile/ProfileTrendChart.vue'

const loading = ref(false)
const exporting = ref(false)

const profiles = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 8

const keyword = ref('')
const persona = ref('')

const overview = ref({
  personas: [],
  dimensions: [],
  confidence: []
})

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const detailHistory = ref(null)

const masteryDetail = ref(null)

const recalcLoading = ref(false)

const masteryRows = computed(() => {
  return masteryDetail.value?.data || []
})

const personaOptions = computed(() => {
  const list = overview.value?.personas || []

  if (list.length > 0) {
    return list
  }

  return [
    { code: 'EXCELLENT', label: '全面优秀型' },
    { code: 'IMBALANCED', label: '偏科型' },
    { code: 'NEEDS_SUPPORT', label: '薄弱待补型' },
    { code: 'EXECUTION', label: '执行待改善型' },
    { code: 'IMPROVING', label: '稳步提升型' },
    { code: 'STEADY', label: '稳步积累型' },
    { code: 'SILENT', label: '沉默型' },
    { code: 'INSUFFICIENT', label: '数据积累中' }
  ]
})

const drawerTitle = computed(() => {
  const name = detail.value?.student?.displayName
    || detail.value?.student?.username

  return name ? `${name} · 学生画像` : '学生画像'
})

const fetchProfiles = async () => {
  loading.value = true

  try {
    const payload = await getAdminProfiles({
      keyword: keyword.value.trim(),
      persona: persona.value,
      page: page.value,
      pageSize
    })

    profiles.value = payload?.students || []
    total.value = Number(payload?.total || 0)
    page.value = Number(payload?.page || page.value)
  } catch (error) {
    console.warn('加载学生画像列表失败', error)

    profiles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const fetchOverview = async () => {
  try {
    overview.value = (await getAdminProfileOverview()) || overview.value
  } catch (error) {
    console.warn('加载画像概览失败', error)
  }
}

const handleSearch = () => {
  page.value = 1
  fetchProfiles()
}

const handlePersonaChip = code => {
  persona.value = persona.value === code ? '' : code
  handleSearch()
}

const handlePageChange = value => {
  page.value = value
  fetchProfiles()
}

const loadDetail = async studentId => {
  detailLoading.value = true
  detail.value = null
  detailHistory.value = null
  masteryDetail.value = null

  const [profileResult, historyResult, masteryResult] =
    await Promise.allSettled([
      getAdminProfileDetail(studentId),
      getAdminProfileHistory(studentId, 30),
      getStudentMasteryDetail(studentId)
    ])

  if (profileResult.status === 'fulfilled') {
    detail.value = profileResult.value
  } else {
    console.warn('加载学生画像详情失败', profileResult.reason)
    ElMessage.error('加载学生画像失败')
  }

  detailHistory.value = historyResult.status === 'fulfilled'
    ? historyResult.value
    : null

  masteryDetail.value = masteryResult.status === 'fulfilled'
    ? masteryResult.value
    : null

  detailLoading.value = false
}

const openDetail = async row => {
  if (!row?.studentId) {
    return
  }

  drawerVisible.value = true

  await loadDetail(row.studentId)
}

const handleRecalculate = async () => {
  const studentId = detail.value?.student?.studentId

  if (!studentId || recalcLoading.value) {
    return
  }

  recalcLoading.value = true

  try {
    await recalculateStudentMastery(studentId)

    ElMessage.success('掌握度已重算')

    await Promise.all([
      loadDetail(studentId),
      fetchProfiles(),
      fetchOverview()
    ])
  } catch (error) {
    console.warn('重算掌握度失败', error)
    ElMessage.error('重算失败，请稍后重试')
  } finally {
    recalcLoading.value = false
  }
}

const riskText = risk => {
  const map = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险',
    UNKNOWN: '未学'
  }

  return map[risk] || risk
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

const handleExport = async () => {
  exporting.value = true

  try {
    const blob = await exportAdminProfiles({
      keyword: keyword.value.trim(),
      persona: persona.value
    })

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')

    link.href = url
    link.download = '学生画像.csv'
    link.click()

    URL.revokeObjectURL(url)
    ElMessage.success('画像已导出')
  } catch (error) {
    console.warn('导出学生画像失败', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

const formatScore = value => {
  if (value === null || value === undefined) {
    return '—'
  }

  return Number(value).toFixed(1)
}

const tagType = polarity => {
  if (polarity === 'POSITIVE') {
    return 'success'
  }

  if (polarity === 'ATTENTION') {
    return 'warning'
  }

  return 'info'
}

const personaTagType = code => {
  if (code === 'EXCELLENT' || code === 'IMPROVING') {
    return 'success'
  }

  if (code === 'IMBALANCED' || code === 'NEEDS_SUPPORT' || code === 'EXECUTION') {
    return 'warning'
  }

  return 'info'
}

onMounted(() => {
  fetchOverview()
  fetchProfiles()
})
</script>

<style scoped>
.admin-profile-section {
  margin-bottom: 20px;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.profile-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.profile-search {
  width: 200px;
}

.profile-persona-select {
  width: 170px;
}

.persona-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.persona-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
}

.persona-chip strong {
  color: #1d4ed8;
}

.persona-chip.active {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.profile-table {
  width: 100%;
}

.profile-table :deep(.el-table__row) {
  cursor: pointer;
}

.student-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.student-cell small {
  color: #94a3b8;
  font-size: 12px;
}

.dimension-bars {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dimension-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
}

.bar-label {
  width: 56px;
  flex: none;
  color: #64748b;
}

.bar-track {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.bar-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #60a5fa, #2563eb);
}

.bar-value {
  width: 30px;
  flex: none;
  color: #475569;
  text-align: right;
}

.risk-high-text {
  color: #dc2626;
  font-weight: 600;
}

.confidence-text {
  color: #475569;
  font-size: 12px;
}

.tag-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.muted-text {
  color: #94a3b8;
  font-size: 12px;
}

.profile-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.profile-drawer {
  padding-right: 4px;
}

.drawer-persona {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #eff6ff;
}

.drawer-persona-label {
  color: #1d4ed8;
  font-size: 17px;
  font-weight: 700;
}

.drawer-persona p {
  margin: 6px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.drawer-block {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
}

.drawer-block h3 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 14px;
}

.drawer-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.kp-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #f1f5f9;
  font-size: 12px;
}

.kp-name {
  flex: 1;
  color: #334155;
}

.kp-score {
  width: 46px;
  color: #1d4ed8;
  text-align: right;
}

.dimension-row {
  margin-bottom: 10px;
}

.dimension-row-head {
  display: flex;
  justify-content: space-between;
  color: #475569;
  font-size: 13px;
}

.dimension-row p {
  margin: 3px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.drawer-tag-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.6;
}

.drawer-next {
  margin: 0 0 10px;
  color: #1e40af;
  font-size: 13px;
  font-weight: 600;
}

.drawer-weak-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 8px;
  background: #fff7ed;
  font-size: 12px;
}

.drawer-weak-row span {
  color: #c2410c;
}

.drawer-weak-row em {
  margin-left: auto;
  color: #94a3b8;
  font-style: normal;
}
</style>
