<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api/http'
import { formatMoney, reservationStatusMap } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

async function loadOrders() {
  loading.value = true
  try {
    const response = await api.get('/reservations/mine', { params: query })
    orders.value = response.data.records
    total.value = response.data.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function statusMeta(status) {
  return reservationStatusMap[status] || { label: status, type: 'info' }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page-shell">
    <div class="toolbar">
      <div><h1 class="page-title">我的订单</h1><p class="page-subtitle">查看订单状态、完成支付或取消未入住订单。</p></div>
      <el-button type="primary" @click="router.push('/rooms')">继续预订</el-button>
    </div>

    <section class="panel table-panel">
      <el-table v-loading="loading" :data="orders">
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column label="入住区间" min-width="210"><template #default="{ row }">{{ row.checkInDate }} 至 {{ row.checkOutDate }}</template></el-table-column>
        <el-table-column prop="nights" label="晚数" width="80" />
        <el-table-column label="金额" width="130"><template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="110" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="router.push(`/orders/${row.orderNo}`)">查看</el-button></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" layout="total, prev, pager, next" :total="total" @current-change="loadOrders" /></div>
    </section>
  </div>
</template>

<style scoped>
.table-panel { padding: 18px; }
.pagination { padding-top: 18px; display: flex; justify-content: flex-end; }
</style>
