<template>
  <div class="space-y-6">
    <section class="overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-slate-800 to-krill-700 p-6 text-white shadow-lg sm:p-8">
      <p class="text-xs font-semibold uppercase tracking-widest text-krill-300">Painel operacional</p>
      <h2 class="mt-2 text-2xl font-bold sm:text-3xl">
        Bem-vindo, {{ auth.user?.name || 'usuário' }}.
      </h2>
      <p class="mt-3 max-w-2xl text-sm leading-relaxed text-slate-300">
        Esta é a base do sistema de agendamento de caminhões da Rede Krill. O cadastro e a gestão
        dos agendamentos serão liberados na próxima entrega, já com os limites de capacidade e o
        envio automático de e-mail.
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
      </div>
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
          <h3 class="text-base font-semibold text-slate-900">Próxima etapa</h3>
          <p class="text-sm text-slate-500">
            As telas de agendamentos, relatórios, usuários e auditoria estão preparadas na
            navegação e serão implementadas sobre esta base.
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

const cards = [
  {
    name: 'appointments',
    title: 'Agendamentos',
    description: 'Consulte, crie e atualize os agendamentos de caminhões por data e faixa horária.',
    icon: 'pi pi-calendar'
  },
  {
    name: 'reports',
    title: 'Relatórios',
    description: 'Visualize o consolidado de agendamentos e pallets efetivos por período.',
    icon: 'pi pi-chart-bar'
  },
  {
    name: 'audit',
    title: 'Auditoria',
    description: 'Acompanhe as operações registradas no sistema. Disponível para administradores.',
    icon: 'pi pi-history',
    adminOnly: true
  }
].filter((card) => !card.adminOnly || auth.isAdmin)
</script>
