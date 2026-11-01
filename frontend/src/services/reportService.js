import { api } from './api'

export const reportService = {
  async getAppointmentReport(startDate, endDate) {
    const { data } = await api.get('/reports/appointments', {
      params: { startDate, endDate }
    })
    return data
  }
}
