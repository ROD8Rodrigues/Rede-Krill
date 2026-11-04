<template>
  <div class="space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">Gestão de Usuários</h2>
        <p class="text-sm text-slate-500">
          Controle de acessos, perfis operacionais e credenciais. A desativação equivale à exclusão
          lógica e preserva o histórico.
        </p>
      </div>
      <div>
        <Button
          label="Novo Usuário"
          icon="pi pi-user-plus"
          class="p-button-primary shadow-sm"
          :disabled="!auth.isAdmin"
          @click="openCreateModal"
        />
      </div>
    </div>

    <!-- Tabela de Usuários -->
    <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <DataTable
        :value="users"
        :loading="loading"
        lazy
        paginator
        :rows="pageSize"
        :totalRecords="totalRecords"
        :first="pageIndex * pageSize"
        @page="onPageChange"
        responsiveLayout="scroll"
        class="p-datatable-sm"
      >
        <template #empty>
          <div class="py-12 text-center">
            <i class="pi pi-users text-4xl text-slate-300 mb-3" />
            <p class="text-base font-medium text-slate-700">Nenhum usuário cadastrado</p>
          </div>
        </template>

        <Column field="id" header="ID" style="width: 80px">
          <template #body="{ data }">
            <span class="font-mono text-xs font-semibold text-slate-500">#{{ data.id }}</span>
          </template>
        </Column>

        <Column header="Usuário" style="min-width: 240px">
          <template #body="{ data }">
            <div class="flex items-center gap-3">
              <div
                class="flex h-9 w-9 items-center justify-center rounded-full bg-krill-100 font-bold text-krill-700 text-xs"
              >
                {{ getInitials(data.name) }}
              </div>
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <p class="font-semibold text-slate-900">{{ data.name }}</p>
                  <Tag
                    v-if="isCurrentUser(data)"
                    severity="warn"
                    value="Sua conta"
                    class="text-[10px]"
                  />
                </div>
                <p class="truncate text-xs text-slate-400">{{ data.email }}</p>
              </div>
            </div>
          </template>
        </Column>

        <Column header="Perfil" style="min-width: 130px">
          <template #body="{ data }">
            <Tag v-if="data.role === 'ADMIN'" severity="info" value="ADMIN" icon="pi pi-shield" />
            <Tag v-else severity="secondary" value="USER" icon="pi pi-user" />
          </template>
        </Column>

        <Column header="Status" style="min-width: 120px">
          <template #body="{ data }">
            <Tag
              :severity="data.active ? 'success' : 'danger'"
              :value="data.active ? 'Ativo' : 'Inativo'"
            />
          </template>
        </Column>

        <Column header="Cadastrado em" style="min-width: 160px">
          <template #body="{ data }">
            <span class="text-xs text-slate-500">{{ formatDateTime(data.createdAt) }}</span>
          </template>
        </Column>

        <Column header="Ações" style="width: 90px" alignFrozen="right">
          <template #body="{ data }">
            <Button
              icon="pi pi-pencil"
              severity="secondary"
              text
              rounded
              size="small"
              aria-label="Editar"
              :disabled="!auth.isAdmin"
              @click="openEditModal(data)"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Modal de Criação / Edição -->
    <Dialog
      v-model:visible="modalVisible"
      :header="editingId ? 'Editar Usuário' : 'Novo Usuário'"
      :modal="true"
      :style="{ width: '520px' }"
      class="p-fluid"
    >
      <form novalidate class="space-y-4 pt-2" @submit.prevent="saveUser">
        <div
          v-if="isEditingSelf"
          class="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-800"
        >
          <i class="pi pi-lock mr-1.5" />
          Por segurança, você não pode desativar sua própria conta nem remover seu perfil ADMIN.
        </div>

        <div>
          <label for="user-name" class="block text-xs font-semibold text-slate-700 mb-1">
            Nome Completo <span class="text-red-500">*</span>
          </label>
          <input
            id="user-name"
            v-model="form.name"
            type="text"
            maxlength="120"
            placeholder="Nome do operador"
            autocomplete="name"
            class="w-full rounded-lg border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1"
            :class="fieldClass('name')"
            :aria-invalid="Boolean(errors.name)"
            @input="clearError('name')"
          />
          <p v-if="errors.name" class="mt-1 text-[11px] font-medium text-red-600">
            {{ errors.name }}
          </p>
        </div>

        <div>
          <label for="user-email" class="block text-xs font-semibold text-slate-700 mb-1">
            E-mail <span class="text-red-500">*</span>
          </label>
          <input
            id="user-email"
            v-model="form.email"
            type="email"
            maxlength="160"
            placeholder="usuario@redekrill.com"
            autocomplete="email"
            class="w-full rounded-lg border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1"
            :class="fieldClass('email')"
            :aria-invalid="Boolean(errors.email)"
            @input="clearError('email')"
          />
          <p v-if="errors.email" class="mt-1 text-[11px] font-medium text-red-600">
            {{ errors.email }}
          </p>
        </div>

        <div>
          <label for="user-role" class="block text-xs font-semibold text-slate-700 mb-1">
            Perfil de Acesso <span class="text-red-500">*</span>
          </label>
          <Select
            id="user-role"
            v-model="form.role"
            :options="roleOptions"
            optionLabel="label"
            optionValue="value"
            :disabled="isEditingSelf"
            class="w-full"
            :class="{ 'p-invalid': errors.role }"
            @change="clearError('role')"
          />
          <p v-if="errors.role" class="mt-1 text-[11px] font-medium text-red-600">
            {{ errors.role }}
          </p>
          <p v-else-if="isEditingSelf" class="mt-1 text-[11px] text-amber-700">
            Seu próprio perfil deve permanecer ADMIN.
          </p>
        </div>

        <div v-if="editingId">
          <label for="user-status" class="block text-xs font-semibold text-slate-700 mb-1">
            Situação da Conta <span class="text-red-500">*</span>
          </label>
          <Select
            id="user-status"
            v-model="form.active"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            :disabled="isEditingSelf"
            class="w-full"
            :class="{ 'p-invalid': errors.active }"
            @change="clearError('active')"
          />
          <p v-if="errors.active" class="mt-1 text-[11px] font-medium text-red-600">
            {{ errors.active }}
          </p>
          <p v-else class="mt-1 text-[11px] text-slate-400">
            Desativar a conta equivale à exclusão lógica; o histórico é preservado.
          </p>
        </div>

        <div>
          <label for="user-password" class="block text-xs font-semibold text-slate-700 mb-1">
            Senha <span v-if="!editingId" class="text-red-500">*</span>
          </label>
          <input
            id="user-password"
            v-model="form.password"
            type="password"
            minlength="12"
            maxlength="72"
            :placeholder="
              editingId ? 'Preencha apenas se for alterar a senha' : 'Mínimo de 12 caracteres'
            "
            autocomplete="new-password"
            class="w-full rounded-lg border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1"
            :class="fieldClass('password')"
            :aria-invalid="Boolean(errors.password)"
            @input="clearError('password')"
          />
          <p v-if="errors.password" class="mt-1 text-[11px] font-medium text-red-600">
            {{ errors.password }}
          </p>
          <p v-else-if="editingId" class="mt-1 text-[11px] text-slate-400">
            Deixe em branco para manter a senha atual.
          </p>
          <p v-else class="mt-1 text-[11px] text-slate-400">
            Mínimo de {{ MIN_PASSWORD_LENGTH }} caracteres, com letras e números.
          </p>
        </div>

        <div class="flex items-center justify-end gap-2 pt-2 border-t border-slate-200">
          <Button
            type="button"
            label="Cancelar"
            severity="secondary"
            text
            :disabled="saving"
            @click="closeModal"
          />
          <Button
            type="submit"
            :label="editingId ? 'Salvar Alterações' : 'Cadastrar Usuário'"
            icon="pi pi-check"
            class="p-button-primary"
            :loading="saving"
            :disabled="saving"
          />
        </div>
      </form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '../stores/auth'
import { userService } from '../services/userService'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const MIN_PASSWORD_LENGTH = 12
const MAX_PASSWORD_LENGTH = 72

const auth = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)
const editingUser = ref(null)

const users = ref([])
const totalRecords = ref(0)
const pageIndex = ref(0)
const pageSize = ref(10)

const errors = reactive({
  name: '',
  email: '',
  password: '',
  role: '',
  active: ''
})

const form = reactive({
  name: '',
  email: '',
  role: 'USER',
  active: true,
  password: ''
})

const roleOptions = [
  { label: 'ADMIN - Acesso Total', value: 'ADMIN' },
  { label: 'USER - Apenas Visualização', value: 'USER' }
]

const statusOptions = [
  { label: 'Ativo', value: true },
  { label: 'Inativo (exclusão lógica)', value: false }
]

const isEditingSelf = computed(() => sameUser(editingUser.value, auth.user))

function sameUser(first, second) {
  if (first?.id == null || second?.id == null) return false
  return String(first.id) === String(second.id)
}

function isCurrentUser(user) {
  return sameUser(user, auth.user)
}

function getInitials(name) {
  if (!name) return 'U'
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

function formatDateTime(isoString) {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function fieldClass(field) {
  if (errors[field]) {
    return 'border-red-400 focus:border-red-500 focus:ring-red-500'
  }
  return 'border-slate-300 focus:border-krill-500 focus:ring-krill-500'
}

function clearError(field) {
  errors[field] = ''
}

function clearErrors() {
  Object.keys(errors).forEach((field) => {
    errors[field] = ''
  })
}

function resetForm() {
  form.name = ''
  form.email = ''
  form.role = 'USER'
  form.active = true
  form.password = ''
  clearErrors()
}

function validateForm() {
  clearErrors()

  const name = form.name.trim()
  const email = form.email.trim()

  if (!name) {
    errors.name = 'Nome é obrigatório.'
  } else if (name.length > 120) {
    errors.name = 'Nome deve ter no máximo 120 caracteres.'
  }

  if (!email) {
    errors.email = 'E-mail é obrigatório.'
  } else if (email.length > 160) {
    errors.email = 'E-mail deve ter no máximo 160 caracteres.'
  } else if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Informe um e-mail válido.'
  }

  if (!['ADMIN', 'USER'].includes(form.role)) {
    errors.role = 'Selecione um perfil de acesso válido.'
  }

  if (editingId.value) {
    if (typeof form.active !== 'boolean') {
      errors.active = 'Selecione a situação da conta.'
    }

    if (isEditingSelf.value) {
      if (!form.active) {
        errors.active = 'Você não pode desativar sua própria conta.'
      }
      if (form.role !== 'ADMIN') {
        errors.role = 'Você não pode remover seu próprio perfil ADMIN.'
      }
    }
  }

  if (!editingId.value && !form.password) {
    errors.password = 'Senha é obrigatória.'
  } else if (form.password) {
    if (form.password.length < MIN_PASSWORD_LENGTH) {
      errors.password = `A senha deve ter pelo menos ${MIN_PASSWORD_LENGTH} caracteres.`
    } else if (form.password.length > MAX_PASSWORD_LENGTH) {
      errors.password = `A senha deve ter no máximo ${MAX_PASSWORD_LENGTH} caracteres.`
    } else if (!/[a-zA-Z]/.test(form.password) || !/\d/.test(form.password)) {
      errors.password = 'A senha deve conter letras e números.'
    }
  }

  return Object.values(errors).every((message) => !message)
}

function needsSensitiveConfirmation() {
  if (!editingUser.value) return false

  const deactivating = editingUser.value.active && !form.active
  const roleChanged = editingUser.value.role !== form.role
  const passwordChanged = form.password.length > 0

  return deactivating || roleChanged || passwordChanged
}

function buildConfirmationMessage() {
  const changes = []

  if (editingUser.value.active && !form.active) {
    changes.push('desativar a conta (exclusão lógica)')
  }
  if (editingUser.value.role !== form.role) {
    changes.push(`alterar o perfil de ${editingUser.value.role} para ${form.role}`)
  }
  if (form.password.length > 0) {
    changes.push('redefinir a senha')
  }

  return `Deseja confirmar as seguintes alterações para ${editingUser.value.name}? ${changes.join('; ')}.`
}

async function loadUsers() {
  if (!auth.isAdmin) return

  loading.value = true
  try {
    const data = await userService.listUsers({
      page: pageIndex.value,
      size: pageSize.value
    })
    users.value = data.content || []
    totalRecords.value = data.totalElements || 0
  } catch (error) {
    console.error('Erro ao carregar usuários:', error)
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: 'Não foi possível carregar a lista de usuários.',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

function onPageChange(event) {
  if (!auth.isAdmin) return
  pageIndex.value = event.page
  pageSize.value = event.rows
  loadUsers()
}

function closeModal() {
  if (saving.value) return
  modalVisible.value = false
  editingUser.value = null
}

function openCreateModal() {
  if (!auth.isAdmin) return

  editingId.value = null
  editingUser.value = null
  resetForm()
  modalVisible.value = true
}

function openEditModal(user) {
  if (!auth.isAdmin) return

  editingId.value = user.id
  editingUser.value = user
  form.name = user.name
  form.email = user.email
  form.role = user.role
  form.active = user.active
  form.password = ''
  clearErrors()
  modalVisible.value = true
}

function saveUser() {
  if (!auth.isAdmin) {
    toast.add({
      severity: 'error',
      summary: 'Acesso negado',
      detail: 'Somente administradores podem alterar usuários.',
      life: 5000
    })
    return
  }

  if (!validateForm()) {
    toast.add({
      severity: 'warn',
      summary: 'Revise os campos',
      detail: 'Corrija os campos destacados antes de salvar.',
      life: 4000
    })
    return
  }

  if (editingId.value && needsSensitiveConfirmation()) {
    confirm.require({
      header: 'Confirmar alteração sensível',
      message: buildConfirmationMessage(),
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: 'Cancelar',
      acceptLabel: 'Confirmar',
      rejectClass: 'p-button-secondary p-button-text',
      acceptClass: 'p-button-primary',
      accept: persistUser
    })
    return
  }

  persistUser()
}

async function persistUser() {
  if (saving.value) return

  saving.value = true
  const isEditing = Boolean(editingId.value)
  const wasDeactivated = isEditing && editingUser.value?.active && !form.active

  try {
    if (isEditing) {
      const payload = {
        name: form.name.trim(),
        email: form.email.trim(),
        role: form.role,
        active: form.active,
        password: form.password ? form.password : null
      }
      await userService.updateUser(editingId.value, payload)
    } else {
      const payload = {
        name: form.name.trim(),
        email: form.email.trim(),
        role: form.role,
        password: form.password
      }
      await userService.createUser(payload)
    }

    modalVisible.value = false
    editingUser.value = null
    await loadUsers()

    toast.add({
      severity: 'success',
      summary: 'Sucesso',
      detail: wasDeactivated
        ? 'Usuário desativado com sucesso. O histórico foi preservado.'
        : isEditing
          ? 'Usuário atualizado com sucesso.'
          : 'Usuário cadastrado com sucesso.',
      life: 4000
    })
  } catch (error) {
    const message = error.response?.data?.message || 'Falha ao salvar usuário.'
    toast.add({
      severity: 'error',
      summary: error.response?.status === 409 ? 'Conflito' : 'Erro',
      detail: message,
      life: 6000
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>
