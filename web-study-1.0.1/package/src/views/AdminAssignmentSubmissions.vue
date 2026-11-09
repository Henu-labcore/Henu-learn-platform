<template>
  <div class="submission-page">
    <div class="page-shell">
      <section v-loading="assignmentLoading" class="hero-card">
        <div class="hero-row">
          <div class="hero-title">
            <el-tag :type="assignmentTag(assignment?.status)" effect="plain">
              {{ assignmentText(assignment?.status) }}
            </el-tag>
            <h1>{{ assignment?.title || '提交记录' }}</h1>
          </div>
          <div class="hero-actions">
            <el-button :icon="ArrowLeft" plain @click="goBack">返回考核管理</el-button>
          </div>
        </div>
        <div class="hero-row hero-row-secondary">
          <div class="hero-meta">
            <span>提交人数 {{ assignment?.submissionCount || 0 }}</span>
            <span>已评分 {{ assignment?.gradedCount || 0 }}</span>
            <span>满分 {{ assignment?.totalScore || 100 }}</span>
          </div>
          <span class="hero-note">提交记录与评分</span>
        </div>
      </section>

      <div class="content-grid">
        <section class="panel submissions-panel">
          <div class="panel-head">
            <div>
              <h2>学生提交</h2>
              <p>选择记录后查看作答。</p>
            </div>
            <el-select v-model="submissionStatus" clearable placeholder="全部状态" class="status-filter" @change="loadSubmissions">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已提交" value="SUBMITTED" />
              <el-option label="逾期" value="LATE" />
              <el-option label="已评分" value="GRADED" />
              <el-option label="已退回" value="RETURNED" />
            </el-select>
            <el-button
              class="batch-download-button"
              type="primary"
              plain
              :loading="batchDownloadLoading"
              :disabled="batchDownloadLoading"
              @click="downloadBatchFiles"
            >
              批量下载附件
            </el-button>
          </div>

          <div v-if="downloadProgress.visible" class="transfer-progress">
            <div class="transfer-progress-head">
              <span>{{ downloadProgress.name }}</span>
              <strong>{{ downloadProgress.label }}</strong>
            </div>
            <el-progress :percentage="downloadProgress.percent" :indeterminate="downloadProgress.indeterminate" />
          </div>

          <div v-loading="submissionsLoading" class="submission-list">
            <button
              v-for="row in submissions"
              :key="row.submissionId"
              type="button"
              class="submission-item"
              :class="{ active: row.submissionId === submissionDetail?.submissionId }"
              @click="openSubmission(row)"
            >
              <div class="submission-item-main">
                <strong>{{ row.studentName }}</strong>
                <span>{{ formatTime(row.submittedAt) }}</span>
              </div>
              <div class="submission-item-meta">
                <el-tag size="small" :type="submissionTag(row.status)">{{ submissionText(row.status) }}</el-tag>
                <span>{{ row.fileCount || 0 }} 附件</span>
                <span>{{ row.score ?? '-' }} 分</span>
              </div>
            </button>
            <el-empty v-if="!submissionsLoading && !submissions.length" description="暂无提交记录" />
          </div>

          <el-pagination
            class="pagination"
            layout="total, prev, pager, next"
            :total="submissionsTotal"
            :page-size="pageSize"
            v-model:current-page="page"
            @current-change="loadSubmissions"
          />
        </section>

        <main class="panel detail-panel">
          <el-empty v-if="!submissionDetail" description="请选择一条提交记录" />
          <template v-else>
            <div class="detail-head">
              <div>
                <h2>{{ submissionDetail.studentName }}</h2>
                <p>{{ formatTime(submissionDetail.submittedAt) }}</p>
              </div>
              <el-tag :type="submissionTag(submissionDetail.status)">
                {{ submissionText(submissionDetail.status) }}
              </el-tag>
            </div>

            <div class="detail-metrics">
              <div>
                <span>附件数</span>
                <strong>{{ submissionDetail.files?.length || 0 }}</strong>
              </div>
              <div>
                <span>当前得分</span>
                <strong>{{ submissionDetail.score ?? '-' }}</strong>
              </div>
            </div>

            <section class="detail-section answer-section">
              <div class="section-title">
                <h3>文字答案</h3>
                <span>学生提交内容</span>
              </div>
              <div class="answer-content">
                {{ submissionDetail.answerText || '学生未填写文字答案。' }}
              </div>
            </section>

            <section class="detail-section">
              <div class="section-title">
                <h3>附件</h3>
                <span>{{ submissionDetail.files?.length || 0 }} 个文件</span>
              </div>
              <el-table :data="submissionDetail.files || []" size="small" stripe>
                <el-table-column prop="originalName" label="文件名" min-width="180" />
                <el-table-column label="类型" width="80">
                  <template #default="{ row }">{{ fileTypeText(row.fileType) }}</template>
                </el-table-column>
                <el-table-column label="大小" width="100">
                  <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="80">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="downloadAdminFile(row)">下载</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>

            <section class="detail-section">
              <div class="section-title">
                <h3>评分反馈</h3>
              </div>
              <el-form label-position="top">
                <el-form-item label="分数">
                  <el-input-number v-model="gradeForm.score" :min="0" :max="Number(assignment?.totalScore || 100)" style="width: 100%" />
                </el-form-item>
                <el-form-item label="评语">
                  <el-input v-model="gradeForm.feedback" type="textarea" :rows="4" placeholder="填写评分说明或退回原因" />
                </el-form-item>
              </el-form>
              <div class="grade-actions">
                <el-button type="warning" plain :disabled="!canReviewCurrent" @click="returnCurrent">退回重交</el-button>
                <el-button type="primary" :disabled="!canReviewCurrent" @click="gradeCurrent">保存评分</el-button>
              </div>
            </section>
          </template>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getAdminAssignment,
  getAdminAssignmentSubmissions,
  getAdminSubmissionBatchDownloadInfo,
  getAdminSubmission,
  gradeSubmission,
  returnSubmission
} from '@/api/assignments'

const route = useRoute()
const router = useRouter()
const assignmentId = route.params.assignmentId

const assignment = ref(null)
const submissions = ref([])
const submissionDetail = ref(null)
const assignmentLoading = ref(false)
const submissionsLoading = ref(false)
const submissionStatus = ref('')
const page = ref(1)
const pageSize = ref(10)
const submissionsTotal = ref(0)
const batchDownloadLoading = ref(false)
const downloadProgress = ref({ visible: false, name: '', percent: 0, label: '', indeterminate: false })
const gradeForm = reactive({ score: null, feedback: '' })
const canReviewCurrent = computed(() => {
  const submission = submissionDetail.value
  return Boolean(submission?.submittedAt && submission.status !== 'DRAFT' && submission.status !== 'RETURNED')
})

const loadAssignment = async () => {
  assignmentLoading.value = true
  try {
    assignment.value = await getAdminAssignment(assignmentId)
  } finally {
    assignmentLoading.value = false
  }
}

const loadSubmissions = async () => {
  submissionsLoading.value = true
  try {
    const data = await getAdminAssignmentSubmissions(assignmentId, {
      status: submissionStatus.value,
      page: page.value,
      pageSize: pageSize.value
    })
    submissions.value = data.records || []
    submissionsTotal.value = data.total || 0
  } finally {
    submissionsLoading.value = false
  }
}

const loadPage = async () => {
  await Promise.all([loadAssignment(), loadSubmissions()])
}

const openSubmission = async row => {
  submissionDetail.value = await getAdminSubmission(row.submissionId)
  gradeForm.score = submissionDetail.value.score
  gradeForm.feedback = submissionDetail.value.feedback || ''
}

const gradeCurrent = async () => {
  if (!submissionDetail.value) return
  await gradeSubmission(submissionDetail.value.submissionId, gradeForm)
  ElMessage.success('评分已保存')
  await refreshSelected()
}

const returnCurrent = async () => {
  if (!submissionDetail.value) return
  await returnSubmission(submissionDetail.value.submissionId, { feedback: gradeForm.feedback })
  ElMessage.success('已退回')
  await refreshSelected()
}

const refreshSelected = async () => {
  const id = submissionDetail.value?.submissionId
  await loadPage()
  if (id) {
    submissionDetail.value = await getAdminSubmission(id)
    gradeForm.score = submissionDetail.value.score
    gradeForm.feedback = submissionDetail.value.feedback || ''
  }
}

const downloadAdminFile = file => downloadWithAuth(`/admin/assignments/files/${file.fileId}/download`, file.originalName)

const downloadBatchFiles = async () => {
  const query = submissionStatus.value ? `?status=${encodeURIComponent(submissionStatus.value)}` : ''
  const fallback = `${assignment.value?.title || '考核'}-提交附件.zip`
  batchDownloadLoading.value = true
  try {
    const info = await getAdminSubmissionBatchDownloadInfo(assignmentId, { status: submissionStatus.value })
    if (!info.fileCount) {
      ElMessage.warning('暂无可下载附件')
      return
    }
    if (!info.allowed) {
      ElMessage.error(`附件总大小 ${formatSize(info.totalBytes)}，超过批量下载上限 ${formatSize(info.maxBytes)}，请按状态筛选或逐个下载`)
      return
    }
    if (!supportsStreamDownload() && info.totalBytes > 1024 * 1024 * 1024) {
      ElMessage.error('当前浏览器不支持大文件流式保存，请使用 Chrome/Edge 下载，或按状态筛选后分批下载')
      return
    }
    await ElMessageBox.confirm(
      `将下载 ${info.fileCount} 个附件，估算大小 ${formatSize(info.totalBytes)}。下载期间请不要关闭页面。`,
      '确认批量下载',
      { type: 'warning', confirmButtonText: '开始下载', cancelButtonText: '取消' }
    )
    await downloadWithAuth(`/admin/assignments/${assignmentId}/submissions/files/download${query}`, fallback, { streamToDisk: true, expectedBytes: info.totalBytes })
  } catch (error) {
    if (error !== 'cancel' && error !== 'close' && error?.name !== 'AbortError') {
      console.error(error)
    }
  } finally {
    batchDownloadLoading.value = false
  }
}

const downloadWithAuth = async (url, fallbackName, options = {}) => {
  if (options.streamToDisk && supportsStreamDownload()) {
    await downloadStreamToDisk(url, fallbackName)
    return
  }
  const prefix = import.meta.env.VITE_API_BASE_URL || '/api'
  const response = await fetch(`${prefix}${url}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
  })
  if (!response.ok) {
    const errorText = await response.text()
    ElMessage.error(resolveErrorMessage(errorText) || '下载失败，请稍后重试')
    return
  }
  const blob = await readResponseBlobWithProgress(response, fallbackName || 'download')
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = resolveDownloadName(response, fallbackName)
  link.click()
  URL.revokeObjectURL(link.href)
}

const downloadStreamToDisk = async (url, fallbackName) => {
  const fileHandle = await window.showSaveFilePicker({
    suggestedName: fallbackName || 'download.zip',
    types: [{ description: 'ZIP 文件', accept: { 'application/zip': ['.zip'] } }]
  })
  const prefix = import.meta.env.VITE_API_BASE_URL || '/api'
  const response = await fetch(`${prefix}${url}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` }
  })
  if (!response.ok) {
    const errorText = await response.text()
    ElMessage.error(resolveErrorMessage(errorText) || '下载失败，请稍后重试')
    return
  }
  const writable = await fileHandle.createWritable()
  const total = Number(response.headers.get('Content-Length') || 0)
  const reader = response.body?.getReader()
  if (!reader) {
    const blob = await response.blob()
    await writable.write(blob)
    await writable.close()
    return
  }
  downloadProgress.value = {
    visible: true,
    name: fallbackName || 'download.zip',
    percent: 0,
    label: total ? '0%' : 'Downloading',
    indeterminate: !total
  }
  try {
    let loaded = 0
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      await writable.write(value)
      loaded += value.length
      updateDownloadProgress(loaded, total)
    }
    downloadProgress.value.percent = 100
    downloadProgress.value.label = '100%'
    await writable.close()
  } catch (error) {
    await writable.abort()
    throw error
  } finally {
    window.setTimeout(resetDownloadProgress, 800)
  }
}

const readResponseBlobWithProgress = async (response, name) => {
  const total = Number(response.headers.get('Content-Length') || 0)
  downloadProgress.value = {
    visible: true,
    name,
    percent: 0,
    label: total ? '0%' : 'Downloading',
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
    window.setTimeout(resetDownloadProgress, 800)
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

const resetDownloadProgress = () => {
  downloadProgress.value = { visible: false, name: '', percent: 0, label: '', indeterminate: false }
}

const supportsStreamDownload = () => Boolean(window.showSaveFilePicker && window.ReadableStream && window.WritableStream)

const resolveDownloadName = (response, fallback) => {
  const disposition = response.headers.get('Content-Disposition') || ''
  const utf8Match = disposition.match(/filename\\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const asciiMatch = disposition.match(/filename="?([^";]+)"?/i)
  return asciiMatch?.[1] || fallback || 'download'
}

const resolveErrorMessage = text => {
  if (!text) return ''
  try {
    const data = JSON.parse(text)
    return data.message || data.msg || ''
  } catch {
    return ''
  }
}

const goBack = () => {
  router.push('/admin-assignments')
}

const formatTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
const formatSize = size => size ? `${(size / 1024 / 1024).toFixed(2)} MB` : '-'
const fileTypeText = value => ({ DOCUMENT: '文档', VIDEO: '视频' }[value] || value || '-')
const assignmentText = value => ({ DRAFT: '草稿', PUBLISHED: '已发布', ARCHIVED: '已归档' }[value] || value || '-')
const assignmentTag = value => ({ PUBLISHED: 'success', ARCHIVED: 'info', DRAFT: 'warning' }[value] || '')
const submissionText = status => ({ DRAFT: '未提交', SUBMITTED: '已提交', LATE: '逾期', GRADED: '已评分', RETURNED: '已退回' }[status] || status || '-')
const submissionTag = status => ({ SUBMITTED: 'warning', LATE: 'danger', GRADED: 'success', RETURNED: 'info' }[status] || '')

onMounted(loadPage)
</script>

<style scoped>
.submission-page {
  min-height: 100%;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.16), transparent 30%),
    linear-gradient(180deg, #f8fbff 0%, #eef4ff 100%);
  padding: 24px;
}

.page-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-head,
.detail-head,
.grade-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hero-card,
.panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.hero-card {
  border-radius: 12px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hero-card h1 {
  margin: 0;
  color: #102a43;
  font-size: 18px;
  line-height: 1.3;
}

.panel-head p,
.detail-head p {
  margin: 0;
  color: #486581;
  line-height: 1.7;
}

.hero-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.hero-title,
.hero-actions,
.hero-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-title {
  min-width: 0;
  flex: 1;
}

.hero-actions {
  flex-shrink: 0;
}

.hero-meta {
  flex-wrap: wrap;
  gap: 6px;
}

.hero-meta span {
  border-radius: 999px;
  border: 1px solid #dbe7f3;
  background: #f8fbff;
  color: #486581;
  font-size: 12px;
  padding: 3px 9px;
  white-space: nowrap;
}

.hero-note,
.detail-metrics span {
  color: #829ab1;
  font-size: 12px;
}

.detail-metrics {
  display: grid;
  gap: 12px;
}

.detail-metrics div {
  border-radius: 16px;
  border: 1px solid #dbe7f3;
  background: #f8fbff;
  padding: 14px;
}

.detail-metrics span {
  display: block;
  margin-bottom: 6px;
}

.detail-metrics strong {
  color: #102a43;
  font-size: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.panel {
  border-radius: 24px;
  padding: 24px;
}

.submissions-panel {
  padding: 20px;
  position: sticky;
  top: 84px;
}

.panel-head {
  margin-bottom: 14px;
}

.submissions-panel .panel-head {
  align-items: flex-start;
  flex-direction: column;
  gap: 10px;
}

.status-filter {
  width: 100%;
}

.batch-download-button {
  width: 100%;
}

.panel-head h2,
.detail-head h2,
.section-title h3 {
  margin: 0;
  color: #102a43;
}

.submission-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 220px;
}

.submission-item {
  width: 100%;
  border: 1px solid #dbe7f3;
  border-radius: 14px;
  background: #f8fbff;
  padding: 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.submission-item:hover,
.submission-item.active {
  border-color: #2f80ed;
  background: #fff;
  box-shadow: 0 10px 20px rgba(47, 128, 237, 0.12);
}

.submission-item-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.submission-item-main strong {
  color: #102a43;
  font-size: 15px;
}

.submission-item-main span,
.submission-item-meta span {
  color: #829ab1;
  font-size: 12px;
}

.submission-item-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.pagination {
  justify-content: center;
  margin-top: 16px;
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

.detail-panel {
  min-height: 680px;
  padding: 28px;
}

.detail-head {
  padding-bottom: 18px;
  border-bottom: 1px solid #e4edf7;
}

.detail-metrics {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 20px 0;
}

.detail-section {
  padding-top: 22px;
  margin-top: 22px;
  border-top: 1px solid #e4edf7;
}

.detail-section.answer-section {
  border-top: none;
  margin-top: 0;
  padding-top: 0;
}

.answer-content {
  min-height: 220px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid #dbe7f3;
  padding: 20px;
  color: #243b53;
  line-height: 1.85;
  white-space: pre-wrap;
  font-size: 15px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-title span {
  color: #829ab1;
  font-size: 13px;
}

.grade-actions {
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .hero-row,
  .hero-title {
    flex-direction: column;
    align-items: flex-start;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    width: 100%;
    justify-content: space-between;
  }

  .hero-row-secondary {
    gap: 10px;
  }

  .submissions-panel {
    position: static;
  }
}
</style>
