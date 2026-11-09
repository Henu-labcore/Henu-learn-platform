<template>
  <div class="user-management-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-description">管理员可以创建和维护平台用户账号。</p>
      </div>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="panel-left">
          <el-input
            v-model="search"
            placeholder="搜索用户名或显示名"
            clearable
            style="width: 320px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>
        <div class="panel-right">
          <el-button type="primary" @click="openCreateDialog">创建用户</el-button>
          <el-button plain @click="fetchUsers">刷新</el-button>
        </div>
      </div>

      <div class="panel-body">
        <el-table :data="users" stripe style="width: 100%">
          <el-table-column prop="userId" label="用户ID" width="120" />
          <el-table-column prop="username" label="用户名" width="180" />
          <el-table-column prop="displayName" label="显示名" width="180" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="role" label="角色" width="120" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column label="操作" width="220" align="center">
            <template #default="{ row }">
              <el-button type="primary" plain size="small" @click="openEditDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pagination"
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchUsers"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="560px">
      <el-form label-position="top">
        <el-form-item v-if="!isEdit" label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item v-if="false" label="角色">
          <el-select v-model="form.role" placeholder="选择角色">
            <el-option label="ADMIN" value="ADMIN" />
            <el-option label="USER" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="false" label="状态">
          <el-select v-model="form.status" placeholder="选择状态">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="INACTIVE" value="INACTIVE" />
            <el-option label="LOCKED" value="LOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空表示不修改密码' : '创建时必须填写密码'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const search = ref('')
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})
const dialogVisible = ref(false)
const dialogTitle = ref('创建用户')
const isEdit = ref(false)
const form = ref({
  userId: null,
  username: '',
  displayName: '',
  email: '',
  realName: '',
  role: 'USER',
  status: 'ACTIVE',
  password: ''
})

const fetchUsers = async () => {
  try {
    const pageData = await request.get('/users', {
      params: {
        page: pagination.page,
        pageSize: pagination.pageSize,
        keyword: search.value || undefined
      }
    })
    users.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch (error) {
    console.error('加载用户失败:', error)
    ElMessage.error('加载用户失败')
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchUsers()
}

const handlePageSizeChange = () => {
  pagination.page = 1
  fetchUsers()
}

const resetForm = () => {
  form.value = {
    userId: null,
    username: '',
    displayName: '',
    email: '',
    realName: '',
    role: 'USER',
    status: 'ACTIVE',
    password: ''
  }
}

const openCreateDialog = () => {
  resetForm()
  isEdit.value = false
  dialogTitle.value = '创建用户'
  dialogVisible.value = true
}

const openEditDialog = row => {
  form.value = {
    userId: row.userId,
    username: row.username,
    displayName: row.displayName || '',
    email: row.email || '',
    realName: row.realName || '',
    role: row.role || 'USER',
    status: row.status || 'ACTIVE',
    password: ''
  }
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  dialogVisible.value = true
}

const saveUser = async () => {
  try {
    if (isEdit.value) {
      await request.put(`/users/${form.value.userId}`, {
        displayName: form.value.displayName,
        email: form.value.email,
        realName: form.value.realName
      })
      ElMessage.success('用户已更新')
    } else {
      if (!form.value.username || !form.value.password) {
        ElMessage.warning('创建用户时必须填写用户名和密码')
        return
      }
      if (!isStrongPassword(form.value.password)) {
        ElMessage.warning('密码至少 12 位，且需包含大小写字母、数字和特殊字符')
        return
      }
      await request.post('/users', {
        username: form.value.username,
        password: form.value.password,
        displayName: form.value.displayName,
        email: form.value.email,
        realName: form.value.realName,
        grade: ''
      })
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await fetchUsers()
  } catch (error) {
    console.error('保存用户失败:', error)
    ElMessage.error('保存用户失败')
  }
}

const confirmDelete = row => {
  ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await request.delete(`/users/${row.userId}`)
      ElMessage.success('用户已删除')
      await fetchUsers()
    })
    .catch(() => {})
}

const isStrongPassword = password => {
  return password.length >= 12
    && /[a-z]/.test(password)
    && /[A-Z]/.test(password)
    && /\d/.test(password)
    && /[^A-Za-z0-9]/.test(password)
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-management-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #111827;
  font-weight: 700;
}

.page-description {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.panel {
  background: white;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-right {
  display: flex;
  gap: 8px;
}

.panel-body {
  width: 100%;
  overflow-x: auto;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
