<template>
  <div class="space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">Tipos de Caminhão</h2>
        <p class="text-sm text-slate-500">
          Cadastro e gerenciamento dos modelos de veículos aceitos para agendamento
        </p>
      </div>
      <div v-if="auth.isAdmin">
        <Button
          label="Novo Tipo de Caminhão"
          icon="pi pi-plus"
          class="p-button-primary shadow-sm"
          @click="openCreateModal"
        />
      </div>
      <div
        v-else
        class="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-semibold text-slate-600"
      >
        <i class="pi pi-eye text-slate-400" />
        Modo de consulta (somente visualização)
      </div>
    </div>

    <!-- Tabela de Tipos de Caminhão -->
    <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <DataTable
        :value="truckTypes"
        :loading="loading"
        responsiveLayout="scroll"
        class="p-datatable-sm"
      >
        <template #empty>
          <div class="py-12 text-center">
            <i class="pi pi-truck text-4xl text-slate-300 mb-3" />
            <p class="text-base font-medium text-slate-700">Nenhum tipo de caminhão cadastrado</p>
            <p class="text-xs text-slate-400 mt-1">Utilize o botão acima para adicionar o primeiro modelo.</p>
          </div>
        </template>

        <Column field="id" header="ID" style="width: 80px">
          <template #body="{ data }">
            <span class="font-mono text-xs font-semibold text-slate-500">#{{ data.id }}</span>
          </template>
        </Column>

        <Column field="name" header="Nome do Modelo" style="min-width: 180px">
          <template #body="{ data }">
            <span class="font-semibold text-slate-900">{{ data.name }}</span>
          </template>
        </Column>

        <Column field="description" header="Descrição" style="min-width: 250px">
          <template #body="{ data }">
            <span class="text-sm text-slate-600">{{ data.description || '—' }}</span>
          </template>
        </Column>

        <Column header="Cadastrado em" style="min-width: 160px">
          <template #body="{ data }">
            <span class="text-xs text-slate-500">{{ formatDateTime(data.createdAt) }}</span>
          </template>
        </Column>

        <Column v-if="auth.isAdmin" header="Ações" style="width: 110px" alignFrozen="right">
          <template #body="{ data }">
            <div class="flex items-center gap-1">
              <Button
                icon="pi pi-pencil"
                severity="secondary"
                text
                rounded
                size="small"
                aria-label="Editar"
                @click="openEditModal(data)"
              />
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                size="small"
                aria-label="Excluir"
                @click="confirmDelete(data)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Modal de Criação / Edição -->
    <Dialog
      v-model:visible="modalVisible"
      :header="editingId ? 'Editar Tipo de Caminhão' : 'Novo Tipo de Caminhão'"
      :modal="true"
      :style="{ width: '480px' }"
      class="p-fluid"
    >
      <form @submit.prevent="saveTruckType" class="space-y-4 pt-2">
        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1">
            Nome do Modelo <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.name"
            type="text"
            maxlength="80"
            placeholder="Ex.: Carreta Baú, Truck, VUC..."
            required
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>

        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1">
            Descrição
          </label>
          <textarea
            v-model="form.description"
            rows="3"
            maxlength="255"
            placeholder="Observações operacionais ou especificações..."
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
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
            :label="editingId ? 'Salvar Alterações' : 'Cadastrar'"
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
import Dialog from 'primevue/dialog'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '../stores/auth'
import { truckTypeService } from '../services/truckTypeService'

const auth = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)
const truckTypes = ref([])

const form = reactive({
  name: '',
  description: ''
})

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

async function loadTruckTypes() {
  loading.value = true
  try {
    truckTypes.value = await truckTypeService.listTruckTypes()
  } catch (error) {
    console.error('Erro ao carregar tipos de caminhão:', error)
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: 'Não foi possível carregar os tipos de caminhão.',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  if (!auth.isAdmin) return

  editingId.value = null
  form.name = ''
  form.description = ''
  modalVisible.value = true
}

function openEditModal(truckType) {
  if (!auth.isAdmin) return

  editingId.value = truckType.id
  form.name = truckType.name
  form.description = truckType.description || ''
  modalVisible.value = true
}

async function saveTruckType() {
  if (!auth.isAdmin) return

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      description: form.description ? form.description.trim() : null
    }

    if (editingId.value) {
      await truckTypeService.updateTruckType(editingId.value, payload)
      toast.add({
        severity: 'success',
        summary: 'Sucesso',
        detail: 'Tipo de caminhão atualizado com sucesso.',
        life: 4000
      })
    } else {
      await truckTypeService.createTruckType(payload)
      toast.add({
        severity: 'success',
        summary: 'Sucesso',
        detail: 'Tipo de caminhão cadastrado com sucesso.',
        life: 4000
      })
    }

    modalVisible.value = false
    await loadTruckTypes()
  } catch (error) {
    const message = error.response?.data?.message || 'Falha ao salvar tipo de caminhão.'
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: message,
      life: 5000
    })
  } finally {
    saving.value = false
  }
}

function confirmDelete(truckType) {
  if (!auth.isAdmin) return

  confirm.require({
    header: 'Confirmar Exclusão',
    message: `Deseja realmente excluir o tipo de caminhão "${truckType.name}"?`,
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancelar',
    acceptLabel: 'Excluir',
    rejectClass: 'p-button-secondary p-button-text',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await truckTypeService.deleteTruckType(truckType.id)
        toast.add({
          severity: 'success',
          summary: 'Excluído',
          detail: `Tipo de caminhão "${truckType.name}" excluído com sucesso.`,
          life: 4000
        })
        await loadTruckTypes()
      } catch (error) {
        const message = error.response?.data?.message || 'Falha ao excluir o tipo de caminhão.'
        toast.add({
          severity: 'error',
          summary: error.response?.status === 409 ? 'Exclusão Bloqueada' : 'Erro ao excluir',
          detail: message,
          life: 6000
        })
      }
    }
  })
}

onMounted(() => {
  loadTruckTypes()
})
</script>
