import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const adminRoutes = [
  // 学情分析：学习过程 / 学生画像 / 计划执行 三个 Tab
  {
    path: 'analytics',
    name: 'StudentAnalytics',
    component: () => import('@/views/analytics/StudentAnalytics.vue'),
    meta: { title: '学情分析', requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/analytics/process' },
      {
        path: 'process',
        name: 'AnalyticsProcess',
        component: () => import('@/views/AdminLearningDashboard.vue'),
        meta: { title: '学习过程', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'profile',
        name: 'AnalyticsProfile',
        component: () => import('@/views/MasteryRiskOverview.vue'),
        meta: { title: '学生画像', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'plan',
        name: 'AnalyticsPlan',
        component: () => import('@/views/AdminLearningRisks.vue'),
        meta: { title: '计划执行', requiresAuth: true, requiresAdmin: true }
      }
    ]
  },
  // 旧入口兼容：原三个管理页已收进学情分析
  { path: 'admin-learning-risks', redirect: '/analytics/plan' },
  { path: 'mastery-risks', redirect: '/analytics/profile' },
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
  { path: 'admin-dashboard', redirect: '/analytics/process' },
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

const adminRouteNames = adminRoutes
  .map(route => route.name)
  .filter(Boolean)
// 由 adminRoutes 派生，避免新增后台路由时漏改前缀判断
const adminPathPrefixes = adminRoutes.map(route => `/${route.path.split('/')[0]}`)
let adminRoutesAdded = false

const routes = [
  {
    // 旧入口兼容：原“我的掌握度”已升级为“我的学习 → 能力画像”
    path: '/my-mastery',
    redirect: '/my-learning/profile'
  },
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
        path: 'my-learning',
        name: 'MyLearning',
        component: () => import('@/views/MyLearning.vue'),
        redirect: '/my-learning/process',
        meta: { title: '我的学习', requiresAuth: true },
        children: [
          { path: '', redirect: '/my-learning/process' },
          {
            path: 'process',
            name: 'MyLearningProcess',
            component: () => import('@/views/LearnDashboard.vue'),
            meta: { title: '学习数据', requiresAuth: true }
          },
          {
            path: 'profile',
            name: 'MyLearningProfile',
            component: () => import('@/views/MyKnowledgeMastery.vue'),
            meta: { title: '能力画像', requiresAuth: true }
          }
        ]
      },
      {
        path: 'learn-dashboard',
        redirect: '/my-learning/process'
      },
      {
        path: 'my-profile',
        redirect: '/my-learning/profile'
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
      // 管理员没有学生个人画像，统一回到学习过程
      if (to.name === 'MyLearningProfile' && authStore.isAdmin) {
        next('/my-learning/process')
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
