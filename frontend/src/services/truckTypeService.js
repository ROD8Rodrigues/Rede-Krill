import { api } from './api'

export const truckTypeService = {
  async listTruckTypes() {
    const { data } = await api.get('/truck-types')
    return data
  },

  async createTruckType(payload) {
    const { data } = await api.post('/truck-types', payload)
    return data
  },

  async updateTruckType(id, payload) {
    const { data } = await api.put(`/truck-types/${id}`, payload)
    return data
  },

  async deleteTruckType(id) {
    await api.delete(`/truck-types/${id}`)
  }
}
