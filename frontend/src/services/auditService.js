import { api } from './api'

export const auditService = {
  async listAuditLogs({ page = 0, size = 20 } = {}) {
    const { data } = await api.get('/audit-logs', {
      params: { page, size }
    })
    return data
  }
}
