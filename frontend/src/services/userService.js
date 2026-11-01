import { api } from './api'

export const userService = {
  async listUsers({ page = 0, size = 10 } = {}) {
    const { data } = await api.get('/users', {
      params: { page, size }
    })
    return data
  },

  async createUser(payload) {
    const { data } = await api.post('/users', payload)
    return data
  },

  async updateUser(id, payload) {
    const { data } = await api.put(`/users/${id}`, payload)
    return data
  }
}
