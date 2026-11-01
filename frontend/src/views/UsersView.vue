<template>
  <div class="space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">Gestão de Usuários</h2>
        <p class="text-sm text-slate-500">
          Controle de acessos, perfis operacionais e credenciais do sistema
        </p>
      </div>
      <div>
        <Button
          label="Novo Usuário"
          icon="pi pi-user-plus"
          class="p-button-primary shadow-sm"
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

        <Column header="Usuário" style="min-width: 220px">
          <template #body="{ data }">
            <div class="flex items-center gap-3">
              <div class="flex h-9 w-9 items-center justify-center rounded-full bg-krill-100 font-bold text-krill-700 text-xs">
                {{ getInitials(data.name) }}
              </div>
              <div>
                <p class="font-semibold text-slate-900">{{ data.name }}</p>
                <p class="text-xs text-slate-400">{{ data.email }}</p>
              </div>
            </div>
          </template>
        </Column>

        <Column header="Perfil" style="min-width: 130px">
          <template #body="{ data }">
            <Tag
              v-if="data.role === 'ADMIN'"
              severity="info"
              value="ADMIN"
              icon="pi pi-shield"
            />
            <Tag
              v-else
              severity="secondary"
              value="USER"
              icon="pi pi-user"
            />
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
      :style="{ width: '480px' }"
      class="p-fluid"
    >
      <form @submit.prevent="saveUser" class="space-y-4 pt-2">
        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1">
            Nome Completo <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.name"
            type="text"
            maxlength="120"
            placeholder="Nome do operador"
            required
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>

        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1">
            E-mail <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.email"
            type="email"
            maxlength="160"
            placeholder="usuario@redekrill.com"
            required
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>

        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1">
            Perfil de Acesso <span class="text-red-500">*</span>
          </label>
          <Select
            v-model="form.role"
            :options="roleOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>

        <div v-if="editingId">
          <label class="block text-xs font-semibold text-slate-700 mb-1">
            Situação da Conta <span class="text-red-500">*</span>
          </label>
          <Select
            v-model="form.active"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            class="w-full"
          />
        </div>

        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1">
            Senha <span v-if="!editingId" class="text-red-500">*</span>
          </label>
          <input
            v-model="form.password"
            type="password"
            minlength="8"
            maxlength="72"
            :placeholder="editingId ? 'Preencha apenas se for alterar a senha' : 'Mínimo de 8 caracteres'"
            :required="!editingId"
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
          <p v-if="editingId" class="mt-1 text-[11px] text-slate-400">
            Deixe em branco para manter a senha atual.
          </p>
        </div>

        <div class="flex items-center justify-end gap-2 pt-2 border-t border-slate-200">
          <Button
            type="button"
            label="Cancelar"
            severity="secondary"
            text
            @click="modalVisible = false"
          />
          <Button
            type="submit"
            :label="editingId ? 'Salvar Alterações' : 'Cadastrar Usuário'"
            icon="pi pi-check"
            class="p-button-primary"
            :loading="saving"
          />
        </div>
      </form>
    </Dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import { useToast } from 'primevue/usetoast'
import { userService } from '../services/userService'

const toast = useToast()

const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)

const users = ref([])
const totalRecords = ref(0)
const pageIndex = ref(0)
const pageSize = ref(10)

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
  { label: 'Inativo', value: false }
]

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

async function loadUsers() {
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
  pageIndex.value = event.page
  pageSize.value = event.rows
  loadUsers()
}

function openCreateModal() {
  editingId.value = null
  form.name = ''
  form.email = ''
  form.role = 'USER'
  form.active = true
  form.password = ''
  modalVisible.value = true
}

function openEditModal(user) {
  editingId.value = user.id
  form.name = user.name
  form.email = user.email
  form.role = user.role
  form.active = user.active
  form.password = ''
  modalVisible.value = true
}

async function saveUser() {
  saving.value = true
  try {
    if (editingId.value) {
      const payload = {
        name: form.name.trim(),
        email: form.email.trim(),
        role: form.role,
        active: form.active,
        password: form.password ? form.password : null
      }
      await userService.updateUser(editingId.value, payload)
      toast.add({
        severity: 'success',
        summary: 'Sucesso',
        detail: 'Usuário atualizado com sucesso.',
        life: 4000
      })
    } else {
      const payload = {
        name: form.name.trim(),
        email: form.email.trim(),
        role: form.role,
        password: form.password
      }
      await userService.createUser(payload)
      toast.add({
        severity: 'success',
        summary: 'Sucesso',
        detail: 'Usuário cadastrado com sucesso.',
        life: 4000
      })
    }

    modalVisible.value = false
    await loadUsers()
  } catch (error) {
    const message = error.response?.data?.message || 'Falha ao salvar usuário.'
    toast.add({
      severity: 'error',
      summary: 'Erro',
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
