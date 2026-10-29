<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api/http'
import { formatMoney } from '../utils/format'

const route = useRoute()
const router = useRouter()
const hotel = ref(null)
const loading = ref(false)
const availByType = ref({})
const checkIn = ref(String(route.query.checkIn || dateAfter(0)))
const checkOut = ref(String(route.query.checkOut || dateAfter(1)))
const reviewTypeId = ref(null)
const reviews = ref([])
const reviewLoading = ref(false)

function dateAfter(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return date.getFullYear() + '-' + month + '-' + day
}

const nights = computed(() => {
  if (!checkIn.value || !checkOut.value) return 0
  return Math.max(Math.round((new Date(checkOut.value) - new Date(checkIn.value)) / 86400000), 0)
})

function normalizeDates() {
  if (!checkIn.value) return
  if (!checkOut.value || checkOut.value <= checkIn.value) {
    const date = new Date(checkIn.value + 'T00:00:00')
    date.setDate(date.getDate() + 1)
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    checkOut.value = date.getFullYear() + '-' + month + '-' + day
  }
}

async function loadHotel() {
  loading.value = true
  try {
    const response = await api.get('/hotels/' + route.params.id)
    hotel.value = response.data
    if (hotel.value?.roomTypes?.length && !reviewTypeId.value) {
      reviewTypeId.value = hotel.value.roomTypes[0].id
      loadReviews()
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function loadAvailability() {
  normalizeDates()
  try {
    const response = await api.get('/availability', {
      params: { checkIn: checkIn.value, checkOut: checkOut.value },
    })
    const map = {}
    for (const item of response.data || []) {
      if (String(item.hotelId) === String(route.params.id)) {
        map[String(item.roomTypeId)] = item
      }
    }
    availByType.value = map
  } catch {
    availByType.value = {}
  }
}

async function loadReviews() {
  if (!reviewTypeId.value) return
  reviewLoading.value = true
  reviews.value = []
  try {
    const response = await api.get('/room-types/' + reviewTypeId.value + '/reviews', {
      params: { page: 1, size: 20 },
    })
    reviews.value = response.data.records || []
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    reviewLoading.value = false
  }
}

function pickReviewType(id) {
  reviewTypeId.value = id
  loadReviews()
}

function book(type) {
  const avail = availByType.value[String(type.id)]
  if (avail && avail.minAvailable < 1) {
    ElMessage.warning('所选日期该房型已订满，换个日期试试')
    return
  }
  router.push({
    name: 'booking',
    params: { typeId: type.id },
    query: { checkIn: checkIn.value, checkOut: checkOut.value },
  })
}

function formatReviewDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 10) : ''
}

onMounted(async () => {
  await loadHotel()
  await loadAvailability()
})
</script>

<template>
  <div class="page-shell detail-shell" v-loading="loading">
    <el-button class="back" @click="router.push({ name: 'hotels' })">← 返回酒店列表</el-button>

    <template v-if="hotel">
      <div class="cover" :class="'g' + ((Number(hotel.id) % 6) + 1)">
        <span class="cover-icon">{{ hotel.icon || '🏨' }}</span>
        <div class="cover-info">
          <h1>{{ hotel.name }}</h1>
          <div class="score-row">
            <span class="score">{{ Number(hotel.score || 0).toFixed(1) }}</span>
            <span class="score-word">{{ hotel.scoreWord }}</span>
            <span>{{ hotel.reviewCount || 0 }} 条住客点评</span>
            <span v-if="hotel.kind" class="kind">{{ hotel.kind }}</span>
          </div>
          <div class="tags">
            <span v-for="t in hotel.tags || []" :key="t" class="tag">{{ t }}</span>
            <span v-if="hotel.promoType" class="promo"><b>{{ hotel.promoType }}</b>{{ hotel.promoText }}</span>
          </div>
          <p class="dist">{{ hotel.distanceText }}</p>
        </div>
      </div>

      <section class="panel rooms-panel">
        <div class="rooms-head">
          <h2 class="section-title">选择房型</h2>
          <div class="date-bar">
            <el-date-picker v-model="checkIn" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="normalizeDates" />
            <span>至</span>
            <el-date-picker v-model="checkOut" type="date" value-format="YYYY-MM-DD" :clearable="false" />
            <span class="muted">共 {{ nights }} 晚</span>
            <el-button type="primary" @click="loadAvailability">刷新库存</el-button>
          </div>
        </div>

        <article v-for="type in hotel.roomTypes || []" :key="type.id" class="room-row">
          <div class="room-main">
            <h3>{{ type.name }}</h3>
            <p class="muted">{{ type.description }}</p>
            <div class="facts">
              <span>{{ type.bedType }}</span><span>{{ type.area }}㎡</span><span>可住 {{ type.maxOccupancy }} 人</span>
              <span class="rate">
                <el-rate :model-value="Number(type.averageScore || 0)" disabled allow-half />
                {{ Number(type.averageScore || 0).toFixed(1) }} · {{ type.reviewCount || 0 }} 条
              </span>
            </div>
            <div class="amenities">
              <span v-for="a in type.amenities || []" :key="a">{{ a }}</span>
            </div>
          </div>
          <div class="room-side">
            <span
              class="avail"
              :class="{ none: availByType[String(type.id)] && availByType[String(type.id)].minAvailable < 1 }"
            >
              {{ availByType[String(type.id)] ? (availByType[String(type.id)].minAvailable > 0 ? `所选日期剩 ${availByType[String(type.id)].minAvailable} 间` : '所选日期已订满') : '库存查询中…' }}
            </span>
            <div class="price-line">
              <span v-if="type.marketPrice && Number(type.marketPrice) > Number(type.basePrice)" class="orig">{{ formatMoney(type.marketPrice) }}</span>
              <span class="price">{{ formatMoney(type.basePrice) }}</span><span class="qi">/ 晚</span>
            </div>
            <el-button
              type="primary"
              size="large"
              class="book-btn"
              :disabled="availByType[String(type.id)] && availByType[String(type.id)].minAvailable < 1"
              @click="book(type)"
            >
              预订
            </el-button>
          </div>
        </article>
      </section>

      <section class="panel reviews-panel">
        <h2 class="section-title">住客点评</h2>
        <div class="type-chips">
          <button
            v-for="type in hotel.roomTypes || []"
            :key="type.id"
            type="button"
            :class="{ on: reviewTypeId === type.id }"
            @click="pickReviewType(type.id)"
          >
            {{ type.name }}
          </button>
        </div>
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
          <el-empty v-if="!reviewLoading && reviews.length === 0" description="这个房型暂时还没有点评" />
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-shell { padding-top: 20px; }
.back { margin-bottom: 14px; }
.cover { border-radius: 18px; padding: 26px; color: #fff; display: flex; gap: 20px; align-items: center; }
.g1 { background: linear-gradient(150deg, #8a6d3b, #d9a441); }
.g2 { background: linear-gradient(150deg, #274156, #4e7ca6); }
.g3 { background: linear-gradient(150deg, #5b4a8a, #9a7bd0); }
.g4 { background: linear-gradient(150deg, #1f5f5b, #3fa796); }
.g5 { background: linear-gradient(150deg, #8a2be2, #4a2b8a); }
.g6 { background: linear-gradient(150deg, #37474f, #78909c); }
.cover-icon { font-size: 56px; }
.cover-info h1 { margin: 0 0 10px; font-size: 26px; }
.score-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.score { background: #ffd100; color: #191919; font-weight: 900; padding: 2px 8px; border-radius: 6px 6px 6px 0; }
.score-word { font-weight: 900; }
.kind { background: rgba(255, 255, 255, 0.25); padding: 2px 8px; border-radius: 5px; font-size: 12px; }
.tags { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 10px; }
.tag { border: 1px solid rgba(255, 255, 255, 0.6); font-size: 12px; padding: 3px 8px; border-radius: 5px; }
.promo { background: #fff1f1; color: #e93d3d; font-size: 12px; font-weight: 700; padding: 3px 8px; border-radius: 5px; }
.promo b { border-right: 1px solid #f5c6c6; padding-right: 6px; margin-right: 6px; }
.dist { margin: 10px 0 0; opacity: 0.9; font-size: 14px; }
.rooms-panel, .reviews-panel { padding: 24px; margin-top: 20px; }
.rooms-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; flex-wrap: wrap; }
.date-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.date-bar :deep(.el-date-editor) { width: 150px; }
.room-row { display: flex; gap: 18px; padding: 20px 0; border-bottom: 1px solid var(--line); }
.room-row:last-child { border-bottom: 0; }
.room-main { flex: 1; min-width: 0; }
.room-main h3 { margin: 0 0 6px; font-size: 19px; }
.room-main p { margin: 0; line-height: 1.6; }
.facts { margin-top: 10px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; color: #475467; font-size: 13px; }
.facts span { padding-right: 10px; border-right: 1px solid #d0d5dd; }
.facts span:last-child { border-right: 0; }
.facts .rate { display: inline-flex; align-items: center; gap: 4px; }
.facts :deep(.el-rate) { height: auto; }
.facts :deep(.el-rate__icon) { margin-right: 0; font-size: 14px; }
.amenities { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 7px; }
.amenities span { padding: 5px 9px; color: #475467; border: 1px solid #e4e7ec; border-radius: 7px; background: #f9fafb; font-size: 12px; }
.room-side { flex: none; width: 210px; text-align: right; display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.avail { color: #2e7d32; font-size: 13px; font-weight: 700; }
.avail.none { color: #c62828; }
.orig { color: #999; font-size: 13px; text-decoration: line-through; margin-right: 6px; }
.price { color: #fa3e3e; font-weight: 900; font-size: 25px; }
.book-btn { background: #ffd100; border-color: #ffd100; color: #191919; font-weight: 800; padding: 0 30px; }
.book-btn:hover { background: #ffdf4d; border-color: #ffdf4d; color: #191919; }
.type-chips { display: flex; gap: 8px; flex-wrap: wrap; margin: 14px 0 16px; }
.type-chips button { border: 1px solid var(--line); background: #f9fafb; border-radius: 999px; padding: 7px 14px; cursor: pointer; font-size: 13px; }
.type-chips button.on { background: #ffd100; border-color: #ffd100; font-weight: 800; }
.review-list { display: grid; gap: 12px; min-height: 120px; }
.review-item { padding: 16px; display: grid; grid-template-columns: 44px 1fr; gap: 13px; border: 1px solid #eaecf0; border-radius: 14px; background: #fcfcfd; }
.review-avatar { width: 44px; height: 44px; display: grid; place-items: center; color: #191919; border-radius: 50%; background: linear-gradient(135deg, #ffd100, #f2b705); font-weight: 800; }
.review-main { min-width: 0; }
.review-top { display: flex; justify-content: space-between; gap: 12px; }
.review-top span { color: var(--muted); font-size: 12px; }
.review-main :deep(.el-rate) { margin: 5px 0 7px; height: auto; }
.review-main :deep(.el-rate__icon) { margin-right: 1px; font-size: 15px; }
.review-main p { margin: 0; color: #475467; line-height: 1.7; }
@media (max-width: 720px) {
  .room-row { flex-direction: column; }
  .room-side { width: auto; align-items: flex-start; text-align: left; }
  .cover { flex-direction: column; align-items: flex-start; }
}
</style>
