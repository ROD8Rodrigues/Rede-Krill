import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore, safeRedirect } from '../stores/auth'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'

const ChangePasswordView = () => import('../views/ChangePasswordView.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const AppointmentsView = () => import('../views/AppointmentsView.vue')
const ReportsView = () => import('../views/ReportsView.vue')
const TruckTypesView = () => import('../views/TruckTypesView.vue')
const TimeSlotsView = () => import('../views/TimeSlotsView.vue')
const PalletizationTypesView = () => import('../views/PalletizationTypesView.vue')
const UsersView = () => import('../views/UsersView.vue')
const AuditView = () => import('../views/AuditView.vue')

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true }
  },
  {
    path: '/change-password',
    name: 'change-password',
    component: ChangePasswordView,
    meta: { requiresAuth: true, title: 'Troca de senha' }
  },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: { name: 'dashboard' }
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: { title: 'Painel' }
      },
      {
        path: 'appointments',
        name: 'appointments',
        component: AppointmentsView,
        meta: { title: 'Agendamentos' }
      },
      {
        path: 'reports',
        name: 'reports',
        component: ReportsView,
        meta: { title: 'Relatórios' }
      },
      {
        path: 'truck-types',
        name: 'truck-types',
        component: TruckTypesView,
        meta: { title: 'Tipos de caminhão' }
      },
      {
        path: 'time-slots',
        name: 'time-slots',
        component: TimeSlotsView,
        meta: { title: 'Faixas horárias' }
      },
      {
        path: 'palletization-types',
        name: 'palletization-types',
        component: PalletizationTypesView,
        meta: { title: 'Tipos de paletização' }
      },
      {
        path: 'users',
        name: 'users',
        component: UsersView,
        meta: { title: 'Usuários', adminOnly: true }
      },
      {
        path: 'audit',
        name: 'audit',
        component: AuditView,
        meta: { title: 'Auditoria', adminOnly: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!auth.initialized) {
    await auth.initialize()
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return auth.mustChangePassword
      ? { name: 'change-password' }
      : { path: safeRedirect(to.query.redirect) }
  }

  if (auth.isAuthenticated && auth.mustChangePassword && to.name !== 'change-password') {
    return { name: 'change-password' }
  }

  if (auth.isAuthenticated && !auth.mustChangePassword && to.name === 'change-password') {
    return { name: 'dashboard' }
  }

  if (to.meta.adminOnly && !auth.isAdmin) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
