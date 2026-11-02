<template>
  <div class="my-learning-page">
    <div class="my-learning-container">
      <header class="my-learning-header">
        <div>
          <h1>我的学习</h1>

          <p>
            学习数据与能力画像分开看，更清楚
          </p>
        </div>

        <el-button
          plain
          @click="goKnowledge"
        >
          返回知识学习
        </el-button>
      </header>

      <nav
        class="learning-tabs"
        aria-label="我的学习视图"
      >
        <router-link
          to="/my-learning/process"
          class="learning-tab"
          :class="{ 'is-active': isProcess }"
        >
          学习数据
        </router-link>

        <router-link
          v-if="!authStore.isAdmin"
          to="/my-learning/profile"
          class="learning-tab"
          :class="{ 'is-active': isProfile }"
        >
          能力画像
        </router-link>
      </nav>

      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'

import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isProcess = computed(() => {
  return route.name === 'MyLearningProcess'
})

const isProfile = computed(() => {
  return route.name === 'MyLearningProfile'
})

const goKnowledge = () => {
  router.push('/knowledge').catch(() => {})
}

// 管理员没有学生个人画像，误入时退回学习数据
const ensureStudentTab = () => {
  if (
    authStore.isAdmin
    && route.name === 'MyLearningProfile'
  ) {
    router.replace('/my-learning/process').catch(() => {})
  }
}

onMounted(ensureStudentTab)

watch(
  () => route.name,
  ensureStudentTab
)
</script>

<style scoped>
.my-learning-page {
  min-height: 100%;
  padding: 24px 28px 40px;
  background: #f8fafc;
}

.my-learning-container {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.my-learning-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.my-learning-header h1 {
  margin: 0;
  color: #0f172a;
  font-size: 26px;
}

.my-learning-header p {
  margin: 7px 0 0;
  color: #64748b;
  font-size: 13px;
}

.learning-tabs {
  width: fit-content;
  display: flex;
  gap: 6px;
  margin: 18px 0 20px;
  padding: 5px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.learning-tab {
  padding: 8px 22px;
  border-radius: 8px;
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.learning-tab:hover {
  color: #1d4ed8;
  background: #f8fafc;
}

.learning-tab.is-active {
  color: #1d4ed8;
  background: #eff6ff;
}

@media (max-width: 900px) {
  .my-learning-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .learning-tabs {
    width: 100%;
  }

  .learning-tab {
    flex: 1;
    text-align: center;
  }
}
</style>
