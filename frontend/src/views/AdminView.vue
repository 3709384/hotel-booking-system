<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/http'
import { formatMoney, roomStatusMap } from '../utils/format'

const activeTab = ref('roomTypes')
const loading = ref(false)
const typeLoading = ref(false)
const roomLoading = ref(false)
const roomTypes = ref([])
const rooms = ref([])

const typeForm = reactive({ code: '', name: '', bedType: '大床1.8m', maxOccupancy: 2, area: 28, basePrice: 268, description: '', amenitiesText: 'wifi,空调,独立卫浴', sortOrder: 99 })
const roomForm = reactive({ roomNo: '', roomTypeId: null, floor: 8, status: 'VC', remark: '' })

async function loadTypes() {
  typeLoading.value = true
  try {
    const response = await api.get('/admin/room-types', { params: { page: 1, size: 100 } })
    roomTypes.value = response.data.records
    if (!roomForm.roomTypeId && roomTypes.value.length) roomForm.roomTypeId = roomTypes.value[0].id
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    typeLoading.value = false
  }
}

async function loadRooms() {
  roomLoading.value = true
  try {
    const response = await api.get('/admin/rooms', { params: { page: 1, size: 100 } })
    rooms.value = response.data.records
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    roomLoading.value = false
  }
}

async function createType() {
  if (!typeForm.code || !typeForm.name || !typeForm.basePrice) {
    ElMessage.warning('请填写房型编码、名称和价格')
    return
  }
  loading.value = true
  try {
    await api.post('/admin/room-types', {
      code: typeForm.code,
      name: typeForm.name,
      bedType: typeForm.bedType,
      maxOccupancy: Number(typeForm.maxOccupancy),
      area: Number(typeForm.area),
      basePrice: Number(typeForm.basePrice),
      description: typeForm.description,
      amenities: typeForm.amenitiesText.split(/[,，]/).map((item) => item.trim()).filter(Boolean),
      status: 1,
      sortOrder: Number(typeForm.sortOrder),
    })
    ElMessage.success('房型创建成功')
    Object.assign(typeForm, { code: '', name: '', bedType: '大床1.8m', maxOccupancy: 2, area: 28, basePrice: 268, description: '', amenitiesText: 'wifi,空调,独立卫浴', sortOrder: 99 })
    await loadTypes()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function createRoom() {
  if (!roomForm.roomNo || !roomForm.roomTypeId) {
    ElMessage.warning('请填写房间号并选择房型')
    return
  }
  loading.value = true
  try {
    await api.post('/admin/rooms', { roomNo: roomForm.roomNo, roomTypeId: Number(roomForm.roomTypeId), floor: Number(roomForm.floor), status: roomForm.status, remark: roomForm.remark })
    ElMessage.success('房间创建成功')
    roomForm.roomNo = ''
    roomForm.remark = ''
    await loadRooms()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function initInventory() {
  loading.value = true
  try {
    const response = await api.post('/admin/inventory/init', null, { params: { days: 90 } })
    ElMessage.success(`库存补齐完成，新增 ${response.data.createdRows} 行`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function toggleType(type) {
  try {
    await api.put('/admin/room-types/' + type.id, { status: type.status === 1 ? 0 : 1 })
    ElMessage.success(type.status === 1 ? '房型已停用' : '房型已恢复上架')
    await loadTypes()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function roomStatus(status) {
  return roomStatusMap[status] || { label: status, type: 'info' }
}

onMounted(async () => {
  await Promise.all([loadTypes(), loadRooms()])
})
</script>

<template>
  <div class="page-shell">
    <div class="toolbar">
      <div><h1 class="page-title">后台资源管理</h1><p class="page-subtitle">维护房型、物理房间以及未来 90 天的库存数据。</p></div>
      <el-button type="primary" plain :loading="loading" @click="initInventory">补齐未来 90 天库存</el-button>
    </div>

    <section class="panel admin-panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="房型管理" name="roomTypes">
          <div class="admin-layout">
            <div class="list-column">
              <h2 class="section-title">现有房型</h2>
              <el-table v-loading="typeLoading" :data="roomTypes">
                <el-table-column prop="code" label="编码" width="90" />
                <el-table-column prop="name" label="房型名称" min-width="130" />
                <el-table-column prop="bedType" label="床型" min-width="110" />
                <el-table-column label="基础价格" width="120"><template #default="{ row }">{{ formatMoney(row.basePrice) }}</template></el-table-column>
                <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '停用' }}</el-tag></template></el-table-column>
                <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleType(row)">{{ row.status === 1 ? '停用' : '上架' }}</el-button></template></el-table-column>
              </el-table>
            </div>
            <el-card shadow="never" class="form-card">
              <template #header><strong>新增房型</strong></template>
              <el-form label-position="top">
                <el-form-item label="房型编码"><el-input v-model="typeForm.code" placeholder="例如 FAM" /></el-form-item>
                <el-form-item label="房型名称"><el-input v-model="typeForm.name" /></el-form-item>
                <el-form-item label="床型"><el-input v-model="typeForm.bedType" /></el-form-item>
                <div class="form-grid"><el-form-item label="最大入住"><el-input-number v-model="typeForm.maxOccupancy" :min="1" /></el-form-item><el-form-item label="面积"><el-input-number v-model="typeForm.area" :min="1" /></el-form-item></div>
                <el-form-item label="基础价格"><el-input-number v-model="typeForm.basePrice" :min="1" :step="10" /></el-form-item>
                <el-form-item label="设施（逗号分隔）"><el-input v-model="typeForm.amenitiesText" /></el-form-item>
                <el-form-item label="描述"><el-input v-model="typeForm.description" type="textarea" :rows="3" /></el-form-item>
                <el-button type="primary" :loading="loading" @click="createType">创建房型</el-button>
              </el-form>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="房间管理" name="rooms">
          <div class="admin-layout">
            <div class="list-column">
              <h2 class="section-title">现有房间</h2>
              <el-table v-loading="roomLoading" :data="rooms">
                <el-table-column prop="roomNo" label="房间号" width="100" />
                <el-table-column prop="floor" label="楼层" width="80" />
                <el-table-column prop="roomTypeId" label="房型 ID" width="100" />
                <el-table-column label="房态" width="110"><template #default="{ row }"><el-tag :type="roomStatus(row.status).type">{{ roomStatus(row.status).label }}</el-tag></template></el-table-column>
                <el-table-column prop="remark" label="备注" />
              </el-table>
            </div>
            <el-card shadow="never" class="form-card">
              <template #header><strong>新增房间</strong></template>
              <el-form label-position="top">
                <el-form-item label="房间号"><el-input v-model="roomForm.roomNo" placeholder="例如 8401" /></el-form-item>
                <el-form-item label="房型"><el-select v-model="roomForm.roomTypeId" placeholder="选择房型"><el-option v-for="type in roomTypes" :key="type.id" :label="`${type.name}（${type.code}）`" :value="type.id" /></el-select></el-form-item>
                <el-form-item label="楼层"><el-input-number v-model="roomForm.floor" :min="1" :max="99" /></el-form-item>
                <el-form-item label="初始房态"><el-select v-model="roomForm.status"><el-option label="空闲可售" value="VC" /><el-option label="待清洁" value="VD" /><el-option label="维修停用" value="OOO" /></el-select></el-form-item>
                <el-form-item label="备注"><el-input v-model="roomForm.remark" /></el-form-item>
                <el-button type="primary" :loading="loading" @click="createRoom">创建房间</el-button>
              </el-form>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped>
.admin-panel { padding: 18px 24px 28px; }
.admin-layout { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.6fr); gap: 24px; }
.list-column h2 { margin-bottom: 15px; }
.form-card { border-radius: 14px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
@media (max-width: 900px) { .admin-layout { grid-template-columns: 1fr; } }
</style>
