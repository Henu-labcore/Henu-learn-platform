<template>
  <div class="user-progress-detail-page">
    <div class="page-container">
      <!-- 返回按钮 -->
      <div class="back-button">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回用户列表
        </el-button>
      </div>

      <!-- 用户基本信息 -->
      <div class="user-info-card" v-if="userInfo">
        <div class="user-avatar">
          {{ userInfo.displayName?.charAt(0)?.toUpperCase() }}
        </div>
        <div class="user-details">
          <h2>{{ userInfo.displayName }}</h2>
          <p class="username">@{{ userInfo.username }}</p>
          <div class="user-meta">
            <span class="meta-item">
              <i class="el-icon-time"></i>
              注册时间: {{ formatDate(userInfo.createdTime) }}
            </span>
            <span class="meta-item">
              <i class="el-icon-user"></i>
              角色: {{ userInfo.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </span>
            <span class="meta-item">
              <i class="el-icon-info"></i>
              状态: {{ userInfo.status === 'ACTIVE' ? '正常' : '禁用' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 学习概览卡片 -->
      <div class="stats-grid" v-if="userProgress">
        <div class="stat-card">
          <div class="stat-icon">
            <i class="el-icon-document-copy"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userProgress.totalItems }}</div>
            <div class="stat-label">总学习项目</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon completed">
            <i class="el-icon-success"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userProgress.completedItems }}</div>
            <div class="stat-label">已完成</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon in-progress">
            <i class="el-icon-loading"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ userProgress.inProgressItems }}</div>
            <div class="stat-label">学习中</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon">
            <i class="el-icon-time"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ formatDuration(userProgress.totalLearningTime) }}</div>
            <div class="stat-label">总学习时长</div>
          </div>
        </div>
      </div>

      <!-- 学习进度详情 -->
      <div class="progress-section">
        <h3>学习进度详情</h3>
        <div class="progress-table-container">
          <el-table
            :data="userProgress?.learningItems || []"
            stripe
            style="width: 100%"
            :header-cell-style="{background:'#f9fafb',color:'#374151',fontWeight:'600'}"
          >
            <el-table-column prop="itemName" label="学习项目" min-width="200" />
            <el-table-column prop="category" label="分类" min-width="120" />
            <el-table-column label="进度" min-width="180">
              <template #default="scope">
                <div class="progress-cell">
                  <div class="progress-bar">
                    <div
                      class="progress-fill"
                      :style="{ width: `${scope.row.progress}%` }"
                    ></div>
                  </div>
                  <span class="progress-text">{{ scope.row.progress }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="100">
              <template #default="scope">
                <el-tag
                  :type="getStatusType(scope.row.status)"
                  size="small"
                >
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="学习时长" min-width="120">
              <template #default="scope">
                {{ formatDuration(scope.row.learningTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="lastAccessTime" label="最后访问" min-width="160">
              <template #default="scope">
                {{ formatDate(scope.row.lastAccessTime) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 学习活动记录 -->
      <div class="activity-section" v-if="userProgress?.recentActivities?.length > 0">
        <h3>最近学习活动</h3>
        <div class="activity-list">
          <div
            v-for="activity in userProgress.recentActivities"
            :key="activity.id"
            class="activity-item"
          >
            <div class="activity-content">
              <div class="activity-title">{{ activity.itemName }}</div>
              <div class="activity-meta">
                {{ activity.action }} · {{ formatDate(activity.timestamp) }}
              </div>
            </div>
            <div class="activity-duration">
              {{ formatDuration(activity.duration) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()

const userId = ref(String(route.params.userId || ''))
const userInfo = ref(null)
const userProgress = ref(null)
const loading = ref(false)

// 获取用户基本信息
const fetchUserInfo = async () => {
  try {
    const response = await request.get(`/users/${userId.value}`)
    userInfo.value = response
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

// 获取用户学习进度详情
const fetchUserProgress = async () => {
  loading.value = true
  try {
    const response = await request.get(`/learning-progress/admin/user/${userId.value}/detail`)
    userProgress.value = response
  } catch (error) {
    console.error('获取用户学习进度失败:', error)
    ElMessage.error('获取用户学习进度失败')
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds || seconds === 0) return '0分钟'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)

  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  } else {
    return `${minutes}分钟`
  }
}

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'IN_PROGRESS': return 'primary'
    case 'NOT_STARTED': return 'info'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'COMPLETED': return '已完成'
    case 'IN_PROGRESS': return '学习中'
    case 'NOT_STARTED': return '未开始'
    default: return '未知'
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

onMounted(() => {
  if (userId.value) {
    fetchUserInfo()
    fetchUserProgress()
  }
})
</script>

<style scoped>
.user-progress-detail-page {
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20px;
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
}

.back-button {
  margin-bottom: 24px;
}

.user-info-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.user-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
}

.user-details h2 {
  margin: 0 0 4px 0;
  color: #1f2937;
  font-size: 24px;
}

.username {
  color: #6b7280;
  margin-bottom: 12px;
  font-size: 16px;
}

.user-meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  font-size: 20px;
}

.stat-icon.completed {
  background: #dcfce7;
  color: #16a34a;
}

.stat-icon.in-progress {
  background: #dbeafe;
  color: #2563eb;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 4px;
}

.stat-label {
  color: #6b7280;
  font-size: 14px;
}

.progress-section,
.activity-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.progress-section h3,
.activity-section h3 {
  margin: 0 0 20px 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
}

.progress-table-container {
  width: 100%;
  overflow-x: auto;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  max-width: 100px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #06b6d4);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #6b7280;
  min-width: 35px;
  text-align: right;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
}

.activity-title {
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
}

.activity-meta {
  color: #6b7280;
  font-size: 14px;
}

.activity-duration {
  color: #3b82f6;
  font-weight: 500;
}
</style>
