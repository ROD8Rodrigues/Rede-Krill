export const DEFAULT_DAILY_LIMIT = 480
export const DEFAULT_SLOT_LIMIT = 120

export function projectCapacity({
  capacity = null,
  editingOriginal = null,
  timeSlotId = null,
  palletQuantity = 0,
  multiplier = 1
} = {}) {
  const dailyLimit = Number(capacity?.dailyLimit) || DEFAULT_DAILY_LIMIT
  const selectedSlot = capacity?.slots?.find((slot) => slot.timeSlot?.id === timeSlotId)
  const slotLimit = Number(selectedSlot?.limit) || DEFAULT_SLOT_LIMIT
  const sameDate = Boolean(capacity && editingOriginal && capacity.date === editingOriginal.date)

  const existingDailyImpact = sameDate ? Number(editingOriginal.dailyPallets || 0) : 0
  const existingSlotImpact =
    sameDate && editingOriginal.timeSlot?.id === timeSlotId
      ? Number(editingOriginal.slotPallets || 0)
      : 0

  const baselineDaily = Math.max(Number(capacity?.dailyPallets || 0) - existingDailyImpact, 0)
  const baselineSlot = Math.max(Number(selectedSlot?.pallets || 0) - existingSlotImpact, 0)
  const dailyPallets = Math.max(Number(palletQuantity) || 0, 0)
  const slotPallets = dailyPallets * (Number(multiplier) || 1)

  const projectedDailyPallets = baselineDaily + dailyPallets
  const projectedSlotPallets = baselineSlot + slotPallets
  const dailyPercentage = dailyLimit > 0 ? (projectedDailyPallets / dailyLimit) * 100 : 0
  const slotPercentage = slotLimit > 0 ? (projectedSlotPallets / slotLimit) * 100 : 0

  return {
    dailyLimit,
    slotLimit,
    baselineDaily,
    baselineSlot,
    projectedDailyPallets,
    projectedSlotPallets,
    dailyPercentage,
    slotPercentage,
    exceeded: dailyPercentage >= 100 || slotPercentage >= 100
  }
}
