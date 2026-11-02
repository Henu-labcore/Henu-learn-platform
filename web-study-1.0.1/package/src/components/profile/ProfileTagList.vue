<template>
  <div class="profile-tags">
    <div class="tags-head">
      <span>画像标签</span>

      <small v-if="tags.length === 0">
        暂无标签
      </small>
    </div>

    <div class="tags-wrap">
      <el-tooltip
        v-for="tag in tags"
        :key="tag.code"
        :content="tag.evidence"
        placement="top"
      >
        <el-tag
          :type="tagType(tag.polarity)"
          effect="light"
          round
        >
          {{ tag.studentLabel || tag.label }}
        </el-tag>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
defineProps({
  // [{ code, label, studentLabel, polarity, evidence }]
  tags: {
    type: Array,
    default: () => []
  }
})

const tagType = polarity => {
  if (polarity === 'POSITIVE') {
    return 'success'
  }

  if (polarity === 'ATTENTION') {
    return 'warning'
  }

  return 'info'
}
</script>

<style scoped>
.profile-tags {
  margin-bottom: 16px;
}

.tags-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.tags-head small {
  color: #94a3b8;
  font-weight: 400;
}

.tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tags-wrap :deep(.el-tag) {
  cursor: help;
}
</style>
