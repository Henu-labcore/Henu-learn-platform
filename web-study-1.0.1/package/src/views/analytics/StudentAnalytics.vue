<template>
  <div class="analytics-page">
    <nav class="analytics-tabs" aria-label="学情分析视图">
      <router-link
        v-for="tab in tabs"
        :key="tab.path"
        :to="tab.path"
        class="analytics-tab"
        :class="{ 'is-active': isActive(tab.key) }"
      >
        {{ tab.label }}
      </router-link>
    </nav>

    <router-view />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const tabs = [
  { key: 'process', label: '学习过程', path: '/analytics/process' },
  { key: 'profile', label: '学生画像', path: '/analytics/profile' },
  { key: 'plan', label: '计划执行', path: '/analytics/plan' }
]

const isActive = key => {
  return route.path.startsWith(`/analytics/${key}`)
}
</script>

<style scoped>
.analytics-page {
  min-height: 100%;
  background: #f1f5f9;
}

.analytics-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 24px 0;
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 5;
}

.analytics-tab {
  padding: 10px 18px;
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: 10px 10px 0 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.analytics-tab:hover {
  color: #1d4ed8;
  background: #f8fafc;
}

.analytics-tab.is-active {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #dbeafe;
}
</style>
