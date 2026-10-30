<template>
  <div class="grid min-h-screen lg:grid-cols-2">
    <section
      class="relative hidden flex-col justify-between overflow-hidden bg-slate-900 px-12 py-14 text-white lg:flex"
    >
      <div class="absolute -right-24 top-16 h-72 w-72 rounded-full bg-krill-500/20 blur-3xl" />
      <div class="absolute -bottom-24 left-10 h-72 w-72 rounded-full bg-krill-400/10 blur-3xl" />

      <div class="relative flex items-center gap-3">
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-krill-500 font-bold">
          RK
        </div>
        <div>
          <p class="text-sm font-semibold uppercase tracking-widest text-krill-300">Rede Krill</p>
          <p class="text-xs text-slate-400">Agendamento de Caminhões</p>
        </div>
      </div>

      <div class="relative max-w-lg">
        <p class="mb-4 inline-flex items-center gap-2 rounded-full border border-krill-500/40 bg-krill-500/10 px-3 py-1 text-xs font-semibold text-krill-200">
          <i class="pi pi-truck" />
          Logística inteligente
        </p>
        <h1 class="text-4xl font-bold leading-tight">
          Organize a chegada dos caminhões com uma visão clara da operação.
        </h1>
        <p class="mt-5 text-base leading-relaxed text-slate-300">
          Agende fornecedores por faixa horária, acompanhe a capacidade diária de pallets e
          mantenha a operação da Rede Krill sob controle.
        </p>
      </div>

      <div class="relative grid grid-cols-2 gap-4 text-sm">
        <div class="rounded-xl border border-slate-700/70 bg-slate-800/50 p-4">
          <p class="font-semibold text-krill-300">4 faixas por dia</p>
          <p class="mt-1 text-slate-400">Janelas de 2 horas para receber os caminhões.</p>
        </div>
        <div class="rounded-xl border border-slate-700/70 bg-slate-800/50 p-4">
          <p class="font-semibold text-krill-300">Limites monitorados</p>
          <p class="mt-1 text-slate-400">Avisos de capacidade sem bloquear a operação.</p>
        </div>
      </div>
    </section>

    <section class="flex items-center justify-center bg-slate-100 px-4 py-12 sm:px-8">
      <div class="w-full max-w-md">
        <div class="mb-8 flex items-center gap-3 lg:hidden">
          <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-krill-500 font-bold text-white">
            RK
          </div>
          <div>
            <p class="text-sm font-semibold uppercase tracking-widest text-krill-600">Rede Krill</p>
            <p class="text-xs text-slate-500">Agendamento de Caminhões</p>
          </div>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-slate-200/60">
          <h2 class="text-2xl font-bold text-slate-900">Acessar o painel</h2>
          <p class="mt-2 text-sm text-slate-500">
            Informe suas credenciais para gerenciar os agendamentos.
          </p>

          <form class="mt-8 space-y-5" @submit.prevent="submit">
            <div>
              <label for="email" class="mb-2 block text-sm font-medium text-slate-700">
                E-mail
              </label>
              <InputText
                id="email"
                v-model="form.email"
                type="email"
                autocomplete="username"
                placeholder="seu.email@redekrill.com"
                class="w-full"
                :invalid="Boolean(fieldErrors.email)"
                required
              />
              <small v-if="fieldErrors.email" class="mt-1 block text-xs text-red-600">
                {{ fieldErrors.email }}
              </small>
            </div>

            <div>
              <label for="password" class="mb-2 block text-sm font-medium text-slate-700">
                Senha
              </label>
              <Password
                id="password"
                v-model="form.password"
                :feedback="false"
                toggle-mask
                autocomplete="current-password"
                placeholder="Sua senha"
                class="w-full"
                input-class="w-full"
                :invalid="Boolean(fieldErrors.password)"
                required
              />
              <small v-if="fieldErrors.password" class="mt-1 block text-xs text-red-600">
                {{ fieldErrors.password }}
              </small>
            </div>

            <div
              v-if="errorMessage"
              class="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-700"
              role="alert"
            >
              <i class="pi pi-exclamation-circle mt-0.5" />
              <span>{{ errorMessage }}</span>
            </div>

            <Button
              type="submit"
              label="Entrar"
              icon="pi pi-sign-in"
              class="w-full"
              :loading="auth.loading"
              :disabled="auth.loading"
            />
          </form>

          <p class="mt-6 text-center text-xs text-slate-400">
            Acesso restrito aos usuários autorizados da Rede Krill.
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive({
  email: '',
  password: ''
})

const errorMessage = ref('')
const fieldErrors = ref({})

async function submit() {
  errorMessage.value = ''
  fieldErrors.value = {}

  try {
    await auth.login({ email: form.email, password: form.password })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.push(redirect)
  } catch (error) {
    const data = error.response?.data
    fieldErrors.value = data?.fieldErrors || {}
    errorMessage.value =
      data?.message || 'Não foi possível autenticar. Verifique suas credenciais e tente novamente.'
  }
}
</script>

<style scoped>
:deep(.p-password),
:deep(.p-password-input) {
  width: 100%;
}
</style>
