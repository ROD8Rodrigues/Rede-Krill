<template>
  <div class="space-y-6">
    <section class="overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-slate-800 to-krill-700 p-6 text-white shadow-lg sm:p-8">
      <p class="text-xs font-semibold uppercase tracking-widest text-krill-300">Painel operacional</p>
      <h2 class="mt-2 text-2xl font-bold sm:text-3xl">
        Bem-vindo, {{ auth.user?.name || 'usuário' }}.
      </h2>
      <p class="mt-3 max-w-2xl text-sm leading-relaxed text-slate-300">
        Acompanhe a ocupação do centro de distribuição, cadastre agendamentos com ID Pedido,
        consulte o histórico de cancelamentos e gere relatórios por período.
      </p>
      <div class="mt-5 flex flex-wrap gap-2 text-xs font-semibold">
        <span class="rounded-full bg-white/10 px-3 py-1.5">
          <i class="pi pi-check-circle mr-1 text-krill-300" />
          Autenticação JWT ativa
        </span>
        <span class="rounded-full bg-white/10 px-3 py-1.5">
          <i class="pi pi-shield mr-1 text-krill-300" />
          Perfil {{ auth.isAdmin ? 'ADMIN' : 'USER' }}
        </span>
        <span class="rounded-full bg-white/10 px-3 py-1.5">
          <i class="pi pi-envelope mr-1 text-krill-300" />
          Confirmação por e-mail
        </span>
      </div>
    </section>

    <!-- Ocupação do dia -->
    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 class="text-base font-semibold text-slate-900">Ocupação de hoje</h3>
          <p class="text-xs text-slate-500">{{ formattedToday }}</p>
        </div>
        <Button
          icon="pi pi-refresh"
          label="Atualizar"
          severity="secondary"
          text
          size="small"
          :loading="capacityLoading"
          @click="loadCapacity"
        />
      </div>

      <div v-if="capacity" class="mt-4 space-y-4">
        <div>
          <div class="flex items-center justify-between text-xs font-semibold text-slate-700">
            <span>{{ capacity.dailyPallets }} de {{ capacity.dailyLimit }} pallets no dia</span>
            <span :class="occupancyTextClass(capacity.dailyOccupancyPercentage)">
              {{ formatPercent(capacity.dailyOccupancyPercentage) }}
            </span>
          </div>
          <div class="mt-1.5 h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
            <div
              class="h-full rounded-full transition-all"
              :class="occupancyBarClass(capacity.dailyOccupancyPercentage)"
              :style="{ width: barWidth(capacity.dailyOccupancyPercentage) }"
            />
          </div>
        </div>

        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <div
            v-for="slot in capacity.slots"
            :key="slot.timeSlot.id"
            class="rounded-lg border border-slate-200 bg-slate-50/70 p-3"
          >
            <div class="flex items-center justify-between text-xs font-semibold text-slate-700">
              <span class="inline-flex items-center gap-1.5">
                <i class="pi pi-clock text-[10px] text-slate-500" />
                {{ slot.timeSlot.label }}
              </span>
              <span :class="occupancyTextClass(slot.occupancyPercentage)">
                {{ formatPercent(slot.occupancyPercentage) }}
              </span>
            </div>
            <p class="mt-1 text-xs text-slate-500">{{ slot.pallets }} de {{ slot.limit }} pallets</p>
            <div class="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-200">
              <div
                class="h-full rounded-full transition-all"
                :class="occupancyBarClass(slot.occupancyPercentage)"
                :style="{ width: barWidth(slot.occupancyPercentage) }"
              />
            </div>
          </div>
        </div>
      </div>
      <p v-else class="mt-4 text-xs text-slate-400">
        {{ capacityLoading ? 'Carregando ocupação...' : 'Não foi possível carregar a ocupação de hoje.' }}
      </p>
    </section>

    <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <RouterLink
        v-for="card in cards"
        :key="card.name"
        :to="{ name: card.name }"
        class="group rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-krill-300 hover:shadow-md"
      >
        <div class="flex items-start justify-between">
          <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-krill-50 text-krill-600">
            <i :class="card.icon" class="text-lg" />
          </div>
          <i class="pi pi-arrow-right text-xs text-slate-300 transition group-hover:text-krill-500" />
        </div>
        <h3 class="mt-4 text-base font-semibold text-slate-900">{{ card.title }}</h3>
        <p class="mt-1 text-sm leading-relaxed text-slate-500">{{ card.description }}</p>
      </RouterLink>
    </section>

    <section class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-center gap-3">
        <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
          <i class="pi pi-info-circle" />
        </div>
        <div>
          <h3 class="text-base font-semibold text-slate-900">Limites operacionais informativos</h3>
          <p class="text-sm text-slate-500">
            A capacidade é de 480 pallets por dia e 120 pallets por faixa horária. Cargas não
            paletizadas dobram apenas o impacto na faixa. O sistema avisa, mas nunca bloqueia o
            cadastro.
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import Button from 'primevue/button'
import { useAuthStore } from '../stores/auth'
import { appointmentService } from '../services/appointmentService'

const auth = useAuthStore()

const capacity = ref(null)
const capacityLoading = ref(false)

const todayIso = computed(() => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
})

const formattedToday = computed(() => {
  const [year, month, day] = todayIso.value.split('-')
  return `${day}/${month}/${year}`
})

const cards = [
  {
    name: 'appointments',
    title: 'Agendamentos',
    description: 'Consulte, crie, edite e cancele agendamentos por data, faixa horária e ID Pedido.',
    icon: 'pi pi-calendar'
  },
  {
    name: 'reports',
    title: 'Relatórios',
    description: 'Visualize o consolidado de agendamentos, pallets do dia e da faixa, com cancelados.',
    icon: 'pi pi-chart-bar'
  },
  {
    name: 'audit',
    title: 'Auditoria',
    description: 'Acompanhe login, logout, acessos negados e operações. Disponível para administradores.',
    icon: 'pi pi-history',
    adminOnly: true
  }
].filter((card) => !card.adminOnly || auth.isAdmin)

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function barWidth(value) {
  return `${Math.min(Math.max(Number(value) || 0, 0), 100)}%`
}

function occupancyBarClass(value) {
  const percent = Number(value) || 0
  if (percent >= 100) return 'bg-red-500'
  if (percent >= 80) return 'bg-amber-500'
  return 'bg-krill-500'
}

function occupancyTextClass(value) {
  const percent = Number(value) || 0
  if (percent >= 100) return 'text-red-600'
  if (percent >= 80) return 'text-amber-600'
  return 'text-krill-700'
}

async function loadCapacity() {
  capacityLoading.value = true
  try {
    capacity.value = await appointmentService.getCapacity(todayIso.value)
  } catch (error) {
    console.error('Erro ao carregar ocupação:', error)
    capacity.value = null
  } finally {
    capacityLoading.value = false
  }
}

onMounted(() => {
  loadCapacity()
})
</script>
