<template>
  <div class="flex min-h-screen items-center justify-center bg-slate-100 px-4 py-10">
    <div class="w-full max-w-lg">
      <div class="mb-6 flex items-center gap-3">
        <div
          class="flex h-12 w-12 items-center justify-center rounded-xl bg-krill-500 font-bold text-white"
        >
          RK
        </div>
        <div>
          <p class="text-sm font-semibold uppercase tracking-widest text-krill-700">Rede Krill</p>
          <p class="text-xs text-slate-500">Agendamento de Caminhões</p>
        </div>
      </div>

      <div class="rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-slate-200/60">
        <div class="flex items-start gap-3">
          <i class="pi pi-lock mt-1 text-xl text-amber-500" />
          <div>
            <h1 class="text-2xl font-bold text-slate-900">Troque sua senha</h1>
            <p class="mt-2 text-sm text-slate-500">
              A senha inicial é temporária. Defina uma nova senha com pelo menos 12 caracteres antes
              de continuar.
            </p>
          </div>
        </div>

        <form class="mt-8 space-y-5" @submit.prevent="submit">
          <div>
            <label for="currentPassword" class="mb-2 block text-sm font-medium text-slate-700">
              Senha atual
            </label>
            <Password
              inputId="currentPassword"
              v-model="form.currentPassword"
              :feedback="false"
              toggle-mask
              autocomplete="current-password"
              class="w-full"
              input-class="w-full"
              required
            />
          </div>

          <div>
            <label for="newPassword" class="mb-2 block text-sm font-medium text-slate-700">
              Nova senha
            </label>
            <Password
              inputId="newPassword"
              v-model="form.newPassword"
              toggle-mask
              autocomplete="new-password"
              class="w-full"
              input-class="w-full"
              required
            />
            <p class="mt-1 text-xs text-slate-400">
              Mínimo de 12 caracteres. Evite senhas óbvias ou já utilizadas.
            </p>
          </div>

          <div>
            <label for="confirmPassword" class="mb-2 block text-sm font-medium text-slate-700">
              Confirme a nova senha
            </label>
            <Password
              inputId="confirmPassword"
              v-model="form.confirmPassword"
              :feedback="false"
              toggle-mask
              autocomplete="new-password"
              class="w-full"
              input-class="w-full"
              required
            />
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
            label="Salvar nova senha"
            icon="pi pi-check"
            class="w-full"
            :loading="saving"
            :disabled="saving"
          />

          <button
            type="button"
            class="w-full text-center text-sm font-medium text-slate-500 underline-offset-2 hover:text-slate-700 hover:underline"
            @click="leave"
          >
            Sair da conta
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Password from 'primevue/password'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const saving = ref(false)
const errorMessage = ref('')

async function submit() {
  errorMessage.value = ''

  if (form.newPassword.length < 12) {
    errorMessage.value = 'A nova senha deve ter pelo menos 12 caracteres.'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    errorMessage.value = 'A confirmação não confere com a nova senha.'
    return
  }
  if (form.currentPassword === form.newPassword) {
    errorMessage.value = 'A nova senha deve ser diferente da senha atual.'
    return
  }

  saving.value = true
  try {
    await auth.changePassword({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword
    })
    await router.push({ name: 'login', query: { passwordChanged: '1' } })
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message || 'Não foi possível alterar a senha. Tente novamente.'
  } finally {
    saving.value = false
  }
}

async function leave() {
  await auth.logout()
  await router.push({ name: 'login' })
}
</script>

<style scoped>
:deep(.p-password),
:deep(.p-password-input) {
  width: 100%;
}
</style>
