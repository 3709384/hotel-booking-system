<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api/http'
import { formatMoney } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const hotels = ref([])
const availByHotel = ref({})
const checkIn = ref(dateAfter(0))
const checkOut = ref(dateAfter(1))
const keyword = ref('')
const sort = ref('smart')
const tag = ref('ALL')
const pointsOn = ref(false)
const POINTS_YUAN = 26

const chips = [
  { key: 'ALL', label: '全部' },
  { key: '神券商家', label: '神券商家', cls: 'hot' },
  { key: '大床房', label: '大床房' },
  { key: '电竞酒店', label: '电竞酒店' },
  { key: '黄金会员', label: '黄金会员', cls: 'gold' },
  { key: '双床房', label: '双床房' },
  { key: '影音酒店', label: '影音酒店' },
  { key: '免费停车场', label: '免费停车场' },
]

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
  return Math.max(Math.round((new Date(checkOut.value) - new Date(checkIn.value)) / 86400000), 0)
})

function normalizeDates() {
  if (!checkIn.value) return
  if (!checkOut.value || checkOut.value <= checkIn.value) {
    const date = new Date(checkIn.value + 'T00:00:00')
    date.setDate(date.getDate() + 1)
    checkOut.value = toDateString(date)
  }
}

async function loadHotels() {
  loading.value = true
  try {
    const response = await api.get('/hotels', {
      params: {
        keyword: keyword.value || undefined,
        sort: sort.value,
        tag: tag.value === 'ALL' ? undefined : tag.value,
      },
    })
    hotels.value = response.data || []
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
      if (item.hotelId == null) continue
      const key = String(item.hotelId)
      map[key] = (map[key] || 0) + Number(item.minAvailable || 0)
    }
    availByHotel.value = map
  } catch {
    availByHotel.value = {}
  }
}

async function reload() {
  normalizeDates()
  if (nights.value < 1) {
    ElMessage.warning('离店日期必须晚于入住日期')
    return
  }
  await Promise.all([loadHotels(), loadAvailability()])
}

function pickTag(key) {
  tag.value = key
  loadHotels()
}

function shownPrice(hotel) {
  const base = Number(hotel.fromPrice || 0)
  return pointsOn.value ? Math.max(base - POINTS_YUAN, 1) : base
}

function openHotel(hotel) {
  router.push({ name: 'hotel-detail', params: { id: hotel.id }, query: { checkIn: checkIn.value, checkOut: checkOut.value } })
}

onMounted(reload)
</script>

<template>
  <div class="hotels-page">
    <!-- 黄色搜索头（参考美团酒店列表页） -->
    <div class="yellow-head">
      <div class="page-shell head-inner">
        <div class="search-pill">
          <span class="city">郑州</span>
          <div class="date-box">
            <div class="date-col">
              <span>住</span>
              <el-date-picker v-model="checkIn" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="normalizeDates" />
            </div>
            <div class="date-col">
              <span>离</span>
              <el-date-picker v-model="checkOut" type="date" value-format="YYYY-MM-DD" :clearable="false" />
            </div>
          </div>
          <el-input v-model="keyword" class="kw" placeholder="二七广场 / 地铁站 / 酒店名" clearable @keyup.enter="reload" />
          <el-button class="go" @click="reload">搜索</el-button>
        </div>

        <div class="sort-row">
          <el-select v-model="sort" class="sort-select" @change="loadHotels">
            <el-option label="智能排序" value="smart" />
            <el-option label="位置距离 · 近到远" value="distance" />
            <el-option label="价格 · 低到高" value="priceAsc" />
            <el-option label="价格 · 高到低" value="priceDesc" />
            <el-option label="评分 · 高到低" value="score" />
          </el-select>
          <span class="sort-note">{{ checkIn }} 住 · {{ checkOut }} 离 · {{ nights }} 晚 · 共 {{ hotels.length }} 家酒店</span>
        </div>

        <div class="chip-row">
          <button
            v-for="chip in chips"
            :key="chip.key"
            type="button"
            class="fchip"
            :class="{ on: tag === chip.key }"
            @click="pickTag(chip.key)"
          >
            <span :class="chip.cls">{{ chip.label }}</span>
          </button>
        </div>

        <div class="points-bar">
          <span class="coin">🪙</span>
          <span>{{ pointsOn ? `已开启积分抵扣：起价按 2600 积分抵 ¥${POINTS_YUAN} 展示抵后价` : `100积分可抵 ¥1，开启后查看抵后价（演示账号 2600 积分）` }}</span>
          <button type="button" :class="{ off: pointsOn }" @click="pointsOn = !pointsOn">{{ pointsOn ? '关闭抵扣' : '立即抵扣' }}</button>
        </div>
      </div>
    </div>

    <div class="page-shell list-shell">
      <div v-loading="loading" class="hotel-list">
        <article v-for="hotel in hotels" :key="hotel.id" class="hotel" @click="openHotel(hotel)">
          <div class="cover" :class="'g' + ((Number(hotel.id) % 6) + 1)">
            <span class="cover-icon">{{ hotel.icon || '🏨' }}</span>
            <span v-if="hotel.badge" class="bdg">✔ {{ hotel.badge }}</span>
            <span class="play">▶</span>
          </div>
          <div class="h-body">
            <div class="h-name">{{ hotel.name }}<span v-if="hotel.kind" class="kind">{{ hotel.kind }}</span> 👑</div>
            <div class="score-row">
              <span class="score">{{ Number(hotel.score || 0).toFixed(1) }}</span>
              <span class="score-word">{{ hotel.scoreWord }}</span>
              <span v-if="hotel.quote" class="quote">“{{ hotel.quote }}”</span>
            </div>
            <div class="tags">
              <span v-for="t in hotel.tags || []" :key="t" class="tag">{{ t }}</span>
            </div>
            <div class="dist">{{ hotel.distanceText }}</div>
            <div v-if="hotel.streak" class="streak">连续{{ hotel.streak }}位住客好评 · {{ hotel.reviewCount || 0 }} 条点评</div>
            <div v-else class="streak">{{ hotel.reviewCount || 0 }} 条住客点评</div>
            <div class="h-bottom">
              <span class="sales">
                {{ hotel.salesText }}
                <span v-if="availByHotel[String(hotel.id)] != null" class="avail" :class="{ none: availByHotel[String(hotel.id)] < 1 }">
                  · {{ availByHotel[String(hotel.id)] > 0 ? `所选日期可订` : '所选日期订满' }}
                </span>
              </span>
              <span class="price-box">
                <span v-if="hotel.marketPrice && Number(hotel.marketPrice) > Number(hotel.fromPrice)" class="orig">{{ formatMoney(hotel.marketPrice) }}</span>
                <span class="price"><small>¥</small>{{ shownPrice(hotel) }}</span>
                <span class="qi">起</span>
                <span v-if="pointsOn" class="promo">积分抵后价 · 已抵 ¥{{ POINTS_YUAN }}</span>
                <span v-else-if="hotel.promoType" class="promo"><b>{{ hotel.promoType }}</b>{{ hotel.promoText }} ›</span>
              </span>
            </div>
          </div>
        </article>

        <el-empty v-if="!loading && hotels.length === 0" description="没有符合条件的酒店，换个关键词或筛选试试" />
      </div>
      <p class="foot-note">郑州地区演示数据（6 家酒店 / 18 个房型），与 database/seed.sql 一致；库存按每一晚核算，订满的房型不可下单。</p>
    </div>
  </div>
</template>

<style scoped>
.hotels-page { background: #f5f5f5; min-height: 100%; }
.yellow-head { background: linear-gradient(180deg, #ffe45c, #ffd100); padding: 18px 0 16px; }
.head-inner { padding-top: 0; padding-bottom: 0; }
.search-pill { background: #fff; border-radius: 999px; display: flex; align-items: center; gap: 12px; padding: 8px 10px 8px 20px; flex-wrap: wrap; }
.city { font-size: 17px; font-weight: 800; white-space: nowrap; }
.date-box { display: flex; gap: 10px; border-left: 1px solid #eee; border-right: 1px solid #eee; padding: 0 12px; }
.date-col { display: flex; align-items: center; gap: 5px; }
.date-col > span { font-size: 11px; color: #767676; }
.date-col :deep(.el-date-editor) { width: 128px; }
.date-col :deep(.el-input__wrapper) { box-shadow: none; padding: 0; }
.date-col :deep(.el-input__inner) { color: #f85a00; font-weight: 800; font-size: 13px; }
.date-col :deep(.el-input__prefix) { display: none; }
.kw { flex: 1; min-width: 150px; }
.kw :deep(.el-input__wrapper) { box-shadow: none; padding: 0; }
.go { border: 0; background: #ffd100; color: #191919; font-weight: 800; border-radius: 999px; padding: 0 22px; height: 38px; }
.sort-row { display: flex; align-items: center; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.sort-select { width: 190px; }
.sort-select :deep(.el-select__wrapper) { background: transparent; box-shadow: none; font-weight: 800; font-size: 16px; }
.sort-note { margin-left: auto; font-size: 12px; color: #6b5600; }
.chip-row { display: flex; gap: 8px; overflow-x: auto; padding: 10px 0 2px; scrollbar-width: none; }
.chip-row::-webkit-scrollbar { display: none; }
.fchip { flex: none; border: 0; background: rgba(255, 255, 255, 0.75); color: #333; font-size: 14px; padding: 8px 14px; border-radius: 8px; cursor: pointer; }
.fchip.on { background: #fff; font-weight: 800; box-shadow: inset 0 0 0 2px #191919; }
.fchip .hot { color: #f5316e; font-weight: 800; }
.fchip .gold { color: #b07d2b; font-weight: 800; }
.points-bar { margin-top: 12px; background: #fff6f3; border-radius: 12px; padding: 11px 14px; display: flex; align-items: center; gap: 8px; font-size: 14px; flex-wrap: wrap; }
.points-bar button { margin-left: auto; border: 0; background: #ff4d2e; color: #fff; font-weight: 800; font-size: 14px; padding: 7px 16px; border-radius: 999px; cursor: pointer; }
.points-bar button.off { background: #191919; }
.list-shell { padding-top: 6px; }
.hotel-list { background: #fff; border-radius: 18px; padding: 4px 20px 10px; min-height: 300px; }
.hotel { display: flex; gap: 14px; padding: 18px 0; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.hotel:last-child { border-bottom: 0; }
.cover { flex: none; width: 150px; height: 176px; border-radius: 12px; position: relative; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.cover-icon { font-size: 46px; }
.g1 { background: linear-gradient(150deg, #8a6d3b, #d9a441); }
.g2 { background: linear-gradient(150deg, #274156, #4e7ca6); }
.g3 { background: linear-gradient(150deg, #5b4a8a, #9a7bd0); }
.g4 { background: linear-gradient(150deg, #1f5f5b, #3fa796); }
.g5 { background: linear-gradient(150deg, #8a2be2, #4a2b8a); }
.g6 { background: linear-gradient(150deg, #37474f, #78909c); }
.bdg { position: absolute; top: 0; left: 0; background: linear-gradient(90deg, #3a2e12, #6b5518); color: #ffe9a8; font-size: 12px; font-weight: 700; padding: 4px 10px; border-radius: 0 0 10px 0; }
.play { position: absolute; right: 8px; bottom: 8px; width: 24px; height: 24px; border-radius: 50%; background: rgba(0, 0, 0, 0.45); color: #fff; font-size: 11px; display: flex; align-items: center; justify-content: center; }
.h-body { flex: 1; min-width: 0; }
.h-name { font-size: 19px; font-weight: 900; line-height: 1.35; color: #191919; }
.kind { display: inline-block; background: #f5f5f5; color: #767676; font-size: 12px; border-radius: 4px; padding: 2px 6px; margin-left: 5px; font-weight: 600; vertical-align: 2px; }
.score-row { display: flex; align-items: center; gap: 7px; margin-top: 7px; flex-wrap: wrap; }
.score { background: #ffd100; color: #191919; font-weight: 900; font-size: 15px; padding: 2px 8px; border-radius: 6px 6px 6px 0; }
.score-word { font-weight: 900; font-size: 15px; }
.quote { color: #767676; font-size: 14px; }
.tags { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
.tag { border: 1px solid #e3e3e3; color: #333; font-size: 12px; padding: 3px 8px; border-radius: 5px; }
.dist { color: #555; font-size: 14px; margin-top: 8px; }
.streak { color: #999; font-size: 13px; margin-top: 5px; }
.h-bottom { display: flex; align-items: flex-end; margin-top: 6px; gap: 10px; }
.sales { color: #999; font-size: 13px; }
.avail { color: #2e7d32; font-weight: 700; }
.avail.none { color: #c62828; }
.price-box { margin-left: auto; text-align: right; }
.orig { color: #999; font-size: 13px; text-decoration: line-through; margin-right: 6px; }
.price { color: #fa3e3e; font-weight: 900; font-size: 26px; }
.price small { font-size: 14px; }
.qi { color: #191919; font-size: 14px; font-weight: 700; }
.promo { display: inline-block; margin-top: 4px; background: #fff1f1; color: #e93d3d; font-size: 12px; font-weight: 700; padding: 3px 8px; border-radius: 5px; }
.promo b { border-right: 1px solid #f5c6c6; padding-right: 6px; margin-right: 6px; }
.foot-note { color: #999; font-size: 13px; margin-top: 14px; }
@media (max-width: 640px) {
  .cover { width: 112px; height: 150px; }
  .cover-icon { font-size: 36px; }
  .h-name { font-size: 16px; }
  .price { font-size: 22px; }
  .search-pill { border-radius: 18px; }
  .date-box { border: 0; padding: 0; }
}
</style>
