import axios from 'axios'
import { message, notification } from 'ant-design-vue'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加loading状态
    return config
  },
  error => {
    message.error('请求发送失败')
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    const res = response.data
    
    // 业务错误处理
    if (res.code && res.code !== 200) {
      const errorMsg = res.message || '操作失败'
      
      // 显示详细错误信息
      if (res.data && res.data.fields) {
        // 参数校验错误，显示字段级别的错误
        const fieldErrors = Object.entries(res.data.fields)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join('\n')
        notification.error({
          message: errorMsg,
          description: fieldErrors,
          duration: 5
        })
      } else if (res.data && res.data.detail) {
        notification.error({
          message: errorMsg,
          description: res.data.detail,
          duration: 4
        })
      } else {
        message.error(errorMsg)
      }
      
      return Promise.reject(new Error(errorMsg))
    }
    
    return res
  },
  error => {
    console.error('API Error:', error)
    
    let errorMsg = '网络错误，请稍后重试'
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          errorMsg = data?.message || '请求参数错误'
          if (data?.data?.fields) {
            const fieldErrors = Object.entries(data.data.fields)
              .map(([field, msg]) => `${field}: ${msg}`)
              .join('\n')
            notification.error({
              message: errorMsg,
              description: fieldErrors,
              duration: 5
            })
            return Promise.reject(error)
          }
          break
        case 404:
          errorMsg = data?.message || '资源不存在'
          break
        case 500:
          errorMsg = data?.message || '服务器内部错误'
          break
        default:
          errorMsg = data?.message || `请求失败 (${status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      errorMsg = '请求超时，请稍后重试'
    }
    
    message.error(errorMsg)
    return Promise.reject(error)
  }
)

// Push Interface API
export const pushApi = {
  list: () => api.get('/push-interfaces'),
  get: (id) => api.get(`/push-interfaces/${id}`),
  create: (data) => api.post('/push-interfaces', data),
  update: (id, data) => api.put(`/push-interfaces/${id}`, data),
  delete: (id) => api.delete(`/push-interfaces/${id}`)
}

// Data Source API
export const dataSourceApi = {
  list: () => api.get('/data-sources'),
  get: (id) => api.get(`/data-sources/${id}`),
  create: (data) => api.post('/data-sources', data),
  update: (id, data) => api.put(`/data-sources/${id}`, data),
  delete: (id) => api.delete(`/data-sources/${id}`)
}

// Binding API
export const bindingApi = {
  list: () => api.get('/bindings'),
  get: (id) => api.get(`/bindings/${id}`),
  create: (data) => api.post('/bindings', data),
  update: (id, data) => api.put(`/bindings/${id}`, data),
  delete: (id) => api.delete(`/bindings/${id}`),
  execute: (id) => api.post(`/bindings/${id}/execute`)
}

// Execution Log API
export const logApi = {
  list: (params) => api.get('/execution-logs', { params }),
  get: (id) => api.get(`/execution-logs/${id}`),
  stats: () => api.get('/execution-logs/stats')
}

export default api
