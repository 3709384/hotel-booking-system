<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api/http'
import { useAuthStore } from '../stores/auth'
import { formatMoney } from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const room = ref(null)
const availability = ref(null)
const form = reactive({ contactName: auth.user?.name || '', contactPhone: auth.user?.phone || '', remark: '', quantity: 1 })
const checkIn = computed(() => String(route.query.checkIn || ''))
const checkOut = computed(() => String(route.query.checkOut || ''))
const totalAmount = computed(() => Number(availability.value?.totalPrice || 0) * form.quantity)

async function loadRoom() {
  loading.value = true
  try {
    const [roomResponse, availabilityResponse] = await Promise.all([
      api.get(`/room-types/${route.params.typeId}`),
      api.get('/availability', { params: { checkIn: checkIn.value, checkOut: checkOut.value } }),
    ])
    room.value = roomResponse.data
    availability.value = availabilityResponse.data.find((item) => String(item.roomTypeId) === String(route.params.typeId))
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.contactName || !form.contactPhone) {
    ElMessage.warning('请填写联系人和联系电话')
    return
  }
  if (Number(form.quantity) > Number(availability.value?.minAvailable || 0)) {
    ElMessage.warning('预订间数超过当前可售库存')
    return
  }
  submitting.value = true
  try {
    const response = await api.post('/reservations', {
      contactName: form.contactName,
      contactPhone: form.contactPhone,
      checkInDate: checkIn.value,
      checkOutDate: checkOut.value,
      remark: form.remark,
      items: [{ roomTypeId: Number(route.params.typeId), quantity: Number(form.quantity) }],
    })
    ElMessage.success('订单创建成功，请完成支付')
    router.replace({ name: 'order-detail', params: { orderNo: response.data.orderNo } })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

onMounted(loadRoom)
</script>

<template>
  <div class="page-shell">
    <div class="toolbar">
      <div><h1 class="page-title">确认预订信息</h1><p class="page-subtitle">{{ checkIn }} 入住，{{ checkOut }} 离店</p></div>
      <el-button @click="router.back()">返回房型列表</el-button>
    </div>

    <div v-loading="loading" class="booking-layout">
      <section class="panel form-panel">
        <h2 class="section-title">入住信息</h2>
        <el-form label-position="top" class="booking-form">
          <el-form-item label="联系人"><el-input v-model="form.contactName" placeholder="请输入入住联系人" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.contactPhone" maxlength="11" placeholder="请输入联系电话" /></el-form-item>
          <el-form-item label="预订间数"><el-input-number v-model="form.quantity" :min="1" :max="availability?.minAvailable || 1" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="4" placeholder="如无烟房、预计到店时间等" /></el-form-item>
        </el-form>
      </section>

      <aside class="panel summary-panel">
        <div class="summary-cover">
          <img :src="room?.coverImage || '/images/rooms/std.jpg'" :alt="room?.name" />
          <div class="cover-overlay">
            <span>{{ room?.code }}</span>
            <strong>{{ room?.name }}</strong>
            <div class="cover-rating">
              <el-rate :model-value="Number(room?.averageScore || 0)" disabled allow-half />
              <small>{{ room?.reviewCount || 0 }} 条评价</small>
            </div>
          </div>
        </div>
        <div class="summary-body">
          <div class="summary-row"><span>房型简介</span><strong class="summary-description">{{ room?.description }}</strong></div>
          <div class="summary-row"><span>床型</span><strong>{{ room?.bedType }}</strong></div>
          <div class="summary-row"><span>入住日期</span><strong>{{ checkIn }}</strong></div>
          <div class="summary-row"><span>离店日期</span><strong>{{ checkOut }}</strong></div>
          <div class="summary-row"><span>入住晚数</span><strong>{{ availability?.nights }} 晚</strong></div>
          <div class="summary-row"><span>剩余库存</span><strong>{{ availability?.minAvailable }} 间</strong></div>
          <div class="summary-total"><span>订单总额</span><strong>{{ formatMoney(totalAmount) }}</strong></div>
          <el-button type="primary" size="large" :loading="submitting" @click="submit">提交订单</el-button>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.booking-layout { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.6fr); gap: 24px; }
.form-panel, .summary-panel { padding: 26px; }
.booking-form { max-width: 650px; margin-top: 24px; }
.summary-panel { overflow: hidden; padding: 0; align-self: start; }
.summary-cover { position: relative; min-height: 210px; overflow: hidden; color: white; }
.summary-cover img { width: 100%; height: 210px; display: block; object-fit: cover; }
.cover-overlay { position: absolute; inset: 0; padding: 24px; display: flex; flex-direction: column; justify-content: flex-end; background: linear-gradient(180deg, rgba(0,0,0,0.08), rgba(0,0,0,0.68)); }
.summary-cover span { opacity: 0.72; font-size: 12px; letter-spacing: 0.16em; }
.summary-cover strong { margin-top: 7px; font-size: 28px; }
.cover-rating { margin-top: 8px; display: flex; align-items: center; gap: 8px; }
.cover-rating :deep(.el-rate) { height: auto; }
.cover-rating :deep(.el-rate__icon) { margin-right: 1px; font-size: 15px; }
.cover-rating small { color: rgba(255,255,255,0.76); }
.summary-description { max-width: 190px; font-size: 13px; line-height: 1.55; }
.summary-body { padding: 22px; }
.summary-row { display: flex; justify-content: space-between; gap: 16px; padding: 11px 0; color: var(--muted); border-bottom: 1px dashed var(--line); }
.summary-row strong { color: var(--ink); text-align: right; }
.summary-total { margin: 22px 0 18px; display: flex; align-items: flex-end; justify-content: space-between; }
.summary-total strong { color: #b45309; font-size: 28px; }
.summary-body .el-button { width: 100%; }
@media (max-width: 900px) { .booking-layout { grid-template-columns: 1fr; } }
</style>
