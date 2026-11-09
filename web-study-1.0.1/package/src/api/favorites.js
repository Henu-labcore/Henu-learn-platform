import request from './request'

// 收藏接口沿用原鉴权封装，不修改知识学习 API。
export const getFavorites = params => request({ url: '/favorites', method: 'get', params })
export const getFavoriteMeta = () => request({ url: '/favorites/meta', method: 'get' })
export const addFavorite = itemPk => request({ url: '/favorites', method: 'post', data: { itemPk } })
export const removeFavorite = itemPk => request({ url: `/favorites/${itemPk}`, method: 'delete' })
export const updateFavorite = (itemPk, data) => request({ url: `/favorites/${itemPk}`, method: 'put', data })
export const importFavorites = itemIds => request({ url: '/favorites/import', method: 'post', data: { confirmed: true, itemIds } })
export const addFavoriteCategory = name => request({ url: '/favorites/categories', method: 'post', data: { name } })
export const renameFavoriteCategory = (id, name) => request({ url: `/favorites/categories/${id}`, method: 'put', data: { name } })
export const deleteFavoriteCategory = id => request({ url: `/favorites/categories/${id}`, method: 'delete' })
