import { api } from './api'

export const appointmentService = {
  async listAppointments({ startDate, endDate, page = 0, size = 10 } = {}) {
    const params = { page, size }
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const { data } = await api.get('/appointments', { params })
    return data
  },

  async getAppointment(id) {
    const { data } = await api.get(`/appointments/${id}`)
    return data
  },

  async getCapacity(date) {
    const { data } = await api.get('/appointments/capacity', { params: { date } })
    return data
  },

  async createAppointment(payload) {
    const { data } = await api.post('/appointments', payload)
    return data
  },

  async updateAppointment(id, payload) {
    const { data } = await api.put(`/appointments/${id}`, payload)
    return data
  },

  async cancelAppointment(id) {
    await api.delete(`/appointments/${id}`)
  }
}
