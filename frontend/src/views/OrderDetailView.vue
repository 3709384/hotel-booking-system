<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api/http'
import { formatDateTime, formatMoney, reservationStatusMap } from '../utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const paying = ref(false)
const order = ref(null)
const statusMeta = computed(() => reservationStatusMap[order.value?.status] || { label: order.value?.status, type: 'info' })
const activeStep = computed(() => {
  if (!order.value) return 0
  return { PENDING_PAYMENT: 1, CONFIRMED: 2, CHECKED_IN: 3, COMPLETED: 4 }[order.value.status] ?? 0
})

async function loadOrder() {
  loading.value = true
  try {
    const response = await api.get(`/reservations/${route.params.orderNo}`)
    order.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function mockPay() {
  await ElMessageBox.confirm(`确认模拟支付 ${formatMoney(order.value.totalAmount)}？`, '模拟支付', {
    confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning',
  })
  paying.value = true
  try {
    const payment = await api.post('/payments', { orderNo: order.value.orderNo, channel: 'WECHAT' })
    const callbackUrl = payment.data.mockCallbackUrl.replace(/^\/api/, '')
    await api.post(callbackUrl, null, { params: { transactionId: `MOCK-${payment.data.paymentNo}` } })
    ElMessage.success('模拟支付成功')
    await loadOrder()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    paying.value = false
  }
}

async function cancelOrder() {
  await ElMessageBox.confirm('确认取消该订单并释放库存？', '取消订单', {
    confirmButtonText: '确认取消', cancelButtonText: '暂不取消', type: 'warning',
  })
  try {
    await api.post(`/reservations/${order.value.orderNo}/cancel`)
    ElMessage.success('订单已取消')
    await loadOrder()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadOrder)
</script>

<template>
  <div v-loading="loading" class="page-shell">
    <div class="toolbar">
      <div><h1 class="page-title">订单详情</h1><p class="page-subtitle">{{ order?.orderNo || '正在加载...' }}</p></div>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <section v-if="order" class="panel detail-panel">
      <div class="status-head"><div><span class="muted">当前状态</span><h2>{{ statusMeta.label }}</h2></div><el-tag :type="statusMeta.type" size="large">{{ statusMeta.label }}</el-tag></div>
      <el-steps :active="activeStep" finish-status="success" align-center class="order-steps">
        <el-step title="提交订单" /><el-step title="完成支付" /><el-step title="前台入住" /><el-step title="完成退房" />
      </el-steps>
      <el-descriptions :column="3" border class="descriptions">
        <el-descriptions-item label="联系人">{{ order.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ order.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="入住晚数">{{ order.nights }} 晚</el-descriptions-item>
        <el-descriptions-item label="入住日期">{{ order.checkInDate }}</el-descriptions-item>
        <el-descriptions-item label="离店日期">{{ order.checkOutDate }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ formatMoney(order.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(order.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(order.paidAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="section-title room-title">客房明细</h3>
      <el-table :data="order.items">
        <el-table-column prop="roomTypeName" label="房型" min-width="150" />
        <el-table-column prop="roomNo" label="房间号"><template #default="{ row }">{{ row.roomNo || '待排房' }}</template></el-table-column>
        <el-table-column label="单价"><template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template></el-table-column>
        <el-table-column prop="nights" label="晚数" />
        <el-table-column label="小计"><template #default="{ row }">{{ formatMoney(row.subtotal) }}</template></el-table-column>
      </el-table>

      <div class="actions">
        <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" :loading="paying" @click="mockPay">模拟支付</el-button>
        <el-button v-if="['PENDING_PAYMENT', 'CONFIRMED'].includes(order.status)" type="danger" plain @click="cancelOrder">取消订单</el-button>
        <el-button v-if="order.status === 'CONFIRMED'" type="success" plain @click="router.push({ name: 'pms', query: { orderNo: order.orderNo } })">前往前台排房</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.detail-panel { padding: 28px; }
.status-head { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.status-head h2 { margin: 5px 0 0; font-size: 30px; }
.order-steps { margin: 35px 0 42px; }
.room-title { margin: 30px 0 15px; }
.actions { margin-top: 25px; display: flex; justify-content: flex-end; gap: 10px; }
</style>
