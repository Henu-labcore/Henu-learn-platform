import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const adminRoutes = [
  // 新增风险看板沿用原管理员路由注册和权限保护。
  {
    path: 'admin-learning-risks', name: 'AdminLearningRisks',
    component: () => import('@/views/AdminLearningRisks.vue'),
    meta: { title: '学习计划风险', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'mastery-risks', name: 'AdminMasteryRisks',
    component: () => import('@/views/MasteryRiskOverview.vue'),
    meta: { title: '掌握度风险预警', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'knowledge-points', name: 'KnowledgePointManagement',
    component: () => import('@/views/KnowledgePointManagement.vue'),
    meta: { title: '知识点管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'users',
    name: 'UserManagement',
    component: () => import('@/views/UserManagement.vue'),
    meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'admin-dashboard',
    name: 'AdminLearningDashboard',
    component: () => import('@/views/AdminLearningDashboard.vue'),
    meta: { title: '平台学习总览', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'content-library',
    name: 'ContentLibrary',
    component: () => import('@/views/ContentLibrary.vue'),
    meta: { title: '学习内容管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'admin-assignments',
    name: 'AdminAssignments',
    component: () => import('@/views/AdminAssignments.vue'),
    meta: { title: '考核管理', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'admin-assignments/:assignmentId/submissions',
    name: 'AdminAssignmentSubmissions',
    component: () => import('@/views/AdminAssignmentSubmissions.vue'),
    meta: { title: '提交记录', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'users/:userId/progress',
    name: 'UserProgressDetail',
    component: () => import('@/views/UserProgressDetail.vue'),
    meta: { title: '用户学习详情', requiresAuth: true, requiresAdmin: true }
  },
  {
    path: 'maintenance',
    name: 'AdminMaintenance',
    component: () => import('@/views/AdminMaintenance.vue'),
    meta: { title: '项目维护', requiresAuth: true, requiresAdmin: true }
  }
]

const adminRouteNames = adminRoutes.map(route => route.name)
// 由 adminRoutes 派生，避免新增后台路由时漏改前缀判断
const adminPathPrefixes = adminRoutes.map(route => `/${route.path.split('/')[0]}`)
let adminRoutesAdded = false

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'RootLayout',
    component: () => import('@/components/Layout/Index.vue'),
    redirect: '/knowledge',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'favorites', name: 'MyFavorites',
        component: () => import('@/views/MyFavorites.vue'),
        meta: { title: '我的收藏', requiresAuth: true }
      },
      {
        path: 'learning-plan', name: 'LearningPlans',
        component: () => import('@/views/LearningPlans.vue'),
        meta: { title: '学习计划', requiresAuth: true }
      },
      {
        path: 'knowledge',
        name: 'KnowledgeMain',
        component: () => import('@/views/KnowledgeMain.vue'),
        meta: { title: '知识学习', requiresAuth: true }
      },
      {
        path: 'knowledge/details',
        name: 'KnowledgeDetails',
        component: () => import('@/views/KnowledgeDetails.vue'),
        meta: { title: '知识详情', requiresAuth: true }
      },
      {
        path: 'learning/item/:itemPk',
        name: 'LearningItem',
        component: () => import('@/views/KnowledgeDetails.vue'),
        meta: { title: '学习卡片', requiresAuth: true }
      },
      {
        path: 'learn-dashboard',
        name: 'LearnDashboard',
        component: () => import('@/views/LearnDashboard.vue'),
        meta: { title: '学习数据面板', requiresAuth: true }
      },
      {
        path: 'my-mastery',
        name: 'MyKnowledgeMastery',
        component: () => import('@/views/MyKnowledgeMastery.vue'),
        meta: { title: '我的掌握度', requiresAuth: true }
      },
      {
        path: 'assignments',
        name: 'Assignments',
        component: () => import('@/views/Assignments.vue'),
        meta: { title: '能力考核', requiresAuth: true }
      },
      {
        path: 'assignments/:assignmentId',
        name: 'AssignmentDetail',
        component: () => import('@/views/AssignmentDetail.vue'),
        meta: { title: '考核详情', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/PersonalCenter.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'contributors',
        name: 'Contributors',
        component: () => import('@/views/Contributors.vue'),
        meta: { title: '项目贡献者', requiresAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export function ensureAdminRoutes() {
  if (adminRoutesAdded) return
  adminRoutes.forEach(route => {
    if (!router.hasRoute(route.name)) {
      router.addRoute('RootLayout', route)
    }
  })
  adminRoutesAdded = true
}

export function resetAdminRoutes() {
  adminRouteNames.forEach(name => {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
  adminRoutesAdded = false
}

function isAdminPath(path) {
  return adminPathPrefixes.some(prefix => path === prefix || path.startsWith(`${prefix}/`))
}

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (!authStore.isAuthenticated) {
    authStore.initUser()
  }

  if (!authStore.isAuthenticated) {
    resetAdminRoutes()
  }

  if (authStore.isAuthenticated && authStore.isAdmin) {
    const hadAdminRoutes = adminRoutesAdded
    ensureAdminRoutes()
    if (!hadAdminRoutes && isAdminPath(to.path)) {
      next({ ...to, replace: true })
      return
    }
  }

  if (to.meta.requiresAuth) {
    if (authStore.isAuthenticated && authStore.token) {
      if (to.meta.requiresAdmin && !authStore.isAdmin) {
        next('/')
        return
      }
      next()
    } else {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    }
    return
  }

  if (isAdminPath(to.path)) {
    if (authStore.isAuthenticated && authStore.isAdmin) {
      ensureAdminRoutes()
      next({ ...to, replace: true })
      return
    }
    next(authStore.isAuthenticated ? '/' : {
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  if (authStore.isAuthenticated && to.path === '/login') {
    next('/')
    return
  }

  next()
})

export default router
