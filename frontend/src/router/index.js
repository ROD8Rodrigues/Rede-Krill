import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import AppointmentsView from '../views/AppointmentsView.vue'
import ReportsView from '../views/ReportsView.vue'
import TruckTypesView from '../views/TruckTypesView.vue'
import UnderConstructionView from '../views/UnderConstructionView.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true }
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
        path: 'users',
        name: 'users',
        component: UnderConstructionView,
        props: {
          title: 'Usuários',
          description: 'Gestão de usuários e perfis de acesso (ADMIN e USER).'
        },
        meta: { title: 'Usuários', adminOnly: true }
      },
      {
        path: 'audit',
        name: 'audit',
        component: UnderConstructionView,
        props: {
          title: 'Auditoria',
          description: 'Trilha de auditoria das operações realizadas no sistema.'
        },
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

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }

  if (auth.isAuthenticated && !auth.user) {
    try {
      await auth.loadCurrentUser()
    } catch {
      auth.logout()
      return { name: 'login' }
    }
  }

  if (to.meta.adminOnly && !auth.isAdmin) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
