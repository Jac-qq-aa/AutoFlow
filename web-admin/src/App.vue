<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { approveOrder, cancelOrder, completeDelivery, createOrder, listDeadLetters, listInventoryQuotas, listOrders, login, logout, payOrder, replayDeadLetter, type DeadLetterEvent, type InventoryQuota, type LoginUser, type SalesOrder } from './api'
import { modelCodes, vehicleModels } from './catalog'
import OrderWorkspace from './views/OrderWorkspace.vue'
import InventoryWorkspace from './views/InventoryWorkspace.vue'
import EventWorkspace from './views/EventWorkspace.vue'
import SettingsWorkspace from './views/SettingsWorkspace.vue'

type WorkspaceView = 'orders' | 'inventory' | 'events' | 'settings'

const user = ref<LoginUser | null>(JSON.parse(localStorage.getItem('autoflow_user') || 'null'))
const orders = ref<SalesOrder[]>([])
const inventoryQuotas = ref<InventoryQuota[]>([])
const deadLetters = ref<DeadLetterEvent[]>([])
const activeView = ref<WorkspaceView>('orders')
const loading = ref(false)
const auxiliaryLoading = ref(false)
const loginForm = reactive({ username: 'manager', password: 'demo123' })
const createVisible = ref(false)
const createForm = reactive({
  channel: 'STORE', channelOrderNo: '', storeId: 'STORE-SH-001', customerName: '', customerPhone: '',
  modelCode: 'AF-SUV-PRO', amount: 219800,
})

const allNavigation: Array<{ key: WorkspaceView; label: string; adminOnly?: boolean }> = [
  { key: 'orders', label: '订单履约' },
  { key: 'inventory', label: '库存视图' },
  { key: 'events', label: '事件监控', adminOnly: true },
  { key: 'settings', label: '系统设置' },
]
const navigation = computed(() => allNavigation.filter(item => !item.adminOnly || user.value?.role === 'ADMIN'))

async function signIn() {
  try { user.value = await login(loginForm.username, loginForm.password); await refresh(); ElMessage.success('登录成功') }
  catch { ElMessage.error('登录失败，请检查演示账号') }
}
function signOut() { logout(); user.value = null; orders.value = [] }
async function refresh() {
  if (!user.value) return
  loading.value = true
  try { orders.value = await listOrders() } catch { ElMessage.error('订单加载失败') } finally { loading.value = false }
}
async function loadInventory() {
  if (!user.value) return
  auxiliaryLoading.value = true
  try { inventoryQuotas.value = await listInventoryQuotas(user.value.storeId, modelCodes) }
  catch { ElMessage.error('库存配额加载失败') }
  finally { auxiliaryLoading.value = false }
}
async function loadEvents() {
  if (user.value?.role !== 'ADMIN') return
  auxiliaryLoading.value = true
  try { deadLetters.value = await listDeadLetters() }
  catch { ElMessage.error('事件监控数据加载失败') }
  finally { auxiliaryLoading.value = false }
}
const viewLoaders: Partial<Record<WorkspaceView, () => Promise<void>>> = {
  orders: refresh,
  inventory: loadInventory,
  events: loadEvents,
}
async function switchView(view: WorkspaceView) {
  activeView.value = view
  await viewLoaders[view]?.()
}
async function refreshActiveView() {
  await viewLoaders[activeView.value]?.()
}
async function replayEvent(event: DeadLetterEvent) {
  try {
    await replayDeadLetter(event.service, event.id)
    ElMessage.success('死信事件已提交重放')
    await loadEvents()
  } catch (error: any) { ElMessage.error(error.response?.data?.message || '死信重放失败') }
}
async function submitCreate() {
  try { await createOrder(createForm); createVisible.value = false; ElMessage.success('订单已创建，等待经理审核'); await refresh() }
  catch (error: any) { ElMessage.error(error.response?.data?.message || '创建失败') }
}
async function act(action: 'approve' | 'pay' | 'cancel' | 'deliver', order: SalesOrder) {
  try {
    if (action === 'approve') await approveOrder(order.orderId)
    if (action === 'pay') {
      const { value } = await ElMessageBox.prompt('SUCCESS / FAILURE / TIMEOUT', '支付模拟场景', { inputValue: 'SUCCESS' })
      await payOrder(order.orderId, value)
    }
    if (action === 'cancel') {
      const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', { inputValue: '客户改变购买计划' })
      await cancelOrder(order.orderId, value)
    }
    if (action === 'deliver') await completeDelivery(order.orderId)
    ElMessage.success('操作已提交，事件正在异步流转'); setTimeout(refresh, 1200)
  } catch (error: any) { if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '操作失败') }
}
onMounted(refresh)
</script>

<template>
  <div v-if="!user" class="login-shell">
    <section class="brand-panel">
      <span class="eyebrow">AUTOMOTIVE ORDER ORCHESTRATION</span>
      <h1>每一笔订单，<br />都有清晰的下一站。</h1>
      <p>AutoFlow 将多渠道销售、库存配额、VIN 分配、支付与交付组织成可追踪的履约链路。</p>
      <div class="signal"><i></i><span>事件驱动服务已就绪</span></div>
    </section>
    <el-card class="login-card" shadow="never">
      <div class="logo-mark">AF</div><h2>登录工作台</h2><p>使用内置账号体验门店业务流程</p>
      <el-form label-position="top" @submit.prevent="signIn">
        <el-form-item label="账号"><el-input v-model="loginForm.username" size="large" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="loginForm.password" type="password" size="large" show-password /></el-form-item>
        <el-button type="primary" size="large" native-type="submit" class="full">进入 AutoFlow</el-button>
      </el-form>
      <div class="demo-users">manager / sales / delivery / admin<br />统一密码：demo123</div>
    </el-card>
  </div>

  <div v-else class="app-shell">
    <aside>
      <div class="side-brand"><span>AF</span><strong>AutoFlow</strong></div>
      <nav>
        <button
          v-for="item in navigation"
          :key="item.key"
          type="button"
          :class="{ active: activeView === item.key }"
          :aria-label="item.label"
          @click="switchView(item.key)"
        >{{ item.label }}</button>
      </nav>
      <div class="aside-foot"><small>{{ user.displayName }}</small><span>{{ user.role }} · {{ user.storeId }}</span><button @click="signOut">退出登录</button></div>
    </aside>
    <OrderWorkspace v-if="activeView === 'orders'" :orders="orders" :loading="loading" @refresh="refresh" @create="createVisible = true" @action="act" />
    <InventoryWorkspace v-else-if="activeView === 'inventory'" :user="user" :quotas="inventoryQuotas" :loading="auxiliaryLoading" @refresh="refreshActiveView" />
    <EventWorkspace v-else-if="activeView === 'events'" :events="deadLetters" :loading="auxiliaryLoading" @refresh="refreshActiveView" @replay="replayEvent" />
    <SettingsWorkspace v-else :user="user" />
  </div>

  <el-dialog v-model="createVisible" title="创建销售订单" width="560px">
    <el-form label-position="top">
      <div class="form-grid"><el-form-item label="销售渠道"><el-select v-model="createForm.channel"><el-option label="线下门店" value="STORE" /><el-option label="官网" value="WEBSITE" /><el-option label="小程序" value="MINI_PROGRAM" /></el-select></el-form-item><el-form-item label="门店"><el-select v-model="createForm.storeId"><el-option label="上海 001" value="STORE-SH-001" /><el-option label="北京 001" value="STORE-BJ-001" /><el-option label="深圳 001" value="STORE-SZ-001" /></el-select></el-form-item></div>
      <div class="form-grid"><el-form-item label="客户姓名"><el-input v-model="createForm.customerName" /></el-form-item><el-form-item label="联系电话"><el-input v-model="createForm.customerPhone" /></el-form-item></div>
      <div class="form-grid"><el-form-item label="车型"><el-select v-model="createForm.modelCode"><el-option v-for="model in vehicleModels" :key="model.code" :label="model.label" :value="model.code" /></el-select></el-form-item><el-form-item label="成交金额"><el-input-number v-model="createForm.amount" :min="1" :step="1000" /></el-form-item></div>
    </el-form>
    <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" @click="submitCreate">提交订单</el-button></template>
  </el-dialog>
</template>
