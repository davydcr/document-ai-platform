import api from './api'

export const authService = {
  login: (email, password) =>
    api.post('/auth/login', { username: email, password }),

  refresh: (refreshToken) =>
    api.post('/auth/refresh', { refreshToken }),

  logout: () =>
    api.post('/auth/logout'),

  me: () =>
    api.get('/auth/me')
}

export const documentService = {
  uploadAsync: (file, timeoutMs = 30000) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/documents/async/upload?timeoutMs=${timeoutMs}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  getDocuments: (page = 0, size = 20) =>
    api.get(`/documents?page=${page}&size=${size}`),

  getDocument: (id) =>
    api.get(`/documents/${id}`),

  getAsyncStatus: (documentId) =>
    api.get(`/documents/async/${documentId}/status`),

  pollStatus: (documentId, timeoutMs = 30000) =>
    api.get(`/documents/async/${documentId}/status/polling?timeoutMs=${timeoutMs}`),

  registerWebhook: (documentId, webhookUrl) =>
    api.post(`/documents/async/${documentId}/webhook/register`, { webhookUrl }),

  unregisterWebhook: (documentId) =>
    api.delete(`/documents/async/${documentId}/webhook`)
}

export const dashboardService = {
  getMetrics: () =>
    api.get('/documents/async/dashboard/metrics'),

  getCircuitBreakerStatus: () =>
    api.get('/documents/async/dashboard/circuit-breaker/status'),

  resetCircuitBreaker: () =>
    api.post('/documents/async/dashboard/circuit-breaker/reset'),

  getHealth: () =>
    api.get('/documents/async/dashboard/health'),

  getQueue: () =>
    api.get('/documents/async/dashboard/queue')
}
