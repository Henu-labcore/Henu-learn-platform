<template>
  <div class="plan-page" v-loading="loading">
    <header class="page-head">
      <div>
        <span class="eyebrow">LEARNING ROUTE</span>
        <h1>学习计划</h1>
        <p>把学习目标拆成可执行的节点，系统会同步进度并提示临期、逾期和停滞风险。</p>
      </div>
      <el-button v-if="!currentPlan" type="primary" size="large" @click="openCreate">创建学习计划</el-button>
      <el-button v-else type="primary" plain @click="openEdit">调整计划</el-button>
    </header>
    <el-button text @click="loadData">刷新进度 / 跨设备同步</el-button>

    <section v-if="currentPlan" class="workspace" :class="`risk-${currentPlan.riskLevel.toLowerCase()}`">
      <div class="workspace-overview">
        <div class="plan-heading">
          <div>
            <span class="status-label">进行中</span>
            <h2>{{ currentPlan.planName }}</h2>
            <p>{{ currentPlan.startDate }} — {{ currentPlan.endDate }}</p>
          </div>
          <div class="risk-badge">
            <span>当前风险</span>
            <strong>{{ riskMeta(currentPlan.riskLevel).label }}</strong>
          </div>
        </div>
        <div class="progress-block">
          <div class="progress-number">{{ currentPlan.progressRate }}<small>%</small></div>
          <div class="progress-copy">
            <div>完成 {{ currentPlan.completedCount }} / {{ currentPlan.totalCount }} 项</div>
            <el-progress :percentage="currentPlan.progressRate" :show-text="false" :stroke-width="12" />
          </div>
        </div>
        <div class="signal-row">
          <div><strong>{{ currentPlan.overdueCount }}</strong><span>逾期</span></div>
          <div><strong>{{ currentPlan.dueSoonCount }}</strong><span>临期</span></div>
          <div><strong>{{ currentPlan.stalledCount }}</strong><span>停滞</span></div>
          <div><strong>{{ currentPlan.totalCount - currentPlan.completedCount }}</strong><span>待完成</span></div>
        </div>
      </div>

      <div class="timeline-panel">
        <div class="section-title">
          <div><h2>执行路径</h2><p>目标日期与实际学习状态实时对照</p></div>
          <el-button text type="danger" @click="handleCancel">取消计划</el-button>
        </div>
        <div class="timeline">
          <article v-for="(item, index) in currentPlan.items" :key="item.planItemId" class="timeline-item">
            <div class="rail"><span :class="{ done: item.completed }">{{ item.completed ? '✓' : index + 1 }}</span></div>
            <div class="item-main">
              <div class="item-title">
                <h3>{{ item.title }}</h3>
                <el-tag :type="riskMeta(item.riskLevel).type" size="small">{{ item.completed ? '已完成' : riskMeta(item.riskLevel).label }}</el-tag>
              </div>
              <div class="item-meta">
                <span>{{ item.category || '未分类' }}</span><span>{{ item.difficulty || '未标注难度' }}</span><span>目标 {{ item.targetDate }}</span>
              </div>
              <div class="item-progress">已学习 {{ formatDuration(item.learnDurationSec) }}</div>
              <LearningPlanFeedback :plan-id="currentPlan.planId" :item-pk="item.itemPk" :completed="item.completed"/>
            </div>
            <el-button type="primary" plain :disabled="item.available === false" @click="goLearning(item.itemPk)">{{ item.available === false ? '内容不可用' : item.completed ? '复习' : '开始学习' }}</el-button>
          </article>
        </div>
      </div>
    </section>

    <section v-else class="empty-state">
      <div class="empty-index">01</div>
      <div><h2>从一个清晰的学习周期开始</h2><p>选择 1—50 个已发布学习条目，为每项设置目标日期。完成情况会自动同步。</p></div>
      <el-button type="primary" size="large" @click="openCreate">创建第一个计划</el-button>
    </section>

    <section v-if="history.length" class="history-section">
      <div class="section-title"><div><h2>历史计划</h2><p>已完成和已取消的计划会保留，可查看完成反馈</p></div></div>
      <el-collapse>
        <el-collapse-item v-for="plan in history" :key="plan.planId" :name="plan.planId">
          <template #title><span class="history-title">{{ plan.planName }}</span><el-tag :type="plan.status === 'COMPLETED' ? 'success' : 'info'" size="small">{{ plan.status === 'COMPLETED' ? '已完成' : '已取消' }}</el-tag><span class="history-rate">{{ plan.progressRate }}%</span></template>
          <div class="history-items"><span v-for="item in plan.items" :key="item.planItemId">{{ item.title }} · {{ item.completed ? '已完成' : '未完成' }} <LearningPlanFeedback :plan-id="plan.planId" :item-pk="item.itemPk" :completed="item.completed"/></span></div>
          <LearningPlanFeedback :plan-id="plan.planId" :completed="plan.status === 'COMPLETED'"/>
        </el-collapse-item>
      </el-collapse>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '调整学习计划' : '创建学习计划'" width="760px" destroy-on-close>
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="计划名称"><el-input v-model="form.planName" maxlength="100" show-word-limit placeholder="例如：密码学两周强化计划" /></el-form-item>
          <el-form-item label="计划周期">
            <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width:100%" />
          </el-form-item>
        </div>
        <div class="form-items-head"><div><strong>学习条目</strong><span>目标日期必须在计划周期内</span></div><el-button type="primary" text @click="addItem">添加条目</el-button></div>
        <!-- 沿用原学习完成状态，创建前明确说明，不重置已完成学习记录。 -->
        <el-alert v-if="!editingId" :type="allSelectedCompleted ? 'warning' : 'info'" :closable="false" show-icon class="completion-hint">
          <template #title>{{ allSelectedCompleted ? '所选条目均已完成：请至少加入一项未完成内容，否则不能创建新计划。' : `原学习完成状态会计入计划：当前已选 ${selectedItemCount} 项，其中 ${selectedCompletedCount} 项已完成、${selectedItemCount - selectedCompletedCount} 项待完成。` }}</template>
        </el-alert>
        <div class="form-items">
          <div v-for="(item, index) in form.items" :key="index" class="form-item-row">
            <span class="order">{{ index + 1 }}</span>
            <el-select v-model="item.itemPk" filterable placeholder="选择学习条目" style="flex:1">
              <el-option v-for="option in availableOptions(index)" :key="option.itemPk" :label="`${option.title}${completedItemIds.has(Number(option.itemPk)) ? '（已学完，计入完成）' : '（待完成）'}`" :value="option.itemPk" />
            </el-select>
            <el-date-picker v-model="item.targetDate" type="date" value-format="YYYY-MM-DD" placeholder="目标日期" style="width:150px" />
            <div class="row-actions"><el-button circle text :disabled="index===0" @click="moveItem(index,-1)">↑</el-button><el-button circle text :disabled="index===form.items.length-1" @click="moveItem(index,1)">↓</el-button><el-button circle text type="danger" :disabled="form.items.length===1" @click="removeItem(index)">×</el-button></div>
          </div>
        </div>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitPlan">保存计划</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import LearningPlanFeedback from '@/components/knowledge/LearningPlanFeedback.vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLearningItems, getUserLearningProgress } from '@/api/knowledge'
import { cancelLearningPlan, createLearningPlan, getCurrentLearningPlan, getLearningPlanHistory, updateLearningPlan } from '@/api/learningPlans'

const router = useRouter()
const loading = ref(false), saving = ref(false), dialogVisible = ref(false), editingId = ref(null)
const currentPlan = ref(null), history = ref([]), learningItems = ref([]), dateRange = ref([])
const completedItemIds = ref(new Set())
const form = reactive({ planName: '', items: [] })
const selectedItemCount = computed(() => form.items.filter(item => item.itemPk).length)
const selectedCompletedCount = computed(() => form.items.filter(item => item.itemPk && completedItemIds.value.has(Number(item.itemPk))).length)
const allSelectedCompleted = computed(() => selectedItemCount.value > 0 && selectedItemCount.value === selectedCompletedCount.value)
const riskMap = { OVERDUE:{label:'逾期',type:'danger'}, DUE_SOON:{label:'临期',type:'warning'}, STALLED:{label:'停滞',type:'warning'}, NORMAL:{label:'正常',type:'success'} }
const riskMeta = value => riskMap[value] || riskMap.NORMAL

watch(dateRange, value => {
  if (value?.length === 2) form.items.forEach(item => { if (!item.targetDate) item.targetDate = value[1] })
})

const loadData = async () => {
  loading.value = true
  try {
    // 先同步当前计划，再取历史，避免并发查询遗漏刚完成的计划。
    const current = await getCurrentLearningPlan()
    const [historyData, items, progress] = await Promise.all([getLearningPlanHistory(), getLearningItems(), getUserLearningProgress()])
    currentPlan.value = current?.planId && current.status === 'ACTIVE' ? current : null
    history.value = Array.isArray(historyData) ? historyData : []
    learningItems.value = Array.isArray(items) ? items.map(item => ({ ...item, itemPk: item.itemPk ?? item.id })) : []
    completedItemIds.value = new Set((Array.isArray(progress) ? progress : []).filter(item => item.completeTime).map(item => Number(item.itemPk)))
  } finally { loading.value = false }
}
const resetForm = () => { form.planName=''; form.items=[{itemPk:null,targetDate:''}]; dateRange.value=[dayjs().format('YYYY-MM-DD'),dayjs().add(30,'day').format('YYYY-MM-DD')]; editingId.value=null }
const openCreate = () => { resetForm(); dialogVisible.value=true }
const openEdit = () => { const p=currentPlan.value; editingId.value=p.planId; form.planName=p.planName; dateRange.value=[p.startDate,p.endDate]; form.items=p.items.map(i=>({itemPk:i.itemPk,targetDate:i.targetDate})); dialogVisible.value=true }
const addItem = () => { if(form.items.length<50) form.items.push({itemPk:null,targetDate:dateRange.value?.[1] || ''}) }
const removeItem = index => form.items.splice(index,1)
const moveItem = (index,delta) => { const target=index+delta; if(target<0||target>=form.items.length)return; const [row]=form.items.splice(index,1); form.items.splice(target,0,row) }
const availableOptions = index => { const selected=new Set(form.items.map((i,idx)=>idx===index?null:i.itemPk)); return learningItems.value.filter(i=>!selected.has(i.itemPk)) }
const submitPlan = async () => {
  if(!form.planName.trim() || dateRange.value?.length!==2 || form.items.some(i=>!i.itemPk||!i.targetDate)){ ElMessage.warning('请完整填写计划名称、周期和学习条目'); return }
  if (saving.value) return
  saving.value=true
  try {
    if (!editingId.value) {
      // 保存前重新读取后端状态，避免其他设备刚完成任务造成页面提示过期。
      const progress = await getUserLearningProgress()
      completedItemIds.value = new Set((Array.isArray(progress) ? progress : []).filter(item => item.completeTime).map(item => Number(item.itemPk)))
      if (allSelectedCompleted.value) { ElMessage.warning('所选条目均已完成，请至少加入一项未完成内容；已保存的原学习记录不会重置。'); return }
      if (selectedCompletedCount.value > 0) {
        try {
          await ElMessageBox.confirm(`所选条目中有 ${selectedCompletedCount.value} 项以前已学完，会直接计入计划完成进度；剩余 ${selectedItemCount.value - selectedCompletedCount.value} 项待完成。是否继续创建？`, '确认学习完成状态', { type: 'info', confirmButtonText: '继续创建', cancelButtonText: '返回调整' })
        } catch (action) { if (action === 'cancel' || action === 'close') return; throw action }
      }
    }
    const payload={version:editingId.value ? currentPlan.value?.version : undefined,planName:form.planName.trim(),startDate:dateRange.value[0],endDate:dateRange.value[1],items:form.items.map((i,index)=>({...i,sortOrder:index+1}))}
    if(editingId.value) await updateLearningPlan(editingId.value,payload); else await createLearningPlan(payload)
    ElMessage.success('学习计划已保存'); dialogVisible.value=false; await loadData()
  } finally { saving.value=false }
}
const handleCancel = async () => { try { await ElMessageBox.confirm('取消后计划会进入历史记录，确定继续吗？','取消学习计划',{type:'warning'}); await cancelLearningPlan(currentPlan.value.planId); ElMessage.success('计划已取消'); await loadData() } catch(error) { if(error!=='cancel') console.error(error) } }
const goLearning = itemPk => router.push(`/learning/item/${itemPk}`)
const formatDuration = sec => { const minutes=Math.floor((sec||0)/60); return minutes<60?`${minutes} 分钟`:`${Math.floor(minutes/60)} 小时 ${minutes%60} 分钟` }
onMounted(loadData)
</script>

<style scoped>
.completion-hint { margin-bottom: 14px; }
.plan-page{max-width:1180px;margin:0 auto;padding:30px 32px 56px;color:#0f172a}.page-head{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:28px}.page-head h1{font-size:30px;margin:4px 0 8px}.page-head p,.section-title p{margin:0;color:#64748b}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.13em;color:#2563eb}.workspace{display:grid;grid-template-columns:360px 1fr;border:1px solid #dbeafe;border-radius:20px;overflow:hidden;background:#fff;box-shadow:0 18px 45px rgba(15,23,42,.07)}.workspace-overview{padding:30px;background:#f8fbff;border-right:1px solid #dbeafe}.plan-heading{display:flex;justify-content:space-between;gap:16px}.plan-heading h2{font-size:22px;margin:8px 0}.plan-heading p{color:#64748b;margin:0}.status-label{font-size:12px;color:#2563eb;font-weight:700}.risk-badge{text-align:right}.risk-badge span{display:block;color:#94a3b8;font-size:12px}.risk-badge strong{display:block;margin-top:5px;color:#2563eb}.progress-block{display:flex;align-items:center;gap:18px;margin:34px 0}.progress-number{font-size:46px;font-weight:800;color:#1d4ed8}.progress-number small{font-size:18px}.progress-copy{flex:1;display:flex;flex-direction:column;gap:10px;color:#475569}.signal-row{display:grid;grid-template-columns:repeat(2,1fr);gap:1px;background:#dbeafe;border:1px solid #dbeafe}.signal-row div{display:flex;flex-direction:column;padding:16px;background:#fff}.signal-row strong{font-size:22px}.signal-row span{color:#64748b;font-size:12px;margin-top:4px}.timeline-panel{padding:30px}.section-title{display:flex;justify-content:space-between;align-items:center;margin-bottom:22px}.section-title h2{margin:0 0 5px;font-size:20px}.timeline{display:flex;flex-direction:column}.timeline-item{display:grid;grid-template-columns:40px 1fr auto;gap:16px;align-items:center;min-height:92px}.rail{height:100%;position:relative;display:flex;justify-content:center}.rail:after{content:'';position:absolute;top:48px;bottom:-44px;width:1px;background:#dbeafe}.timeline-item:last-child .rail:after{display:none}.rail span{z-index:1;width:30px;height:30px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:#eff6ff;color:#2563eb;font-weight:700;border:1px solid #bfdbfe}.rail span.done{background:#16a34a;color:#fff;border-color:#16a34a}.item-title{display:flex;align-items:center;gap:10px}.item-title h3{margin:0;font-size:16px}.item-meta{display:flex;gap:12px;color:#64748b;font-size:12px;margin-top:8px}.item-progress{font-size:12px;color:#94a3b8;margin-top:6px}.empty-state{display:grid;grid-template-columns:100px 1fr auto;gap:28px;align-items:center;padding:46px;border:1px dashed #93c5fd;border-radius:20px;background:#f8fbff}.empty-index{font-size:54px;font-weight:800;color:#bfdbfe}.empty-state h2{margin:0 0 8px}.empty-state p{margin:0;color:#64748b}.history-section{margin-top:32px}.history-title{font-weight:600;margin-right:12px}.history-rate{margin-left:auto;margin-right:16px;color:#64748b}.history-items{display:flex;flex-direction:column;gap:8px;color:#64748b}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px}.form-items-head{display:flex;justify-content:space-between;align-items:center;margin:4px 0 12px}.form-items-head div{display:flex;flex-direction:column;gap:4px}.form-items-head span{font-size:12px;color:#94a3b8}.form-items{display:flex;flex-direction:column;gap:10px;max-height:350px;overflow:auto}.form-item-row{display:flex;align-items:center;gap:10px}.order{width:28px;height:28px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:#eff6ff;color:#2563eb;font-weight:700}.row-actions{display:flex}.risk-overdue .workspace-overview{background:#fff7f7}.risk-overdue .risk-badge strong{color:#dc2626}.risk-due_soon .workspace-overview,.risk-stalled .workspace-overview{background:#fffbeb}.risk-due_soon .risk-badge strong,.risk-stalled .risk-badge strong{color:#d97706}@media(max-width:900px){.plan-page{padding:20px}.page-head,.empty-state{display:flex;flex-direction:column;align-items:flex-start;gap:18px}.workspace{grid-template-columns:1fr}.workspace-overview{border-right:0;border-bottom:1px solid #dbeafe}.form-grid{grid-template-columns:1fr}.form-item-row{flex-wrap:wrap}}
</style>
