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

    <!-- Banner de Avisos (capacidade e/ou falha de e-mail) -->
    <div
      v-if="capacityWarnings.length > 0"
      class="rounded-xl border border-amber-300 bg-amber-50 p-4 shadow-sm"
    >
      <div class="flex items-start justify-between">
        <div class="flex items-start gap-3">
          <i class="pi pi-exclamation-triangle mt-0.5 text-lg text-amber-600" />
          <div>
            <h3 class="text-sm font-semibold text-amber-900">
              Atenção: o agendamento foi salvo com avisos
            </h3>
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

    <!-- Painel de Ocupação Diária e por Faixa -->
    <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h3 class="text-sm font-semibold text-slate-900">Ocupação da capacidade</h3>
          <p class="text-xs text-slate-500">
            Percentuais informativos: 480 pallets por dia e 120 pallets por faixa horária.
          </p>
        </div>
        <div class="flex items-center gap-2">
          <label
            class="text-xs font-semibold uppercase tracking-wider text-slate-600"
            for="capacity-date"
          >
            Data
          </label>
          <input
            id="capacity-date"
            v-model="capacityDate"
            type="date"
            class="rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            @change="loadCapacity()"
          />
          <Button
            icon="pi pi-refresh"
            severity="secondary"
            text
            rounded
            aria-label="Atualizar ocupação"
            :loading="capacityLoading"
            @click="loadCapacity()"
          />
        </div>
      </div>

      <div v-if="capacity" class="mt-4 space-y-4">
        <div>
          <div class="flex items-center justify-between text-xs font-semibold text-slate-700">
            <span>
              Dia {{ formatDate(capacity.date) }}: {{ capacity.dailyPallets }} de
              {{ capacity.dailyLimit }} pallets
            </span>
            <span :class="occupancyTextClass(capacity.dailyOccupancyPercentage)">
              {{ formatPercent(capacity.dailyOccupancyPercentage) }}
            </span>
          </div>
          <div class="mt-1.5 h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
            <div
              class="h-full rounded-full transition-all"
              :class="occupancyBarClass(capacity.dailyOccupancyPercentage)"
              :style="{ width: barWidth(capacity.dailyOccupancyPercentage) }"
            />
          </div>
        </div>

        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <div
            v-for="slot in capacity.slots"
            :key="slot.timeSlot.id"
            class="rounded-lg border border-slate-200 bg-slate-50/70 p-3"
          >
            <div class="flex items-center justify-between text-xs font-semibold text-slate-700">
              <span class="inline-flex items-center gap-1.5">
                <i class="pi pi-clock text-[10px] text-slate-500" />
                {{ slot.timeSlot.label }}
              </span>
              <span :class="occupancyTextClass(slot.occupancyPercentage)">
                {{ formatPercent(slot.occupancyPercentage) }}
              </span>
            </div>
            <p class="mt-1 text-xs text-slate-500">
              {{ slot.pallets }} de {{ slot.limit }} pallets na faixa
            </p>
            <div class="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-200">
              <div
                class="h-full rounded-full transition-all"
                :class="occupancyBarClass(slot.occupancyPercentage)"
                :style="{ width: barWidth(slot.occupancyPercentage) }"
              />
            </div>
          </div>
        </div>
      </div>
      <p v-else class="mt-4 text-xs text-slate-400">
        {{
          capacityLoading
            ? 'Carregando ocupação...'
            : 'Selecione uma data para consultar a ocupação.'
        }}
      </p>
    </div>

    <!-- Barra de Filtros -->
    <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 lg:items-end">
        <div>
          <label
            for="filter-start-date"
            class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
          >
            Data Inicial
          </label>
          <input
            id="filter-start-date"
            v-model="filters.startDate"
            type="date"
            class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
          />
        </div>
        <div>
          <label
            for="filter-end-date"
            class="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
          >
            Data Final
          </label>
          <input
            id="filter-end-date"
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
            <p class="text-xs text-slate-400 mt-1">
              Tente ajustar o filtro de datas ou cadastre um novo agendamento.
            </p>
          </div>
        </template>

        <Column field="orderId" header="ID Pedido" sortable style="min-width: 120px">
          <template #body="{ data }">
            <span
              class="rounded border border-slate-300 bg-slate-50 px-2 py-1 font-mono text-xs font-bold text-slate-800"
            >
              {{ data.orderId }}
            </span>
          </template>
        </Column>

        <Column field="date" header="Data" sortable style="min-width: 110px">
          <template #body="{ data }">
            <span class="font-medium text-slate-900">{{ formatDate(data.date) }}</span>
          </template>
        </Column>

        <Column header="Faixa Horária" style="min-width: 140px">
          <template #body="{ data }">
            <span
              class="inline-flex items-center gap-1.5 rounded-md bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-700"
            >
              <i class="pi pi-clock text-[10px] text-slate-500" />
              {{ data.timeSlot?.startTime }} às {{ data.timeSlot?.endTime }}
            </span>
          </template>
        </Column>

        <Column header="Fornecedor" style="min-width: 220px">
          <template #body="{ data }">
            <div>
              <p class="font-semibold text-slate-900">{{ data.supplierName }}</p>
              <p class="text-xs text-slate-400">{{ data.supplierEmail }}</p>
              <p v-if="data.notes" class="mt-1 text-xs italic text-slate-500">
                <i class="pi pi-comment mr-1 text-[10px]" />
                {{ data.notes }}
              </p>
            </div>
          </template>
        </Column>

        <Column field="truckPlate" header="Placa" style="min-width: 110px">
          <template #body="{ data }">
            <span
              class="rounded border border-slate-300 bg-slate-50 px-2 py-1 font-mono text-xs font-bold uppercase tracking-wider text-slate-800"
            >
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
            <Tag v-else severity="warn" value="Não Paletizado (2x)" />
          </template>
        </Column>

        <Column header="Pallets (dia / faixa)" style="min-width: 150px">
          <template #body="{ data }">
            <div class="text-xs">
              <p class="text-slate-500">
                Dia: <span class="font-semibold text-slate-800">{{ data.dailyPallets }}</span>
              </p>
              <p class="text-slate-700">
                Faixa: <span class="font-bold text-krill-700">{{ data.slotPallets }}</span>
              </p>
            </div>
          </template>
        </Column>

        <Column header="Status" style="min-width: 130px">
          <template #body="{ data }">
            <Tag
              v-if="data.status === 'CANCELED'"
              severity="danger"
              value="Cancelado"
              icon="pi pi-ban"
            />
            <Tag v-else severity="success" value="Ativo" icon="pi pi-check" />
            <p
              v-if="data.status === 'CANCELED' && data.canceledBy"
              class="mt-1 text-[11px] text-slate-400"
            >
              por {{ data.canceledBy.name }}
            </p>
          </template>
        </Column>

        <Column v-if="auth.isAdmin" header="Ações" style="width: 120px" alignFrozen="right">
          <template #body="{ data }">
            <div class="flex items-center gap-1">
              <Button
                icon="pi pi-pencil"
                severity="secondary"
                text
                rounded
                size="small"
                aria-label="Editar"
                :disabled="data.status === 'CANCELED'"
                @click="openEditModal(data)"
              />
              <Button
                v-if="data.status !== 'CANCELED'"
                icon="pi pi-ban"
                severity="danger"
                text
                rounded
                size="small"
                aria-label="Cancelar agendamento"
                @click="confirmCancel(data)"
              />
              <span v-else class="text-[11px] text-slate-400">—</span>
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
      :style="{ width: '620px' }"
      class="p-fluid"
    >
      <form @submit.prevent="saveAppointment" class="space-y-4 pt-2">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <!-- ID Pedido -->
          <div>
            <label for="modal-order-id" class="block text-xs font-semibold text-slate-700 mb-1">
              ID Pedido <span class="text-red-500">*</span>
            </label>
            <input
              id="modal-order-id"
              v-model.number="form.orderId"
              type="number"
              min="1"
              step="1"
              placeholder="Ex.: 123456"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 font-mono text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
            <p class="mt-1 text-[11px] text-slate-400">
              Identificador único do pedido, sem repetição.
            </p>
          </div>

          <!-- Data do Agendamento -->
          <div>
            <label for="modal-date" class="block text-xs font-semibold text-slate-700 mb-1">
              Data do Agendamento <span class="text-red-500">*</span>
            </label>
            <input
              id="modal-date"
              v-model="form.date"
              type="date"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
          </div>

          <!-- Nome do Fornecedor -->
          <div class="sm:col-span-2">
            <label
              for="modal-supplier-name"
              class="block text-xs font-semibold text-slate-700 mb-1"
            >
              Nome do Fornecedor <span class="text-red-500">*</span>
            </label>
            <input
              id="modal-supplier-name"
              v-model="form.supplierName"
              type="text"
              maxlength="80"
              placeholder="Ex.: Distribuidora de Alimentos Silva"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
            <p class="mt-1 text-[11px] text-slate-400">Máximo de 80 caracteres.</p>
          </div>

          <!-- E-mail do Fornecedor -->
          <div class="sm:col-span-2">
            <label
              for="modal-supplier-email"
              class="block text-xs font-semibold text-slate-700 mb-1"
            >
              E-mail do Fornecedor <span class="text-red-500">*</span>
            </label>
            <input
              id="modal-supplier-email"
              v-model="form.supplierEmail"
              type="email"
              maxlength="80"
              placeholder="fornecedor@empresa.com"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
            <p class="mt-1 text-[11px] text-slate-400">Máximo de 80 caracteres.</p>
          </div>

          <!-- Placa do Caminhão -->
          <div>
            <label for="modal-truck-plate" class="block text-xs font-semibold text-slate-700 mb-1">
              Placa do Caminhão <span class="text-red-500">*</span>
            </label>
            <input
              id="modal-truck-plate"
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
            <label for="modal-truck-type" class="block text-xs font-semibold text-slate-700 mb-1">
              Tipo de Caminhão <span class="text-red-500">*</span>
            </label>
            <Select
              input-id="modal-truck-type"
              v-model="form.truckTypeId"
              :options="truckTypes"
              optionLabel="name"
              optionValue="id"
              placeholder="Selecione o tipo"
              class="w-full"
            />
          </div>

          <!-- Faixa Horária -->
          <div>
            <label for="modal-time-slot" class="block text-xs font-semibold text-slate-700 mb-1">
              Faixa Horária <span class="text-red-500">*</span>
            </label>
            <Select
              input-id="modal-time-slot"
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
            <label
              for="modal-palletization"
              class="block text-xs font-semibold text-slate-700 mb-1"
            >
              Paletização <span class="text-red-500">*</span>
            </label>
            <Select
              input-id="modal-palletization"
              v-model="form.palletizationTypeId"
              :options="palletizationTypes"
              optionLabel="name"
              optionValue="id"
              class="w-full"
            />
          </div>

          <!-- Quantidade de Pallets -->
          <div>
            <label
              for="modal-pallet-quantity"
              class="block text-xs font-semibold text-slate-700 mb-1"
            >
              Quantidade de Pallets <span class="text-red-500">*</span>
            </label>
            <input
              id="modal-pallet-quantity"
              v-model.number="form.palletQuantity"
              type="number"
              min="1"
              max="1000000"
              required
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
          </div>

          <!-- Observação -->
          <div class="sm:col-span-2">
            <label for="modal-notes" class="block text-xs font-semibold text-slate-700 mb-1">
              Observação
            </label>
            <textarea
              id="modal-notes"
              v-model="form.notes"
              rows="3"
              maxlength="500"
              placeholder="Informações adicionais sobre a entrega (opcional)"
              class="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-krill-500 focus:outline-none focus:ring-1 focus:ring-krill-500"
            />
            <p class="mt-1 text-[11px] text-slate-400">{{ form.notes.length }}/500 caracteres.</p>
          </div>
        </div>

        <!-- Card de Cálculo em Tempo Real de Pallets -->
        <div class="rounded-xl border border-krill-100 bg-krill-50/60 p-4">
          <div
            class="flex items-center justify-between text-xs font-semibold uppercase tracking-wider text-krill-800"
          >
            <span>Impacto na capacidade</span>
            <span
              v-if="selectedMultiplier > 1"
              class="rounded bg-amber-100 px-2 py-0.5 text-amber-800"
            >
              Multiplicador: {{ selectedMultiplier }}x na faixa
            </span>
            <span v-else class="text-slate-500">Multiplicador: 1x</span>
          </div>
          <div class="mt-2 grid grid-cols-2 gap-3 text-xs text-slate-600">
            <div>
              <p>Pallets no dia</p>
              <p class="text-xl font-bold text-slate-900">{{ calculatedDailyPallets }}</p>
              <p class="text-[11px] text-slate-400">quantidade original</p>
            </div>
            <div>
              <p>Pallets na faixa</p>
              <p class="text-xl font-bold text-krill-700">{{ calculatedSlotPallets }}</p>
              <p class="text-[11px] text-slate-400">com multiplicador</p>
            </div>
          </div>
          <p v-if="selectedMultiplier > 1" class="mt-2 text-[11px] text-amber-700">
            * Cargas Não Paletizadas dobram o impacto somente na faixa horária.
          </p>

          <div class="mt-3 border-t border-krill-100 pt-3">
            <div class="flex items-center justify-between text-xs font-semibold text-slate-700">
              <span>Ocupação projetada para {{ formatDate(form.date) }}</span>
              <span v-if="modalCapacityLoading" class="text-[11px] font-normal text-slate-400">
                Calculando...
              </span>
            </div>
            <div class="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
              <div class="rounded-lg bg-white/70 px-3 py-2">
                <div class="flex items-center justify-between text-[11px] text-slate-500">
                  <span>Dia (480 pallets)</span>
                  <span :class="occupancyTextClass(projectedDailyPercentage)">
                    {{ formatPercent(projectedDailyPercentage) }}
                  </span>
                </div>
                <p class="mt-1 text-sm font-bold text-slate-900">
                  {{ projectedDailyPallets }} / {{ capacityDailyLimit }}
                </p>
                <div class="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-slate-200">
                  <div
                    class="h-full rounded-full"
                    :class="occupancyBarClass(projectedDailyPercentage)"
                    :style="{ width: barWidth(projectedDailyPercentage) }"
                  />
                </div>
              </div>
              <div class="rounded-lg bg-white/70 px-3 py-2">
                <div class="flex items-center justify-between text-[11px] text-slate-500">
                  <span>Faixa selecionada (120 pallets)</span>
                  <span :class="occupancyTextClass(projectedSlotPercentage)">
                    {{ formatPercent(projectedSlotPercentage) }}
                  </span>
                </div>
                <p class="mt-1 text-sm font-bold text-slate-900">
                  {{ projectedSlotPallets }} / {{ capacitySlotLimit }}
                </p>
                <div class="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-slate-200">
                  <div
                    class="h-full rounded-full"
                    :class="occupancyBarClass(projectedSlotPercentage)"
                    :style="{ width: barWidth(projectedSlotPercentage) }"
                  />
                </div>
              </div>
            </div>
            <p
              v-if="projectionExceeded"
              class="mt-2 rounded bg-amber-100 px-2 py-1 text-[11px] font-medium text-amber-800"
            >
              <i class="pi pi-exclamation-triangle mr-1 text-[10px]" />
              A projeção ultrapassa um limite informativo. O salvamento não será bloqueado.
            </p>
          </div>

          <p class="mt-2 text-[11px] text-slate-500">
            Limites informativos: 480 pallets/dia e 120 pallets/faixa. O cadastro nunca é bloqueado.
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
import { projectCapacity } from '../utils/capacity'

const auth = useAuthStore()
const toast = useToast()
const confirm = useConfirm()

const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref(null)
const capacityWarnings = ref([])

const capacityDate = ref(getTodayIso())
const capacity = ref(null)
const capacityLoading = ref(false)

const modalCapacity = ref(null)
const modalCapacityLoading = ref(false)
const editingOriginal = ref(null)

const projection = computed(() =>
  projectCapacity({
    capacity: modalCapacity.value,
    editingOriginal: editingOriginal.value,
    timeSlotId: form.timeSlotId,
    palletQuantity: calculatedDailyPallets.value,
    multiplier: selectedMultiplier.value
  })
)

const capacityDailyLimit = computed(() => projection.value.dailyLimit)
const capacitySlotLimit = computed(() => projection.value.slotLimit)
const projectedDailyPallets = computed(() => projection.value.projectedDailyPallets)
const projectedSlotPallets = computed(() => projection.value.projectedSlotPallets)
const projectedDailyPercentage = computed(() => projection.value.dailyPercentage)
const projectedSlotPercentage = computed(() => projection.value.slotPercentage)
const projectionExceeded = computed(() => projection.value.exceeded)

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
  orderId: null,
  supplierName: '',
  supplierEmail: '',
  truckPlate: '',
  date: '',
  timeSlotId: null,
  truckTypeId: null,
  palletizationTypeId: null,
  palletQuantity: 1,
  notes: ''
})

const selectedPalletization = computed(() =>
  palletizationTypes.value.find((type) => type.id === form.palletizationTypeId)
)

const selectedMultiplier = computed(() => selectedPalletization.value?.capacityMultiplier || 1)

const calculatedDailyPallets = computed(() => Number(form.palletQuantity) || 0)

const calculatedSlotPallets = computed(
  () => calculatedDailyPallets.value * selectedMultiplier.value
)

function formatDate(isoDate) {
  if (!isoDate) return ''
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function barWidth(value) {
  return `${Math.min(Math.max(Number(value) || 0, 0), 100)}%`
}

function occupancyBarClass(value) {
  const percent = Number(value) || 0
  if (percent >= 100) return 'bg-red-500'
  if (percent >= 80) return 'bg-amber-500'
  return 'bg-krill-500'
}

function occupancyTextClass(value) {
  const percent = Number(value) || 0
  if (percent >= 100) return 'text-red-600'
  if (percent >= 80) return 'text-amber-600'
  return 'text-krill-700'
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

async function loadCapacity(date = capacityDate.value) {
  if (!date) return
  capacityLoading.value = true
  try {
    capacity.value = await appointmentService.getCapacity(date)
  } catch (error) {
    console.error('Erro ao carregar ocupação:', error)
    capacity.value = null
    toast.add({
      severity: 'error',
      summary: 'Erro',
      detail: 'Não foi possível consultar a ocupação da data selecionada.',
      life: 5000
    })
  } finally {
    capacityLoading.value = false
  }
}

async function loadModalCapacity(date = form.date) {
  if (!date) {
    modalCapacity.value = null
    return
  }
  modalCapacityLoading.value = true
  try {
    modalCapacity.value = await appointmentService.getCapacity(date)
  } catch (error) {
    console.error('Erro ao carregar ocupação do modal:', error)
    modalCapacity.value = null
  } finally {
    modalCapacityLoading.value = false
  }
}

watch(
  () => form.date,
  (date) => {
    if (modalVisible.value) {
      loadModalCapacity(date)
    }
  }
)

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
  editingOriginal.value = null
  form.orderId = null
  form.supplierName = ''
  form.supplierEmail = ''
  form.truckPlate = ''
  form.date = getTodayIso()
  form.timeSlotId = timeSlots.value[0]?.id || null
  form.truckTypeId = truckTypes.value[0]?.id || null
  form.palletizationTypeId = palletizationTypes.value[0]?.id || null
  form.palletQuantity = 1
  form.notes = ''
  modalVisible.value = true
  loadModalCapacity(form.date)
}

function openEditModal(appointment) {
  if (!auth.isAdmin || appointment.status === 'CANCELED') return

  editingId.value = appointment.id
  editingOriginal.value = appointment
  form.orderId = appointment.orderId
  form.supplierName = appointment.supplierName
  form.supplierEmail = appointment.supplierEmail
  form.truckPlate = appointment.truckPlate
  form.date = appointment.date
  form.timeSlotId = appointment.timeSlot?.id || null
  form.truckTypeId = appointment.truckType?.id || null
  form.palletizationTypeId = appointment.palletizationType?.id || null
  form.palletQuantity = appointment.palletQuantity
  form.notes = appointment.notes || ''
  modalVisible.value = true
  loadModalCapacity(form.date)
}

async function saveAppointment() {
  if (!auth.isAdmin) return

  if (!Number.isInteger(Number(form.orderId)) || Number(form.orderId) <= 0) {
    toast.add({
      severity: 'error',
      summary: 'ID Pedido inválido',
      detail: 'Informe um ID Pedido inteiro maior que zero.',
      life: 5000
    })
    return
  }

  saving.value = true
  try {
    const payload = {
      orderId: Number(form.orderId),
      supplierName: form.supplierName.trim(),
      supplierEmail: form.supplierEmail.trim(),
      truckPlate: form.truckPlate.trim().toUpperCase(),
      date: form.date,
      timeSlotId: form.timeSlotId,
      truckTypeId: form.truckTypeId,
      palletizationTypeId: form.palletizationTypeId,
      palletQuantity: form.palletQuantity,
      notes: form.notes.trim() || null
    }

    const isEditing = Boolean(editingId.value)
    const result = isEditing
      ? await appointmentService.updateAppointment(editingId.value, payload)
      : await appointmentService.createAppointment(payload)

    modalVisible.value = false
    capacityDate.value = payload.date
    await Promise.all([loadAppointments(), loadCapacity(payload.date)])

    if (result?.warnings && result.warnings.length > 0) {
      capacityWarnings.value = result.warnings
      toast.add({
        severity: 'warn',
        summary: 'Salvo com avisos',
        detail: result.warnings.join(' | '),
        life: 8000
      })
    } else {
      capacityWarnings.value = []
      toast.add({
        severity: 'success',
        summary: 'Sucesso',
        detail: isEditing
          ? 'Agendamento atualizado com sucesso.'
          : result?.emailQueued
            ? 'Agendamento cadastrado. O e-mail de confirmação foi colocado na fila de envio.'
            : 'Agendamento cadastrado. Nenhum e-mail foi enfileirado.',
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

function confirmCancel(appointment) {
  if (!auth.isAdmin) return

  confirm.require({
    header: 'Confirmar Cancelamento',
    message: `Deseja cancelar o agendamento do fornecedor "${appointment.supplierName}" (ID Pedido ${appointment.orderId}) em ${formatDate(appointment.date)}? O registro será mantido no histórico.`,
    icon: 'pi pi-exclamation-triangle',
    rejectLabel: 'Voltar',
    acceptLabel: 'Cancelar agendamento',
    rejectClass: 'p-button-secondary p-button-text',
    acceptClass: 'p-button-danger',
    accept: async () => {
      try {
        await appointmentService.cancelAppointment(appointment.id)
        toast.add({
          severity: 'success',
          summary: 'Cancelado',
          detail: 'Agendamento cancelado e mantido no histórico.',
          life: 4000
        })
        await Promise.all([loadAppointments(), loadCapacity()])
      } catch (error) {
        toast.add({
          severity: 'error',
          summary: 'Erro ao cancelar',
          detail: error.response?.data?.message || 'Não foi possível cancelar o agendamento.',
          life: 5000
        })
      }
    }
  })
}

onMounted(async () => {
  await loadReferenceData()
  await Promise.all([loadAppointments(), loadCapacity()])
})
</script>
