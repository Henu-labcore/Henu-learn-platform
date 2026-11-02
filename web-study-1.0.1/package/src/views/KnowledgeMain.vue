<template>
  <div class="knowledge-main-container">
    <div class="layui-fluid">
      <!-- 顶部标题区域 -->
      <div class="page-header">
        <div class="header-row">
          <div class="title-group">
            <h1 class="page-title">知识学习中心</h1>
            <div class="section-switch" role="tablist" aria-label="内容切换">
              <button
                type="button"
                class="section-tab"
                :class="{ 'is-active': activeSection === 'cards' }"
                @click="activeSection = 'cards'"
              >
                知识卡片
              </button>
              <button
                type="button"
                class="section-tab"
                @click="goLearningData"
              >
                学习数据
              </button>
            </div>
          </div>
          <!-- 分类选择下拉框 -->
          <div class="header-tools" v-show="activeSection === 'cards'">
            <div class="header-filters">
              <div class="category-filter">
                <span class="category-filter-label">分类</span>
                <el-select
                  v-model="selectedCategory"
                  placeholder="全部"
                  class="category-select"
                  @change="filterByCategory"
                >
                  <el-option label="全部" value="" />
                  <el-option
                    v-for="category in categories"
                    :key="category"
                    :label="category"
                    :value="category"
                  />
                </el-select>
              </div>

              <!-- 搜索框 -->
              <el-autocomplete
                v-model="searchKeyword"
                :fetch-suggestions="querySearch"
                placeholder="搜索知识标题"
                clearable
                style="width: 240px"
                @select="handleSuggestionSelect"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 目录面包屑：知识目录 / 栏目 / 类别 -->
      <nav
        v-if="activeSection === 'cards' && (level > 1 || searchKeyword.trim())"
        class="directory-breadcrumb"
        aria-label="知识目录路径"
      >
        <button type="button" class="breadcrumb-link" @click="goRoot">
          知识目录
        </button>

        <template v-if="currentGroup">
          <span class="breadcrumb-sep">/</span>

          <button
            type="button"
            class="breadcrumb-link"
            @click="openGroup(currentGroup.key)"
          >
            {{ currentGroup.name }}
          </button>
        </template>

        <template v-if="selectedCategory">
          <span class="breadcrumb-sep">/</span>
          <span class="breadcrumb-current">{{ selectedCategory }}</span>
        </template>

        <span v-if="searchKeyword.trim()" class="breadcrumb-search">
          搜索：{{ searchKeyword.trim() }}
        </span>
      </nav>

      <!-- 文件夹目录：栏目 → 类别 -->
      <section
        v-if="activeSection === 'cards' && !showCards"
        class="knowledge-directory"
      >
        <template v-if="level === 1">
          <p class="directory-hint">选择一个栏目，逐层进入学习卡片</p>

          <div class="folder-grid">
            <button
              v-for="group in knowledgeGroups"
              :key="group.key"
              type="button"
              class="folder-card"
              :class="{ 'is-empty': group.categories.length === 0 }"
              @click="openGroup(group.key)"
            >
              <el-icon class="folder-card-icon">
                <FolderOpened v-if="group.categories.length" />
                <Folder v-else />
              </el-icon>

              <span class="folder-card-name">{{ group.name }}</span>

              <span class="folder-card-meta">
                {{
                  group.categories.length
                    ? `${group.categories.length} 个类别 · ${groupItemCount(group)} 张卡片`
                    : '暂无栏目'
                }}
              </span>

              <span class="folder-card-desc">{{ group.description }}</span>
            </button>
          </div>
        </template>

        <template v-else>
          <p class="directory-hint">
            选择「{{ currentGroup?.name }}」下的类别查看卡片
          </p>

          <div v-if="currentGroup?.categories.length" class="folder-grid">
            <button
              v-for="category in currentGroup.categories"
              :key="category"
              type="button"
              class="folder-card is-category"
              @click="openCategory(currentGroup.key, category)"
            >
              <el-icon class="folder-card-icon">
                <FolderOpened />
              </el-icon>

              <span class="folder-card-name">{{ category }}</span>

              <span class="folder-card-meta">
                {{ categoryItemCount(category) }} 张卡片
              </span>
            </button>
          </div>

          <div v-else class="folder-empty">
            该栏目暂无内容
          </div>
        </template>
      </section>

      <!-- 新增计划摘要；原卡片、数据面板及实验事件保持不变。 -->
      <LearningPlanSummary v-if="activeSection === 'cards' && showCards" />

      <keep-alive>
        <KnowledgeCardsSection
          v-if="activeSection === 'cards' && showCards"
          :loading="loading"
          :items="filteredItems"
          @show-details="showKnowledgeDetails"
          @open-details="openKnowledgeDetails"
          @start-learning="startLearning"
        />
      </keep-alive>
    </div>

    <!-- 知识详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="currentKnowledge?.title + ' 详情'"
      width="600px"
      class="knowledge-detail-dialog"
    >
      <div v-if="currentKnowledge" class="knowledge-detail-popup">
        <div class="popup-content">
          <div class="popup-info">
            <div class="info-item">
              <span class="info-label">难度等级：</span>
              <span
                class="info-value"
                :class="getDifficultyClass(currentKnowledge.difficulty)"
              >
                {{ currentKnowledge.difficulty }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">预计时长：</span>
              <span class="info-value">{{ currentKnowledge.duration }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">前置要求：</span>
              <span class="info-value">{{ currentKnowledge.prerequisites }}</span>
            </div>
          </div>
          <div class="popup-description">
            <h4>知识简介</h4>
            <p>{{ currentKnowledge.summary }}</p>
          </div>
          <div class="popup-objectives">
            <h4>学习目标</h4>
            <p>{{ formatObjectives(currentKnowledge.objectives) }}</p>
          </div>
        </div>
      </div>
    </el-dialog>

    <ExperimentOverlay
      v-model="experimentVisible"
      :experiment-id="activeExperimentId"
      :experiment-title="activeExperimentTitle"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Folder, FolderOpened } from '@element-plus/icons-vue'
import { getLearningItems, getLearningItemPreview } from '@/api/knowledge'
import {
  buildKnowledgeGroups,
  findGroupKeyOfCategory
} from '@/config/knowledgeGroups'
import ExperimentOverlay from '@/components/ExperimentOverlay.vue'
import KnowledgeCardsSection from '@/components/knowledge/KnowledgeCardsSection.vue'
import LearningPlanSummary from '@/components/knowledge/LearningPlanSummary.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const goLearningData = () => {
  router.push(
    authStore.isAdmin
      ? '/analytics/process'
      : '/my-learning/process'
  ).catch(() => {})
}

const loading = ref(false)
const activeSection = ref('cards')
const selectedCategory = ref('')
const detailDialogVisible = ref(false)
const currentKnowledge = ref(null)
const items = ref([])
const searchKeyword = ref('')
const experimentVisible = ref(false)
const activeExperimentId = ref(null)
const activeExperimentTitle = ref('')

const categories = computed(() => {
  if (!items.value.length) return []
  const cats = new Set()
  items.value.forEach(item => {
    if (item.category) {
      cats.add(item.category)
    }
  })
  return Array.from(cats).sort()
})

// 栏目 → 类别 → 卡片 的目录结构
const knowledgeGroups = computed(() => buildKnowledgeGroups(categories.value))

const selectedGroupKey = computed(() => {
  const queryGroup = typeof route.query.group === 'string' ? route.query.group : ''

  if (queryGroup) {
    return queryGroup
  }

  return selectedCategory.value
    ? findGroupKeyOfCategory(categories.value, selectedCategory.value)
    : ''
})

const currentGroup = computed(() => {
  return knowledgeGroups.value.find(group => group.key === selectedGroupKey.value) || null
})

// 1 = 栏目列表，2 = 类别列表，3 = 卡片列表
const level = computed(() => {
  if (selectedCategory.value) {
    return 3
  }

  return selectedGroupKey.value ? 2 : 1
})

// 搜索时直接展示卡片结果，不受当前层级限制
const showCards = computed(() => {
  return level.value === 3 || searchKeyword.value.trim() !== ''
})

const categoryItemCount = category => {
  return items.value.filter(item => item.category === category).length
}

const groupItemCount = group => {
  return group.categories.reduce(
    (total, category) => total + categoryItemCount(category),
    0
  )
}

const goRoot = () => {
  searchKeyword.value = ''
  router.push({ path: '/knowledge' }).catch(() => {})
}

const openGroup = key => {
  searchKeyword.value = ''
  router.push({ path: '/knowledge', query: { group: key } }).catch(() => {})
}

const openCategory = (groupKey, category) => {
  searchKeyword.value = ''

  const query = groupKey
    ? { group: groupKey, category }
    : { category }

  router.push({ path: '/knowledge', query }).catch(() => {})
}

const filteredItems = computed(() => {
  if (!items.value.length) return []
  let list = items.value
  if (selectedCategory.value) {
    list = list.filter(item => item.category === selectedCategory.value)
  }
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    list = list.filter(item => item.title?.toLowerCase().includes(keyword))
  }
  return list
})

const filterByCategory = () => {
  if (!selectedCategory.value) {
    goRoot()
    return
  }

  const groupKey = findGroupKeyOfCategory(
    categories.value,
    selectedCategory.value
  )

  openCategory(groupKey, selectedCategory.value)
}

const syncCategoryFromRoute = () => {
  selectedCategory.value = typeof route.query.category === 'string' ? route.query.category : ''
}
const querySearch = (queryString, cb) => {
  const keyword = queryString.trim().toLowerCase()
  if (!keyword) {
    cb([])
    return
  }
  const suggestions = items.value
    .filter(item => item.title?.toLowerCase().includes(keyword))
    .slice(0, 8)
    .map(item => ({
      value: item.title,
      id: item.id
    }))
  cb(suggestions)
}

const handleSuggestionSelect = (suggestion) => {
  if (suggestion?.value) {
    searchKeyword.value = suggestion.value
  }
}

const unwrapData = (payload) => {
  if (payload && payload.data !== undefined) {
    return payload.data
  }
  return payload
}

const formatObjectives = (objectives) => {
  if (Array.isArray(objectives)) {
    return objectives.join('；')
  }
  return objectives || ''
}

const loadItems = async () => {
  loading.value = true
  try {
    const res = await getLearningItems()
    const list = unwrapData(res)
    items.value = Array.isArray(list) ? list : []
  } catch (error) {
    items.value = []
    console.error('知识数据加载失败:', error)
  } finally {
    loading.value = false
  }
}

const showKnowledgeDetails = async (knowledgeId) => {
  try {
    const res = await getLearningItemPreview(knowledgeId)
    const data = unwrapData(res)
    if (data) {
      currentKnowledge.value = data
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('知识详情加载失败:', error)
  }
}

const openKnowledgeDetails = (knowledgeId) => {
  router.push({
    path: '/knowledge/details',
    query: { id: knowledgeId }
  }).catch(err => {
    if (err.name !== 'NavigationDuplicated') {
      console.error('路由跳转失败:', err)
    }
  })
}

const startLearning = (knowledgeId) => {
  if (!knowledgeId) return
  const match = items.value.find(item => String(item.id) === String(knowledgeId))
  activeExperimentTitle.value = match?.title || ''
  activeExperimentId.value = knowledgeId
  experimentVisible.value = true
}

const getDifficultyClass = (difficulty) => {
  if (difficulty === '简单') return 'difficulty-easy'
  if (difficulty === '中等') return 'difficulty-medium'
  return 'difficulty-hard'
}

onMounted(() => {
  syncCategoryFromRoute()
  loadItems()
})

watch(
  () => route.query.category,
  () => {
    syncCategoryFromRoute()
  }
)

// 侧边栏切换栏目 / 类别时清空搜索，避免仍停留在上一次搜索结果
watch(
  () => [route.query.group, route.query.category],
  () => {
    searchKeyword.value = ''
  }
)

watch(experimentVisible, (visible) => {
  if (!visible) {
    activeExperimentId.value = null
    activeExperimentTitle.value = ''
  }
})
</script>

<style scoped>
.knowledge-main-container {
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f2f3f7;
  min-height: 100%;
  padding: 0;
}

.layui-fluid {
  width: 100%;
  max-width: 100%;
  margin: 0;
  background: #ffffff;
  border-radius: 0 0 16px 16px;
  padding: 24px 28px 32px;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 16px;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-filters {
  display: flex;
  align-items: center;
  gap: 14px;
}

.category-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px 4px 10px;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  background: #eff6ff;
  box-shadow: 0 6px 14px rgba(30, 136, 229, 0.12);
}

.category-filter-label {
  color: #1565C0;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.category-select {
  width: 200px;
}

.category-select :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #1E88E5 inset;
}

.section-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-radius: 999px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.08);
}

.section-tab {
  border: none;
  background: transparent;
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 14px;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

.section-tab:hover {
  color: #1E88E5;
  background: rgba(30, 136, 229, 0.1);
}

.section-tab.is-active {
  background: #ffffff;
  color: #1E88E5;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.12);
}

@media (max-width: 860px) {
  .header-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .title-group {
    width: 100%;
    justify-content: space-between;
  }
  .header-tools {
    width: 100%;
    justify-content: flex-start;
  }
  .header-filters {
    width: 100%;
    flex-wrap: wrap;
  }
  .category-filter {
    width: 100%;
    box-sizing: border-box;
  }
  .category-select {
    flex: 1;
    width: auto;
  }
  .section-switch {
    margin-left: 0;
    padding: 3px;
  }
  .section-tab {
    padding: 6px 12px;
    font-size: 13px;
  }
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #111827;
  letter-spacing: 0.5px;
  margin: 0;
}


.popup-info {
  background-color: #f0f9ff;
  border: 1px solid #e0f2fe;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(30, 136, 229, 0.05);
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 4px 0;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-weight: 600;
  color: #1E88E5;
  width: 100px;
  flex-shrink: 0;
  font-size: 14px;
}

.info-value {
  color: #4b5563;
  flex: 1;
  font-size: 14px;
}

.difficulty-easy {
  color: #10b981 !important;
  font-weight: 600;
}

.difficulty-medium {
  color: #f59e0b !important;
  font-weight: 600;
}

.difficulty-hard {
  color: #ef4444 !important;
  font-weight: 600;
}

.popup-description,
.popup-objectives {
  margin-bottom: 24px;
  padding: 0 4px;
}

.popup-description h4,
.popup-objectives h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1E88E5;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #e0f2fe;
}

.popup-description p,
.popup-objectives p {
  color: #4b5563;
  margin: 0;
  padding: 4px 0;
  line-height: 1.7;
}

/* 知识目录：面包屑 + 文件夹卡片 */
.directory-breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 24px 16px;
  padding: 10px 16px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #ffffff;
  font-size: 13px;
  color: #64748b;
}

.breadcrumb-link {
  padding: 0;
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  cursor: pointer;
}

.breadcrumb-link:hover {
  text-decoration: underline;
}

.breadcrumb-sep {
  color: #cbd5e1;
}

.breadcrumb-current {
  color: #0f172a;
  font-weight: 600;
}

.breadcrumb-search {
  margin-left: auto;
  color: #94a3b8;
  font-size: 12px;
}

.knowledge-directory {
  margin: 0 24px 24px;
}

.directory-hint {
  margin: 0 0 14px;
  color: #64748b;
  font-size: 13px;
}

.folder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.folder-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 20px 20px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: linear-gradient(160deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.folder-card:hover {
  transform: translateY(-3px);
  border-color: #93c5fd;
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.15);
}

.folder-card.is-empty {
  background: #f8fafc;
  border-style: dashed;
  cursor: default;
}

.folder-card.is-empty:hover {
  transform: none;
  border-color: #e2e8f0;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05);
}

.folder-card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  margin-bottom: 4px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 22px;
}

.folder-card.is-empty .folder-card-icon {
  background: #f1f5f9;
  color: #94a3b8;
}

.folder-card-name {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.folder-card-meta {
  color: #2563eb;
  font-size: 12px;
}

.folder-card.is-empty .folder-card-meta {
  color: #94a3b8;
}

.folder-card-desc {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.6;
}

.folder-empty {
  padding: 28px;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  color: #94a3b8;
  text-align: center;
  font-size: 13px;
}

@media (max-width: 768px) {
  .directory-breadcrumb,
  .knowledge-directory {
    margin-left: 12px;
    margin-right: 12px;
  }

  .folder-grid {
    grid-template-columns: 1fr;
  }
}
</style>
