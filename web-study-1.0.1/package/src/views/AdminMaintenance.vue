<template>
  <div class="maintenance-page">
    <div class="page-header">
      <h1 class="page-title">贡献者维护</h1>
      <el-button type="primary" @click="openCreate">新增贡献者</el-button>
    </div>

    <div class="section-head">
      <span class="section-note">
        共 {{ contributors.length }} 条，其中 {{ visibleCount }} 条在「项目贡献者」页面对外展示
      </span>
    </div>

    <el-table v-loading="tableLoading" :data="contributors" border stripe size="small">
      <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
      <el-table-column label="展示名" min-width="140">
        <template #default="{ row }">
          <span>{{ row.displayName || '-' }}</span>
          <el-tag v-if="row.username" size="small" type="info" class="inline-tag">
            {{ row.username }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="roleTitle" label="职责" min-width="110" />
      <el-table-column prop="moduleScope" label="负责模块" min-width="160" show-overflow-tooltip />
      <el-table-column prop="grade" label="年级" width="90" />
      <el-table-column prop="joinDate" label="加入时间" width="110" />
      <el-table-column prop="githubUrl" label="GitHub" min-width="150" show-overflow-tooltip />
      <el-table-column label="展示" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.visible === 1 ? 'success' : 'info'" size="small">
            {{ row.visible === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无贡献者，点击右上角新增" />
      </template>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑贡献者' : '新增贡献者'"
      width="620px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关联账号">
          <el-select
            v-model="form.userId"
            filterable
            clearable
            placeholder="留空表示无平台账号的外部贡献者"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.userId"
              :label="`${user.displayName || user.username} (${user.username})`"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="展示名" prop="displayName">
          <el-input
            v-model="form.displayName"
            :placeholder="form.userId ? '留空则取账号的显示名' : '未关联账号时必填'"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="职责标题">
          <el-input v-model="form.roleTitle" placeholder="如：内容维护、前端开发" maxlength="100" />
        </el-form-item>

        <el-form-item label="负责模块">
          <el-input
            v-model="form.moduleScope"
            placeholder="多个模块用顿号分隔，如：学习卡片、模板管理"
            maxlength="255"
          />
        </el-form-item>

        <el-form-item label="贡献描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="GitHub">
          <el-input v-model="form.githubUrl" placeholder="https://github.com/username" maxlength="255" />
        </el-form-item>

        <el-form-item label="加入时间">
          <el-date-picker
            v-model="form.joinDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <span class="form-hint">数字越小越靠前</span>
        </el-form-item>

        <el-form-item label="对外展示">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import {
  createAdminContributor,
  deleteAdminContributor,
  getAdminContributors,
  updateAdminContributor
} from '@/api/contributors'

const contributors = ref([])
const tableLoading = ref(false)
const userOptions = ref([])

const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  userId: null,
  displayName: '',
  roleTitle: '',
  moduleScope: '',
  description: '',
  githubUrl: '',
  joinDate: '',
  sortOrder: 0,
  visible: 1
})

const rules = {
  displayName: [
    {
      validator: (rule, value, callback) => {
        if (!form.userId && !value) {
          callback(new Error('未关联平台账号时必须填写展示名'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const visibleCount = computed(() => contributors.value.filter(item => item.visible === 1).length)

const loadContributors = async () => {
  tableLoading.value = true
  try {
    contributors.value = await getAdminContributors()
  } catch (error) {
    ElMessage.error(error?.message || '加载贡献者名单失败')
  } finally {
    tableLoading.value = false
  }
}

const loadUserOptions = async () => {
  try {
    const page = await request({ url: '/users', method: 'get', params: { page: 1, pageSize: 100 } })
    userOptions.value = page?.records || []
  } catch (error) {
    // 用户列表加载失败不阻塞贡献者维护主体
    userOptions.value = []
  }
}

const resetForm = () => {
  editingId.value = null
  Object.assign(form, {
    userId: null,
    displayName: '',
    roleTitle: '',
    moduleScope: '',
    description: '',
    githubUrl: '',
    joinDate: '',
    sortOrder: 0,
    visible: 1
  })
  formRef.value?.clearValidate()
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = row => {
  resetForm()
  editingId.value = row.contributorId
  Object.assign(form, {
    userId: row.userId ?? null,
    displayName: row.displayName || '',
    roleTitle: row.roleTitle || '',
    moduleScope: row.moduleScope || '',
    description: row.description || '',
    githubUrl: row.githubUrl || '',
    joinDate: row.joinDate || '',
    sortOrder: row.sortOrder ?? 0,
    visible: row.visible ?? 1
  })
  dialogVisible.value = true
}

const submit = async () => {
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }

  saving.value = true
  try {
    const payload = {
      userId: form.userId || null,
      displayName: form.displayName || null,
      roleTitle: form.roleTitle || null,
      moduleScope: form.moduleScope || null,
      description: form.description || null,
      githubUrl: form.githubUrl || null,
      joinDate: form.joinDate || null,
      sortOrder: Number(form.sortOrder) || 0,
      visible: form.visible
    }

    if (editingId.value) {
      await updateAdminContributor(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createAdminContributor(payload)
      ElMessage.success('已新增')
    }

    dialogVisible.value = false
    await loadContributors()
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const confirmDelete = async row => {
  const name = row.displayName || row.username || `#${row.contributorId}`
  try {
    await ElMessageBox.confirm(`确定删除贡献者「${name}」吗？此操作不可撤销。`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (error) {
    return
  }

  try {
    await deleteAdminContributor(row.contributorId)
    ElMessage.success('已删除')
    await loadContributors()
  } catch (error) {
    ElMessage.error(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadContributors()
  loadUserOptions()
})
</script>

<style scoped>
.maintenance-page {
  padding: 24px 32px 40px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1f2d3d;
}

.section-head {
  margin-bottom: 12px;
}

.section-note {
  font-size: 12px;
  color: #909399;
}

.inline-tag {
  margin-left: 6px;
}

.form-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
