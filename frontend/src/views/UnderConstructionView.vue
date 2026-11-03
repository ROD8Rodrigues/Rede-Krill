<template>
  <div class="mx-auto max-w-3xl">
    <section class="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
      <span
        class="inline-flex items-center gap-2 rounded-full bg-krill-50 px-3 py-1 text-xs font-semibold text-krill-700"
      >
        <i class="pi pi-wrench" />
        Em construção
      </span>

      <h2 class="mt-4 text-2xl font-bold text-slate-900">{{ title }}</h2>
      <p class="mt-3 text-sm leading-relaxed text-slate-500">{{ description }}</p>

      <div class="mt-8 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-6">
        <p class="text-sm font-semibold text-slate-700">
          O backend deste módulo já está disponível.
        </p>
        <p class="mt-1 text-sm text-slate-500">
          A interface será conectada aos endpoints na próxima etapa do desafio. Use o painel para
          navegar pelas demais áreas.
        </p>
      </div>

      <div class="mt-8 flex flex-wrap gap-3">
        <RouterLink
          :to="{ name: 'dashboard' }"
          class="inline-flex items-center gap-2 rounded-lg bg-krill-500 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-krill-600"
        >
          <i class="pi pi-home" />
          Voltar ao painel
        </RouterLink>
        <a
          href="/swagger-ui.html"
          target="_blank"
          rel="noreferrer"
          class="inline-flex items-center gap-2 rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:border-krill-300 hover:text-krill-700"
        >
          <i class="pi pi-book" />
          Documentação da API
        </a>
      </div>
    </section>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'

defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>
