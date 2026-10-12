<template>
  <div class="favorites-page" v-loading="loading">
    <header><div><h1>我的收藏</h1><p>按个人分类与标签整理学习内容，收藏会同步到当前账号的其他设备。</p></div><el-button @click="reload">刷新同步</el-button></header>
    <el-alert v-if="legacyIds.length" title="检测到旧浏览器收藏：请确认这些收藏属于你，再导入当前账号。原本地数据不会被删除。" type="warning" :closable="false">
      <el-button size="small" @click="importLegacy">确认归属并导入 {{ legacyIds.length }} 项</el-button>
    </el-alert>
    <div class="filters">
      <el-input v-model="filters.keyword" clearable placeholder="检索标题、摘要或个人备注" @keyup.enter="reload" />
      <el-select v-model="filters.categoryId" clearable placeholder="全部个人分类" @change="reload"><el-option label="未分类" :value="0"/><el-option v-for="c in meta.categories" :key="c.id" :label="c.name" :value="c.id"/></el-select>
      <el-select v-model="filters.tag" clearable placeholder="全部标签" @change="reload"><el-option v-for="t in meta.tags" :key="t.id" :label="t.name" :value="t.name"/></el-select>
      <el-select v-model="filters.sort" @change="reload"><el-option label="最近收藏优先" value="newest"/><el-option label="最早收藏优先" value="oldest"/></el-select>
      <el-button type="primary" @click="reload">检索</el-button><el-button @click="categoryVisible=true">管理分类</el-button>
    </div>
    <el-empty v-if="!rows.length" description="暂无符合条件的收藏，可在知识学习卡片上点击星标收藏"/>
    <div class="favorite-grid">
      <article v-for="row in rows" :key="row.id">
        <div class="card-heading"><h2>{{ row.title }}</h2><el-tag v-if="!row.available" type="info">内容不可用</el-tag></div>
        <p>{{ row.summary || '暂无摘要' }}</p><div class="labels"><el-tag>{{ row.categoryName || '未分类' }}</el-tag><el-tag v-for="tag in row.tags" :key="tag" type="success">{{ tag }}</el-tag></div>
        <p v-if="row.note" class="note">备注：{{ row.note }}</p><small>收藏时间 {{ row.favoritedAt?.replace('T',' ') }}</small>
        <!-- 学习与取消收藏固定同排；整理入口另起一排，避免窄卡片自动换行错位。 -->
        <div class="actions">
          <div class="primary-actions">
            <el-button type="primary" :disabled="!row.available" @click="router.push({path:'/knowledge/details',query:{id:row.itemPk}})">开始学习</el-button>
            <el-button type="danger" plain @click="remove(row)">取消收藏</el-button>
          </div>
          <el-button @click="edit(row)">分类 / 标签 / 备注</el-button>
        </div>
      </article>
    </div>
    <el-dialog v-model="editVisible" title="整理收藏" width="520px">
      <el-form label-position="top"><el-form-item label="个人分类"><el-select v-model="form.categoryId" clearable style="width:100%" placeholder="未分类"><el-option v-for="c in meta.categories" :key="c.id" :label="c.name" :value="c.id"/></el-select></el-form-item>
        <el-form-item label="标签（可输入新标签，回车确认，最多10个）"><el-select v-model="form.tags" multiple filterable allow-create default-first-option :multiple-limit="10" style="width:100%"><el-option v-for="t in meta.tags" :key="t.id" :label="t.name" :value="t.name"/></el-select></el-form-item>
        <el-form-item label="个人备注"><el-input v-model="form.note" type="textarea" :rows="4" maxlength="1000" show-word-limit/></el-form-item></el-form>
      <template #footer><el-button @click="editVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存并同步</el-button></template>
    </el-dialog>
    <el-dialog v-model="categoryVisible" title="管理个人分类" width="480px">
      <div class="category-add"><el-input v-model="newCategory" maxlength="30" placeholder="新分类名称" @keyup.enter="createCategory"/><el-button type="primary" @click="createCategory">新增</el-button></div>
      <div v-for="c in meta.categories" :key="c.id" class="category-row"><span>{{ c.name }}</span><el-button text @click="renameCategory(c)">重命名</el-button><el-button text type="danger" @click="removeCategory(c)">删除分类</el-button></div>
      <p class="note">删除分类只将收藏移至“未分类”，不会删除收藏或学习内容。</p>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useFavoritesStore } from '@/stores/studyStore'
import { getFavorites, getFavoriteMeta, updateFavorite, removeFavorite, importFavorites, addFavoriteCategory, renameFavoriteCategory, deleteFavoriteCategory } from '@/api/favorites'
const router=useRouter(), store=useFavoritesStore()
const rows=ref([]), loading=ref(false), saving=ref(false), editVisible=ref(false), categoryVisible=ref(false), newCategory=ref(''), legacyIds=ref([])
const meta=reactive({categories:[],tags:[]}), filters=reactive({keyword:'',categoryId:null,tag:'',sort:'newest'}), form=reactive({itemPk:null,categoryId:null,tags:[],note:'',version:null})
let queryVersion=0
const reload=async()=>{const version=++queryVersion;loading.value=true;try{const [list,labels]=await Promise.all([getFavorites({...filters,categoryId:filters.categoryId ?? undefined}),getFavoriteMeta()]);if(version===queryVersion){rows.value=list||[];Object.assign(meta,labels)}}catch{/* 原请求层统一提示，保留当前页面。 */}finally{if(version===queryVersion)loading.value=false}}
const edit=row=>{Object.assign(form,{itemPk:row.itemPk,categoryId:row.categoryId,tags:[...row.tags],note:row.note,version:row.version});editVisible.value=true}
const save=async()=>{saving.value=true;try{await updateFavorite(form.itemPk,{...form,categoryId:form.categoryId||null});editVisible.value=false;ElMessage.success('收藏已同步');await Promise.all([reload(),store.load()])}catch{/* 冲突时不关闭表单，用户可刷新后重试。 */}finally{saving.value=false}}
const remove=async row=>{try{await ElMessageBox.confirm('确定取消收藏吗？不会删除学习内容。','取消收藏');await removeFavorite(row.itemPk);await Promise.all([reload(),store.load()])}catch{/* 取消操作或请求失败不修改页面。 */}}
const importLegacy=async()=>{try{await ElMessageBox.confirm('旧收藏未按账号隔离。请确认它们属于你，导入后可在其他设备查看；原本地记录仍会保留。','确认旧收藏归属',{type:'warning'});const result=await importFavorites(legacyIds.value);ElMessage.success(`已处理 ${result.imported} 项，跳过不可用内容 ${result.skipped} 项`);await Promise.all([reload(),store.load()])}catch{/* 不自动清空旧收藏。 */}}
const createCategory=async()=>{if(!newCategory.value.trim())return;try{await addFavoriteCategory(newCategory.value.trim());newCategory.value='';await reload()}catch{}}
const renameCategory=async c=>{try{const {value}=await ElMessageBox.prompt('新名称（1至30字）','重命名分类',{inputValue:c.name});await renameFavoriteCategory(c.id,value);await reload()}catch{}}
const removeCategory=async c=>{try{await ElMessageBox.confirm('收藏会移至未分类，不会被删除。','删除分类');await deleteFavoriteCategory(c.id);if(filters.categoryId===c.id)filters.categoryId=null;await reload()}catch{}}
onMounted(()=>{try{const legacy=JSON.parse(localStorage.getItem('labcore_favorites')||'[]');if(Array.isArray(legacy))legacyIds.value=[...new Set(legacy.map(x=>Number(x?.itemPk??x?.id)).filter(x=>Number.isSafeInteger(x)&&x>0))]}catch{/* 损坏的旧数据不影响新模块。 */}reload()})
</script>

<style scoped>
.favorite-grid .actions { flex-direction: column; align-items: flex-start; }
.primary-actions { display: flex; align-items: center; gap: 8px; flex-wrap: nowrap; }
/* 使用 gap 控制同排间距，取消组件默认的相邻按钮左边距。 */
.primary-actions :deep(.el-button + .el-button) { margin-left: 0; }
.favorites-page{max-width:1200px;margin:auto;padding:28px}.favorites-page header{display:flex;justify-content:space-between;align-items:center;gap:16px}.favorites-page h1{margin:0 0 8px}.favorites-page header p,.note{color:#64748b}.filters{display:flex;flex-wrap:wrap;gap:10px;margin:22px 0}.filters>.el-input{width:280px}.filters>.el-select{width:155px}.favorite-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:18px}.favorite-grid article{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:22px}.card-heading{display:flex;align-items:center;gap:10px}.card-heading h2{font-size:18px;margin:0}.favorite-grid p{line-height:1.7;overflow-wrap:anywhere}.labels,.actions,.category-add{display:flex;flex-wrap:wrap;gap:8px}.actions{margin-top:18px}.favorite-grid small{color:#94a3b8}.category-add{flex-wrap:nowrap}.category-row{display:flex;align-items:center;margin-top:12px}.category-row span{flex:1}@media(max-width:600px){.favorites-page{padding:16px}.filters>*{width:100%!important}.favorites-page header{align-items:flex-start}}
</style>
