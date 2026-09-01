import axios from 'axios'

export interface LoginUser {
  username: string
  role: string
  storeId: string
  displayName: string
}

export interface SalesOrder {
  orderId: string
  orderNo: string
  channel: string
  storeId: string
  customerName: string
  customerPhone: string
  modelCode: string
  amount: number
  status: string
  inventoryStatus: string
  paymentStatus: string
  fulfillmentStatus: string
  vin?: string
  createdAt: string
}

export interface InventoryQuota {
  id: number
  storeId: string
  modelCode: string
  available: number
  reserved: number
  version: number
  updatedAt: string
}

export interface DeadLetterEvent {
  id: string
  service: string
  topic: string
  event_id: string
  event_type: string
  aggregate_id: string
  reason: string
  status: string
  created_at: string
  replayed_at?: string
  replayed_by?: string
}

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '' })
client.interceptors.request.use(config => {
  const token = localStorage.getItem('autoflow_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export async function login(username: string, password: string): Promise<LoginUser> {
  const { data } = await client.post('/api/auth/login', { username, password })
  localStorage.setItem('autoflow_token', data.token)
  const user = { username: data.username, role: data.role, storeId: data.storeId, displayName: data.displayName }
  localStorage.setItem('autoflow_user', JSON.stringify(user))
  return user
}

export async function listOrders(): Promise<SalesOrder[]> {
  const { data } = await client.get('/api/orders')
  return data.data
}

export async function listInventoryQuotas(storeId: string, modelCodes: string[]): Promise<InventoryQuota[]> {
  const quotas = await Promise.all(modelCodes.map(async modelCode => {
    try {
      const { data } = await client.get('/api/inventory/quota', { params: { storeId, modelCode } })
      return data.data as InventoryQuota
    } catch (error) {
      // A model without an initialized quota is a valid empty slot, not a page-level failure.
      if (axios.isAxiosError(error) && error.response?.status === 409) return null
      throw error
    }
  }))
  return quotas.filter((quota): quota is InventoryQuota => quota !== null)
}

export async function listDeadLetters(): Promise<DeadLetterEvent[]> {
  const services = ['order', 'inventory', 'fulfillment']
  const responses = await Promise.all(services.map(service => client.get(`/api/events/${service}/dead-letters`)))
  return responses
    .flatMap((response, index) => response.data.map((event: Omit<DeadLetterEvent, 'service'>) => ({ ...event, service: services[index] })))
    .sort((left, right) => String(right.created_at).localeCompare(String(left.created_at)))
}

export async function replayDeadLetter(service: string, id: string): Promise<void> {
  await client.post(`/api/events/${service}/dead-letters/${id}/replay`)
}

export async function createOrder(payload: Record<string, unknown>) {
  return (await client.post('/api/orders', payload)).data.data
}

export async function approveOrder(orderId: string) {
  return (await client.post(`/api/orders/${orderId}/approve`)).data.data
}

export async function payOrder(orderId: string, scenario: string) {
  return (await client.post(`/api/orders/${orderId}/pay`, { scenario })).data.data
}

export async function cancelOrder(orderId: string, reason: string) {
  return (await client.post(`/api/orders/${orderId}/cancel`, { reason })).data.data
}

export async function completeDelivery(orderId: string) {
  return (await client.post(`/api/fulfillment/deliveries/${orderId}/complete`)).data.data
}

export function logout() {
  localStorage.removeItem('autoflow_token')
  localStorage.removeItem('autoflow_user')
}
