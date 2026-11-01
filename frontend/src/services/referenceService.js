import { api } from './api'

export const referenceService = {
  async getTimeSlots() {
    const { data } = await api.get('/time-slots')
    return data
  },

  async getPalletizationTypes() {
    const { data } = await api.get('/palletization-types')
    return data
  },

  async getTruckTypes() {
    const { data } = await api.get('/truck-types')
    return data
  }
}
