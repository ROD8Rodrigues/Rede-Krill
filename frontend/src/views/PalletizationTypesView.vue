<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">Tipos de Paletização</h2>
        <p class="text-sm text-slate-500">
          Os tipos obrigatórios têm multiplicadores fixos. Tipos adicionais podem ser cadastrados
          com multiplicador entre 1 e 100.
        </p>
      </div>
      <div v-if="auth.isAdmin">
        <Button label="Novo Tipo" icon="pi pi-plus" @click="openCreateModal" />
      </div>
      <div
        v-else
        class="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-semibold text-slate-600"
      >
        <i class="pi pi-eye text-slate-400" />
        Modo de consulta (somente visualização)
      </div>
    </div>

    <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <DataTable
        :value="palletizationTypes"
        :loading="loading"
        responsiveLayout="scroll"
        class="p-datatable-sm"
      >
        <template #empty>
          <div class="py-12 text-center">
            <i class="pi pi-box text-4xl text-slate-300 mb-3" />
            <p class="text-base font-medium text-slate-700">
              Nenhum tipo de paletização cadastrado
            </p>
          </div>
        </template>

        <Column field="id" header="ID" style="width: 80px">
          <template #body="{ data }">
            <span class="font-mono text-xs font-semibold text-slate-500">#{{ data.id }}</span>
          </template>
        </Column>

        <Column field="name" header="Nome" style="min-width: 180px">
          <template #body="{ data }">
            <span class="font-semibold text-slate-900">{{ data.name }}</span>
          </template>
        </Column>

        <Column header="Código" style="min-width: 150px">
          <template #body="{ data }">
            <span class="font-mono text-xs text-slate-500">{{ data.code }}</span>
          </template>
        </Column>

        <Column header="Multiplicador na faixa" style="min-width: 170px">
          <template #body="{ data }">
            <Tag
              :severity="data.capacityMultiplier > 1 ? 'warn' : 'success'"
              :value="`${data.capacityMultiplier}x`"
            />
          </template>
        </Column>

        <Column header="Obrigatório" style="min-width: 130px">
          <template #body="{ data }">
            <Tag
              :severity="isMandatory(data) ? 'info' : 'secondary'"
              :value="isMandatory(data) ? 'Sim' : 'Não'"
            />
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
                aria-label="Editar tipo de paletização"
                @click="openEditModal(data)"
              />
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                size="small"
                aria-label="Excluir tipo de paletização"
                :disabled="isMandatory(data)"
                @click="confirmDelete(data)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <Dialog
      v-model:visible="modalVisible"
      :header="editingId ? 'Editar Tipo de Paletização' : 'Novo Tipo de Paletização'"
      :modal="true"
      :style="{ width: '480px' }"
      class="p-fluid"
    >
      <form class="space-y-4 pt-2" @submit.prevent="savePalletizationType">
        <div>
          <label for="name" class="mb-1 block text-xs font-semibold text-slate-700">
            Nome <span class="text-red-500">*</span>
          </label>
          <input
            id="name"
            v-model="form.name"
            type="text"
            maxlength="80"
            placeholder="Ex.: Paletizado especial"
            required
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>

        <div>
          <label for="capacityMultiplier" class="mb-1 block text-xs font-semibold text-slate-700">
            Multiplicador na faixa <span class="text-red-500">*</span>
          </label>
          <input
            id="capacityMultiplier"
            v-model.number="form.capacityMultiplier"
            type="number"
            min="1"
            max="100"
            step="1"
            required
            :disabled="editingMandatory"
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500 disabled:bg-slate-100 disabled:text-slate-500"
          />
          <p class="mt-1 text-xs text-slate-400">
            <template v-if="editingMandatory">
              O multiplicador dos tipos obrigatórios é fixo e não pode ser alterado.
            </template>
            <template v-else>
              O multiplicador afeta apenas a ocupação da faixa horária, não a capacidade diária.
            </template>
          </p>
        </div>

        <div class="flex items-center justify-end gap-2 border-t border-slate-200 pt-2">
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
import Tag from 'primevue/tag'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '../stores/auth'
import { referenceService } from '../services/referenceService'

const auth = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)
const editingMandatory = ref(false)
const palletizationTypes = ref([])

const form = reactive({
  name: '',
  capacityMultiplier: 1
})

const mandatoryCodes = ['PALETIZADO', 'NAO_PALETIZADO']
const isMandatory = (type) => mandatoryCodes.includes(type.code)

async function loadPalletizationTypes() {
  loading.value = true
  try {
    palletizationTypes.value = await referenceService.getPalletizationTypes()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: 'Não foi possível carregar os tipos de paletização.',
      life: 5000
    })
    console.error(error)
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  if (!auth.isAdmin) return
  editingId.value = null
  editingMandatory.value = false
  form.name = ''
  form.capacityMultiplier = 1
  modalVisible.value = true
}

function openEditModal(type) {
  if (!auth.isAdmin) return
  editingId.value = type.id
  editingMandatory.value = isMandatory(type)
  form.name = type.name
  form.capacityMultiplier = type.capacityMultiplier
  modalVisible.value = true
}

async function savePalletizationType() {
  if (!auth.isAdmin) return

  const multiplier = Number(form.capacityMultiplier)
  if (!Number.isInteger(multiplier) || multiplier < 1 || multiplier > 100) {
    toast.add({
      severity: 'error',
      summary: 'Validação',
      detail: 'O multiplicador deve ser um número inteiro entre 1 e 100.',
      life: 5000
    })
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      capacityMultiplier: multiplier
    }
    if (editingId.value) {
      await referenceService.updatePalletizationType(editingId.value, payload)
    } else {
      await referenceService.createPalletizationType(payload)
    }
    modalVisible.value = false
    toast.add({
      severity: 'success',
      summary: 'Sucesso',
      detail: editingId.value
        ? 'Tipo de paletização atualizado.'
        : 'Tipo de paletização cadastrado.',
      life: 4000
    })
    await loadPalletizationTypes()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: error.response?.data?.message || 'Não foi possível salvar o tipo de paletização.',
      life: 6000
    })
  } finally {
    saving.value = false
  }
}

function confirmDelete(type) {
  if (!auth.isAdmin || isMandatory(type)) return
  confirm.require({
    header: 'Confirmar Exclusão',
    message: `Deseja excluir o tipo de paletização "${type.name}"?`,
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancelar',
    acceptLabel: 'Excluir',
    rejectClass: 'p-button-secondary p-button-text',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await referenceService.deletePalletizationType(type.id)
        toast.add({
          severity: 'success',
          summary: 'Excluído',
          detail: 'Tipo de paletização excluído.',
          life: 4000
        })
        await loadPalletizationTypes()
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: error.response?.status === 409 ? 'Exclusão bloqueada' : 'Erro ao excluir',
          detail:
            error.response?.data?.message || 'Não foi possível excluir o tipo de paletização.',
          life: 6000
        })
      }
    }
  })
}

onMounted(loadPalletizationTypes)
</script>
