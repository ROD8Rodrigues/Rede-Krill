<template>
  <div class="space-y-6">
    <!-- Cabeçalho -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold tracking-tight text-slate-900">Trilha de Auditoria</h2>
        <p class="text-sm text-slate-500">
          Histórico das operações e alterações realizadas no sistema
        </p>
      </div>
      <div>
        <Button
          label="Atualizar"
          icon="pi pi-refresh"
          severity="secondary"
          class="shadow-sm"
          :loading="loading"
          @click="loadAuditLogs"
        />
      </div>
    </div>

    <!-- Tabela de Auditoria -->
    <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <DataTable
        :value="auditLogs"
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
            <i class="pi pi-history text-4xl text-slate-300 mb-3" />
            <p class="text-base font-medium text-slate-700">
              Nenhum registro de auditoria encontrado
            </p>
          </div>
        </template>

        <Column header="Data e Hora" style="min-width: 160px">
          <template #body="{ data }">
            <span class="font-mono text-xs text-slate-600">{{
              formatDateTime(data.createdAt)
            }}</span>
          </template>
        </Column>

        <Column header="Operador" style="min-width: 200px">
          <template #body="{ data }">
            <div class="flex items-center gap-2">
              <i class="pi pi-user text-xs text-slate-400" />
              <span class="text-xs font-medium text-slate-800">{{ data.actorEmail }}</span>
            </div>
          </template>
        </Column>

        <Column header="Ação" style="min-width: 120px">
          <template #body="{ data }">
            <Tag
              v-if="data.action === 'CREATE'"
              severity="success"
              value="Criação"
              icon="pi pi-plus"
            />
            <Tag
              v-else-if="data.action === 'UPDATE'"
              severity="info"
              value="Alteração"
              icon="pi pi-pencil"
            />
            <Tag
              v-else-if="data.action === 'DELETE'"
              severity="danger"
              value="Exclusão"
              icon="pi pi-trash"
            />
            <Tag
              v-else-if="data.action === 'CANCEL'"
              severity="danger"
              value="Cancelamento"
              icon="pi pi-ban"
            />
            <Tag
              v-else-if="data.action === 'LOGIN'"
              severity="success"
              value="Login"
              icon="pi pi-sign-in"
            />
            <Tag
              v-else-if="data.action === 'LOGOUT'"
              severity="secondary"
              value="Logout"
              icon="pi pi-sign-out"
            />
            <Tag
              v-else-if="data.action === 'ACCESS_DENIED'"
              severity="warn"
              value="Acesso negado"
              icon="pi pi-lock"
            />
            <Tag v-else severity="secondary" :value="data.action" />
          </template>
        </Column>

        <Column header="Entidade" style="min-width: 160px">
          <template #body="{ data }">
            <span
              class="inline-flex items-center gap-1 rounded bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-700"
            >
              {{ translateEntity(data.entityName) }}
              <span v-if="data.entityId" class="text-slate-400 font-mono"
                >#{{ data.entityId }}</span
              >
            </span>
          </template>
        </Column>

        <Column field="details" header="Detalhes da Operação" style="min-width: 300px">
          <template #body="{ data }">
            <span class="text-xs text-slate-700 leading-relaxed">{{ data.details || '—' }}</span>
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { useToast } from 'primevue/usetoast'
import { auditService } from '../services/auditService'

const toast = useToast()

const loading = ref(false)
const auditLogs = ref([])
const totalRecords = ref(0)
const pageIndex = ref(0)
const pageSize = ref(20)

function formatDateTime(isoString) {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function translateEntity(entityName) {
  const map = {
    Appointment: 'Agendamento',
    TruckType: 'Tipo de Caminhão',
    User: 'Usuário',
    Endpoint: 'Endpoint'
  }
  return map[entityName] || entityName
}

async function loadAuditLogs() {
  loading.value = true
  try {
    const data = await auditService.listAuditLogs({
      page: pageIndex.value,
      size: pageSize.value
    })
    auditLogs.value = data.content || []
    totalRecords.value = data.totalElements || 0
  } catch (error) {
    console.error('Erro ao carregar auditoria:', error)
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: 'Não foi possível carregar os registros de auditoria.',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

function onPageChange(event) {
  pageIndex.value = event.page
  pageSize.value = event.rows
  loadAuditLogs()
}

onMounted(() => {
  loadAuditLogs()
})
</script>
