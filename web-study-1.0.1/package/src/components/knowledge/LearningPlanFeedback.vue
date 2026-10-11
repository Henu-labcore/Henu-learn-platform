<template>
  <el-button v-if="completed" size="small" type="success" plain @click="open">{{ itemPk === 0 ? '计划完成总结' : '完成反馈' }}</el-button>
  <el-dialog v-model="visible" :title="itemPk === 0 ? '计划完成总结' : '学习完成反馈'" width="560px" append-to-body>
    <el-alert title="心得只保存到学习计划模块，不会覆盖原学习记录或完成备注。" type="info" :closable="false"/>
    <el-form label-position="top" style="margin-top:18px">
      <el-form-item label="自评"><el-rate v-model="form.rating"/></el-form-item>
      <el-form-item label="学习心得（必填）"><el-input v-model="form.reflection" type="textarea" :rows="4" maxlength="2000" show-word-limit/></el-form-item>
      <el-form-item label="遇到的困难"><el-input v-model="form.difficulty" type="textarea" maxlength="1000" show-word-limit/></el-form-item>
      <el-form-item label="下一步安排"><el-input v-model="form.nextStep" type="textarea" maxlength="1000" show-word-limit/></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存并同步</el-button></template>
  </el-dialog>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getLearningPlanFeedback, saveLearningPlanFeedback } from '@/api/learningPlans'
const props=defineProps({planId:{type:Number,required:true},itemPk:{type:Number,default:0},completed:{type:Boolean,default:false}})
const visible=ref(false),saving=ref(false),form=reactive({rating:3,reflection:'',difficulty:'',nextStep:'',version:0})
// 每次打开重新读取后端版本，支持其他设备已提交的心得。
const open=async()=>{try{const rows=await getLearningPlanFeedback(props.planId);const row=rows.find(r=>Number(r.itemPk)===props.itemPk);Object.assign(form,{rating:3,reflection:'',difficulty:'',nextStep:'',version:0},row||{});visible.value=true}catch{}}
const save=async()=>{if(!form.reflection.trim()){ElMessage.warning('请填写学习心得');return}saving.value=true;try{await saveLearningPlanFeedback(props.planId,props.itemPk,{...form});visible.value=false;ElMessage.success('完成反馈已保存，可在其他设备查看')}catch{}finally{saving.value=false}}
</script>
