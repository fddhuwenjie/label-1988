import { vi, beforeEach, describe, it, expect } from 'vitest'

const { mockGet, mockPost, mockPut, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
}))

vi.mock('axios', () => ({
  default: {
    create: () => ({
      get: mockGet,
      post: mockPost,
      put: mockPut,
      delete: mockDelete,
      interceptors: {
        request: { use: () => {} },
        response: { use: () => {} },
      },
    }),
  },
}))

vi.mock('ant-design-vue', () => ({
  message: { error: vi.fn() },
  notification: { error: vi.fn() },
}))

import { pushApi, dataSourceApi, bindingApi, logApi } from './index'

describe('API 模块', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ code: 200, data: [] })
    mockPost.mockResolvedValue({ code: 200, data: {} })
    mockPut.mockResolvedValue({ code: 200, data: {} })
    mockDelete.mockResolvedValue({ code: 200 })
  })

  it('pushApi.list 调用 GET /push-interfaces', async () => {
    await pushApi.list()
    expect(mockGet).toHaveBeenCalledWith('/push-interfaces')
  })

  it('dataSourceApi.create 调用 POST /data-sources', async () => {
    await dataSourceApi.create({ name: 'test', url: 'https://a.com', method: 'GET' })
    expect(mockPost).toHaveBeenCalledWith('/data-sources', expect.any(Object))
  })

  it('bindingApi.execute 调用 POST bindings/:id/execute', async () => {
    await bindingApi.execute('bind-001')
    expect(mockPost).toHaveBeenCalledWith('/bindings/bind-001/execute')
  })

  it('logApi.list 支持分页参数', async () => {
    await logApi.list({ current: 1, size: 10 })
    expect(mockGet).toHaveBeenCalledWith('/execution-logs', { params: { current: 1, size: 10 } })
  })
})
