import { describe, expect, it } from 'vitest'
import { projectCapacity } from './capacity'

const capacity = {
  date: '2026-04-10',
  dailyPallets: 100,
  dailyLimit: 480,
  slots: [
    { timeSlot: { id: 1 }, pallets: 40, limit: 120 },
    { timeSlot: { id: 2 }, pallets: 60, limit: 120 }
  ]
}

describe('projectCapacity', () => {
  it('projects daily and slot occupancy using the selected multiplier', () => {
    const projection = projectCapacity({
      capacity,
      timeSlotId: 1,
      palletQuantity: 30,
      multiplier: 2
    })

    expect(projection.projectedDailyPallets).toBe(130)
    expect(projection.projectedSlotPallets).toBe(100)
    expect(projection.dailyPercentage).toBeCloseTo(27.08, 2)
    expect(projection.slotPercentage).toBeCloseTo(83.33, 2)
    expect(projection.exceeded).toBe(false)
  })

  it('does not double the daily impact when the palletization is not palletized', () => {
    const projection = projectCapacity({
      capacity,
      timeSlotId: 1,
      palletQuantity: 30,
      multiplier: 2
    })

    expect(projection.projectedDailyPallets).toBe(130)
    expect(projection.projectedSlotPallets).toBe(100)
  })

  it('subtracts the original appointment when editing the same date and slot', () => {
    const editingOriginal = {
      date: '2026-04-10',
      dailyPallets: 30,
      slotPallets: 60,
      timeSlot: { id: 1 }
    }

    const projection = projectCapacity({
      capacity,
      editingOriginal,
      timeSlotId: 1,
      palletQuantity: 30,
      multiplier: 2
    })

    expect(projection.baselineDaily).toBe(70)
    expect(projection.baselineSlot).toBe(0)
    expect(projection.projectedDailyPallets).toBe(100)
    expect(projection.projectedSlotPallets).toBe(60)
  })

  it('keeps the original appointment impact when editing to another date', () => {
    const editingOriginal = {
      date: '2026-04-09',
      dailyPallets: 30,
      slotPallets: 60,
      timeSlot: { id: 1 }
    }

    const projection = projectCapacity({
      capacity,
      editingOriginal,
      timeSlotId: 1,
      palletQuantity: 10,
      multiplier: 1
    })

    expect(projection.baselineDaily).toBe(100)
    expect(projection.baselineSlot).toBe(40)
    expect(projection.projectedDailyPallets).toBe(110)
    expect(projection.projectedSlotPallets).toBe(50)
  })

  it('marks the projection as exceeded without blocking it', () => {
    const projection = projectCapacity({
      capacity,
      timeSlotId: 2,
      palletQuantity: 100,
      multiplier: 2
    })

    expect(projection.slotPercentage).toBeCloseTo(216.67, 2)
    expect(projection.exceeded).toBe(true)
  })

  it('falls back to the official limits when capacity is unavailable', () => {
    const projection = projectCapacity({
      capacity: null,
      timeSlotId: 1,
      palletQuantity: 12,
      multiplier: 1
    })

    expect(projection.dailyLimit).toBe(480)
    expect(projection.slotLimit).toBe(120)
    expect(projection.projectedDailyPallets).toBe(12)
    expect(projection.projectedSlotPallets).toBe(12)
  })
})
