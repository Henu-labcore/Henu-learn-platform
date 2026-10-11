import request from './request'
export const getLearningPlanNotifications = () => request({url:'/learning-plans/notifications',method:'get'})
export const readLearningPlanNotification = id => request({url:`/learning-plans/notifications/${id}/read`,method:'post'})
export const readAllLearningPlanNotifications = () => request({url:'/learning-plans/notifications/read-all',method:'post'})
export const getLearningPlanReminderSetting = () => request({url:'/learning-plans/reminder-setting',method:'get'})
export const saveLearningPlanReminderSetting = data => request({url:'/learning-plans/reminder-setting',method:'put',data})
// 追加反馈子资源，不改变原计划 API。
export const getLearningPlanFeedback = planId => request({url:`/learning-plans/${planId}/feedback`,method:'get'})
export const saveLearningPlanFeedback = (planId,itemPk,data) => request({url:`/learning-plans/${planId}/feedback/${itemPk}`,method:'put',data})

export const getCurrentLearningPlan = () => request({ url: '/learning-plans/current', method: 'get' })
export const getLearningPlanHistory = () => request({ url: '/learning-plans/history', method: 'get' })
export const getLearningPlan = planId => request({ url: `/learning-plans/${planId}`, method: 'get' })
export const createLearningPlan = data => request({ url: '/learning-plans', method: 'post', data })
export const updateLearningPlan = (planId, data) => request({ url: `/learning-plans/${planId}`, method: 'put', data })
export const cancelLearningPlan = planId => request({ url: `/learning-plans/${planId}/cancel`, method: 'post' })
export const getAdminLearningPlanRisks = params => request({ url: '/admin/learning-plan-risks', method: 'get', params })
