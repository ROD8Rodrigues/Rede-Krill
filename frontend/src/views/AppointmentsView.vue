<template>
  <div class="space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">Agendamentos de Caminhões</h2>
        <p class="text-sm text-slate-500">
          Gerenciamento e consulta de recebimento de cargas no centro de distribuição
        </p>
      </div>
      <div v-if="auth.isAdmin">
        <Button
          label="Novo Agendamento"
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

    <!-- Banner de Avisos de Capacidade (se houver avisos no último agendamento salvo) -->
    <div
      v-if="capacityWarnings.length > 0"
      class="rounded-xl border border-amber-300 bg-amber-50 p-4 shadow-sm"
    >
      <div class="flex items-start justify-between">
        <div class="flex items-start gap-3">
          <i class="pi pi-exclamation-triangle mt-0.5 text-lg text-amber-600" />
          <div>
            <h3 class="text-sm font-semibold text-amber-900">
              Atenção: Limite operacional de capacidade ultrapassado
            </h3>
            <p class="mt-0.5 text-xs text-amber-700">
              O agendamento foi salvo com sucesso, porém excedeu os parâmetros de recebimento:
            </p>
            <ul class="mt-2 list-inside list-disc space-y-1 text-xs font-medium text-amber-800">
              <li v-for="(warn, index) in capacityWarnings" :key="index">{{ warn }}</li>
            </ul>
          </div>
        </div>
        <button
          type="button"
          class="rounded p-1 text-amber-500 hover:bg-amber-100 hover:text-amber-700"
          aria-label="Fechar alerta"
          @click="capacityWarnings = []"
        >
          <i class="pi pi-times text-xs" />
        </button>
      </div>
    </div>

    <!-- Barra de Filtros -->
    <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 lg:items-end">
        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">
            Data Inicial
          </label>
          <input
            v-model="filters.startDate"
            type="date"
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>
        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">
            Data Final
          </label>
          <input
            v-model="filters.endDate"
            type="date"
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>
        <div class="flex items-center gap-2 lg:col-span-2">
          <Button
            label="Filtrar"
            icon="pi pi-filter"
            class="p-button-outlined"
            :loading="loading"
            @click="applyFilters"
          />
          <Button
            label="Limpar"
            icon="pi pi-filter-slash"
            severity="secondary"
            text
            @click="clearFilters"
          />
          <Button
            icon="pi pi-refresh"
            severity="secondary"
            text
            rounded
            aria-label="Atualizar"
            :loading="loading"
            @click="loadAppointments"
          />
        </div>
      </div>
    </div>

    <!-- Tabela de Agendamentos -->
    <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <DataTable
        :value="appointments"
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
            <i class="pi pi-calendar-times text-4xl text-slate-300 mb-3" />
            <p class="text-base font-medium text-slate-700">Nenhum agendamento encontrado</p>
            <p class="text-xs text-slate-400 mt-1">Tente ajustar o filtro de datas ou cadastre um novo agendamento.</p>
          </div>
        </template>

        <Column field="date" header="Data" sortable style="min-width: 110px">
          <template #body="{ data }">
            <span class="font-medium text-slate-900">{{ formatDate(data.date) }}</span>
          </template>
        </Column>

        <Column header="Faixa Horária" style="min-width: 140px">
          <template #body="{ data }">
            <span class="inline-flex items-center gap-1.5 rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700">
              <i class="pi pi-clock text-[10px] text-slate-500" />
              {{ data.timeSlot?.startTime }} às {{ data.timeSlot?.endTime }}
            </span>
          </template>
        </Column>

        <Column header="Fornecedor" style="min-width: 200px">
          <template #body="{ data }">
            <div>
              <p class="font-semibold text-slate-900">{{ data.supplierName }}</p>
              <p class="text-xs text-slate-400">{{ data.supplierEmail }}</p>
            </div>
          </template>
        </Column>

        <Column field="truckPlate" header="Placa" style="min-width: 110px">
          <template #body="{ data }">
            <span class="rounded border border-slate-300 bg-slate-50 px-2 py-1 font-mono text-xs font-bold uppercase tracking-wider text-slate-800">
              {{ data.truckPlate }}
            </span>
          </template>
        </Column>

        <Column header="Tipo Caminhão" style="min-width: 130px">
          <template #body="{ data }">
            <span class="text-sm text-slate-700">{{ data.truckType?.name }}</span>
          </template>
        </Column>

        <Column header="Paletização" style="min-width: 150px">
          <template #body="{ data }">
            <Tag
              v-if="data.palletizationType?.code === 'PALETIZADO'"
              severity="success"
              value="Paletizado"
            />
            <Tag
              v-else
              severity="warn"
              value="Não Paletizado (2x)"
            />
          </template>
        </Column>

        <Column header="Pallets" style="min-width: 140px">
          <template #body="{ data }">
            <div class="text-xs">
              <p class="text-slate-500">Informado: <span class="font-semibold text-slate-800">{{ data.palletQuantity }}</span></p>
              <p class="text-slate-700">Efetivo: <span class="font-bold text-krill-700">{{ data.effectivePallets }}</span></p>
            </div>
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
      :header="editingId ? 'Editar Agendamento' : 'Novo Agendamento'"
      :modal="true"
      :style="{ width: '560px' }"
      class="p-fluid"
    >
      <form @submit.prevent="saveAppointment" class="space-y-4 pt-2">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <!-- Nome do Fornecedor -->
          <div class="sm:col-span-2">
            <label class="block text-xs font-semibold text-slate-700 mb-1">
              Nome do Fornecedor <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.supplierName"
              type="text"
              maxlength="160"
              placeholder="Ex.: Distribuidora de Alimentos Silva"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
          </div>

          <!-- E-mail do Fornecedor -->
          <div class="sm:col-span-2">
            <label class="block text-xs font-semibold text-slate-700 mb-1">
              E-mail do Fornecedor <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.supplierEmail"
              type="email"
              maxlength="160"
              placeholder="fornecedor@empresa.com"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
          </div>

          <!-- Placa do Caminhão -->
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1">
              Placa do Caminhão <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.truckPlate"
              type="text"
              maxlength="20"
              placeholder="ABC1D23"
              required
              class="w-full uppercase rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
          </div>

          <!-- Tipo de Caminhão -->
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1">
              Tipo de Caminhão <span class="text-red-500">*</span>
            </label>
            <Select
              v-model="form.truckTypeId"
              :options="truckTypes"
              optionLabel="name"
              optionValue="id"
              placeholder="Selecione o tipo"
              class="w-full"
            />
          </div>

          <!-- Data do Agendamento -->
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1">
              Data do Agendamento <span class="text-red-500">*</span>
            </label>
            <input
              v-model="form.date"
              type="date"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
          </div>

          <!-- Faixa Horária -->
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1">
              Faixa Horária <span class="text-red-500">*</span>
            </label>
            <Select
              v-model="form.timeSlotId"
              :options="timeSlots"
              optionLabel="label"
              optionValue="id"
              placeholder="Selecione a faixa"
              class="w-full"
            />
          </div>

          <!-- Tipo de Paletização -->
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1">
              Paletização <span class="text-red-500">*</span>
            </label>
            <Select
              v-model="form.palletizationTypeId"
              :options="palletizationTypes"
              optionLabel="name"
              optionValue="id"
              class="w-full"
            />
          </div>

          <!-- Quantidade de Pallets -->
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1">
              Quantidade de Pallets <span class="text-red-500">*</span>
            </label>
            <input
              v-model.number="form.palletQuantity"
              type="number"
              min="1"
              max="1000000"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
          </div>
        </div>

        <!-- Card de Cálculo em Tempo Real de Pallets Efetivos -->
        <div class="rounded-xl border border-krill-100 bg-krill-50/60 p-4">
          <div class="flex items-center justify-between text-xs font-semibold uppercase tracking-wider text-krill-800">
            <span>Cálculo de Capacidade</span>
            <span v-if="selectedMultiplier > 1" class="rounded bg-amber-100 px-2 py-0.5 text-amber-800">
              Multiplicador: {{ selectedMultiplier }}x
            </span>
            <span v-else class="text-slate-500">Multiplicador: 1x</span>
          </div>
          <div class="mt-2 flex items-baseline justify-between">
            <span class="text-xs text-slate-600">Pallets Efetivos considerados:</span>
            <span class="text-xl font-bold text-krill-700">{{ calculatedEffectivePallets }}</span>
          </div>
          <p v-if="selectedMultiplier > 1" class="mt-1 text-[11px] text-amber-700">
            * Cargas Não Paletizadas dobram o impacto na capacidade operacional.
          </p>
        </div>

        <!-- Rodapé do Modal -->
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
import { appointmentService } from '../services/appointmentService'
import { referenceService } from '../services/referenceService'

const auth = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)
const capacityWarnings = ref([])

const appointments = ref([])
const totalRecords = ref(0)
const pageIndex = ref(0)
const pageSize = ref(10)

const timeSlots = ref([])
const palletizationTypes = ref([])
const truckTypes = ref([])

const filters = reactive({
  startDate: '',
  endDate: ''
})

const form = reactive({
  supplierName: '',
  supplierEmail: '',
  truckPlate: '',
  date: '',
  timeSlotId: null,
  truckTypeId: null,
  palletizationTypeId: null,
  palletQuantity: 1
})

const selectedPalletization = computed(() =>
  palletizationTypes.value.find((type) => type.id === form.palletizationTypeId)
)

const selectedMultiplier = computed(() => selectedPalletization.value?.capacityMultiplier || 1)

const calculatedEffectivePallets = computed(() => {
  const qty = Number(form.palletQuantity) || 0
  return qty * selectedMultiplier.value
})

function formatDate(isoDate) {
  if (!isoDate) return ''
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

function getTodayIso() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function loadReferenceData() {
  try {
    const [slots, pTypes, tTypes] = await Promise.all([
      referenceService.getTimeSlots(),
      referenceService.getPalletizationTypes(),
      referenceService.getTruckTypes()
    ])
    timeSlots.value = slots.map((s) => ({
      ...s,
      label: s.label || `${s.startTime} às ${s.endTime}`
    }))
    palletizationTypes.value = pTypes
    truckTypes.value = tTypes
  } catch (error) {
    console.error('Erro ao carregar dados de referência:', error)
  }
}

async function loadAppointments() {
  loading.value = true
  try {
    const data = await appointmentService.listAppointments({
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      page: pageIndex.value,
      size: pageSize.value
    })
    appointments.value = data.content || []
    totalRecords.value = data.totalElements || 0
  } catch (error) {
    console.error('Erro ao carregar agendamentos:', error)
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: 'Não foi possível carregar a lista de agendamentos.',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

function onPageChange(event) {
  pageIndex.value = event.page
  pageSize.value = event.rows
  loadAppointments()
}

function applyFilters() {
  pageIndex.value = 0
  loadAppointments()
}

function clearFilters() {
  filters.startDate = ''
  filters.endDate = ''
  pageIndex.value = 0
  loadAppointments()
}

function openCreateModal() {
  if (!auth.isAdmin) return

  editingId.value = null
  form.supplierName = ''
  form.supplierEmail = ''
  form.truckPlate = ''
  form.date = getTodayIso()
  form.timeSlotId = timeSlots.value[0]?.id || null
  form.truckTypeId = truckTypes.value[0]?.id || null
  form.palletizationTypeId = palletizationTypes.value[0]?.id || null
  form.palletQuantity = 1
  modalVisible.value = true
}

function openEditModal(appointment) {
  if (!auth.isAdmin) return

  editingId.value = appointment.id
  form.supplierName = appointment.supplierName
  form.supplierEmail = appointment.supplierEmail
  form.truckPlate = appointment.truckPlate
  form.date = appointment.date
  form.timeSlotId = appointment.timeSlot?.id || null
  form.truckTypeId = appointment.truckType?.id || null
  form.palletizationTypeId = appointment.palletizationType?.id || null
  form.palletQuantity = appointment.palletQuantity
  modalVisible.value = true
}

async function saveAppointment() {
  if (!auth.isAdmin) return

  saving.value = true
  try {
    const payload = {
      supplierName: form.supplierName.trim(),
      supplierEmail: form.supplierEmail.trim(),
      truckPlate: form.truckPlate.trim().toUpperCase(),
      date: form.date,
      timeSlotId: form.timeSlotId,
      truckTypeId: form.truckTypeId,
      palletizationTypeId: form.palletizationTypeId,
      palletQuantity: form.palletQuantity
    }

    let result
    if (editingId.value) {
      result = await appointmentService.updateAppointment(editingId.value, payload)
    } else {
      result = await appointmentService.createAppointment(payload)
    }

    modalVisible.value = false
    await loadAppointments()

    if (result?.warnings && result.warnings.length > 0) {
      capacityWarnings.value = result.warnings
      toast.add({
        severity: 'warn',
        summary: 'Salvo com aviso de capacidade',
        detail: result.warnings.join(' | '),
        life: 8000
      })
    } else {
      capacityWarnings.value = []
      toast.add({
        severity: 'success',
        summary: 'Sucesso',
        detail: editingId.value
          ? 'Agendamento atualizado com sucesso.'
          : 'Agendamento cadastrado e e-mail de confirmação enviado.',
        life: 5000
      })
    }
  } catch (error) {
    const message = error.response?.data?.message || 'Falha ao salvar agendamento.'
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

function confirmDelete(appointment) {
  if (!auth.isAdmin) return

  confirm.require({
    header: 'Confirmar Exclusão',
    message: `Deseja realmente excluir o agendamento do fornecedor "${appointment.supplierName}" em ${formatDate(appointment.date)}?`,
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Cancelar',
    acceptLabel: 'Excluir',
    rejectClass: 'p-button-secondary p-button-text',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await appointmentService.deleteAppointment(appointment.id)
        toast.add({
          severity: 'success',
          summary: 'Excluído',
          detail: 'Agendamento excluído com sucesso.',
          life: 4000
        })
        await loadAppointments()
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: 'Erro ao excluir',
          detail: error.response?.data?.message || 'Não foi possível excluir o agendamento.',
          life: 5000
        })
      }
    }
  })
}

onMounted(async () => {
  await loadReferenceData()
  await loadAppointments()
})
</script>
