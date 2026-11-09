<template>
  <div class="assessment-detail-page">
    <div class="detail-shell">
      <section v-loading="loading" class="detail-hero">
        <div class="hero-row">
          <div class="hero-copy">
            <el-tag effect="plain">{{ detail?.category || '综合考核' }}</el-tag>
            <h1>{{ detail?.title || '能力考核详情' }}</h1>
          </div>
          <el-button :icon="ArrowLeft" plain @click="goBack">返回列表</el-button>
        </div>
        <div class="hero-row hero-row-secondary">
          <div v-if="detail" class="hero-meta">
            <span>截止 {{ formatTime(detail.deadline) }}</span>
            <span>满分 {{ detail.totalScore || 100 }}</span>
            <span>得分 {{ detail.mySubmission?.score ?? '-' }}</span>
          </div>
          <el-tag :type="displayStatusTag" size="large">
            {{ displayStatusText }}
          </el-tag>
        </div>
      </section>

      <div v-if="detail" class="detail-grid">
        <main class="main-column">
          <section class="section-block requirement-focus">
            <div class="section-title">
              <div>
                <h2>考核要求</h2>
                <p>请阅读本次能力考核说明，再在右侧填写文字答案或上传材料。</p>
              </div>
            </div>
            <div class="requirement-body">
              {{ assessmentRequirement }}
            </div>
            <div v-if="detail.materials?.length" class="material-section">
              <div class="section-title material-title">
                <h2>说明材料</h2>
                <span>{{ detail.materials.length }} 个材料</span>
              </div>
              <div class="material-list">
                <article v-for="material in detail.materials" :key="material.materialId" class="material-card">
                  <template v-if="material.materialType === 'IMAGE'">
                    <img :src="materialPreviewUrls[material.materialId]" :alt="material.title || material.originalName" />
                  </template>
                  <template v-else-if="material.materialType === 'VIDEO'">
                    <video :src="materialPreviewUrls[material.materialId]" controls />
                  </template>
                  <div v-else class="document-material">
                    <strong>{{ material.title || material.originalName }}</strong>
                    <span>{{ formatSize(material.fileSize) }}</span>
                  </div>
                  <div class="material-meta">
                    <span>{{ material.title || material.originalName }}</span>
                    <el-button link type="primary" @click="downloadMaterial(material)">下载</el-button>
                  </div>
                </article>
              </div>
            </div>
          </section>

        </main>

        <aside class="side-column">
          <section class="section-block submit-card">
            <div class="section-title">
              <h2>提交材料</h2>
              <span>文档 / 视频</span>
            </div>
            <div class="answer-editor">
              <div class="answer-editor-head">
                <span>文字答案</span>
                <el-button size="small" plain :loading="answerSaving" :disabled="locked" @click="saveAnswer">
                  保存草稿
                </el-button>
              </div>
              <el-input
                v-model="answerText"
                type="textarea"
                :rows="7"
                maxlength="5000"
                show-word-limit
                resize="none"
                placeholder="可以在这里直接填写作答说明、方案摘要、实验过程或补充说明。附件不是必需项。"
                :disabled="locked"
              />
            </div>
            <div class="upload-actions">
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="file => uploadFile('DOCUMENT', file)">
                <el-button :icon="Document" :disabled="locked || uploadProgress.visible">上传文档</el-button>
              </el-upload>
              <el-upload :auto-upload="false" :show-file-list="false" :on-change="file => uploadFile('VIDEO', file)">
                <el-button :icon="VideoPlay" :disabled="locked || uploadProgress.visible">上传视频</el-button>
              </el-upload>
            </div>
            <div v-if="uploadProgress.visible" class="transfer-progress">
              <div class="transfer-progress-head">
                <span>{{ uploadProgress.name }}</span>
                <strong>{{ uploadProgress.percent }}%</strong>
              </div>
              <el-progress :percentage="uploadProgress.percent" :status="uploadProgress.status" />
            </div>
            <div v-if="downloadProgress.visible" class="transfer-progress">
              <div class="transfer-progress-head">
                <span>{{ downloadProgress.name }}</span>
                <strong>{{ downloadProgress.label }}</strong>
              </div>
              <el-progress :percentage="downloadProgress.percent" :indeterminate="downloadProgress.indeterminate" />
            </div>
            <el-table :data="detail.mySubmission?.files || []" size="small" stripe class="file-table">
              <el-table-column prop="originalName" label="文件名" min-width="180" />
              <el-table-column label="类型" width="78">
                <template #default="{ row }">{{ fileTypeText(row.fileType) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button link type="primary" @click="downloadFile(row)">下载</el-button>
                  <el-button link type="danger" :disabled="locked" @click="deleteFile(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button
              class="submit-button"
              type="primary"
              size="large"
              :disabled="submitDisabled"
              @click="submitCurrent"
            >
              {{ deadlinePassed ? '考核已截止' : '提交考核' }}
            </el-button>
            <p class="submit-hint">
              {{ submitHint }}
            </p>
          </section>

          <section class="section-block result-card">
            <div class="section-title">
              <h2>批改结果</h2>
            </div>
            <div class="result-row">
              <span>状态</span>
              <strong>{{ submissionText(detail.mySubmission?.status) }}</strong>
            </div>
            <div class="result-row">
              <span>得分</span>
              <strong>{{ detail.mySubmission?.score ?? '-' }}</strong>
            </div>
            <div class="feedback-box">
              {{ detail.mySubmission?.feedback || '暂无评语' }}
            </div>
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Document, VideoPlay } from '@element-plus/icons-vue'
import { deleteSubmissionFile, getAssignment, saveSubmissionAnswer, submitAssignment, uploadSubmissionFile } from '@/api/assignments'

const route = useRoute()
const router = useRouter()
const detail = ref(null)
const loading = ref(false)
const answerText = ref('')
const answerSaving = ref(false)
const materialPreviewUrls = ref({})
const uploadProgress = ref({ visible: false, name: '', percent: 0, status: '' })
const downloadProgress = ref({ visible: false, name: '', percent: 0, label: '', indeterminate: false })
const maxSingleUploadBytes = 300 * 1024 * 1024
const currentTime = ref(Date.now())
let deadlineTimer = null

const assignmentId = computed(() => route.params.assignmentId)

const deadlinePassed = computed(() => {
  const deadline = detail.value?.deadline
  if (!deadline) return false
  const timestamp = new Date(deadline).getTime()
  return Number.isFinite(timestamp) && currentTime.value >= timestamp
})

const locked = computed(() => detail.value?.mySubmission?.status === 'GRADED' || deadlinePassed.value)

const displayStatusText = computed(() => {
  const status = detail.value?.mySubmission?.status
  if (deadlinePassed.value && (!status || status === 'DRAFT' || status === 'RETURNED')) return '已截止'
  return submissionText(status)
})

const displayStatusTag = computed(() => {
  const status = detail.value?.mySubmission?.status
  if (deadlinePassed.value && (!status || status === 'DRAFT' || status === 'RETURNED')) return 'info'
  return submissionTag(status)
})

const assessmentRequirement = computed(() => {
  const current = detail.value
  if (!current) return '暂无考核要求'
  const parts = []
  if (current.description) {
    parts.push(current.description)
  }
  const questionTexts = (current.questions || [])
    .map(item => [item.title, item.content].filter(Boolean).join('\n'))
    .filter(Boolean)
  parts.push(...questionTexts)
  return parts.join('\n\n') || '暂无考核要求'
})

const submitDisabled = computed(() => {
  const submission = detail.value?.mySubmission
  if (!answerText.value.trim() && !submission?.files?.length) return true
  return locked.value
})

const submitHint = computed(() => {
  const submission = detail.value?.mySubmission
  if (submission?.status === 'GRADED') return '该考核已完成评分，不能再修改提交。'
  if (deadlinePassed.value) return '考核已截止，不能再保存草稿、上传材料或提交。'
  if (!answerText.value.trim() && !submission?.files?.length) return '请填写文字答案，或上传至少一份文档/视频材料后再提交。'
  return '确认文字答案和材料无误后提交，提交后老师即可在后台查看并评分。'
})

const loadDetail = async () => {
  loading.value = true
  try {
    detail.value = await getAssignment(assignmentId.value)
    answerText.value = detail.value?.mySubmission?.answerText || ''
    await loadMaterialPreviews()
  } finally {
    loading.value = false
  }
}

const loadMaterialPreviews = async () => {
  revokeMaterialPreviews()
  const previewMaterials = (detail.value?.materials || []).filter(item => item.materialType === 'IMAGE' || item.materialType === 'VIDEO')
  const nextUrls = {}
  await Promise.all(previewMaterials.map(async item => {
    const blob = await fetchMaterialBlob(item)
    if (blob) {
      nextUrls[item.materialId] = URL.createObjectURL(blob)
    }
  }))
  materialPreviewUrls.value = nextUrls
}

const revokeMaterialPreviews = () => {
  Object.values(materialPreviewUrls.value).forEach(url => URL.revokeObjectURL(url))
  materialPreviewUrls.value = {}
}

const saveAnswer = async () => {
  if (!assignmentId.value || locked.value) return
  answerSaving.value = true
  try {
    const submission = await saveSubmissionAnswer(assignmentId.value, answerText.value)
    if (detail.value) {
      detail.value.mySubmission = submission
    }
    ElMessage.success('文字答案已保存')
  } finally {
    answerSaving.value = false
  }
}

const uploadFile = async (fileType, uploadFileInfo) => {
  if (locked.value) return
  if (!assignmentId.value || !uploadFileInfo?.raw) return
  const file = uploadFileInfo.raw
  if (file.size > maxSingleUploadBytes) {
    ElMessage.error(`单个文件不能超过 ${formatSize(maxSingleUploadBytes)}`)
    return
  }
  uploadProgress.value = { visible: true, name: file.name || '上传附件', percent: 0, status: '' }
  try {
    await uploadSubmissionFile(assignmentId.value, fileType, file, {
      onUploadProgress: event => {
        if (!event.total) return
        uploadProgress.value.percent = Math.min(99, Math.round((event.loaded / event.total) * 100))
      }
    })
    uploadProgress.value.percent = 100
    uploadProgress.value.status = 'success'
    ElMessage.success('上传成功')
    await loadDetail()
  } catch (error) {
    uploadProgress.value.status = 'exception'
  } finally {
    window.setTimeout(() => {
      uploadProgress.value = { visible: false, name: '', percent: 0, status: '' }
    }, 800)
  }
}

const deleteFile = async file => {
  if (locked.value) return
  try {
    await ElMessageBox.confirm(`确认删除“${file.originalName}”？`, '删除附件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  const submission = await deleteSubmissionFile(file.fileId)
  if (detail.value) {
    detail.value.mySubmission = submission || null
  }
  ElMessage.success('附件已删除')
}

const submitCurrent = async () => {
  if (locked.value) return
  await submitAssignment(assignmentId.value, answerText.value)
  ElMessage.success('提交成功')
  await loadDetail()
}

const downloadFile = async file => {
  const prefix = import.meta.env.VITE_API_BASE_URL || '/api'
  const response = await fetch(`${prefix}/assignments/files/${file.fileId}/download`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
  })
  if (!response.ok) {
    ElMessage.error('下载失败，请稍后重试')
    return
  }
  const blob = await readResponseBlobWithProgress(response, file.originalName || '下载附件')
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = resolveDownloadName(response, file.originalName)
  link.click()
  URL.revokeObjectURL(link.href)
}

const downloadMaterial = async material => {
  const blob = await fetchMaterialBlob(material, { showProgress: true })
  if (!blob) return
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = material.originalName || material.title || 'material'
  link.click()
  URL.revokeObjectURL(link.href)
}

const fetchMaterialBlob = async (material, options = {}) => {
  const prefix = import.meta.env.VITE_API_BASE_URL || '/api'
  const response = await fetch(`${prefix}/assignments/materials/${material.materialId}/download`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
  })
  if (!response.ok) {
    ElMessage.error('材料加载失败，请稍后重试')
    return null
  }
  if (options.showProgress) {
    return readResponseBlobWithProgress(response, material.originalName || material.title || '下载材料')
  }
  return response.blob()
}

const readResponseBlobWithProgress = async (response, name) => {
  const total = Number(response.headers.get('Content-Length') || 0)
  downloadProgress.value = {
    visible: true,
    name,
    percent: 0,
    label: total ? '0%' : '下载中',
    indeterminate: !total
  }
  try {
    if (!response.body || !response.body.getReader) {
      const blob = await response.blob()
      downloadProgress.value.percent = 100
      downloadProgress.value.label = '100%'
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
      updateDownloadProgress(loaded, total)
    }
    downloadProgress.value.percent = 100
    downloadProgress.value.label = '100%'
    return new Blob(chunks)
  } finally {
    window.setTimeout(() => {
      downloadProgress.value = { visible: false, name: '', percent: 0, label: '', indeterminate: false }
    }, 800)
  }
}

const updateDownloadProgress = (loaded, total) => {
  if (total) {
    const percent = Math.min(99, Math.round((loaded / total) * 100))
    downloadProgress.value.percent = percent
    downloadProgress.value.label = `${percent}%`
    return
  }
  downloadProgress.value.label = formatSize(loaded)
}

const resolveDownloadName = (response, fallback) => {
  const disposition = response.headers.get('Content-Disposition') || ''
  const utf8Match = disposition.match(/filename\\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const asciiMatch = disposition.match(/filename="?([^";]+)"?/i)
  return asciiMatch?.[1] || fallback || 'download'
}

const goBack = () => {
  router.push('/assignments')
}

const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
const formatSize = value => {
  if (!value) return '-'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}
const fileTypeText = value => ({ DOCUMENT: '文档', VIDEO: '视频' }[value] || value || '-')
const submissionText = status => ({
  DRAFT: '未提交',
  SUBMITTED: '已提交',
  LATE: '逾期提交',
  GRADED: '已评分',
  RETURNED: '已退回'
}[status] || '未提交')
const submissionTag = status => ({
  SUBMITTED: 'warning',
  LATE: 'danger',
  GRADED: 'success',
  RETURNED: 'info'
}[status] || '')

onMounted(() => {
  loadDetail()
  deadlineTimer = window.setInterval(() => {
    currentTime.value = Date.now()
  }, 1000)
})
onBeforeUnmount(() => {
  revokeMaterialPreviews()
  if (deadlineTimer) {
    window.clearInterval(deadlineTimer)
  }
})
</script>

<style scoped>
.assessment-detail-page {
  min-height: 100%;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.14), transparent 30%),
    linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
  padding: 24px;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-title,
.result-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-hero,
.section-block {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.detail-hero {
  border-radius: 12px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-hero h1 {
  margin: 0;
  color: #102a43;
  font-size: 18px;
  line-height: 1.3;
}

.hero-copy {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.hero-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.hero-row-secondary {
  align-items: center;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.hero-meta span {
  border-radius: 999px;
  background: #eef6ff;
  border: 1px solid #d6e8fb;
  color: #486581;
  font-size: 12px;
  padding: 3px 9px;
  white-space: nowrap;
}

.section-title span,
.result-row span,
.submit-hint {
  color: #829ab1;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 18px;
  align-items: start;
}

.main-column,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-column {
  position: sticky;
  top: 84px;
}

.section-block {
  border-radius: 22px;
  padding: 22px;
}

.section-title {
  margin-bottom: 16px;
}

.section-title h2 {
  margin: 0;
  color: #102a43;
  font-size: 20px;
}

.section-title p {
  margin: 6px 0 0;
  color: #6b7c93;
  font-size: 13px;
  line-height: 1.6;
}

.requirement-focus {
  padding: 26px;
  border-color: rgba(37, 99, 235, 0.22);
  box-shadow: 0 22px 46px rgba(37, 99, 235, 0.1);
}

.requirement-body {
  border: 1px solid #dbe7f3;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  padding: 22px 24px;
  color: #486581;
  font-size: 15px;
  line-height: 1.85;
  white-space: pre-wrap;
}

.material-section {
  margin-top: 18px;
}

.material-title {
  margin-bottom: 12px;
}

.material-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.material-card {
  border: 1px solid #dbe7f3;
  border-radius: 14px;
  background: #ffffff;
  overflow: hidden;
}

.material-card img,
.material-card video {
  display: block;
  width: 100%;
  max-height: 260px;
  object-fit: contain;
  background: #f8fbff;
}

.document-material {
  min-height: 120px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  background: #f8fbff;
  color: #243b53;
}

.document-material span {
  color: #829ab1;
  font-size: 13px;
}

.material-meta {
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.material-meta span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #486581;
  font-size: 13px;
}

.upload-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.transfer-progress {
  margin: 10px 0 14px;
  padding: 10px 12px;
  border: 1px solid #dbe7f3;
  border-radius: 10px;
  background: #f8fbff;
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

.answer-editor {
  margin-bottom: 16px;
}

.answer-editor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: #243b53;
  font-weight: 700;
}

.file-table {
  margin: 10px 0 16px;
}

.submit-button {
  width: 100%;
}

.submit-hint {
  margin: 12px 0 0;
  line-height: 1.6;
  font-size: 13px;
}

.result-row {
  padding: 12px 0;
  border-top: 1px solid #e4edf7;
}

.result-row:first-of-type {
  border-top: none;
}

.result-row strong {
  color: #102a43;
}

.feedback-box {
  margin-top: 10px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #dbe7f3;
  padding: 14px;
  color: #486581;
  line-height: 1.7;
}

@media (max-width: 1180px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .side-column {
    position: static;
  }

  .hero-row,
  .hero-copy {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-meta {
    justify-content: flex-start;
  }

}
</style>
