<template>
  <el-badge :value="unread" :hidden="!unread" :max="99"><el-button text class="notification-button" @click="open">学习提醒</el-button></el-badge>
  <el-drawer v-model="visible" title="学习计划提醒" size="420px" append-to-body>
    <el-alert title="站内提醒：需登录平台查看。关闭网页后不会发送系统弹窗、短信或微信。" type="info" :closable="false"/>
    <section class="settings"><h3>截止提醒设置</h3><el-switch v-model="setting.enabled" active-text="启用截止提醒"/><div>提前 <el-input-number v-model="setting.dueDays" :min="1" :max="7" size="small"/> 天提醒</div><el-button type="primary" size="small" :loading="saving" @click="save">保存设置</el-button></section>
    <div class="notification-actions"><el-button size="small" @click="load">刷新</el-button><el-button size="small" @click="readAll">全部已读</el-button></div>
    <el-empty v-if="!rows.length" description="暂无提醒"/>
    <article v-for="row in rows" :key="row.id" class="notification-row" :class="{'is-unread':!row.isRead}"><h4>{{ row.title }}</h4><p>{{ row.message }}</p><small>{{ row.createdAt }}</small><div><el-button text type="primary" @click="go(row)">查看计划</el-button><el-button v-if="!row.isRead" text @click="read(row.id)">标记已读</el-button></div></article>
  </el-drawer>
</template>
<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { getLearningPlanNotifications,readLearningPlanNotification,readAllLearningPlanNotifications,getLearningPlanReminderSetting,saveLearningPlanReminderSetting } from '@/api/learningPlans'
const auth=useAuthStore(),router=useRouter(),rows=ref([]),visible=ref(false),saving=ref(false),setting=reactive({enabled:true,dueDays:1})
const unread=computed(()=>rows.value.filter(r=>!r.isRead).length)
let timer,epoch=0
const load=async()=>{if(!auth.isAuthenticated)return;const version=++epoch;try{const data=await getLearningPlanNotifications();if(version===epoch)rows.value=(data||[]).map(r=>({...r,isRead:!!r.isRead}))}catch{/* 失败不影响原页面。 */}}
const open=async()=>{visible.value=true;await load();try{Object.assign(setting,await getLearningPlanReminderSetting())}catch{}}
const save=async()=>{saving.value=true;try{await saveLearningPlanReminderSetting({...setting});ElMessage.success('提醒设置已同步');await load()}catch{}finally{saving.value=false}}
const read=async id=>{try{await readLearningPlanNotification(id);await load()}catch{}}
const readAll=async()=>{try{await readAllLearningPlanNotifications();await load()}catch{}}
const go=async row=>{await read(row.id);visible.value=false;router.push({path:'/learning-plan',query:{planId:row.planId}})}
watch(()=>[auth.isAuthenticated,auth.user?.userId??auth.user?.id],()=>{epoch++;rows.value=[];visible.value=false;load()})
onMounted(()=>{load();timer=setInterval(load,60000)})
onUnmounted(()=>{epoch++;clearInterval(timer)})
</script>
<style scoped>
.notification-button{color:inherit}.settings{background:#f8fafc;padding:16px;border-radius:10px;margin:20px 0}.settings>div{margin:12px 0}.notification-actions{display:flex;gap:8px}.notification-row{border:1px solid #e2e8f0;border-radius:10px;padding:14px;margin-top:12px}.notification-row.is-unread{border-color:#93c5fd;background:#eff6ff}.notification-row h4{margin:0}.notification-row p{color:#475569;line-height:1.7}.notification-row small{color:#94a3b8}
</style>
