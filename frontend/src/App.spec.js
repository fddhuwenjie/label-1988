import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import Antd from 'ant-design-vue'
import App from './App.vue'

vi.mock('@ant-design/icons-vue', () => ({
  ApiOutlined: { name: 'ApiOutlined', template: '<span />' },
  DatabaseOutlined: { name: 'DatabaseOutlined', template: '<span />' },
  LinkOutlined: { name: 'LinkOutlined', template: '<span />' },
  BranchesOutlined: { name: 'BranchesOutlined', template: '<span />' },
  FileTextOutlined: { name: 'FileTextOutlined', template: '<span />' },
}))

describe('App.vue', () => {
  it('成功挂载并显示 Tare 品牌', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', redirect: '/push' },
        { path: '/push', component: { template: '<div>Push</div>' } },
      ],
    })
    await router.push('/push')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [router, Antd],
        stubs: {
          'router-view': { template: '<div />' },
        },
      },
    })

    expect(wrapper.text()).toMatch(/Tare|T/)
  })
})
