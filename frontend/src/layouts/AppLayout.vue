<template>
  <div class="min-h-screen bg-slate-100 lg:flex">
    <div
      v-if="mobileMenuOpen"
      class="fixed inset-0 z-30 bg-slate-900/50 lg:hidden"
      @click="mobileMenuOpen = false"
    />

    <aside
      class="fixed inset-y-0 left-0 z-40 flex w-72 transform flex-col bg-slate-900 text-slate-100 transition-transform duration-200 lg:static lg:translate-x-0"
      :class="mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <div class="flex items-center gap-3 border-b border-slate-800 px-6 py-6">
        <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-krill-500 font-bold text-white">
          RK
        </div>
        <div>
          <p class="text-sm font-semibold uppercase tracking-wider text-krill-300">Rede Krill</p>
          <p class="text-xs text-slate-400">Agendamento de Caminhões</p>
        </div>
      </div>

      <nav class="flex-1 space-y-1 overflow-y-auto px-4 py-6">
        <p class="px-3 pb-2 text-xs font-semibold uppercase tracking-wider text-slate-500">
          Operação
        </p>
        <RouterLink
          v-for="item in visibleItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition"
          :class="
            route.name === item.name
              ? 'bg-krill-500 text-white shadow-sm'
              : 'text-slate-300 hover:bg-slate-800 hover:text-white'
          "
          @click="mobileMenuOpen = false"
        >
          <i :class="item.icon" class="text-base" />
          {{ item.label }}
          <span
            v-if="item.adminOnly"
            class="ml-auto rounded bg-slate-800 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-300"
          >
            Admin
          </span>
        </RouterLink>
      </nav>

      <div class="border-t border-slate-800 p-4">
        <div class="flex items-center gap-3 rounded-lg bg-slate-800/60 px-3 py-3">
          <div class="flex h-9 w-9 items-center justify-center rounded-full bg-slate-700 text-sm font-semibold">
            {{ initials }}
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium text-white">{{ auth.user?.name || 'Usuário' }}</p>
            <p class="truncate text-xs text-slate-400">{{ auth.user?.email }}</p>
          </div>
        </div>
        <button
          type="button"
          class="mt-3 flex w-full items-center justify-center gap-2 rounded-lg border border-slate-700 px-3 py-2 text-sm font-medium text-slate-300 transition hover:border-slate-500 hover:text-white"
          @click="logout"
        >
          <i class="pi pi-sign-out" />
          Sair
        </button>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-4 lg:px-8">
        <div class="flex items-center gap-3">
          <button
            type="button"
            class="rounded-lg border border-slate-200 p-2 text-slate-600 lg:hidden"
            aria-label="Abrir menu"
            @click="mobileMenuOpen = true"
          >
            <i class="pi pi-bars" />
          </button>
          <div>
            <h1 class="text-lg font-semibold text-slate-900">{{ pageTitle }}</h1>
            <p class="text-xs text-slate-500">Gestão logística da Rede Krill</p>
          </div>
        </div>

        <span
          class="hidden items-center gap-2 rounded-full border border-krill-200 bg-krill-50 px-3 py-1.5 text-xs font-semibold text-krill-700 sm:inline-flex"
        >
          <i class="pi pi-shield" />
          {{ auth.isAdmin ? 'Administrador' : 'Consulta' }}
        </span>
      </header>

      <main class="flex-1 px-4 py-6 lg:px-8 lg:py-8">
        <RouterView />
      </main>

      <footer class="border-t border-slate-200 px-4 py-4 text-center text-xs text-slate-400 lg:px-8">
        Rede Krill · Desafio Dev · {{ new Date().getFullYear() }}
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mobileMenuOpen = ref(false)

const navigation = [
  { name: 'dashboard', label: 'Painel', icon: 'pi pi-home' },
  { name: 'appointments', label: 'Agendamentos', icon: 'pi pi-calendar' },
  { name: 'reports', label: 'Relatórios', icon: 'pi pi-chart-bar' },
  { name: 'truck-types', label: 'Tipos de caminhão', icon: 'pi pi-truck' },
  { name: 'users', label: 'Usuários', icon: 'pi pi-users', adminOnly: true },
  { name: 'audit', label: 'Auditoria', icon: 'pi pi-history', adminOnly: true }
]

const visibleItems = computed(() =>
  navigation.filter((item) => !item.adminOnly || auth.isAdmin)
)

const pageTitle = computed(() => route.meta.title || 'Painel')

const initials = computed(() => {
  const name = auth.user?.name || 'Usuário'
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
})

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>
