// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'

vi.mock('./api', () => ({
  approveOrder: vi.fn(),
  cancelOrder: vi.fn(),
  completeDelivery: vi.fn(),
  createOrder: vi.fn(),
  listDeadLetters: vi.fn().mockResolvedValue([]),
  listInventoryQuotas: vi.fn().mockResolvedValue([]),
  listOrders: vi.fn().mockResolvedValue([]),
  login: vi.fn(),
  logout: vi.fn(),
  payOrder: vi.fn(),
  replayDeadLetter: vi.fn(),
}))

describe('sidebar navigation', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('autoflow_user', JSON.stringify({
      username: 'manager',
      role: 'ADMIN',
      storeId: 'STORE-SH-001',
      displayName: '上海门店经理',
    }))
  })

  it.each([
    ['库存视图', '库存视图'],
    ['事件监控', '事件监控'],
    ['系统设置', '系统设置'],
  ])('switches to %s when its menu item is clicked', async (menuLabel, heading) => {
    const wrapper = mount(App, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    await wrapper.get('nav').get(`[aria-label="${menuLabel}"]`).trigger('click')

    expect(wrapper.get('main h1').text()).toBe(heading)
  })
})
