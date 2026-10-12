import { defineStore } from 'pinia'
import { useAuthStore } from './auth'
import { getFavorites, addFavorite, removeFavorite } from '@/api/favorites'

/** 收藏状态管理（沿用原模块文件名）；仅缓存后端结果，账号切换使旧请求失效。 */
export const useFavoritesStore = defineStore('favorites', {
  state: () => ({ rows: [], loading: false, owner: null, epoch: 0, pending: [] }),
  getters: {
    isFavorited: state => id => state.rows.some(row => String(row.itemPk) === String(id))
  },
  actions: {
    reset() { this.epoch++; this.owner = null; this.rows = []; this.pending = []; this.loading = false },
    async load() {
      const auth = useAuthStore()
      const user = auth.user?.userId ?? auth.user?.id
      if (!auth.isAuthenticated || !user) { this.reset(); return }
      if (String(this.owner) !== String(user)) this.reset()
      this.owner = user
      const epoch = ++this.epoch
      this.loading = true
      try { const rows = await getFavorites(); if (epoch === this.epoch) this.rows = Array.isArray(rows) ? rows : [] }
      finally { if (epoch === this.epoch) this.loading = false }
    },
    async toggle(id) {
      if (this.pending.includes(String(id))) return
      const owner = this.owner, epoch = this.epoch
      this.pending.push(String(id))
      try {
        if (this.isFavorited(id)) await removeFavorite(id)
        else await addFavorite(id)
        if (owner === this.owner) await this.load()
      } finally { this.pending = this.pending.filter(value => value !== String(id)) }
    }
  }
})
