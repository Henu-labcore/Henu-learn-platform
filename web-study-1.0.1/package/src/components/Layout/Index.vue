<template>
  <div class="layout-container">
    <header class="header">
      <div class="system-logo">
        <el-icon class="logo-icon"><Reading /></el-icon>
        <div class="system-logo-text">大数据智能处理团队研究生学习平台</div>
      </div>

      <div class="user-info-container">
        <LearningPlanNotifications />
        <el-dropdown trigger="click" @command="handleUserCommand">
          <div class="user-avatar">
            {{ authStore.displayName.charAt(0).toUpperCase() }}
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" divided disabled>内容管理</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" command="content">学习内容管理</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" command="knowledge-points">知识点管理</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" divided disabled>数据监控</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" command="analytics">学情分析</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" divided disabled>系统管理</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" command="users">用户管理</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" command="assignments-admin">考核管理</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" command="maintenance">项目维护</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <div class="user-welcome">
          欢迎，<span class="user-name">{{ authStore.displayName }}</span>
        </div>
      </div>
    </header>

    <div class="main-wrapper">
      <aside class="sidebar">
        <div class="side-menu-title">
          <el-icon><Menu /></el-icon>
          <span>学习导航</span>
        </div>

        <div class="menu-section">
          <div
            class="menu-group"
            :class="{ 'is-expanded': knowledgeExpanded, 'is-active': isKnowledgeRoute }"
          >
            <div
              class="menu-item menu-item-expandable"
              :class="{ active: isKnowledgeRoute, expanded: knowledgeExpanded }"
              @click="toggleKnowledgeMenu"
            >
              <el-icon><Reading /></el-icon>
              <span>知识学习</span>
              <el-icon class="menu-expand-icon"><ArrowDown /></el-icon>
            </div>

            <transition name="submenu-slide">
              <div v-show="knowledgeExpanded" class="submenu">
                <button
                  type="button"
                  class="submenu-item"
                  :class="{ active: isKnowledgeRoute && !selectedCategory && !selectedGroup }"
                  @click.stop="handleCategoryChange('')"
                >
                  知识目录
                </button>

                <template v-if="categoriesLoading">
                  <button type="button" class="submenu-item submenu-item-muted" disabled>
                    加载分类中…
                  </button>
                </template>

                <template v-else>
                  <div
                    v-for="group in knowledgeGroups"
                    :key="group.key"
                    class="submenu-group"
                  >
                    <div
                      class="submenu-item submenu-folder"
                      :class="{
                        expanded: expandedGroups[group.key],
                        'group-active':
                          isKnowledgeRoute
                          && selectedGroup === group.key
                      }"
                    >
                      <button
                        type="button"
                        class="folder-main"
                        @click.stop="handleGroupOpen(group.key)"
                      >
                        <el-icon class="folder-icon">
                          <FolderOpened v-if="expandedGroups[group.key]" />
                          <Folder v-else />
                        </el-icon>

                        <span>{{ group.name }}</span>
                      </button>

                      <button
                        type="button"
                        class="folder-toggle"
                        :aria-expanded="expandedGroups[group.key] ? 'true' : 'false'"
                        :aria-label="`${expandedGroups[group.key] ? '收起' : '展开'}${group.name}`"
                        @click.stop="toggleGroup(group.key)"
                      >
                        <el-icon class="folder-arrow">
                          <ArrowDown />
                        </el-icon>
                      </button>
                    </div>

                    <transition name="submenu-slide">
                      <div
                        v-show="expandedGroups[group.key]"
                        class="submenu-children"
                      >
                        <button
                          v-for="category in group.categories"
                          :key="category"
                          type="button"
                          class="submenu-item submenu-child"
                          :class="{
                            active:
                              isKnowledgeRoute
                              && selectedCategory === category
                          }"
                          @click.stop="handleCategoryChange(category, group.key)"
                        >
                          {{ category }}
                        </button>

                        <div
                          v-if="group.categories.length === 0"
                          class="submenu-empty"
                        >
                          暂无栏目
                        </div>
                      </div>
                    </transition>
                  </div>
                </template>
              </div>
            </transition>
          </div>

          <!-- 学习过程与能力画像合并为一个入口 -->
          <router-link
            v-if="!authStore.isAdmin"
            to="/my-learning"
            class="menu-item"
            :class="{ active: route.path.startsWith('/my-learning') }"
          >
            <el-icon><TrendCharts /></el-icon>
            <span>我的学习</span>
          </router-link>

          <router-link
            to="/assignments"
            class="menu-item"
            :class="{ active: route.path === '/assignments' }"
          >
            <el-icon><DocumentChecked /></el-icon>
            <span>能力考核</span>
          </router-link>

          <!-- 新入口放在能力考核下面，复用同一 menu-item 样式和选中规则。 -->
          <router-link
            to="/favorites"
            class="menu-item"
            :class="{ active: route.path === '/favorites' }"
          >
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </router-link>

          <router-link
            to="/learning-plan"
            class="menu-item"
            :class="{ active: route.path === '/learning-plan' }"
          >
            <el-icon><Calendar /></el-icon>
            <span>学习计划</span>
          </router-link>

          <router-link
            to="/contributors"
            class="menu-item"
            :class="{ active: route.path === '/contributors' }"
          >
            <el-icon><UserFilled /></el-icon>
            <span>项目贡献者</span>
          </router-link>
        </div>
      </aside>

      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Calendar, DocumentChecked, Folder, FolderOpened, Menu, Reading, Star, TrendCharts, UserFilled } from '@element-plus/icons-vue'
import { useFavoritesStore } from '@/stores/studyStore'
import LearningPlanNotifications from '@/components/knowledge/LearningPlanNotifications.vue'
import { getLearningItems } from '@/api/knowledge'
import { buildKnowledgeGroups, findGroupKeyOfCategory } from '@/config/knowledgeGroups'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const favoritesStore = useFavoritesStore()
// 与原账号状态绑定；退出/切换账号时清空本模块缓存，不修改原登录逻辑。
watch(() => [authStore.isAuthenticated, authStore.user?.userId ?? authStore.user?.id], () => {
  favoritesStore.reset()
  if (authStore.isAuthenticated) favoritesStore.load().catch(() => {})
}, { immediate: true })
const categories = ref([])
const selectedCategory = ref('')
const selectedGroup = ref('')
const categoriesLoading = ref(false)
const knowledgeExpanded = ref(false)

// 知识学习目录：栏目 → 类别，映射规则统一放在 config/knowledgeGroups.js
const expandedGroups = ref({
  security: true,
  ai: true,
  dev: false
})

const knowledgeGroups = computed(() => buildKnowledgeGroups(categories.value))

const groupKeyOf = category => findGroupKeyOfCategory(categories.value, category)

const isKnowledgeRoute = computed(() => {
  return route.path.startsWith('/knowledge') || route.path.startsWith('/learning/item/')
})

const canLoadCategories = computed(() => {
  return authStore.isAuthenticated
})

const unwrapData = payload => {
  if (payload && payload.data !== undefined) {
    return payload.data
  }
  return payload
}

const loadCategories = async () => {
  if (!canLoadCategories.value) {
    categories.value = []
    return
  }
  categoriesLoading.value = true
  try {
    const res = await getLearningItems()
    const list = unwrapData(res)
    const values = new Set()
    if (Array.isArray(list)) {
      list.forEach(item => {
        if (item.category) {
          values.add(item.category)
        }
      })
    }
    categories.value = Array.from(values).sort()
  } catch (error) {
    categories.value = []
    console.error('学习分类加载失败:', error)
  } finally {
    categoriesLoading.value = false
    syncCategoryFromRoute()
  }
}

const syncCategoryFromRoute = () => {
  selectedCategory.value = typeof route.query.category === 'string' ? route.query.category : ''
  selectedGroup.value = typeof route.query.group === 'string' ? route.query.group : ''

  if (!selectedGroup.value && selectedCategory.value) {
    selectedGroup.value = groupKeyOf(selectedCategory.value)
  }

  if (selectedCategory.value || selectedGroup.value) {
    knowledgeExpanded.value = true
  }

  if (selectedGroup.value) {
    expandedGroups.value[selectedGroup.value] = true
  }
}

const toggleGroup = key => {
  expandedGroups.value[key] = !expandedGroups.value[key]
}

// 点击栏目名称：右侧页面进入该栏目的子文件夹列表
const handleGroupOpen = key => {
  knowledgeExpanded.value = true

  const query = { ...route.query, group: key }

  delete query.category

  router.push({ path: '/knowledge', query }).catch(() => {})
}

const handleCategoryChange = (value, groupKey = '') => {
  const category = value || ''
  const query = { ...route.query }

  if (category) {
    query.category = category

    const targetGroup = groupKey || groupKeyOf(category)

    if (targetGroup) {
      query.group = targetGroup
    }
  } else {
    delete query.category
    delete query.group
  }

  knowledgeExpanded.value = true

  if (category) {
    const targetGroup = query.group

    if (targetGroup) {
      expandedGroups.value[targetGroup] = true
    }
  }

  router.push({ path: '/knowledge', query }).catch(() => {})
}

const toggleKnowledgeMenu = () => {
  knowledgeExpanded.value = !knowledgeExpanded.value
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    authStore.logout()
    router.push('/login')
  } catch (error) {
    // user cancelled
  }
}

const handleUserCommand = command => {
  if (command === 'analytics') {
    router.push('/analytics').catch(() => {})
    return
  }
  if (command === 'knowledge-points') {
    router.push('/knowledge-points').catch(() => {})
    return
  }
  if (command === 'profile') {
    router.push('/profile').catch(() => {})
    return
  }
  if (command === 'users') {
    router.push('/users').catch(() => {})
    return
  }
  if (command === 'content') {
    router.push('/content-library').catch(() => {})
    return
  }
  if (command === 'assignments-admin') {
    router.push('/admin-assignments').catch(() => {})
    return
  }
  if (command === 'maintenance') {
    router.push('/maintenance').catch(() => {})
    return
  }
  if (command === 'logout') {
    handleLogout()
  }
}

onMounted(async () => {
  await loadCategories()
  syncCategoryFromRoute()
})

watch(
  () => route.query.category,
  () => {
    syncCategoryFromRoute()
  }
)

watch(
  isKnowledgeRoute,
  onKnowledgeRoute => {
    if (onKnowledgeRoute) {
      knowledgeExpanded.value = true
    }
  },
  { immediate: true }
)

watch(
  canLoadCategories,
  allowed => {
    if (allowed && categories.value.length === 0) {
      loadCategories()
    }
  }
)
</script>

<style scoped>
.layout-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f0f2f5;
}

.header {
  height: 60px;
  background: linear-gradient(135deg, #1e88e5 0%, #1976d2 100%);
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 4px 12px rgba(30, 136, 229, 0.25);
  z-index: 1002;
}

.system-logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  font-size: 28px;
}

.system-logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

.user-info-container {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.3) 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  cursor: pointer;
}

.user-welcome {
  font-size: 14px;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

.user-name {
  font-weight: 600;
}

.main-wrapper {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.sidebar {
  width: 250px;
  background: linear-gradient(180deg, #ffffff 0%, #f0f4f8 100%);
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.1);
  border-right: 1px solid #e2e8f0;
  overflow-y: auto;
  padding: 20px 0;
}

.side-menu-title {
  font-size: 14px;
  color: #1e88e5;
  padding: 15px 20px 10px 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-section {
  margin-bottom: 15px;
}

.menu-group {
  margin-bottom: 8px;
  transition:
    transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    margin-bottom 0.3s ease;
}
.menu-group.is-expanded:hover,
.menu-group.is-expanded.is-active {
  transform: translateX(4px) translateY(-1px);
}
.menu-group.is-expanded .menu-item-expandable:hover {
  transform: none;
}
.menu-group.is-expanded {
  margin-bottom: 18px;
}

.menu-group.is-expanded::after {
  content: '';
  display: block;
  margin: 12px 18px 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, #cbd5e1 15%, #94a3b8 50%, #cbd5e1 85%, transparent);
}

.menu-item-expandable {
  cursor: pointer;
}

.menu-group.is-expanded .menu-item-expandable {
  margin-bottom: 0;
  border: 1px solid #bfdbfe;
  border-bottom: none;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  box-shadow: none;
}

.menu-expand-icon {
  margin-left: auto;
  font-size: 14px;
  transition: transform 0.45s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.menu-item.expanded .menu-expand-icon {
  transform: rotate(180deg);
}

.submenu-slide-enter-active,
.submenu-slide-leave-active {
  overflow: hidden;
  transition:
    max-height 0.42s cubic-bezier(0.34, 1.15, 0.64, 1),
    opacity 0.32s ease,
    transform 0.42s cubic-bezier(0.34, 1.15, 0.64, 1),
    padding 0.42s cubic-bezier(0.34, 1.15, 0.64, 1);
}

.submenu-slide-enter-from,
.submenu-slide-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-6px);
  padding-top: 0;
  padding-bottom: 0;
}

.submenu-slide-enter-to,
.submenu-slide-leave-from {
  max-height: 480px;
  opacity: 1;
  transform: translateY(0);
}

.submenu {
  margin: 0 10px 8px 10px;
  padding: 0;
  box-sizing: border-box;
  overflow: hidden;
}

.menu-group.is-expanded .submenu {
  padding: 0;
  background: linear-gradient(180deg, #f8fbff 0%, #eff6ff 100%);
  border: 1px solid #bfdbfe;
  border-top: none;
  border-radius: 0 0 12px 12px;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.12);
}

.submenu-item {
  display: block;
  width: 100%;
  height: 40px;
  box-sizing: border-box;
  text-align: left;
  padding: 0 20px;
  margin: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  line-height: 40px;
  cursor: pointer;
  transition:
    background-color 0.25s ease,
    color 0.25s ease,
    opacity 0.3s ease,
    transform 0.35s cubic-bezier(0.34, 1.15, 0.64, 1);
}

.submenu-slide-enter-from .submenu-item,
.submenu-slide-leave-to .submenu-item {
  opacity: 0;
  transform: translateY(-4px);
}

.submenu-item:hover:not(:disabled) {
  background: rgba(219, 234, 254, 0.85);
  color: #1d4ed8;
}

.submenu-item.active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #ffffff;
  font-weight: 600;
  box-shadow: none;
}

.submenu-item-muted {
  cursor: default;
  opacity: 0.7;
}

.submenu-group {
  margin: 0;
}

.submenu-folder {
  display: flex;
  align-items: center;
  padding: 0;
  color: #1e40af;
  font-weight: 600;
}

.folder-main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 4px 0 20px;
  border: none;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.folder-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 40px;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.folder-toggle:hover {
  background: rgba(219, 234, 254, 0.85);
}

.folder-main:focus-visible,
.folder-toggle:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: -2px;
}

.submenu-folder.group-active {
  background: rgba(219, 234, 254, 0.75);
  color: #1d4ed8;
}

.folder-icon {
  font-size: 15px;
}

.folder-arrow {
  margin-left: 0;
  font-size: 12px;
  transition: transform 0.2s ease;
}

.submenu-folder.expanded .folder-arrow {
  transform: rotate(180deg);
}

.submenu-children {
  margin-left: 16px;
  padding-left: 10px;
  border-left: 1px dashed #bfdbfe;
  overflow: hidden;
}

.submenu-child {
  padding-left: 16px;
  font-size: 12.5px;
}

.submenu-empty {
  padding: 8px 18px;
  color: #94a3b8;
  font-size: 12px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 22px;
  color: #374151;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 14px;
  font-weight: 500;
  border-radius: 12px;
  margin: 0 10px 8px 10px;
  border: 1px solid transparent;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(249, 250, 251, 0.8) 100%);
  backdrop-filter: blur(10px);
}

.menu-item:hover {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
  transform: translateX(4px) translateY(-1px);
  border-color: #93c5fd;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
}

.menu-item.active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
  border-color: #3b82f6;
}

.main-content {
  flex: 1;
  background-color: #f8fafc;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
}
</style>
