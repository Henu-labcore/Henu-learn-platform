<template>
  <div class="knowledge-management-page">
    <div class="page-shell">
      <section class="hero-card">
        <div>
          <h1>知识点管理</h1>

          <p>
            维护知识点基础信息、预警阈值、前置关系，以及学习资源和能力考核绑定。
          </p>
        </div>

        <div class="hero-actions">
          <el-button
            plain
            :loading="loading"
            @click="loadPage"
          >
            刷新数据
          </el-button>

          <el-button
            type="primary"
            @click="openCreateEditor"
          >
            新建知识点
          </el-button>
        </div>
      </section>

      <section class="stats-grid">
        <article class="stat-card">
          <span>知识点总数</span>
          <strong>{{ stats.totalCount }}</strong>
        </article>

        <article class="stat-card">
          <span>启用知识点</span>
          <strong>{{ stats.activeCount }}</strong>
        </article>

        <article class="stat-card">
          <span>停用知识点</span>
          <strong>{{ stats.inactiveCount }}</strong>
        </article>

        <article class="stat-card">
          <span>已绑定学习资源</span>
          <strong>
            {{ stats.boundLearningItemCount }}
          </strong>
        </article>

        <article class="stat-card">
          <span>已绑定能力考核</span>
          <strong>
            {{ stats.boundAssignmentCount }}
          </strong>
        </article>

        <article class="stat-card">
          <span>已配置前置关系</span>
          <strong>
            {{ stats.prerequisiteConfiguredCount }}
          </strong>
        </article>
      </section>

      <section class="panel">
        <div class="panel-toolbar">
          <div class="filter-area">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索知识点编码、名称或说明"
              class="keyword-input"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />

            <el-select
              v-model="filters.status"
              clearable
              placeholder="全部状态"
              class="filter-select"
              @change="handleSearch"
            >
              <el-option
                label="启用"
                value="ACTIVE"
              />

              <el-option
                label="停用"
                value="INACTIVE"
              />
            </el-select>

            <el-input
              v-model="filters.category"
              clearable
              placeholder="知识点分类"
              class="category-input"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />

            <el-button
              type="primary"
              @click="handleSearch"
            >
              查询
            </el-button>

            <el-button @click="resetFilters">
              重置
            </el-button>
          </div>

          <div class="toolbar-actions">
            <el-button
              type="primary"
              @click="openCreateEditor"
            >
              新建知识点
            </el-button>

            <el-button
              plain
              :loading="loading"
              @click="loadPage"
            >
              刷新
            </el-button>
          </div>
        </div>

        <div class="result-summary">
          当前共查询到
          <strong>{{ pagination.total }}</strong>
          个知识点
        </div>

        <el-table
          v-loading="loading"
          :data="knowledgePoints"
          stripe
          border
          empty-text="没有符合条件的知识点"
          style="width: 100%"
        >
          <el-table-column
            prop="code"
            label="知识点编码"
            width="135"
            fixed="left"
          />

          <el-table-column
            prop="name"
            label="知识点名称"
            min-width="190"
            show-overflow-tooltip
          />

          <el-table-column
            prop="category"
            label="分类"
            min-width="130"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.category || '-' }}
            </template>
          </el-table-column>

          <el-table-column
            label="难度"
            width="105"
            align="center"
          >
            <template #default="{ row }">
              <el-tag
                :type="
                  getDifficultyTagType(
                    row.difficulty
                  )
                "
                effect="plain"
              >
                {{
                  getDifficultyText(
                    row.difficulty
                  )
                }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            label="预警阈值"
            width="105"
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
            label="前置知识点"
            width="105"
            align="center"
          >
            <template #default="{ row }">
              {{ row.prerequisiteCount || 0 }}
            </template>
          </el-table-column>

          <el-table-column
            label="学习资源"
            width="95"
            align="center"
          >
            <template #default="{ row }">
              {{ row.learningItemCount || 0 }}
            </template>
          </el-table-column>

          <el-table-column
            label="能力考核"
            width="95"
            align="center"
          >
            <template #default="{ row }">
              {{ row.assignmentCount || 0 }}
            </template>
          </el-table-column>

          <el-table-column
            label="排序"
            width="80"
            align="center"
          >
            <template #default="{ row }">
              {{ row.sortOrder || 0 }}
            </template>
          </el-table-column>

          <el-table-column
            label="状态"
            width="95"
            align="center"
          >
            <template #default="{ row }">
              <el-tag
                :type="
                  row.status === 'ACTIVE'
                    ? 'success'
                    : 'info'
                "
              >
                {{
                  row.status === 'ACTIVE'
                    ? '启用'
                    : '停用'
                }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            label="更新时间"
            width="165"
            align="center"
          >
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>

          <el-table-column
            label="操作"
            width="260"
            fixed="right"
            align="center"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                @click="openDetail(row)"
              >
                查看详情
              </el-button>

              <el-button
                type="primary"
                link
                @click="openEditEditor(row)"
              >
                编辑
              </el-button>

              <el-button
                v-if="row.status === 'ACTIVE'"
                type="warning"
                link
                :loading="
                  changingStatusId
                  === row.knowledgePointId
                "
                @click="
                  changeStatus(
                    row,
                    'INACTIVE'
                  )
                "
              >
                停用
              </el-button>

              <el-button
                v-else
                type="success"
                link
                :loading="
                  changingStatusId
                  === row.knowledgePointId
                "
                @click="
                  changeStatus(
                    row,
                    'ACTIVE'
                  )
                "
              >
                启用
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          class="pagination"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList"
          @size-change="handlePageSizeChange"
        />
      </section>
    </div>

    <!-- 知识点详情 -->
    <el-dialog
      v-model="detailVisible"
      title="知识点详情"
      width="900px"
      top="5vh"
      destroy-on-close
    >
      <div
        v-loading="detailLoading"
        class="detail-content"
      >
        <template v-if="detail">
          <el-descriptions
            :column="3"
            border
          >
            <el-descriptions-item label="知识点编码">
              {{ detail.code }}
            </el-descriptions-item>

            <el-descriptions-item label="知识点名称">
              {{ detail.name }}
            </el-descriptions-item>

            <el-descriptions-item label="状态">
              <el-tag
                :type="
                  detail.status === 'ACTIVE'
                    ? 'success'
                    : 'info'
                "
              >
                {{
                  detail.status === 'ACTIVE'
                    ? '启用'
                    : '停用'
                }}
              </el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="分类">
              {{ detail.category || '-' }}
            </el-descriptions-item>

            <el-descriptions-item label="难度">
              {{
                getDifficultyText(
                  detail.difficulty
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="预警阈值">
              {{
                formatPercent(
                  detail.warningThreshold
                )
              }}
            </el-descriptions-item>

            <el-descriptions-item label="显示顺序">
              {{ detail.sortOrder || 0 }}
            </el-descriptions-item>

            <el-descriptions-item label="创建时间">
              {{ formatDateTime(detail.createdAt) }}
            </el-descriptions-item>

            <el-descriptions-item label="更新时间">
              {{ formatDateTime(detail.updatedAt) }}
            </el-descriptions-item>

            <el-descriptions-item
              label="知识点说明"
              :span="3"
            >
              {{ detail.description || '暂无说明' }}
            </el-descriptions-item>
          </el-descriptions>

          <section class="detail-section">
            <div class="detail-section-header">
              <h3>前置知识点</h3>

              <el-tag type="info">
                {{
                  detail.prerequisites?.length
                  || 0
                }}
                项
              </el-tag>
            </div>

            <el-table
              :data="detail.prerequisites || []"
              size="small"
              border
              empty-text="未配置前置知识点"
            >
              <el-table-column
                prop="code"
                label="编码"
                width="135"
              />

              <el-table-column
                prop="name"
                label="名称"
                min-width="180"
              />

              <el-table-column
                prop="category"
                label="分类"
                min-width="130"
              />

              <el-table-column
                label="状态"
                width="90"
                align="center"
              >
                <template #default="{ row }">
                  {{
                    row.status === 'ACTIVE'
                      ? '启用'
                      : '停用'
                  }}
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section class="detail-section">
            <div class="detail-section-header">
              <h3>学习资源绑定</h3>

              <el-tag type="primary">
                {{
                  detail
                    .learningItemBindings
                    ?.length
                  || 0
                }}
                项
              </el-tag>
            </div>

            <el-table
              :data="
                detail.learningItemBindings
                || []
              "
              size="small"
              border
              empty-text="未绑定学习资源"
            >
              <el-table-column
                prop="itemPk"
                label="资源ID"
                width="90"
              />

              <el-table-column
                prop="title"
                label="学习资源"
                min-width="220"
              />

              <el-table-column
                prop="category"
                label="分类"
                min-width="130"
              />

              <el-table-column
                label="权重"
                width="90"
                align="center"
              >
                <template #default="{ row }">
                  {{ formatWeight(row.weight) }}
                </template>
              </el-table-column>

              <el-table-column
                label="状态"
                width="105"
                align="center"
              >
                <template #default="{ row }">
                  {{
                    getResourceStatusText(
                      row.status
                    )
                  }}
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section class="detail-section">
            <div class="detail-section-header">
              <h3>能力考核绑定</h3>

              <el-tag type="warning">
                {{
                  detail.assignmentBindings
                    ?.length
                  || 0
                }}
                项
              </el-tag>
            </div>

            <el-table
              :data="
                detail.assignmentBindings
                || []
              "
              size="small"
              border
              empty-text="未绑定能力考核"
            >
              <el-table-column
                prop="assignmentId"
                label="考核ID"
                width="90"
              />

              <el-table-column
                prop="title"
                label="能力考核"
                min-width="220"
              />

              <el-table-column
                prop="category"
                label="分类"
                min-width="130"
              />

              <el-table-column
                label="满分"
                width="100"
                align="center"
              >
                <template #default="{ row }">
                  {{ formatScore(row.totalScore) }}
                </template>
              </el-table-column>

              <el-table-column
                label="权重"
                width="90"
                align="center"
              >
                <template #default="{ row }">
                  {{ formatWeight(row.weight) }}
                </template>
              </el-table-column>

              <el-table-column
                label="状态"
                width="105"
                align="center"
              >
                <template #default="{ row }">
                  {{
                    getResourceStatusText(
                      row.status
                    )
                  }}
                </template>
              </el-table-column>
            </el-table>
          </section>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>

        <el-button
          type="primary"
          :disabled="!detail"
          @click="editCurrentDetail"
        >
          编辑知识点
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增、编辑知识点 -->
    <el-dialog
      v-model="editorVisible"
      :title="
        editorForm.knowledgePointId
          ? '编辑知识点'
          : '新建知识点'
      "
      width="1000px"
      top="3vh"
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
    >
      <div
        v-loading="editorLoading"
        class="editor-content"
      >
        <el-form
          label-position="top"
          class="knowledge-form"
        >
          <section class="editor-section first-section">
            <div class="editor-section-title">
              <div>
                <h3>基础信息</h3>

                <p>
                  设置知识点编码、名称、分类、难度和预警阈值。
                </p>
              </div>
            </div>

            <div class="form-grid">
              <el-form-item
                label="知识点编码"
                required
              >
                <el-input
                  v-model="editorForm.code"
                  maxlength="64"
                  placeholder="例如：KP-AI-006"
                />
              </el-form-item>

              <el-form-item
                label="知识点名称"
                required
              >
                <el-input
                  v-model="editorForm.name"
                  maxlength="120"
                  placeholder="请输入知识点名称"
                />
              </el-form-item>

              <el-form-item label="知识点分类">
                <el-input
                  v-model="editorForm.category"
                  maxlength="100"
                  placeholder="例如：智能体基础"
                />
              </el-form-item>

              <el-form-item
                label="知识点难度"
                required
              >
                <el-select
                  v-model="editorForm.difficulty"
                  style="width: 100%"
                >
                  <el-option
                    label="基础"
                    value="BASIC"
                  />

                  <el-option
                    label="中级"
                    value="INTERMEDIATE"
                  />

                  <el-option
                    label="高级"
                    value="ADVANCED"
                  />
                </el-select>
              </el-form-item>

              <el-form-item
                label="预警阈值"
                required
              >
                <el-input-number
                  v-model="
                    editorForm.warningThreshold
                  "
                  :min="0"
                  :max="100"
                  :precision="2"
                  :step="1"
                  style="width: 100%"
                />

                <div class="form-tip">
                  综合掌握度低于该百分比时判定为高风险。
                </div>
              </el-form-item>

              <el-form-item
                label="显示顺序"
                required
              >
                <el-input-number
                  v-model="editorForm.sortOrder"
                  :min="0"
                  :max="99999"
                  :step="1"
                  step-strictly
                  style="width: 100%"
                />
              </el-form-item>

              <el-form-item
                label="知识点状态"
                required
              >
                <el-radio-group
                  v-model="editorForm.status"
                >
                  <el-radio value="ACTIVE">
                    启用
                  </el-radio>

                  <el-radio value="INACTIVE">
                    停用
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </div>

            <el-form-item label="知识点说明">
              <el-input
                v-model="editorForm.description"
                type="textarea"
                :rows="4"
                maxlength="2000"
                show-word-limit
                placeholder="说明该知识点的学习内容、能力要求和应用场景"
              />
            </el-form-item>
          </section>

          <section class="editor-section">
            <div class="editor-section-title">
              <div>
                <h3>前置知识点</h3>

                <p>
                  学生学习当前知识点前建议先掌握的内容。
                </p>
              </div>

              <el-tag type="info">
                {{
                  editorForm
                    .prerequisiteIds
                    .length
                }}
                项
              </el-tag>
            </div>

            <el-form-item label="选择前置知识点">
              <el-select
                v-model="
                  editorForm.prerequisiteIds
                "
                multiple
                filterable
                clearable
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择前置知识点"
                style="width: 100%"
              >
                <el-option
                  v-for="
                    option
                    in knowledgePointOptions
                  "
                  :key="
                    option.knowledgePointId
                  "
                  :label="
                    getKnowledgePointOptionLabel(
                      option
                    )
                  "
                  :value="
                    option.knowledgePointId
                  "
                />
              </el-select>
            </el-form-item>

            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="系统会在后端检查自身依赖和循环依赖，防止出现 A 依赖 B、B 又依赖 A 的错误关系。"
            />
          </section>

          <section class="editor-section">
            <div class="editor-section-title">
              <div>
                <h3>学习资源绑定</h3>

                <p>
                  将原系统学习卡片与当前知识点关联，并设置计算权重。
                </p>
              </div>

              <el-button
                type="primary"
                plain
                @click="addLearningBinding"
              >
                添加学习资源
              </el-button>
            </div>

            <el-empty
              v-if="
                editorForm
                  .learningItemBindings
                  .length === 0
              "
              description="暂未绑定学习资源"
              :image-size="75"
            />

            <div
              v-for="(
                binding,
                index
              ) in editorForm
                .learningItemBindings"
              :key="`learning-${index}`"
              class="binding-row"
            >
              <div class="binding-main">
                <el-select
                  v-model="binding.itemPk"
                  filterable
                  clearable
                  placeholder="请选择学习资源"
                  style="width: 100%"
                >
                  <el-option
                    v-for="
                      option
                      in learningItemOptions
                    "
                    :key="option.itemPk"
                    :label="
                      getLearningItemOptionLabel(
                        option
                      )
                    "
                    :value="option.itemPk"
                    :disabled="
                      isLearningItemDisabled(
                        option.itemPk,
                        index
                      )
                    "
                  />
                </el-select>
              </div>

              <div class="binding-weight">
                <span>权重</span>

                <el-input-number
                  v-model="binding.weight"
                  :min="0.01"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  style="width: 125px"
                />
              </div>

              <el-button
                type="danger"
                plain
                @click="
                  removeLearningBinding(
                    index
                  )
                "
              >
                删除
              </el-button>
            </div>
          </section>

          <section class="editor-section">
            <div class="editor-section-title">
              <div>
                <h3>能力考核绑定</h3>

                <p>
                  将原系统能力考核与当前知识点关联，并设置成绩计算权重。
                </p>
              </div>

              <el-button
                type="warning"
                plain
                @click="addAssignmentBinding"
              >
                添加能力考核
              </el-button>
            </div>

            <el-empty
              v-if="
                editorForm
                  .assignmentBindings
                  .length === 0
              "
              description="暂未绑定能力考核"
              :image-size="75"
            />

            <div
              v-for="(
                binding,
                index
              ) in editorForm
                .assignmentBindings"
              :key="`assignment-${index}`"
              class="binding-row"
            >
              <div class="binding-main">
                <el-select
                  v-model="
                    binding.assignmentId
                  "
                  filterable
                  clearable
                  placeholder="请选择能力考核"
                  style="width: 100%"
                >
                  <el-option
                    v-for="
                      option
                      in assignmentOptions
                    "
                    :key="
                      option.assignmentId
                    "
                    :label="
                      getAssignmentOptionLabel(
                        option
                      )
                    "
                    :value="
                      option.assignmentId
                    "
                    :disabled="
                      isAssignmentDisabled(
                        option.assignmentId,
                        index
                      )
                    "
                  />
                </el-select>
              </div>

              <div class="binding-weight">
                <span>权重</span>

                <el-input-number
                  v-model="binding.weight"
                  :min="0.01"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  style="width: 125px"
                />
              </div>

              <el-button
                type="danger"
                plain
                @click="
                  removeAssignmentBinding(
                    index
                  )
                "
              >
                删除
              </el-button>
            </div>
          </section>
        </el-form>
      </div>

      <template #footer>
        <el-button
          :disabled="saving"
          @click="editorVisible = false"
        >
          取消
        </el-button>

        <el-button
          type="primary"
          :loading="saving"
          :disabled="editorLoading"
          @click="saveKnowledgePoint"
        >
          保存知识点
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {
  onMounted,
  reactive,
  ref
} from 'vue'

import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  createAdminKnowledgePoint,
  getAdminKnowledgePointDetail,
  getAdminKnowledgePoints,
  getAdminKnowledgePointStats,
  getKnowledgePointAssignmentOptions,
  getKnowledgePointLearningItemOptions,
  getKnowledgePointOptions,
  updateAdminKnowledgePoint,
  updateAdminKnowledgePointStatus
} from '@/api/knowledgePointAdmin'

const loading = ref(false)

const detailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)

const editorLoading = ref(false)
const editorVisible = ref(false)
const saving = ref(false)

const changingStatusId = ref(null)

const knowledgePoints = ref([])

const knowledgePointOptions = ref([])
const learningItemOptions = ref([])
const assignmentOptions = ref([])

const filters = reactive({
  keyword: '',
  status: '',
  category: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const stats = reactive({
  totalCount: 0,
  activeCount: 0,
  inactiveCount: 0,
  boundLearningItemCount: 0,
  boundAssignmentCount: 0,
  prerequisiteConfiguredCount: 0
})

const editorForm = reactive(
  createBlankEditorForm()
)

function createBlankEditorForm() {
  return {
    knowledgePointId: null,
    code: '',
    name: '',
    description: '',
    category: '',
    difficulty: 'INTERMEDIATE',
    status: 'ACTIVE',
    warningThreshold: 60,
    sortOrder: 0,
    prerequisiteIds: [],
    learningItemBindings: [],
    assignmentBindings: []
  }
}

function createBlankLearningBinding() {
  return {
    itemPk: null,
    weight: 1
  }
}

function createBlankAssignmentBinding() {
  return {
    assignmentId: null,
    weight: 1
  }
}

const resetEditorForm = () => {
  Object.assign(
    editorForm,
    createBlankEditorForm()
  )
}

const loadStats = async () => {
  const data =
    await getAdminKnowledgePointStats()

  Object.assign(stats, {
    totalCount:
      Number(data?.totalCount) || 0,

    activeCount:
      Number(data?.activeCount) || 0,

    inactiveCount:
      Number(data?.inactiveCount) || 0,

    boundLearningItemCount:
      Number(
        data?.boundLearningItemCount
      ) || 0,

    boundAssignmentCount:
      Number(
        data?.boundAssignmentCount
      ) || 0,

    prerequisiteConfiguredCount:
      Number(
        data?.prerequisiteConfiguredCount
      ) || 0
  })
}

const loadList = async () => {
  const data =
    await getAdminKnowledgePoints({
      keyword:
        filters.keyword.trim()
        || undefined,

      status:
        filters.status
        || undefined,

      category:
        filters.category.trim()
        || undefined,

      page:
        pagination.page,

      pageSize:
        pagination.pageSize
    })

  knowledgePoints.value =
    data?.records
    || []

  pagination.total =
    Number(data?.total)
    || 0
}

const loadPage = async () => {
  if (loading.value) {
    return
  }

  loading.value = true

  try {
    await Promise.all([
      loadStats(),
      loadList()
    ])
  } catch (error) {
    console.error(
      '加载知识点管理数据失败：',
      error
    )
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadPage()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  filters.category = ''

  pagination.page = 1
  loadPage()
}

const handlePageSizeChange = () => {
  pagination.page = 1
  loadList()
}

const openDetail = async row => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null

  try {
    detail.value =
      await getAdminKnowledgePointDetail(
        row.knowledgePointId
      )
  } catch (error) {
    console.error(
      '加载知识点详情失败：',
      error
    )

    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

const loadEditorOptions = async (
  excludeKnowledgePointId = null
) => {
  const [
    knowledgePointData,
    learningItemData,
    assignmentData
  ] = await Promise.all([
    getKnowledgePointOptions(
      excludeKnowledgePointId
    ),

    getKnowledgePointLearningItemOptions(),

    getKnowledgePointAssignmentOptions()
  ])

  knowledgePointOptions.value =
    Array.isArray(knowledgePointData)
      ? knowledgePointData
      : []

  learningItemOptions.value =
    Array.isArray(learningItemData)
      ? learningItemData
      : []

  assignmentOptions.value =
    Array.isArray(assignmentData)
      ? assignmentData
      : []
}

const openCreateEditor = async () => {
  resetEditorForm()

  editorVisible.value = true
  editorLoading.value = true

  try {
    await loadEditorOptions()
  } catch (error) {
    console.error(
      '加载知识点编辑选项失败：',
      error
    )

    editorVisible.value = false
  } finally {
    editorLoading.value = false
  }
}

const openEditEditor = async row => {
  const knowledgePointId =
    row?.knowledgePointId

  if (!knowledgePointId) {
    ElMessage.error(
      '没有获取到知识点编号'
    )

    return
  }

  resetEditorForm()

  editorVisible.value = true
  editorLoading.value = true

  try {
    const [
      detailData
    ] = await Promise.all([
      getAdminKnowledgePointDetail(
        knowledgePointId
      ),

      loadEditorOptions(
        knowledgePointId
      )
    ])

    fillEditorForm(detailData)
  } catch (error) {
    console.error(
      '加载知识点编辑数据失败：',
      error
    )

    editorVisible.value = false
  } finally {
    editorLoading.value = false
  }
}

const fillEditorForm = data => {
  Object.assign(editorForm, {
    knowledgePointId:
      data.knowledgePointId,

    code:
      data.code || '',

    name:
      data.name || '',

    description:
      data.description || '',

    category:
      data.category || '',

    difficulty:
      data.difficulty
      || 'INTERMEDIATE',

    status:
      data.status
      || 'ACTIVE',

    warningThreshold:
      Number(
        data.warningThreshold
        ?? 60
      ),

    sortOrder:
      Number(
        data.sortOrder
        ?? 0
      ),

    prerequisiteIds:
      Array.isArray(data.prerequisites)
        ? data.prerequisites.map(
          item =>
            item.knowledgePointId
        )
        : [],

    learningItemBindings:
      Array.isArray(
        data.learningItemBindings
      )
        ? data.learningItemBindings.map(
          item => ({
            itemPk:
              item.itemPk,

            weight:
              Number(
                item.weight
                ?? 1
              )
          })
        )
        : [],

    assignmentBindings:
      Array.isArray(
        data.assignmentBindings
      )
        ? data.assignmentBindings.map(
          item => ({
            assignmentId:
              item.assignmentId,

            weight:
              Number(
                item.weight
                ?? 1
              )
          })
        )
        : []
  })
}

const editCurrentDetail = () => {
  if (!detail.value) {
    return
  }

  const currentDetail =
    detail.value

  detailVisible.value = false

  openEditEditor(
    currentDetail
  )
}

const addLearningBinding = () => {
  editorForm.learningItemBindings.push(
    createBlankLearningBinding()
  )
}

const removeLearningBinding = index => {
  editorForm.learningItemBindings.splice(
    index,
    1
  )
}

const addAssignmentBinding = () => {
  editorForm.assignmentBindings.push(
    createBlankAssignmentBinding()
  )
}

const removeAssignmentBinding = index => {
  editorForm.assignmentBindings.splice(
    index,
    1
  )
}

const isLearningItemDisabled = (
  itemPk,
  currentIndex
) => {
  return editorForm
    .learningItemBindings
    .some(
      (binding, index) =>
        index !== currentIndex
        && binding.itemPk === itemPk
    )
}

const isAssignmentDisabled = (
  assignmentId,
  currentIndex
) => {
  return editorForm
    .assignmentBindings
    .some(
      (binding, index) =>
        index !== currentIndex
        && binding.assignmentId
          === assignmentId
    )
}

const validateEditorForm = () => {
  if (!editorForm.code.trim()) {
    ElMessage.warning(
      '请填写知识点编码'
    )

    return false
  }

  if (!editorForm.name.trim()) {
    ElMessage.warning(
      '请填写知识点名称'
    )

    return false
  }

  const warningThreshold =
    Number(
      editorForm.warningThreshold
    )

  if (
    !Number.isFinite(
      warningThreshold
    )
    || warningThreshold < 0
    || warningThreshold > 100
  ) {
    ElMessage.warning(
      '预警阈值必须在 0 到 100 之间'
    )

    return false
  }

  const sortOrder =
    Number(editorForm.sortOrder)

  if (
    !Number.isInteger(sortOrder)
    || sortOrder < 0
  ) {
    ElMessage.warning(
      '显示顺序必须是大于或等于 0 的整数'
    )

    return false
  }

  const learningItemIds =
    new Set()

  for (
    let index = 0;
    index
      < editorForm
        .learningItemBindings
        .length;
    index += 1
  ) {
    const binding =
      editorForm
        .learningItemBindings[index]

    if (!binding.itemPk) {
      ElMessage.warning(
        `第 ${index + 1} 项学习资源尚未选择`
      )

      return false
    }

    if (
      learningItemIds.has(
        binding.itemPk
      )
    ) {
      ElMessage.warning(
        '同一学习资源不能重复绑定'
      )

      return false
    }

    learningItemIds.add(
      binding.itemPk
    )

    if (!isValidWeight(
      binding.weight
    )) {
      ElMessage.warning(
        `第 ${index + 1} 项学习资源权重必须在 0.01 到 1.00 之间`
      )

      return false
    }
  }

  const assignmentIds =
    new Set()

  for (
    let index = 0;
    index
      < editorForm
        .assignmentBindings
        .length;
    index += 1
  ) {
    const binding =
      editorForm
        .assignmentBindings[index]

    if (!binding.assignmentId) {
      ElMessage.warning(
        `第 ${index + 1} 项能力考核尚未选择`
      )

      return false
    }

    if (
      assignmentIds.has(
        binding.assignmentId
      )
    ) {
      ElMessage.warning(
        '同一能力考核不能重复绑定'
      )

      return false
    }

    assignmentIds.add(
      binding.assignmentId
    )

    if (!isValidWeight(
      binding.weight
    )) {
      ElMessage.warning(
        `第 ${index + 1} 项能力考核权重必须在 0.01 到 1.00 之间`
      )

      return false
    }
  }

  return true
}

const isValidWeight = value => {
  const weight =
    Number(value)

  return Number.isFinite(weight)
    && weight >= 0.01
    && weight <= 1
}

const buildSavePayload = () => {
  return {
    code:
      editorForm.code
        .trim()
        .toUpperCase(),

    name:
      editorForm.name.trim(),

    description:
      editorForm.description.trim()
      || null,

    category:
      editorForm.category.trim()
      || null,

    difficulty:
      editorForm.difficulty,

    status:
      editorForm.status,

    warningThreshold:
      Number(
        editorForm.warningThreshold
      ),

    sortOrder:
      Number(
        editorForm.sortOrder
      ),

    prerequisiteIds:
      [...editorForm.prerequisiteIds],

    learningItemBindings:
      editorForm
        .learningItemBindings
        .map(binding => ({
          itemPk:
            binding.itemPk,

          weight:
            Number(binding.weight)
        })),

    assignmentBindings:
      editorForm
        .assignmentBindings
        .map(binding => ({
          assignmentId:
            binding.assignmentId,

          weight:
            Number(binding.weight)
        }))
  }
}

const saveKnowledgePoint = async () => {
  if (
    saving.value
    || !validateEditorForm()
  ) {
    return
  }

  saving.value = true

  try {
    const payload =
      buildSavePayload()

    if (editorForm.knowledgePointId) {
      await updateAdminKnowledgePoint(
        editorForm.knowledgePointId,
        payload
      )

      ElMessage.success(
        `知识点“${payload.name}”已更新`
      )
    } else {
      await createAdminKnowledgePoint(
        payload
      )

      ElMessage.success(
        `知识点“${payload.name}”已创建`
      )
    }

    editorVisible.value = false

    await loadPage()
  } catch (error) {
    console.error(
      '保存知识点失败：',
      error
    )
  } finally {
    saving.value = false
  }
}

const changeStatus = async (
  row,
  targetStatus
) => {
  const enabling =
    targetStatus === 'ACTIVE'

  const actionText =
    enabling
      ? '启用'
      : '停用'

  try {
    await ElMessageBox.confirm(
      `确定${actionText}知识点“${row.name}”吗？`,
      `${actionText}知识点`,
      {
        confirmButtonText:
          `确认${actionText}`,

        cancelButtonText:
          '取消',

        type:
          enabling
            ? 'success'
            : 'warning'
      }
    )
  } catch {
    return
  }

  changingStatusId.value =
    row.knowledgePointId

  try {
    await updateAdminKnowledgePointStatus(
      row.knowledgePointId,
      targetStatus
    )

    ElMessage.success(
      `知识点“${row.name}”已${actionText}`
    )

    await loadPage()
  } catch (error) {
    console.error(
      `${actionText}知识点失败：`,
      error
    )
  } finally {
    changingStatusId.value = null
  }
}

const getKnowledgePointOptionLabel = option => {
  const statusText =
    option.status === 'ACTIVE'
      ? '启用'
      : '停用'

  return `${option.code}｜${option.name}｜${statusText}`
}

const getLearningItemOptionLabel = option => {
  const category =
    option.category
    || '未分类'

  const status =
    getResourceStatusText(
      option.status
    )

  return `#${option.itemPk}｜${option.title}｜${category}｜${status}`
}

const getAssignmentOptionLabel = option => {
  const category =
    option.category
    || '未分类'

  const status =
    getResourceStatusText(
      option.status
    )

  return `#${option.assignmentId}｜${option.title}｜${category}｜${status}`
}

const getDifficultyText = value => {
  const textMap = {
    BASIC: '基础',
    INTERMEDIATE: '中级',
    ADVANCED: '高级'
  }

  return textMap[value]
    || value
    || '-'
}

const getDifficultyTagType = value => {
  const typeMap = {
    BASIC: 'success',
    INTERMEDIATE: 'warning',
    ADVANCED: 'danger'
  }

  return typeMap[value]
    || 'info'
}

const getResourceStatusText = value => {
  const textMap = {
    ACTIVE: '启用',
    INACTIVE: '停用',
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }

  return textMap[value]
    || value
    || '-'
}

const formatPercent = value => {
  return `${Number(value || 0).toFixed(2)}%`
}

const formatWeight = value => {
  return Number(value || 0).toFixed(2)
}

const formatScore = value => {
  return `${Number(value || 0).toFixed(2)} 分`
}

const formatDateTime = value => {
  if (!value) {
    return '-'
  }

  return String(value)
    .replace('T', ' ')
    .slice(0, 19)
}

onMounted(() => {
  loadPage()
})
</script>

<style scoped>
.knowledge-management-page {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(
      circle at top left,
      rgba(37, 99, 235, 0.14),
      transparent 30%
    ),
    linear-gradient(
      180deg,
      #f8fbff 0%,
      #eef4ff 100%
    );
}

.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.panel,
.stat-card {
  border:
    1px solid
    rgba(148, 163, 184, 0.2);
  background:
    rgba(255, 255, 255, 0.94);
  box-shadow:
    0 16px 38px
    rgba(15, 23, 42, 0.07);
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 24px;
}

.hero-card h1 {
  margin: 0 0 8px;
  color: #102a43;
  font-size: 30px;
}

.hero-card p {
  margin: 0;
  color: #486581;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns:
    repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px;
  border-radius: 18px;
}

.stat-card span {
  color: #64748b;
  font-size: 13px;
}

.stat-card strong {
  color: #0f172a;
  font-size: 29px;
}

.panel {
  padding: 22px;
  border-radius: 22px;
}

.panel-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.filter-area,
.toolbar-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.keyword-input {
  width: 290px;
}

.filter-select {
  width: 135px;
}

.category-input {
  width: 175px;
}

.result-summary {
  margin-bottom: 14px;
  color: #64748b;
  font-size: 13px;
}

.result-summary strong {
  color: #2563eb;
}

.pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

.detail-content {
  min-height: 250px;
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 6px;
}

.detail-section {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid #dbe7f3;
  border-radius: 14px;
  background: #f8fbff;
}

.detail-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 12px;
}

.detail-section-header h3 {
  margin: 0;
  color: #102a43;
  font-size: 17px;
}

.editor-content {
  min-height: 300px;
  max-height: 76vh;
  overflow-y: auto;
  padding-right: 8px;
}

.knowledge-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.editor-section {
  padding: 18px;
  border: 1px solid #dbe7f3;
  border-radius: 16px;
  background: #f8fbff;
}

.first-section {
  margin-top: 2px;
}

.editor-section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.editor-section-title h3 {
  margin: 0;
  color: #102a43;
  font-size: 18px;
}

.editor-section-title p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  column-gap: 18px;
}

.form-tip {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.binding-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 13px;
  border: 1px solid #dbe7f3;
  border-radius: 12px;
  background: #ffffff;
}

.binding-row:last-child {
  margin-bottom: 0;
}

.binding-main {
  flex: 1;
  min-width: 0;
}

.binding-weight {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 8px;
}

.binding-weight span {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns:
      repeat(3, minmax(0, 1fr));
  }

  .panel-toolbar {
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .knowledge-management-page {
    padding: 14px;
  }

  .hero-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions .el-button {
    flex: 1;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .filter-area,
  .toolbar-actions {
    width: 100%;
  }

  .keyword-input,
  .filter-select,
  .category-input {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .binding-row {
    align-items: stretch;
    flex-direction: column;
  }

  .binding-weight {
    justify-content: space-between;
  }
}
</style>