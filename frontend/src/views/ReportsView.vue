<template>
  <div class="space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-col gap-2">
      <h2 class="text-2xl font-bold tracking-tight text-slate-900">Relatório de Agendamentos</h2>
      <p class="text-sm text-slate-500">
        Consolidado operacional de movimentação de cargas e capacidade por período
      </p>
    </div>

    <!-- Barra de Filtros por Período -->
    <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 lg:items-end">
        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">
            Data Inicial <span class="text-red-500">*</span>
          </label>
          <input
            v-model="filters.startDate"
            type="date"
            required
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>
        <div>
          <label class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1">
            Data Final <span class="text-red-500">*</span>
          </label>
          <input
            v-model="filters.endDate"
            type="date"
            required
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>
        <div class="flex items-center gap-2 lg:col-span-2">
          <Button
            label="Gerar Relatório"
            icon="pi pi-search"
            class="p-button-primary shadow-sm"
            :loading="loading"
            @click="loadReport"
          />
          <Button
            label="Este Mês"
            severity="secondary"
            text
            size="small"
            @click="setThisMonth"
          />
          <Button
            label="Últimos 7 dias"
            severity="secondary"
            text
            size="small"
            @click="setLast7Days"
          />
        </div>
      </div>
    </div>

    <!-- Cards de Indicadores (KPIs) -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Agendamentos</p>
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-krill-50 text-krill-600">
            <i class="pi pi-calendar text-lg" />
          </div>
        </div>
        <p class="mt-3 text-2xl font-bold text-slate-900">{{ report?.totalAppointments || 0 }}</p>
        <p class="mt-1 text-xs text-slate-400">caminhões agendados</p>
      </div>

      <div class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Pallets Efetivos</p>
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600">
            <i class="pi pi-box text-lg" />
          </div>
        </div>
        <p class="mt-3 text-2xl font-bold text-emerald-700">{{ report?.totalEffectivePallets || 0 }}</p>
        <p class="mt-1 text-xs text-slate-400">considerados na capacidade</p>
      </div>

      <div class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Pallets Declarados</p>
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50 text-blue-600">
            <i class="pi pi-database text-lg" />
          </div>
        </div>
        <p class="mt-3 text-2xl font-bold text-slate-900">{{ totalDeclaredPallets }}</p>
        <p class="mt-1 text-xs text-slate-400">quantidade física informada</p>
      </div>

      <div class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div class="flex items-center justify-between">
          <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Não Paletizados (2x)</p>
          <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-50 text-amber-600">
            <i class="pi pi-exclamation-circle text-lg" />
          </div>
        </div>
        <p class="mt-3 text-2xl font-bold text-amber-700">{{ nonPalletizedCount }}</p>
        <p class="mt-1 text-xs text-slate-400">agendamentos com impacto 2x</p>
      </div>
    </div>

    <!-- Tabela Detalhada -->
    <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div class="border-b border-slate-200 px-6 py-4">
        <h3 class="text-base font-semibold text-slate-900">Detalhamento dos Agendamentos no Período</h3>
      </div>
      <DataTable
        :value="report?.appointments || []"
        :loading="loading"
        paginator
        :rows="10"
        responsiveLayout="scroll"
        class="p-datatable-sm"
      >
        <template #empty>
          <div class="py-12 text-center">
            <i class="pi pi-inbox text-4xl text-slate-300 mb-3" />
            <p class="text-base font-medium text-slate-700">Nenhum agendamento encontrado no período selecionado</p>
            <p class="text-xs text-slate-400 mt-1">Selecione outro intervalo de datas e clique em "Gerar Relatório".</p>
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

        <Column header="Pallets Informados" style="min-width: 140px">
          <template #body="{ data }">
            <span class="font-medium text-slate-800">{{ data.palletQuantity }}</span>
          </template>
        </Column>

        <Column header="Pallets Efetivos" style="min-width: 140px">
          <template #body="{ data }">
            <span class="font-bold text-krill-700">{{ data.effectivePallets }}</span>
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useToast } from 'primevue/usetoast'
import { reportService } from '../services/reportService'

const toast = useToast()

const loading = ref(false)
const report = ref(null)

const filters = reactive({
  startDate: '',
  endDate: ''
})

const totalDeclaredPallets = computed(() => {
  if (!report.value?.appointments) return 0
  return report.value.appointments.reduce((sum, item) => sum + (item.palletQuantity || 0), 0)
})

const nonPalletizedCount = computed(() => {
  if (!report.value?.appointments) return 0
  return report.value.appointments.filter(
    (item) => item.palletizationType?.code === 'NAO_PALETIZADO'
  ).length
})

function formatDate(isoDate) {
  if (!isoDate) return ''
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

function setThisMonth() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)

  const pad = (n) => String(n).padStart(2, '0')
  filters.startDate = `${year}-${pad(month + 1)}-01`
  filters.endDate = `${year}-${pad(month + 1)}-${pad(lastDay.getDate())}`
  loadReport()
}

function setLast7Days() {
  const now = new Date()
  const past = new Date()
  past.setDate(now.getDate() - 7)

  const pad = (n) => String(n).padStart(2, '0')
  filters.startDate = `${past.getFullYear()}-${pad(past.getMonth() + 1)}-${pad(past.getDate())}`
  filters.endDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  loadReport()
}

async function loadReport() {
  if (!filters.startDate || !filters.endDate) {
    toast.add({
      severity: 'warn',
      summary: 'Campos obrigatórios',
      detail: 'Informe a data inicial e a data final.',
      life: 4000
    })
    return
  }

  if (filters.startDate > filters.endDate) {
    toast.add({
      severity: 'error',
      summary: 'Período inválido',
      detail: 'A data inicial não pode ser posterior à data final.',
      life: 5000
    })
    return
  }

  loading.value = true
  try {
    report.value = await reportService.getAppointmentReport(filters.startDate, filters.endDate)
  } catch (error) {
    console.error('Erro ao gerar relatório:', error)
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: error.response?.data?.message || 'Falha ao gerar o relatório.',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setThisMonth()
})
</script>
