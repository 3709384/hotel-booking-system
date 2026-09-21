<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ phone: '13900000001', password: '123456' })

const demoAccounts = [
  { label: '会员演示', phone: '13900000001', password: '123456', role: '查询、下单、支付' },
  { label: '前台演示', phone: '13700000001', password: 'front123', role: '排房、入住、退房' },
  { label: '管理员演示', phone: '13800000000', password: 'admin123', role: '资源与库存维护' },
]

function fillAccount(account) {
  form.phone = account.phone
  form.password = account.password
}

async function submit() {
  if (!form.phone || !form.password) {
    ElMessage.warning('请输入手机号和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.phone, form.password)
    const target = typeof route.query.redirect === 'string' ? route.query.redirect : auth.defaultRoute
    await router.replace(target)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="intro-panel">
      <div class="intro-content">
        <span class="eyebrow">HOTEL BOOKING SYSTEM</span>
        <h1>让客房、订单与前台接待保持同一份事实。</h1>
        <p>从在线查询、预订支付，到前台排房入住与退房，完整演示酒店客房业务闭环。</p>
        <div class="feature-list">
          <div><strong>按夜库存</strong><span>每个日期独立管理可售数量</span></div>
          <div><strong>事务防超卖</strong><span>条件更新保证并发一致性</span></div>
          <div><strong>角色权限</strong><span>会员、前台、管理员各司其职</span></div>
        </div>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-box">
        <div class="brand-row">
          <span class="brand-mark">云</span>
          <div><h2>云栖酒店</h2><p>客房预订与接待系统</p></div>
        </div>

        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" size="large" placeholder="请输入登录手机号" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="submit">登录系统</el-button>
        </el-form>

        <div class="demo-title">点击填入演示账号</div>
        <div class="demo-accounts">
          <button v-for="account in demoAccounts" :key="account.phone" type="button" @click="fillAccount(account)">
            <strong>{{ account.label }}</strong>
            <span>{{ account.role }}</span>
            <small>{{ account.phone }} / {{ account.password }}</small>
          </button>
        </div>
        <div class="register-link">还没有账号？<router-link to="/register">立即注册</router-link></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(420px, 0.85fr); }
.intro-panel { position: relative; display: grid; align-items: center; padding: 8vw; color: white; overflow: hidden; background: linear-gradient(145deg, rgba(6, 48, 63, 0.95), rgba(23, 107, 135, 0.88)), radial-gradient(circle at 70% 20%, #45c2b0, transparent 35%), #0d4f65; }
.intro-panel::after { content: ''; position: absolute; right: -90px; bottom: -130px; width: 420px; height: 420px; border: 70px solid rgba(255, 255, 255, 0.08); border-radius: 50%; }
.intro-content { position: relative; z-index: 1; max-width: 670px; }
.eyebrow { font-size: 12px; letter-spacing: 0.24em; opacity: 0.78; }
.intro-content h1 { margin: 24px 0 20px; font-size: clamp(38px, 5vw, 68px); line-height: 1.08; letter-spacing: -0.04em; }
.intro-content > p { max-width: 560px; color: rgba(255, 255, 255, 0.78); font-size: 18px; line-height: 1.8; }
.feature-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-top: 50px; }
.feature-list div { padding: 18px; border: 1px solid rgba(255, 255, 255, 0.16); border-radius: 16px; background: rgba(255, 255, 255, 0.08); backdrop-filter: blur(10px); }
.feature-list strong, .feature-list span { display: block; }
.feature-list span { margin-top: 8px; color: rgba(255, 255, 255, 0.68); font-size: 13px; }
.login-panel { display: grid; place-items: center; padding: 40px; background: rgba(255, 255, 255, 0.9); }
.login-box { width: min(420px, 100%); }
.brand-row { display: flex; align-items: center; gap: 14px; margin-bottom: 34px; }
.brand-mark { width: 52px; height: 52px; display: grid; place-items: center; border-radius: 16px; color: white; font-size: 26px; font-weight: 800; background: linear-gradient(135deg, #176b87, #27a5a7); }
.brand-row h2, .brand-row p { margin: 0; }
.brand-row p { margin-top: 5px; color: var(--muted); }
.login-button { width: 100%; margin-top: 4px; }
.demo-title { margin: 28px 0 12px; color: var(--muted); font-size: 13px; }
.demo-accounts { display: grid; gap: 9px; }
.demo-accounts button { display: grid; grid-template-columns: 1fr auto; gap: 5px 12px; padding: 12px 14px; text-align: left; border: 1px solid var(--line); border-radius: 12px; color: var(--ink); background: #fafbfc; cursor: pointer; }
.demo-accounts button:hover { border-color: #8cc8d7; background: var(--brand-soft); }
.demo-accounts span, .demo-accounts small { color: var(--muted); font-size: 12px; }
.demo-accounts small { grid-column: 1 / -1; }
.register-link { margin-top: 20px; text-align: center; color: var(--muted); font-size: 14px; }
.register-link a { color: var(--brand); font-weight: 700; }
@media (max-width: 900px) { .login-page { grid-template-columns: 1fr; } .intro-panel { min-height: 360px; padding: 54px 26px; } .feature-list { display: none; } .login-panel { padding: 34px 20px 50px; } }
</style>
