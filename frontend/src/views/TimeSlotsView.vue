<template>
  <div class="space-y-6">
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">Faixas Horárias</h2>
        <p class="text-sm text-slate-500">
          Janelas de recebimento com exatamente 2 horas, sem sobreposição entre si.
        </p>
      </div>
      <div v-if="auth.isAdmin">
        <Button label="Nova Faixa" icon="pi pi-plus" @click="openCreateModal" />
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
        :value="timeSlots"
        :loading="loading"
        responsiveLayout="scroll"
        class="p-datatable-sm"
      >
        <template #empty>
          <div class="py-12 text-center">
            <i class="pi pi-clock text-4xl text-slate-300 mb-3" />
            <p class="text-base font-medium text-slate-700">Nenhuma faixa horária cadastrada</p>
          </div>
        </template>

        <Column field="id" header="ID" style="width: 80px">
          <template #body="{ data }">
            <span class="font-mono text-xs font-semibold text-slate-500">#{{ data.id }}</span>
          </template>
        </Column>

        <Column header="Faixa" style="min-width: 180px">
          <template #body="{ data }">
            <span
              class="inline-flex items-center gap-2 rounded-md bg-slate-100 px-2.5 py-1 text-sm font-semibold text-slate-700"
            >
              <i class="pi pi-clock text-xs text-slate-500" />
              {{ data.label || `${data.startTime} - ${data.endTime}` }}
            </span>
          </template>
        </Column>

        <Column field="startTime" header="Início" style="min-width: 100px" />
        <Column field="endTime" header="Fim" style="min-width: 100px" />

        <Column v-if="auth.isAdmin" header="Ações" style="width: 110px" alignFrozen="right">
          <template #body="{ data }">
            <div class="flex items-center gap-1">
              <Button
                icon="pi pi-pencil"
                severity="secondary"
                text
                rounded
                size="small"
                aria-label="Editar faixa horária"
                @click="openEditModal(data)"
              />
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                size="small"
                aria-label="Excluir faixa horária"
                @click="confirmDelete(data)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <Dialog
      v-model:visible="modalVisible"
      :header="editingId ? 'Editar Faixa Horária' : 'Nova Faixa Horária'"
      :modal="true"
      :style="{ width: '440px' }"
      class="p-fluid"
    >
      <form class="space-y-4 pt-2" @submit.prevent="saveTimeSlot">
        <div>
          <label for="startTime" class="mb-1 block text-xs font-semibold text-slate-700">
            Horário inicial <span class="text-red-500">*</span>
          </label>
          <input
            id="startTime"
            v-model="form.startTime"
            type="time"
            step="60"
            required
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>

        <div>
          <label for="endTime" class="mb-1 block text-xs font-semibold text-slate-700">
            Horário final <span class="text-red-500">*</span>
          </label>
          <input
            id="endTime"
            v-model="form.endTime"
            type="time"
            step="60"
            required
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>

        <p class="rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-500">
          A faixa deve ter exatamente 2 horas. Exemplo: 08:00 às 10:00.
        </p>

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
const timeSlots = ref([])

const form = reactive({
  startTime: '08:00',
  endTime: '10:00'
})

function minutes(value) {
  if (!value) return null
  const [hours, mins] = value.split(':').map(Number)
  return hours * 60 + mins
}

function validate() {
  const start = minutes(form.startTime)
  const end = minutes(form.endTime)
  if (start === null || end === null) return 'Informe os horários inicial e final.'
  if (end - start !== 120) return 'Cada faixa horária deve ter exatamente 2 horas.'
  return null
}

async function loadTimeSlots() {
  loading.value = true
  try {
    timeSlots.value = await referenceService.getTimeSlots()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: 'Não foi possível carregar as faixas horárias.',
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
  form.startTime = '08:00'
  form.endTime = '10:00'
  modalVisible.value = true
}

function openEditModal(timeSlot) {
  if (!auth.isAdmin) return
  editingId.value = timeSlot.id
  form.startTime = timeSlot.startTime
  form.endTime = timeSlot.endTime
  modalVisible.value = true
}

async function saveTimeSlot() {
  if (!auth.isAdmin) return
  const validationError = validate()
  if (validationError) {
    toast.add({ severity: 'error', summary: 'Validação', detail: validationError, life: 5000 })
    return
  }

  saving.value = true
  try {
    const payload = { startTime: form.startTime, endTime: form.endTime }
    if (editingId.value) {
      await referenceService.updateTimeSlot(editingId.value, payload)
    } else {
      await referenceService.createTimeSlot(payload)
    }
    modalVisible.value = false
    toast.add({
      severity: 'success',
      summary: 'Sucesso',
      detail: editingId.value ? 'Faixa horária atualizada.' : 'Faixa horária cadastrada.',
      life: 4000
    })
    await loadTimeSlots()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: error.response?.data?.message || 'Não foi possível salvar a faixa horária.',
      life: 6000
    })
  } finally {
    saving.value = false
  }
}

function confirmDelete(timeSlot) {
  if (!auth.isAdmin) return
  confirm.require({
    header: 'Confirmar Exclusão',
    message: `Deseja excluir a faixa ${timeSlot.label || `${timeSlot.startTime} - ${timeSlot.endTime}`}?`,
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancelar',
    acceptLabel: 'Excluir',
    rejectClass: 'p-button-secondary p-button-text',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await referenceService.deleteTimeSlot(timeSlot.id)
        toast.add({
          severity: 'success',
          summary: 'Excluída',
          detail: 'Faixa horária excluída.',
          life: 4000
        })
        await loadTimeSlots()
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: error.response?.status === 409 ? 'Exclusão bloqueada' : 'Erro ao excluir',
          detail: error.response?.data?.message || 'Não foi possível excluir a faixa horária.',
          life: 6000
        })
      }
    }
  })
}

onMounted(loadTimeSlots)
</script>
