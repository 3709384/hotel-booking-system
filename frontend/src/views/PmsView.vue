<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api/http'
import { formatMoney, reservationStatusMap, roomStatusMap } from '../utils/format'

const route = useRoute()
const board = ref([])
const loadingBoard = ref(false)
const orderNo = ref(String(route.query.orderNo || ''))
const order = ref(null)
const loadingOrder = ref(false)
const assignmentRooms = reactive({})
const floor = ref(8)
const availableRooms = computed(() => board.value.filter((room) => room.status === 'VC'))

async function loadBoard() {
  loadingBoard.value = true
  try {
    const response = await api.get('/pms/rooms/board', { params: { floor: floor.value } })
    board.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loadingBoard.value = false
  }
}

async function loadOrder() {
  if (!orderNo.value) {
    ElMessage.warning('请输入订单号')
    return
  }
  loadingOrder.value = true
  try {
    const response = await api.get(`/reservations/${orderNo.value}`)
    order.value = response.data
    Object.keys(assignmentRooms).forEach((key) => delete assignmentRooms[key])
    order.value.items.forEach((item) => {
      if (item.roomId) {
        assignmentRooms[item.itemId] = item.roomId
      } else {
        const suggestion = availableRooms.value.find((room) => room.roomTypeId === item.roomTypeId)
        if (suggestion) assignmentRooms[item.itemId] = suggestion.roomId
      }
    })
  } catch (error) {
    order.value = null
    ElMessage.error(error.message)
  } finally {
    loadingOrder.value = false
  }
}

async function checkIn() {
  const assignments = order.value.items.map((item) => ({ itemId: item.itemId, roomId: assignmentRooms[item.itemId] }))
  if (assignments.some((item) => !item.roomId)) {
    ElMessage.warning('请为每条客房明细选择房间')
    return
  }
  try {
    const response = await api.post(`/pms/reservations/${order.value.orderNo}/check-in`, { assignments })
    order.value = response.data
    ElMessage.success('排房入住成功')
    await loadBoard()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function checkOut() {
  await ElMessageBox.confirm('确认办理退房？退房后房间将转为待清洁。', '办理退房', {
    confirmButtonText: '确认退房', cancelButtonText: '取消', type: 'warning',
  })
  try {
    const response = await api.post(`/pms/reservations/${order.value.orderNo}/check-out`)
    order.value = response.data
    ElMessage.success('退房完成')
    await loadBoard()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function updateRoomStatus(room, status) {
  try {
    await api.put(`/pms/rooms/${room.roomId}/status`, { status, remark: '前台操作' })
    ElMessage.success('房态已更新')
    await loadBoard()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function orderStatusMeta(status) {
  return reservationStatusMap[status] || { label: status, type: 'info' }
}

function roomStatusMeta(status) {
  return roomStatusMap[status] || { label: status, type: 'info' }
}

onMounted(async () => {
  await loadBoard()
  if (orderNo.value) await loadOrder()
})
</script>

<template>
  <div class="page-shell">
    <div class="toolbar">
      <div><h1 class="page-title">前台接待工作台</h1><p class="page-subtitle">查看实时房态，为已确认订单排房、入住和退房。</p></div>
      <div class="floor-control"><span>楼层</span><el-input-number v-model="floor" :min="1" :max="99" /><el-button :loading="loadingBoard" @click="loadBoard">刷新房态</el-button></div>
    </div>

    <section class="panel order-search">
      <div><h2 class="section-title">订单查询与办理</h2><p class="page-subtitle">输入订单号，核对订单后分配具体房间。</p></div>
      <div class="search-line"><el-input v-model="orderNo" placeholder="输入订单号" clearable @keyup.enter="loadOrder" /><el-button type="primary" :loading="loadingOrder" @click="loadOrder">查询订单</el-button></div>
    </section>

    <section v-if="order" class="panel order-workbench">
      <div class="order-head"><div><span class="muted">订单号</span><h2>{{ order.orderNo }}</h2></div><el-tag :type="orderStatusMeta(order.status).type">{{ orderStatusMeta(order.status).label }}</el-tag></div>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="客人">{{ order.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ order.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="入住">{{ order.checkInDate }}</el-descriptions-item>
        <el-descriptions-item label="离店">{{ order.checkOutDate }}</el-descriptions-item>
        <el-descriptions-item label="晚数">{{ order.nights }} 晚</el-descriptions-item>
        <el-descriptions-item label="金额">{{ formatMoney(order.totalAmount) }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="section-title room-assignment-title">排房分配</h3>
      <div class="assignment-list">
        <div v-for="item in order.items" :key="item.itemId" class="assignment-row">
          <div><strong>{{ item.roomTypeName }}</strong><span>明细 #{{ item.itemId }}，{{ item.nights }} 晚</span></div>
          <el-select v-model="assignmentRooms[item.itemId]" placeholder="选择房间" :disabled="order.status !== 'CONFIRMED'">
            <el-option v-for="room in availableRooms.filter((room) => room.roomTypeId === item.roomTypeId)" :key="room.roomId" :label="room.roomNo" :value="room.roomId" />
          </el-select>
          <el-tag v-if="item.roomNo" type="success">{{ item.roomNo }}</el-tag>
        </div>
      </div>
      <div class="order-actions"><el-button v-if="order.status === 'CONFIRMED'" type="primary" @click="checkIn">确认排房并入住</el-button><el-button v-if="order.status === 'CHECKED_IN'" type="success" @click="checkOut">办理退房</el-button></div>
    </section>

    <section class="panel board-panel">
      <div class="toolbar board-toolbar"><div><h2 class="section-title">{{ floor }} 楼房态盘</h2><p class="page-subtitle">绿色可排房，黄色待清洁，红色维修停用。</p></div></div>
      <el-table v-loading="loadingBoard" :data="board">
        <el-table-column prop="roomNo" label="房间号" width="100" />
        <el-table-column prop="roomTypeName" label="房型" min-width="150" />
        <el-table-column label="房态" width="120"><template #default="{ row }"><el-tag :type="roomStatusMeta(row.status).type">{{ roomStatusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column prop="orderNo" label="当前订单"><template #default="{ row }">{{ row.orderNo || '-' }}</template></el-table-column>
        <el-table-column label="操作" width="230">
          <template #default="{ row }">
            <el-button v-if="row.status === 'VD'" link type="success" @click="updateRoomStatus(row, 'VC')">清洁完成</el-button>
            <el-button v-if="row.status === 'OOO'" link type="primary" @click="updateRoomStatus(row, 'VC')">恢复可用</el-button>
            <el-button v-if="['VC', 'VD'].includes(row.status)" link type="danger" @click="updateRoomStatus(row, 'OOO')">设为维修</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.floor-control, .search-line { display: flex; align-items: center; gap: 10px; }
.order-search { padding: 22px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.search-line { min-width: min(520px, 100%); }
.search-line .el-input { flex: 1; }
.order-workbench, .board-panel { margin-top: 22px; padding: 24px; }
.order-head { margin-bottom: 22px; display: flex; align-items: center; justify-content: space-between; }
.order-head h2 { margin: 5px 0 0; }
.room-assignment-title { margin: 28px 0 15px; }
.assignment-list { display: grid; gap: 10px; }
.assignment-row { padding: 14px; display: grid; grid-template-columns: 1fr minmax(160px, 220px) auto; align-items: center; gap: 14px; border: 1px solid var(--line); border-radius: 12px; background: #fafbfc; }
.assignment-row strong, .assignment-row span { display: block; }
.assignment-row span { margin-top: 4px; color: var(--muted); font-size: 12px; }
.order-actions { margin-top: 22px; display: flex; justify-content: flex-end; }
.board-toolbar { margin-bottom: 10px; }
@media (max-width: 850px) { .order-search { align-items: stretch; flex-direction: column; } .search-line { min-width: 0; } .assignment-row { grid-template-columns: 1fr; } }
</style>
