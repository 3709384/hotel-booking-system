<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems = computed(() => {
  const items = [
    { label: '酒店预订', path: '/hotels' },
    { label: '全部房型', path: '/rooms' },
  ]
  if (auth.role === 'MEMBER') {
    items.push({ label: '我的订单', path: '/orders' })
  }
  if (['RECEPTIONIST', 'ADMIN'].includes(auth.role)) {
    items.push({ label: '前台接待', path: '/pms' })
  }
  if (auth.role === 'ADMIN') {
    items.push({ label: '后台管理', path: '/admin' })
  }
  return items
})

onMounted(() => {
  if (auth.isLoggedIn) {
    auth.refreshProfile().catch(() => auth.logout())
  }
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-layout">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/hotels" class="brand">
          <span class="brand-mark">住</span>
          <span>
            <strong>云栖住吧</strong>
            <small>郑州酒店预订平台</small>
          </span>
        </router-link>

        <nav class="main-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            :class="{ active: route.path.startsWith(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="user-area">
          <span class="user-name">{{ auth.user?.name || auth.user?.phone }}</span>
          <el-tag size="small" effect="plain">{{ auth.roleLabel }}</el-tag>
          <el-button link type="primary" @click="logout">退出</el-button>
        </div>
      </div>
    </header>

    <main>
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(14px);
}

.topbar-inner {
  width: min(1180px, calc(100% - 32px));
  min-height: 72px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 210px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: #191919;
  font-size: 22px;
  font-weight: 800;
  border-radius: 13px;
  background: linear-gradient(135deg, #ffe45c, #ffd100);
  box-shadow: 0 8px 18px rgba(255, 209, 0, 0.35);
}

.brand strong,
.brand small {
  display: block;
}

.brand small {
  margin-top: 2px;
  color: var(--muted);
  font-size: 11px;
}

.main-nav {
  display: flex;
  gap: 8px;
  flex: 1;
}

.main-nav a {
  padding: 9px 13px;
  border-radius: 10px;
  color: #4b5563;
  font-size: 14px;
}

.main-nav a:hover,
.main-nav a.active {
  color: var(--brand-dark);
  background: var(--brand-soft);
}

.user-area {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 14px;
}

@media (max-width: 800px) {
  .topbar-inner {
    min-height: auto;
    padding: 12px 0;
    flex-wrap: wrap;
    gap: 10px;
  }

  .brand {
    min-width: 0;
    flex: 1;
  }

  .brand small,
  .user-name {
    display: none;
  }

  .main-nav {
    order: 3;
    width: 100%;
    overflow-x: auto;
  }

  .main-nav a {
    white-space: nowrap;
  }
}
</style>
