<template>
  <section v-if="plan" class="plan-summary" :class="`risk-${plan.riskLevel.toLowerCase()}`">
    <div class="plan-copy">
      <div class="eyebrow">当前学习计划</div>
      <div class="plan-title-row">
        <h2>{{ plan.planName }}</h2>
        <el-tag :type="riskMeta.type" effect="dark">{{ riskMeta.label }}</el-tag>
      </div>
      <p>{{ plan.startDate }} 至 {{ plan.endDate }} · 已完成 {{ plan.completedCount }}/{{ plan.totalCount }}</p>
    </div>
    <div class="plan-progress">
      <strong>{{ plan.progressRate }}%</strong>
      <el-progress :percentage="plan.progressRate" :show-text="false" :stroke-width="10" />
      <span v-if="warningText">{{ warningText }}</span>
      <span v-else>计划进度正常，继续保持当前节奏。</span>
    </div>
    <el-button type="primary" @click="$router.push('/learning-plan')">查看计划</el-button>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCurrentLearningPlan } from '@/api/learningPlans'

const plan = ref(null)
const riskMap = {
  OVERDUE: { label: '存在逾期', type: 'danger' },
  DUE_SOON: { label: '即将到期', type: 'warning' },
  STALLED: { label: '学习停滞', type: 'warning' },
  NORMAL: { label: '进度正常', type: 'success' }
}
const riskMeta = computed(() => riskMap[plan.value?.riskLevel] || riskMap.NORMAL)
const warningText = computed(() => {
  if (!plan.value) return ''
  const values = []
  if (plan.value.overdueCount) values.push(`${plan.value.overdueCount} 项逾期`)
  if (plan.value.dueSoonCount) values.push(`${plan.value.dueSoonCount} 项临期`)
  if (plan.value.stalledCount) values.push(`${plan.value.stalledCount} 项停滞`)
  return values.join('，')
})

onMounted(async () => {
  try {
    const current = await getCurrentLearningPlan()
    plan.value = current?.planId && current.status === 'ACTIVE' ? current : null
  } catch (error) {
    console.error('学习计划摘要加载失败', error)
  }
})
</script>

<style scoped>
.plan-summary { display:grid; grid-template-columns:1fr minmax(260px, 360px) auto; gap:28px; align-items:center; padding:22px 24px; border:1px solid #bfdbfe; border-left:5px solid #2563eb; border-radius:16px; background:#f8fbff; }
.plan-title-row { display:flex; align-items:center; gap:12px; margin:4px 0 8px; }
.plan-title-row h2 { margin:0; color:#0f172a; font-size:20px; }
.eyebrow { color:#2563eb; font-size:12px; font-weight:700; letter-spacing:.08em; }
.plan-copy p, .plan-progress span { margin:0; color:#64748b; font-size:13px; }
.plan-progress { display:flex; flex-direction:column; gap:8px; }
.plan-progress strong { color:#0f172a; font-size:24px; }
.risk-overdue { border-left-color:#dc2626; background:#fff9f9; }
.risk-due_soon, .risk-stalled { border-left-color:#d97706; background:#fffbeb; }
@media (max-width:900px) { .plan-summary { grid-template-columns:1fr; gap:16px; } }
</style>
