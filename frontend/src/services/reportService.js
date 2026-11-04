import { api } from './api'

export const reportService = {
  async getAppointmentReport(startDate, endDate, { page = 0, size = 10, status = null } = {}) {
    const params = { startDate, endDate, page, size }
    if (status) params.status = status
    const { data } = await api.get('/reports/appointments', { params })
    return data
  }
}
