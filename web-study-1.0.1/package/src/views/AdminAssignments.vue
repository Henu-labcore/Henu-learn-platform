<template>
  <div class="admin-assessment-page">
    <div class="page-shell">
      <section class="hero-card">
        <div>
          <h1>考核管理</h1>
          <p>创建能力考核要求，控制发布状态，并进入提交记录页完成查看、下载和评分。</p>
        </div>
        <el-button type="primary" size="large" @click="openEditor()">
          新建考核
        </el-button>
      </section>

      <section class="stats-grid">
        <article class="stat-card">
          <span>考核总数</span>
          <strong>{{ stats.total }}</strong>
        </article>
        <article class="stat-card">
          <span>已发布</span>
          <strong>{{ publishedCount }}</strong>
        </article>
        <article class="stat-card">
          <span>学生提交</span>
          <strong>{{ submissionCount }}</strong>
        </article>
        <article class="stat-card">
          <span>已评分</span>
          <strong>{{ gradedCount }}</strong>
        </article>
      </section>

      <section class="panel">
        <div class="panel-toolbar">
          <div class="toolbar-left">
            <el-input
              v-model="keyword"
              clearable
              placeholder="搜索考核标题或分类"
              style="width: 260px"
              @keyup.enter="loadAssignments"
              @clear="loadAssignments"
            />
            <el-select v-model="status" clearable placeholder="全部状态" style="width: 150px" @change="loadAssignments">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="已归档" value="ARCHIVED" />
            </el-select>
            <el-button plain @click="loadAssignments">筛选</el-button>
          </div>
          <el-button plain @click="loadAssignments">刷新</el-button>
        </div>

        <el-table v-loading="loading" :data="assignments" stripe class="assessment-table">
          <el-table-column prop="title" label="考核标题" min-width="240" />
          <el-table-column prop="category" label="分类" width="140" />
          <el-table-column label="截止时间" width="170">
            <template #default="{ row }">{{ formatTime(row.deadline) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="assignmentTag(row.status)">{{ assignmentText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交/已评" width="120">
            <template #default="{ row }">{{ row.submissionCount || 0 }} / {{ row.gradedCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="满分" width="90">
            <template #default="{ row }">{{ row.totalScore || 100 }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="320" fixed="right">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button size="small" @click="openEditor(row.assignmentId)">编辑</el-button>
                <el-button
                  v-if="row.status !== 'PUBLISHED'"
                  size="small"
                  type="success"
                  @click="changeStatus(row, 'PUBLISHED')"
                >
                  发布
                </el-button>
                <el-button
                  v-if="row.status === 'PUBLISHED'"
                  size="small"
                  type="warning"
                  @click="changeStatus(row, 'DRAFT')"
                >
                  撤回
                </el-button>
                <el-button
                  v-if="row.status !== 'ARCHIVED'"
                  size="small"
                  type="info"
                  @click="changeStatus(row, 'ARCHIVED')"
                >
                  归档
                </el-button>
                <el-button size="small" type="primary" plain @click="openSubmissions(row)">
                  查看提交
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pagination"
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="page"
          @current-change="loadAssignments"
        />
      </section>
    </div>

    <el-dialog
      v-model="editorVisible"
      :title="form.assignmentId ? '编辑考核' : '新建考核'"
      width="880px"
      destroy-on-close
    >
      <el-form label-position="top" class="assessment-form">
        <div class="form-grid">
          <el-form-item label="考核标题">
            <el-input v-model="form.title" placeholder="请输入考核标题" />
          </el-form-item>
          <el-form-item label="分类">
            <el-input v-model="form.category" placeholder="如：智能体实践" />
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker
              v-model="form.deadline"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="满分">
            <el-input-number v-model="form.totalScore" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
        </div>

        <el-form-item label="考核说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="说明提交背景、整体目标、评分标准或注意事项" />
        </el-form-item>

        <section class="editor-section">
          <div class="section-header">
            <h3>考核要求</h3>
            <el-button link type="primary" @click="addRequirement">添加要求</el-button>
          </div>
          <div v-for="(requirement, index) in form.questions" :key="index" class="question-card">
            <div class="question-card-header">
              <span>{{ requirementLabel(index) }}</span>
              <el-button link type="danger" :disabled="form.questions.length <= 1" @click="form.questions.splice(index, 1)">删除</el-button>
            </div>
            <el-input v-model="requirement.title" class="requirement-title-input" :placeholder="`${requirementLabel(index)}标题（可选）`" />
            <el-input
              v-model="requirement.content"
              type="textarea"
              :rows="4"
              maxlength="4000"
              show-word-limit
              placeholder="请输入这条要求的具体内容"
            />
          </div>
        </section>

        <section class="editor-section">
          <div class="section-header">
            <h3>考核材料</h3>
            <span class="section-note">图片 / 视频 / 文档</span>
          </div>
          <el-alert
            v-if="!form.assignmentId"
            type="info"
            show-icon
            :closable="false"
            title="保存考核后可上传说明材料"
          />
          <template v-else>
            <div class="material-actions">
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="file => uploadMaterial('IMAGE', file)">
                <el-button>上传图片</el-button>
              </el-upload>
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="file => uploadMaterial('VIDEO', file)">
                <el-button>上传视频</el-button>
              </el-upload>
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="file => uploadMaterial('DOCUMENT', file)">
                <el-button>上传文档</el-button>
              </el-upload>
            </div>
            <div v-if="materialUploadProgress.visible" class="transfer-progress">
              <div class="transfer-progress-head">
                <span>{{ materialUploadProgress.name }}</span>
                <strong>{{ materialUploadProgress.percent }}%</strong>
              </div>
              <el-progress :percentage="materialUploadProgress.percent" :status="materialUploadProgress.status" />
            </div>
            <div v-if="materialDownloadProgress.visible" class="transfer-progress">
              <div class="transfer-progress-head">
                <span>{{ materialDownloadProgress.name }}</span>
                <strong>{{ materialDownloadProgress.label }}</strong>
              </div>
              <el-progress :percentage="materialDownloadProgress.percent" :indeterminate="materialDownloadProgress.indeterminate" />
            </div>
            <el-table :data="form.materials" size="small" stripe>
              <el-table-column prop="title" label="材料" min-width="180" />
              <el-table-column label="类型" width="90">
                <template #default="{ row }">{{ materialTypeText(row.materialType) }}</template>
              </el-table-column>
              <el-table-column label="大小" width="100">
                <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="130">
                <template #default="{ row }">
                  <el-button link type="primary" @click="downloadMaterial(row)">下载</el-button>
                  <el-button link type="danger" @click="deleteMaterial(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssignment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminAssignment,
  deleteAdminAssignmentMaterial,
  getAdminAssignment,
  getAdminAssignments,
  getAdminAssignmentStats,
  updateAdminAssignment,
  updateAdminAssignmentStatus,
  uploadAdminAssignmentMaterial
} from '@/api/assignments'

const router = useRouter()
const assignments = ref([])
const loading = ref(false)
const editorVisible = ref(false)
const keyword = ref('')
const status = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const materialUploadProgress = ref({ visible: false, name: '', percent: 0, status: '' })
const materialDownloadProgress = ref({ visible: false, name: '', percent: 0, label: '', indeterminate: false })
const maxSingleUploadBytes = 300 * 1024 * 1024

const form = reactive(blankForm())
const stats = reactive({
  total: 0,
  draft: 0,
  published: 0,
  archived: 0,
  submissions: 0,
  graded: 0
})

const publishedCount = computed(() => stats.published)
const submissionCount = computed(() => stats.submissions)
const gradedCount = computed(() => stats.graded)

function blankForm() {
  return {
    assignmentId: null,
    title: '',
    category: '',
    deadline: '',
    totalScore: 100,
    description: '',
    questions: [blankRequirement()],
    materials: []
  }
}

function blankRequirement() {
  return { title: '', content: '', score: 0, sortOrder: 1 }
}

const loadAssignments = async () => {
  loading.value = true
  try {
    const params = { keyword: keyword.value, status: status.value, page: page.value, pageSize: pageSize.value }
    const [data, statData] = await Promise.all([
      getAdminAssignments(params),
      getAdminAssignmentStats({ keyword: keyword.value, status: status.value })
    ])
    assignments.value = data.records || []
    total.value = data.total || 0
    stats.total = Number(statData.total || 0)
    stats.draft = Number(statData.draft || 0)
    stats.published = Number(statData.published || 0)
    stats.archived = Number(statData.archived || 0)
    stats.submissions = Number(statData.submissions || 0)
    stats.graded = Number(statData.graded || 0)
  } finally {
    loading.value = false
  }
}

const openEditor = async id => {
  Object.assign(form, blankForm())
  if (id) {
    const detail = await getAdminAssignment(id)
    Object.assign(form, {
      assignmentId: detail.assignmentId,
      title: detail.title,
      category: detail.category,
      deadline: detail.deadline,
      totalScore: Number(detail.totalScore || 100),
      description: detail.description || '',
      questions: normalizeRequirements(detail.questions),
      materials: detail.materials || []
    })
  }
  editorVisible.value = true
}

const addRequirement = () => {
  form.questions.push({ ...blankRequirement(), sortOrder: form.questions.length + 1 })
}

const saveAssignment = async () => {
  const questions = form.questions
    .map((item, index) => {
      const title = item.title?.trim() || ''
      const content = item.content || ''
      return {
        title: title || requirementLabel(index),
        content,
        score: 0,
        sortOrder: index + 1,
        hasContent: Boolean(title || content.trim())
      }
    })
    .filter(item => item.hasContent)
    .map(({ hasContent, ...item }) => item)

  const payload = {
    ...form,
    questions
  }
  if (form.assignmentId) {
    await updateAdminAssignment(form.assignmentId, payload)
  } else {
    await createAdminAssignment(payload)
  }
  ElMessage.success('保存成功')
  editorVisible.value = false
  await loadAssignments()
}

const uploadMaterial = async (materialType, uploadFileInfo) => {
  if (!form.assignmentId || !uploadFileInfo?.raw) return
  const file = uploadFileInfo.raw
  if (file.size > maxSingleUploadBytes) {
    ElMessage.error(`单个文件不能超过 ${formatSize(maxSingleUploadBytes)}`)
    return
  }
  materialUploadProgress.value = { visible: true, name: file.name || 'upload', percent: 0, status: '' }
  const material = await uploadAdminAssignmentMaterial(form.assignmentId, materialType, file, '', {
    onUploadProgress: event => {
      if (!event.total) return
      materialUploadProgress.value.percent = Math.min(99, Math.round((event.loaded / event.total) * 100))
    }
  })
  materialUploadProgress.value.percent = 100
  materialUploadProgress.value.status = 'success'
  form.materials.push(material)
  window.setTimeout(() => {
    materialUploadProgress.value = { visible: false, name: '', percent: 0, status: '' }
  }, 800)
  ElMessage.success('材料上传成功')
}

const deleteMaterial = async material => {
  try {
    await ElMessageBox.confirm(`确认删除“${material.title || material.originalName}”？`, '删除材料', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteAdminAssignmentMaterial(material.materialId)
  form.materials = form.materials.filter(item => item.materialId !== material.materialId)
  ElMessage.success('材料已删除')
}

const downloadMaterial = async material => {
  const prefix = import.meta.env.VITE_API_BASE_URL || '/api'
  const response = await fetch(`${prefix}/admin/assignments/materials/${material.materialId}/download`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
  })
  if (!response.ok) {
    ElMessage.error('下载失败，请稍后重试')
    return
  }
  const blob = await readMaterialBlobWithProgress(response, material.originalName || material.title || 'download')
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = resolveDownloadName(response, material.originalName)
  link.click()
  URL.revokeObjectURL(link.href)
}

const readMaterialBlobWithProgress = async (response, name) => {
  const total = Number(response.headers.get('Content-Length') || 0)
  materialDownloadProgress.value = {
    visible: true,
    name,
    percent: 0,
    label: total ? '0%' : 'Downloading',
    indeterminate: !total
  }
  try {
    if (!response.body || !response.body.getReader) {
      const blob = await response.blob()
      materialDownloadProgress.value.percent = 100
      materialDownloadProgress.value.label = '100%'
      return blob
    }
    const reader = response.body.getReader()
    const chunks = []
    let loaded = 0
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      chunks.push(value)
      loaded += value.length
      if (total) {
        const percent = Math.min(99, Math.round((loaded / total) * 100))
        materialDownloadProgress.value.percent = percent
        materialDownloadProgress.value.label = `${percent}%`
      } else {
        materialDownloadProgress.value.label = formatSize(loaded)
      }
    }
    materialDownloadProgress.value.percent = 100
    materialDownloadProgress.value.label = '100%'
    return new Blob(chunks)
  } finally {
    window.setTimeout(() => {
      materialDownloadProgress.value = { visible: false, name: '', percent: 0, label: '', indeterminate: false }
    }, 800)
  }
}

const normalizeRequirements = questions => {
  if (!questions?.length) {
    return [blankRequirement()]
  }
  return questions.map((item, index) => ({
    title: item.title || requirementLabel(index),
    content: item.content || '',
    score: Number(item.score || 0),
    sortOrder: item.sortOrder || index + 1
  }))
}

const requirementLabel = index => {
  const numerals = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
  return `要求${numerals[index] || index + 1}`
}

const changeStatus = async (row, nextStatus) => {
  await updateAdminAssignmentStatus(row.assignmentId, nextStatus)
  ElMessage.success('状态已更新')
  await loadAssignments()
}

const openSubmissions = row => {
  router.push(`/admin-assignments/${row.assignmentId}/submissions`)
}

const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
const formatSize = value => {
  if (!value) return '-'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}
const materialTypeText = value => ({ IMAGE: '图片', VIDEO: '视频', DOCUMENT: '文档' }[value] || value || '-')
const resolveDownloadName = (response, fallback) => {
  const disposition = response.headers.get('Content-Disposition') || ''
  const utf8Match = disposition.match(/filename\\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const asciiMatch = disposition.match(/filename="?([^";]+)"?/i)
  return asciiMatch?.[1] || fallback || 'download'
}
const assignmentText = value => ({ DRAFT: '草稿', PUBLISHED: '已发布', ARCHIVED: '已归档' }[value] || value)
const assignmentTag = value => ({ PUBLISHED: 'success', ARCHIVED: 'info', DRAFT: 'warning' }[value] || '')

onMounted(loadAssignments)
</script>

<style scoped>
.admin-assessment-page {
  min-height: 100%;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.16), transparent 30%),
    linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
  padding: 24px;
}

.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card,
.panel,
.stat-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  border-radius: 24px;
  padding: 28px 32px;
}

.hero-card h1 {
  margin: 0 0 8px;
  font-size: 30px;
  color: #102a43;
}

.hero-card p {
  margin: 0;
  color: #486581;
  line-height: 1.7;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-card span {
  color: #486581;
  font-size: 14px;
}

.stat-card strong {
  font-size: 32px;
  color: #0f172a;
}

.panel {
  border-radius: 24px;
  padding: 24px;
}

.panel-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.toolbar-left,
.row-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.row-actions {
  gap: 8px;
}

.assessment-table {
  width: 100%;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.assessment-form {
  max-height: 68vh;
  overflow-y: auto;
  padding-right: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.editor-section {
  margin-top: 18px;
  border: 1px solid #dbe7f3;
  border-radius: 16px;
  padding: 16px;
  background: #f8fbff;
}

.section-header,
.question-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-header {
  margin-bottom: 12px;
}

.section-header h3 {
  margin: 0;
  color: #102a43;
  font-size: 17px;
}

.section-note {
  color: #829ab1;
  font-size: 13px;
}

.material-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.transfer-progress {
  margin: 10px 0 14px;
  padding: 10px 12px;
  border: 1px solid #dbe7f3;
  border-radius: 10px;
  background: #ffffff;
}

.transfer-progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #486581;
  font-size: 13px;
}

.transfer-progress-head span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.transfer-progress-head strong {
  color: #102a43;
}

.question-card {
  border: 1px solid #dbe7f3;
  background: #ffffff;
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 12px;
}

.question-card:last-child {
  margin-bottom: 0;
}

.question-card-header {
  margin-bottom: 10px;
  color: #243b53;
  font-weight: 700;
}

.requirement-title-input {
  margin-bottom: 10px;
}

@media (max-width: 980px) {
  .stats-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
