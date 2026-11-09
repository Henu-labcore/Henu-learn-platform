<template>
  <div class="contributors-page">
    <div class="page-header">
      <h1 class="page-title">项目贡献者</h1>
      <p class="page-subtitle">本项目由以下成员参与开发与维护，感谢他们的贡献。</p>
    </div>

    <div v-loading="loading" class="content-area">
      <el-empty v-if="!loading && contributors.length === 0" description="暂无贡献者信息" />

      <div v-else class="contributor-grid">
        <div
          v-for="person in contributors"
          :key="person.contributorId"
          class="contributor-card"
        >
          <div class="card-head">
            <div class="avatar">{{ initialOf(person.displayName) }}</div>
            <div class="head-text">
              <div class="name-row">
                <span class="name">{{ person.displayName || '未命名' }}</span>
                <el-tag v-if="person.grade" size="small" type="info">{{ person.grade }}</el-tag>
              </div>
              <div v-if="person.roleTitle" class="role-title">{{ person.roleTitle }}</div>
            </div>
          </div>

          <div v-if="person.moduleScope" class="module-row">
            <span class="module-label">负责模块</span>
            <div class="module-tags">
              <el-tag
                v-for="mod in splitModules(person.moduleScope)"
                :key="mod"
                size="small"
                effect="plain"
              >
                {{ mod }}
              </el-tag>
            </div>
          </div>

          <p v-if="person.description" class="description">{{ person.description }}</p>

          <div class="card-foot">
            <span v-if="person.joinDate" class="join-date">加入于 {{ person.joinDate }}</span>
            <a
              v-if="person.githubUrl"
              class="github-link"
              :href="person.githubUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              GitHub
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getContributors } from '@/api/contributors'

const loading = ref(false)
const contributors = ref([])

const initialOf = name => (name ? name.charAt(0).toUpperCase() : '?')

const splitModules = scope =>
  (scope || '')
    .split(/[、,，;；/]/)
    .map(item => item.trim())
    .filter(Boolean)

const load = async () => {
  loading.value = true
  try {
    contributors.value = await getContributors()
  } catch (error) {
    // 拦截器已统一提示，这里只避免中断渲染
    ElMessage.error(error?.message || '加载贡献者信息失败')
    contributors.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.contributors-page {
  padding: 24px 32px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2d3d;
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #7a8699;
}

.content-area {
  min-height: 200px;
}

.contributor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.contributor-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.contributor-card:hover {
  box-shadow: 0 6px 20px rgba(31, 45, 61, 0.08);
  transform: translateY(-2px);
}

.card-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar {
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #2f7fe0);
  color: #ffffff;
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.head-text {
  min-width: 0;
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-title {
  margin-top: 4px;
  font-size: 13px;
  color: #409eff;
}

.module-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.module-label {
  font-size: 12px;
  color: #909399;
}

.module-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.description {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #5a6577;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #909399;
}

.github-link {
  color: #409eff;
  text-decoration: none;
}

.github-link:hover {
  text-decoration: underline;
}
</style>
