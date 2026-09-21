<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api/http'
import { formatMoney } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const availability = ref([])

function dateAfter(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

const query = reactive({ dates: [dateAfter(1), dateAfter(3)] })

async function loadAvailability() {
  if (!query.dates?.[0] || !query.dates?.[1]) {
    ElMessage.warning('请选择入住和离店日期')
    return
  }
  loading.value = true
  try {
    const response = await api.get('/availability', { params: { checkIn: query.dates[0], checkOut: query.dates[1] } })
    availability.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function book(room) {
  router.push({ name: 'booking', params: { typeId: room.roomTypeId }, query: { checkIn: query.dates[0], checkOut: query.dates[1] } })
}

onMounted(loadAvailability)
</script>

<template>
  <div class="rooms-hero">
    <div class="page-shell hero-inner">
      <div>
        <span class="hero-kicker">SELECT YOUR STAY</span>
        <h1>住得舒适，预订简单。</h1>
        <p>选择入住日期，系统会按“每一夜”的真实库存展示可订房型。</p>
      </div>
      <div class="search-box">
        <el-date-picker v-model="query.dates" type="daterange" unlink-panels range-separator="入住至" start-placeholder="入住日期" end-placeholder="离店日期" value-format="YYYY-MM-DD" :clearable="false" />
        <el-button type="primary" size="large" :loading="loading" @click="loadAvailability">查询可售客房</el-button>
      </div>
    </div>
  </div>

  <div class="page-shell">
    <div class="toolbar">
      <div>
        <h2 class="section-title">可售房型</h2>
        <p class="page-subtitle">{{ query.dates[0] }} 入住，{{ query.dates[1] }} 离店</p>
      </div>
      <el-tag effect="plain">实库存按日期动态计算</el-tag>
    </div>

    <div v-loading="loading" class="room-grid">
      <article v-for="room in availability" :key="room.roomTypeId" class="room-card panel">
        <div class="room-visual" :class="`theme-${room.roomTypeId % 4}`">
          <span>{{ room.code }}</span><strong>{{ room.name }}</strong>
        </div>
        <div class="room-body">
          <div class="room-meta"><span>{{ room.bedType }}</span><span>{{ room.nights }} 晚</span></div>
          <div class="amenities">
            <el-tag v-for="item in room.amenities" :key="item" size="small" effect="plain">{{ item }}</el-tag>
          </div>
          <div class="room-footer">
            <div>
              <small>每晚</small>
              <div class="money">{{ formatMoney(room.basePrice) }}</div>
              <p>合计 {{ formatMoney(room.totalPrice) }}</p>
            </div>
            <div class="room-action">
              <el-tag :type="room.minAvailable > 0 ? 'success' : 'info'" effect="light">剩余 {{ room.minAvailable }} 间</el-tag>
              <el-button type="primary" :disabled="room.minAvailable < 1" @click="book(room)">立即预订</el-button>
            </div>
          </div>
        </div>
      </article>
    </div>
    <div v-if="!loading && availability.length === 0" class="empty-state panel">当前日期没有可展示的房型</div>
  </div>
</template>

<style scoped>
.rooms-hero { padding: 58px 0 70px; color: white; background: linear-gradient(120deg, rgba(8, 61, 78, 0.96), rgba(23, 107, 135, 0.78)), radial-gradient(circle at 82% 20%, #3cc4b6, transparent 27%), #0d4f65; }
.hero-inner { padding-top: 0; padding-bottom: 0; }
.hero-kicker { font-size: 12px; letter-spacing: 0.2em; opacity: 0.7; }
.hero-inner h1 { margin: 15px 0 13px; font-size: clamp(36px, 5vw, 58px); line-height: 1.1; }
.hero-inner p { margin: 0; color: rgba(255, 255, 255, 0.76); }
.search-box { margin-top: 34px; padding: 14px; display: flex; gap: 12px; align-items: center; border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 18px; background: rgba(255, 255, 255, 0.1); backdrop-filter: blur(12px); }
.room-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; min-height: 180px; }
.room-card { overflow: hidden; }
.room-visual { min-height: 150px; padding: 24px; display: flex; flex-direction: column; justify-content: flex-end; color: white; background: linear-gradient(140deg, rgba(8, 61, 78, 0.9), rgba(43, 166, 158, 0.7)), #176b87; }
.theme-1 { background: linear-gradient(140deg, #263b68, #5978b6); }
.theme-2 { background: linear-gradient(140deg, #6d4326, #c58b54); }
.theme-3 { background: linear-gradient(140deg, #40543b, #80a06c); }
.room-visual span { font-size: 12px; letter-spacing: 0.18em; opacity: 0.75; }
.room-visual strong { margin-top: 7px; font-size: 28px; }
.room-body { padding: 21px; }
.room-meta { display: flex; justify-content: space-between; color: var(--muted); font-size: 14px; }
.amenities { min-height: 30px; margin: 17px 0 20px; display: flex; flex-wrap: wrap; gap: 6px; }
.room-footer { padding-top: 18px; display: flex; justify-content: space-between; gap: 18px; border-top: 1px solid var(--line); }
.room-footer small, .room-footer p { color: var(--muted); }
.room-footer p { margin: 4px 0 0; font-size: 12px; }
.room-action { display: flex; flex-direction: column; align-items: flex-end; justify-content: space-between; gap: 12px; }
@media (max-width: 900px) { .room-grid { grid-template-columns: 1fr; } .search-box { align-items: stretch; flex-direction: column; } }
</style>
