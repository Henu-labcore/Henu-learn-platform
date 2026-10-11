<template>
  <div class="knowledge-content">
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="40"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
    <div v-else class="knowledge-cards">
      <div
        v-for="item in items"
        :key="item.id"
        class="knowledge-card"
        :data-id="item.id"
      >
        <button
          class="knowledge-more"
          type="button"
          aria-label="查看详情"
          @click.stop="emit('show-details', item.id)"
        >
          <el-icon><MoreFilled /></el-icon>
        </button>
        <!-- 收藏固定在卡片上，不放入会旋转/缩放的 knowledge-icon 中。 -->
        <button class="favorite-toggle" type="button" :aria-label="favoritesStore.isFavorited(item.id) ? '取消收藏' : '收藏'" :aria-pressed="favoritesStore.isFavorited(item.id)" :disabled="favoritesStore.loading || favoritesStore.pending.includes(String(item.id))" @click.stop="toggleFavorite(item.id)">
          <el-icon :size="21"><StarFilled v-if="favoritesStore.isFavorited(item.id)"/><Star v-else/></el-icon>
        </button>
        <div class="knowledge-icon">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <h3 class="knowledge-title">{{ item.title }}</h3>
        <p class="knowledge-description">{{ item.summary }}</p>
        <div class="knowledge-actions">
          <!-- 先学习，跳转到详情/学习页 -->
          <button class="btn-primary" @click="emit('open-details', item.id)">
            开始学习
          </button>

          <!-- 再练习，进入在线编辑/实验（触发 start-learning 以打开实验覆盖层/编辑器） -->
          <button class="btn-outline" @click="emit('start-learning', item.id)">
            开始练习
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Loading, Document, MoreFilled, Star, StarFilled } from '@element-plus/icons-vue'
import { useFavoritesStore } from '@/stores/studyStore'
const favoritesStore = useFavoritesStore()
const toggleFavorite = async id => { try { await favoritesStore.toggle(id) } catch { /* 原请求层统一提示失败，不伪造收藏成功。 */ } }

defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  items: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['show-details', 'open-details', 'start-learning'])
</script>

<style scoped>
.favorite-toggle {
  position: absolute;
  right: 48px;
  top: 12px;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #d97706;
  cursor: pointer;
  padding: 0;
  /* 空心/实心和请求中的状态仅改变图案/透明度，不改变位置和尺寸。 */
  transform: none;
  transition: none;
}
.favorite-toggle:disabled { opacity: .5; cursor: wait; }
.knowledge-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-top: 0;
}

@media (max-width: 1400px) {
  .knowledge-cards {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1024px) {
  .knowledge-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .knowledge-cards {
    grid-template-columns: 1fr;
  }
}

.knowledge-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 24px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.knowledge-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1E88E5 0%, #1565C0 100%);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.knowledge-card:hover {
  /* 卡片不再上移，确保点击收藏时星标的屏幕位置固定；保留原阴影和图标效果。 */
  transform: none;
  box-shadow: 0 12px 24px rgba(30, 136, 229, 0.15);
  border-color: #1E88E5;
}

.knowledge-card:hover::before {
  transform: scaleX(1);
}

.knowledge-more {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #94a3b8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.knowledge-more:hover {
  color: #1E88E5;
  border-color: #bfdbfe;
  box-shadow: 0 6px 12px rgba(15, 23, 42, 0.08);
}

.knowledge-more :deep(svg) {
  width: 16px;
  height: 16px;
}

.knowledge-icon {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #1E88E5 0%, #1565C0 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(30, 136, 229, 0.3);
  transition: all 0.3s ease;
}

.knowledge-card:hover .knowledge-icon {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 16px rgba(30, 136, 229, 0.4);
}

.knowledge-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.knowledge-description {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  margin-bottom: 20px;
  flex: 0 0 42px;
  max-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-word;
}

.knowledge-actions {
  display: flex;
  gap: 12px;
}

.btn-primary {
  padding: 8px 16px;
  background: #1E88E5;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  background: #1565C0;
}

.btn-outline {
  padding: 8px 16px;
  background: #ffffff;
  color: #1E88E5;
  border: 1px solid #1E88E5;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-outline:hover {
  background: #1E88E5;
  color: #ffffff;
}

.loading-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 400px;
  color: #1E88E5;
}
</style>
