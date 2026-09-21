<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api/http'
import { formatMoney } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const availability = ref([])
const checkIn = ref(dateAfter(1))
const checkOut = ref(dateAfter(3))
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewRoom = ref(null)
const reviews = ref([])

function dateAfter(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return toDateString(date)
}

function toDateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return year + '-' + month + '-' + day
}

const nights = computed(() => {
  if (!checkIn.value || !checkOut.value) return 0
  const start = new Date(checkIn.value + 'T00:00:00')
  const end = new Date(checkOut.value + 'T00:00:00')
  return Math.max(Math.round((end - start) / 86400000), 0)
})

const presets = [
  { label: '明日入住', inDays: 1, nights: 1 },
  { label: '两天一夜', inDays: 1, nights: 2 },
  { label: '周末小住', inDays: 5, nights: 2 },
]

function applyPreset(preset) {
  checkIn.value = dateAfter(preset.inDays)
  const date = new Date(checkIn.value + 'T00:00:00')
  date.setDate(date.getDate() + preset.nights)
  checkOut.value = toDateString(date)
  loadAvailability()
}

function normalizeDates() {
  if (!checkIn.value) return
  if (!checkOut.value || checkOut.value <= checkIn.value) {
    const date = new Date(checkIn.value + 'T00:00:00')
    date.setDate(date.getDate() + 1)
    checkOut.value = toDateString(date)
  }
}

async function loadAvailability() {
  normalizeDates()
  if (nights.value < 1) {
    ElMessage.warning('离店日期必须晚于入住日期')
    return
  }
  loading.value = true
  try {
    const response = await api.get('/availability', {
      params: { checkIn: checkIn.value, checkOut: checkOut.value },
    })
    availability.value = response.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function openReviews(room) {
  reviewRoom.value = room
  reviewVisible.value = true
  reviewLoading.value = true
  reviews.value = []
  try {
    const response = await api.get('/room-types/' + room.roomTypeId + '/reviews', {
      params: { page: 1, size: 20 },
    })
    reviews.value = response.data.records
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    reviewLoading.value = false
  }
}

function book(room) {
  router.push({
    name: 'booking',
    params: { typeId: room.roomTypeId },
    query: { checkIn: checkIn.value, checkOut: checkOut.value },
  })
}

function formatReviewDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 10) : ''
}

onMounted(loadAvailability)
</script>

<template>
  <div class="rooms-hero">
    <div class="page-shell hero-inner">
      <div class="hero-copy">
        <span class="hero-kicker">SELECT YOUR STAY</span>
        <h1>找到适合这一晚的房间。</h1>
        <p>按每一夜的真实库存查询，日期、价格和可订数量同步更新。</p>
      </div>

      <div class="search-panel">
        <div class="date-field">
          <span>入住</span>
          <el-date-picker
            v-model="checkIn"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择入住日期"
            :clearable="false"
            @change="normalizeDates"
          />
        </div>
        <div class="stay-badge"><strong>{{ nights || 1 }}</strong><small>晚</small></div>
        <div class="date-field">
          <span>离店</span>
          <el-date-picker
            v-model="checkOut"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择离店日期"
            :clearable="false"
          />
        </div>
        <el-button class="search-button" type="primary" size="large" :loading="loading" @click="loadAvailability">
          查询空房
        </el-button>
      </div>

      <div class="presets">
        <span class="preset-label">快捷选择</span>
        <button v-for="preset in presets" :key="preset.label" type="button" @click="applyPreset(preset)">
          {{ preset.label }}
        </button>
      </div>
    </div>
  </div>

  <div class="page-shell result-section">
    <div class="toolbar">
      <div>
        <h2 class="section-title">可订房型</h2>
        <p class="page-subtitle">{{ checkIn }} 至 {{ checkOut }}，共 {{ nights }} 晚</p>
      </div>
      <div class="result-meta">
        <span>{{ availability.length }} 种房型</span>
        <span class="dot"></span>
        <span>价格含税及服务费</span>
      </div>
    </div>

    <div v-if="loading" class="room-grid">
      <div v-for="index in 4" :key="index" class="room-card skeleton-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="image" class="skeleton-image" />
            <div class="skeleton-content">
              <el-skeleton-item variant="h3" style="width: 42%" />
              <el-skeleton-item variant="text" style="width: 88%" />
              <el-skeleton-item variant="text" style="width: 70%" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <div v-else class="room-grid">
      <article v-for="room in availability" :key="room.roomTypeId" class="room-card" :class="{ soldout: room.minAvailable < 1 }">
        <div class="room-image-wrap">
          <img :src="room.coverImage || '/images/rooms/std.jpg'" :alt="room.name" class="room-image" />
          <div class="image-shade"></div>
          <span class="room-code">{{ room.code }}</span>
          <div class="rating-pill">
            <strong>{{ Number(room.averageScore || 0).toFixed(1) }}</strong>
            <el-rate :model-value="Number(room.averageScore || 0)" disabled allow-half />
            <span>{{ room.reviewCount || 0 }} 条评价</span>
          </div>
        </div>

        <div class="room-content">
          <div class="room-heading">
            <div>
              <h3>{{ room.name }}</h3>
              <p>{{ room.description || '舒适客房，配备基础生活设施。' }}</p>
            </div>
            <el-tag v-if="room.minAvailable > 0" type="success" effect="light">可订</el-tag>
            <el-tag v-else type="info" effect="light">已满房</el-tag>
          </div>

          <div class="room-facts">
            <span>{{ room.bedType }}</span>
            <span>{{ room.area }} m²</span>
            <span>至多 {{ room.maxOccupancy }} 人</span>
          </div>

          <div class="amenities">
            <span v-for="item in room.amenities" :key="item">{{ item }}</span>
          </div>

          <div class="room-bottom">
            <button type="button" class="review-link" @click="openReviews(room)">
              <el-rate :model-value="Number(room.averageScore || 0)" disabled allow-half />
              <span>查看客户评论</span>
            </button>
            <div class="price-block">
              <div><strong>{{ formatMoney(room.basePrice) }}</strong><span>/ 晚</span></div>
              <small>总价 {{ formatMoney(room.totalPrice) }}</small>
            </div>
            <el-button type="primary" size="large" :disabled="room.minAvailable < 1" @click="book(room)">
              {{ room.minAvailable > 0 ? '立即预订' : '暂无空房' }}
            </el-button>
          </div>

          <div class="inventory-note">
            所选日期最少剩余 <strong>{{ room.minAvailable }}</strong> 间
          </div>
        </div>
      </article>
    </div>

    <div v-if="!loading && availability.length === 0" class="empty-state panel">当前日期没有可展示的房型</div>
  </div>

  <el-dialog v-model="reviewVisible" width="680px" class="review-dialog" destroy-on-close>
    <template #header>
      <div class="dialog-heading">
        <div>
          <span>客户评价</span>
          <h3>{{ reviewRoom?.name }}</h3>
        </div>
        <div class="dialog-rating">
          <strong>{{ Number(reviewRoom?.averageScore || 0).toFixed(1) }}</strong>
          <small>{{ reviewRoom?.reviewCount || 0 }} 条历史入住评价</small>
        </div>
      </div>
    </template>

    <div v-loading="reviewLoading" class="review-list">
      <article v-for="review in reviews" :key="review.id" class="review-item">
        <div class="review-avatar">{{ (review.userName || '客').slice(0, 1) }}</div>
        <div class="review-main">
          <div class="review-top">
            <strong>{{ review.userName || '匿名客户' }}</strong>
            <span>{{ formatReviewDate(review.createdAt) }}</span>
          </div>
          <el-rate :model-value="review.score" disabled />
          <p>{{ review.content }}</p>
        </div>
      </article>
      <el-empty v-if="!reviewLoading && reviews.length === 0" description="暂时还没有客户评价" />
    </div>
  </el-dialog>
</template>

<style scoped>
.rooms-hero {
  position: relative;
  padding: 66px 0 72px;
  color: white;
  overflow: hidden;
  background:
    linear-gradient(105deg, rgba(5, 43, 57, 0.97), rgba(16, 92, 111, 0.88)),
    #0d4f65;
}

.rooms-hero::before {
  content: '';
  position: absolute;
  width: 520px;
  height: 520px;
  right: -180px;
  top: -280px;
  border: 80px solid rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.hero-inner { position: relative; z-index: 1; padding-top: 0; padding-bottom: 0; }
.hero-copy { max-width: 720px; }
.hero-kicker { font-size: 12px; letter-spacing: 0.22em; opacity: 0.72; }
.hero-copy h1 { margin: 15px 0 13px; font-size: clamp(38px, 5vw, 62px); line-height: 1.06; letter-spacing: -0.045em; }
.hero-copy p { margin: 0; color: rgba(255, 255, 255, 0.76); font-size: 17px; }

.search-panel {
  margin-top: 35px;
  padding: 15px;
  display: grid;
  grid-template-columns: minmax(190px, 1fr) auto minmax(190px, 1fr) auto;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: 0 22px 50px rgba(0, 0, 0, 0.14);
  backdrop-filter: blur(16px);
}

.date-field { padding: 7px 13px; border-radius: 13px; background: rgba(255, 255, 255, 0.94); }
.date-field > span { display: block; margin-bottom: 4px; color: #667085; font-size: 11px; font-weight: 700; letter-spacing: 0.08em; }
.date-field :deep(.el-date-editor) { width: 100%; }
.date-field :deep(.el-input__wrapper) { padding: 0; box-shadow: none; }
.date-field :deep(.el-input__inner) { color: #172033; font-size: 16px; font-weight: 700; }
.stay-badge { min-width: 58px; height: 58px; display: grid; place-items: center; align-content: center; border: 1px solid rgba(255, 255, 255, 0.16); border-radius: 50%; background: rgba(255, 255, 255, 0.1); }
.stay-badge strong { font-size: 20px; line-height: 1; }
.stay-badge small { margin-top: 3px; font-size: 11px; color: rgba(255, 255, 255, 0.7); }
.search-button { height: 58px; padding: 0 30px; border: 0; border-radius: 14px; background: linear-gradient(135deg, #ffb454, #f59e0b); font-weight: 800; }
.presets { margin-top: 15px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.preset-label { margin-right: 5px; color: rgba(255, 255, 255, 0.58); font-size: 12px; }
.presets button { padding: 7px 12px; color: rgba(255, 255, 255, 0.84); border: 1px solid rgba(255, 255, 255, 0.18); border-radius: 999px; background: rgba(255, 255, 255, 0.08); cursor: pointer; transition: 0.2s ease; }
.presets button:hover { color: white; border-color: rgba(255, 255, 255, 0.45); background: rgba(255, 255, 255, 0.16); }

.result-section { padding-top: 34px; }
.result-meta { display: flex; align-items: center; gap: 9px; color: var(--muted); font-size: 13px; }
.dot { width: 4px; height: 4px; border-radius: 50%; background: #98a2b3; }
.room-grid { display: grid; gap: 22px; }
.room-card { display: grid; grid-template-columns: minmax(340px, 0.85fr) minmax(0, 1.15fr); overflow: hidden; border: 1px solid #e5e7eb; border-radius: 22px; background: white; box-shadow: 0 18px 45px rgba(31, 41, 55, 0.07); transition: transform 0.25s ease, box-shadow 0.25s ease; }
.room-card:hover { transform: translateY(-3px); box-shadow: 0 25px 60px rgba(31, 41, 55, 0.12); }
.room-image-wrap { position: relative; min-height: 330px; overflow: hidden; }
.room-image { width: 100%; height: 100%; display: block; object-fit: cover; transition: transform 0.6s ease; }
.room-card:hover .room-image { transform: scale(1.035); }
.image-shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.48)); }
.room-code { position: absolute; top: 18px; left: 18px; padding: 7px 11px; border: 1px solid rgba(255, 255, 255, 0.25); border-radius: 9px; color: white; font-size: 12px; font-weight: 800; letter-spacing: 0.13em; background: rgba(5, 43, 57, 0.5); backdrop-filter: blur(10px); }
.rating-pill { position: absolute; left: 18px; bottom: 18px; padding: 10px 12px; display: grid; grid-template-columns: auto auto; align-items: center; gap: 1px 8px; color: white; border-radius: 13px; background: rgba(0, 0, 0, 0.48); backdrop-filter: blur(12px); }
.rating-pill strong { font-size: 24px; line-height: 1; }
.rating-pill :deep(.el-rate) { height: auto; }
.rating-pill :deep(.el-rate__icon) { margin-right: 1px; font-size: 14px; }
.rating-pill span { grid-column: 1 / -1; color: rgba(255, 255, 255, 0.72); font-size: 11px; }
.room-content { padding: 25px 27px 22px; display: flex; flex-direction: column; }
.room-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.room-heading h3 { margin: 0; font-size: 27px; letter-spacing: -0.02em; }
.room-heading p { max-width: 620px; margin: 9px 0 0; color: var(--muted); line-height: 1.65; }
.room-facts { margin-top: 15px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; color: #475467; font-size: 13px; }
.room-facts span { padding-right: 10px; border-right: 1px solid #d0d5dd; }
.room-facts span:last-child { border-right: 0; }
.amenities { margin: 17px 0 21px; display: flex; flex-wrap: wrap; gap: 7px; }
.amenities span { padding: 5px 9px; color: #475467; border: 1px solid #e4e7ec; border-radius: 7px; background: #f9fafb; font-size: 12px; }
.room-bottom { margin-top: auto; padding-top: 18px; display: grid; grid-template-columns: 1fr auto auto; align-items: end; gap: 20px; border-top: 1px solid var(--line); }
.review-link { padding: 0; display: flex; flex-direction: column; align-items: flex-start; gap: 4px; color: var(--brand); border: 0; background: transparent; cursor: pointer; }
.review-link :deep(.el-rate) { height: auto; }
.review-link :deep(.el-rate__icon) { margin-right: 0; font-size: 16px; }
.review-link span { font-size: 12px; }
.price-block { text-align: right; }
.price-block strong { color: #c2410c; font-size: 27px; }
.price-block span { margin-left: 3px; color: var(--muted); font-size: 12px; }
.price-block small { display: block; margin-top: 2px; color: var(--muted); }
.inventory-note { margin-top: 13px; color: #667085; font-size: 12px; text-align: right; }
.inventory-note strong { color: #175cd3; }
.soldout { opacity: 0.68; }
.skeleton-card { min-height: 330px; display: block; }
.skeleton-image { width: 100%; height: 330px; border-radius: 22px 0 0 22px; }
.skeleton-content { padding: 25px; display: grid; gap: 18px; }

.dialog-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; }
.dialog-heading span { color: var(--brand); font-size: 12px; font-weight: 800; letter-spacing: 0.14em; }
.dialog-heading h3 { margin: 6px 0 0; font-size: 25px; }
.dialog-rating { text-align: right; }
.dialog-rating strong { display: block; color: #d97706; font-size: 32px; line-height: 1; }
.dialog-rating small { color: var(--muted); }
.review-list { min-height: 180px; display: grid; gap: 12px; }
.review-item { padding: 16px; display: grid; grid-template-columns: 44px 1fr; gap: 13px; border: 1px solid #eaecf0; border-radius: 14px; background: #fcfcfd; }
.review-avatar { width: 44px; height: 44px; display: grid; place-items: center; color: white; border-radius: 50%; background: linear-gradient(135deg, #176b87, #27a5a7); font-weight: 800; }
.review-main { min-width: 0; }
.review-top { display: flex; justify-content: space-between; gap: 12px; }
.review-top span { color: var(--muted); font-size: 12px; }
.review-main :deep(.el-rate) { margin: 5px 0 7px; height: auto; }
.review-main :deep(.el-rate__icon) { margin-right: 1px; font-size: 15px; }
.review-main p { margin: 0; color: #475467; line-height: 1.7; }

@media (max-width: 980px) {
  .search-panel { grid-template-columns: 1fr 1fr; }
  .stay-badge { display: none; }
  .search-button { grid-column: 1 / -1; }
  .room-card { grid-template-columns: 1fr; }
  .room-image-wrap { min-height: 260px; }
}

@media (max-width: 640px) {
  .rooms-hero { padding: 48px 0 55px; }
  .search-panel { grid-template-columns: 1fr; padding: 12px; }
  .search-button { grid-column: auto; }
  .room-content { padding: 21px 18px 19px; }
  .room-heading h3 { font-size: 23px; }
  .room-bottom { grid-template-columns: 1fr; align-items: stretch; }
  .price-block { text-align: left; }
  .inventory-note { text-align: left; }
}
</style>
