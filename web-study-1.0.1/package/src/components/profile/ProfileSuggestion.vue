<template>
  <div class="profile-suggestion">
    <div
      v-if="nextAction"
      class="next-action"
    >
      <span class="next-label">下一步</span>

      <strong>{{ nextAction.label }}</strong>
    </div>

    <div
      v-if="weakPoints.length > 0"
      class="weak-block"
    >
      <span class="block-title">优先补强</span>

      <div class="weak-list">
        <div
          v-for="point in weakPoints"
          :key="point.knowledgePointId"
          class="weak-item"
        >
          <div class="weak-head">
            <span class="weak-name">
              {{ point.knowledgePointName }}
            </span>

            <el-tag
              :type="riskTagType(point.riskLevel)"
              effect="light"
              size="small"
            >
              {{ riskText(point.riskLevel) }}
            </el-tag>

            <span class="weak-score">
              {{ point.masteryScore }} 分
            </span>
          </div>

          <p>{{ point.action }}</p>
        </div>
      </div>
    </div>

    <div
      v-else-if="continueItem"
      class="continue-block"
    >
      <span class="block-title">继续学习</span>

      <p>{{ continueItem.title }}</p>
    </div>

    <div
      v-else
      class="all-good"
    >
      当前没有明显薄弱项，按学习计划继续即可。
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // { weakPoints, continueItem, nextAction }
  recommendation: {
    type: Object,
    default: () => ({
      weakPoints: [],
      continueItem: null,
      nextAction: null
    })
  }
})

const weakPoints = computed(
  () => props.recommendation?.weakPoints || []
)

const continueItem = computed(
  () => props.recommendation?.continueItem || null
)

const nextAction = computed(
  () => props.recommendation?.nextAction || null
)

const riskText = risk => {
  const map = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险'
  }

  return map[risk] || '待评估'
}

const riskTagType = risk => {
  if (risk === 'HIGH') {
    return 'danger'
  }

  if (risk === 'MEDIUM') {
    return 'warning'
  }

  return 'success'
}
</script>

<style scoped>
.profile-suggestion {
  border-top: 1px dashed #e2e8f0;
  padding-top: 14px;
}

.next-action {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1e40af;
  font-size: 13px;
}

.next-label {
  flex: none;
  padding: 2px 8px;
  border-radius: 999px;
  background: #2563eb;
  color: #ffffff;
  font-size: 11px;
}

.block-title {
  display: block;
  margin-bottom: 8px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.weak-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.weak-item {
  padding: 10px 12px;
  border: 1px solid #fee2e2;
  border-radius: 10px;
  background: #fffafa;
}

.weak-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.weak-name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.weak-score {
  margin-left: auto;
  color: #ef4444;
  font-size: 12px;
}

.weak-item p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
}

.continue-block p {
  margin: 0;
  color: #475569;
  font-size: 13px;
}

.all-good {
  padding: 10px 12px;
  border-radius: 10px;
  background: #f0fdf4;
  color: #15803d;
  font-size: 13px;
}
</style>
