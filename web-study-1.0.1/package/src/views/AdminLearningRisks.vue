<template>
  <div class="risk-page">
    <header class="page-head">
      <div><span class="eyebrow">TEACHING SIGNALS</span><h1>学习风险预警</h1><p>按计划期限与学习活跃度识别需要关注的学生。</p></div>
      <div class="rule-note"><strong>透明规则</strong><span>临期 3 天 · 停滞 7 天</span></div>
    </header>

    <section class="filter-bar">
      <el-input v-model="filters.keyword" clearable placeholder="搜索学生或计划" style="width:280px" @keyup.enter="search" />
      <el-select v-model="filters.riskLevel" clearable placeholder="全部风险" style="width:150px" @change="search">
        <el-option label="逾期" value="OVERDUE" /><el-option label="临期" value="DUE_SOON" /><el-option label="停滞" value="STALLED" /><el-option label="正常" value="NORMAL" />
      </el-select>
      <el-button type="primary" @click="search">筛选</el-button>
      <span class="result-count">共 {{ total }} 个进行中计划</span>
    </section>

    <section class="risk-table" v-loading="loading">
      <div class="table-head"><span>学生与计划</span><span>计划进度</span><span>风险信号</span><span>最近活动</span><span>结束日期</span></div>
      <article v-for="row in rows" :key="row.planId" class="risk-row">
        <div class="identity"><div class="avatar">{{ (row.displayName || row.username).slice(0,1) }}</div><div><strong>{{ row.displayName || row.username }}</strong><span>@{{ row.username }} · {{ row.planName }}</span></div></div>
        <div class="progress-cell"><div><strong>{{ row.progressRate }}%</strong><span>{{ riskMeta(row.riskLevel).label }}</span></div><el-progress :percentage="row.progressRate" :show-text="false" :stroke-width="8" /></div>
        <div class="signals"><el-tag v-if="row.overdueCount" type="danger">逾期 {{ row.overdueCount }}</el-tag><el-tag v-if="row.dueSoonCount" type="warning">临期 {{ row.dueSoonCount }}</el-tag><el-tag v-if="row.stalledCount" type="warning" effect="plain">停滞 {{ row.stalledCount }}</el-tag><el-tag v-if="!row.overdueCount&&!row.dueSoonCount&&!row.stalledCount" type="success">正常</el-tag></div>
        <div class="date-cell">{{ formatTime(row.lastActiveTime) }}</div>
        <div class="date-cell">{{ row.endDate }}</div>
      </article>
      <el-empty v-if="!loading && !rows.length" description="当前筛选条件下没有计划" />
    </section>
    <el-pagination v-if="total>filters.pageSize" class="pagination" background layout="prev, pager, next" :total="total" :page-size="filters.pageSize" v-model:current-page="filters.page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { getAdminLearningPlanRisks } from '@/api/learningPlans'

const loading=ref(false), rows=ref([]), total=ref(0)
const filters=reactive({keyword:'',riskLevel:'',page:1,pageSize:20})
const labels={OVERDUE:'逾期',DUE_SOON:'临期',STALLED:'停滞',NORMAL:'正常'}
const riskMeta=value=>({label:labels[value]||'正常'})
const formatTime=value=>value?dayjs(value).format('YYYY-MM-DD HH:mm'):'尚未开始'
const load=async()=>{loading.value=true;try{const data=await getAdminLearningPlanRisks({...filters});rows.value=data?.records||[];total.value=data?.total||0}finally{loading.value=false}}
const search=()=>{filters.page=1;load()}
onMounted(load)
</script>

<style scoped>
.risk-page{max-width:1180px;margin:0 auto;padding:30px 32px 56px;color:#0f172a}.page-head{display:flex;justify-content:space-between;align-items:flex-end}.page-head h1{font-size:30px;margin:4px 0 8px}.page-head p{margin:0;color:#64748b}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.13em;color:#2563eb}.rule-note{display:flex;flex-direction:column;align-items:flex-end;padding-left:24px;border-left:3px solid #2563eb}.rule-note span{color:#64748b;font-size:13px;margin-top:5px}.filter-bar{display:flex;align-items:center;gap:12px;margin:26px 0 18px;padding:16px 0;border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0}.result-count{margin-left:auto;color:#64748b;font-size:13px}.risk-table{min-height:280px}.table-head,.risk-row{display:grid;grid-template-columns:minmax(280px,1.4fr) minmax(160px,.8fr) minmax(190px,1fr) 145px 100px;gap:22px;align-items:center}.table-head{padding:10px 18px;color:#94a3b8;font-size:12px;font-weight:700}.risk-row{padding:18px;border-top:1px solid #e2e8f0;transition:background .2s ease}.risk-row:hover{background:#f8fbff}.identity{display:flex;gap:12px;align-items:center}.avatar{width:40px;height:40px;border-radius:12px;background:#dbeafe;color:#1d4ed8;display:flex;align-items:center;justify-content:center;font-weight:800}.identity div:last-child{display:flex;flex-direction:column;gap:5px}.identity span,.date-cell{font-size:12px;color:#64748b}.progress-cell{display:flex;flex-direction:column;gap:8px}.progress-cell div{display:flex;justify-content:space-between;align-items:center}.progress-cell span{font-size:12px;color:#64748b}.signals{display:flex;gap:6px;flex-wrap:wrap}.pagination{justify-content:flex-end;margin-top:24px}@media(max-width:980px){.risk-page{padding:20px}.page-head,.filter-bar{align-items:flex-start;flex-direction:column}.rule-note{align-items:flex-start}.result-count{margin-left:0}.table-head{display:none}.risk-row{grid-template-columns:1fr 1fr}.date-cell:last-child{display:none}}
</style>

