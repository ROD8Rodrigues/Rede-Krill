import { api } from './api'

export const referenceService = {
  async getTimeSlots() {
    const { data } = await api.get('/time-slots')
    return data
  },

  async createTimeSlot(payload) {
    const { data } = await api.post('/time-slots', payload)
    return data
  },

  async updateTimeSlot(id, payload) {
    const { data } = await api.put(`/time-slots/${id}`, payload)
    return data
  },

  async deleteTimeSlot(id) {
    await api.delete(`/time-slots/${id}`)
  },

  async getPalletizationTypes() {
    const { data } = await api.get('/palletization-types')
    return data
  },

  async createPalletizationType(payload) {
    const { data } = await api.post('/palletization-types', payload)
    return data
  },

  async updatePalletizationType(id, payload) {
    const { data } = await api.put(`/palletization-types/${id}`, payload)
    return data
  },

  async deletePalletizationType(id) {
    await api.delete(`/palletization-types/${id}`)
  },

  async getTruckTypes() {
    const { data } = await api.get('/truck-types')
    return data
  }
}
