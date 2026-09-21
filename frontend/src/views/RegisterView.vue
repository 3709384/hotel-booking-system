<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ name: '', phone: '', password: '', confirmPassword: '' })

async function submit() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (form.password.length < 6 || form.password.length > 32) {
    ElMessage.warning('密码长度必须为 6 到 32 位')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await api.post('/auth/register', { phone: form.phone, password: form.password, name: form.name })
    await auth.login(form.phone, form.password)
    ElMessage.success('注册成功')
    await router.replace('/rooms')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <section class="register-card">
      <div class="register-brand"><span>云</span><div><h1>创建会员账号</h1><p>注册后即可查询房型并在线预订</p></div></div>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="姓名或昵称"><el-input v-model="form.name" size="large" placeholder="请输入姓名或昵称" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" size="large" maxlength="11" placeholder="请输入 11 位手机号" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" size="large" type="password" show-password placeholder="6 到 32 位密码" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="form.confirmPassword" size="large" type="password" show-password placeholder="再次输入密码" @keyup.enter="submit" /></el-form-item>
        <el-button type="primary" size="large" class="submit-button" :loading="loading" @click="submit">注册并登录</el-button>
      </el-form>
      <div class="back-link">已有账号？<router-link to="/login">返回登录</router-link></div>
    </section>
  </div>
</template>

<style scoped>
.register-page { min-height: 100vh; display: grid; place-items: center; padding: 32px 20px; background: linear-gradient(135deg, rgba(8, 61, 78, 0.97), rgba(39, 165, 167, 0.72)), #0d4f65; }
.register-card { width: min(460px, 100%); padding: 34px; border-radius: 22px; background: white; box-shadow: 0 24px 70px rgba(0, 0, 0, 0.22); }
.register-brand { margin-bottom: 28px; display: flex; align-items: center; gap: 14px; }
.register-brand > span { width: 50px; height: 50px; display: grid; place-items: center; border-radius: 15px; color: white; font-size: 25px; font-weight: 800; background: linear-gradient(135deg, #176b87, #27a5a7); }
.register-brand h1, .register-brand p { margin: 0; }
.register-brand h1 { font-size: 24px; }
.register-brand p { margin-top: 5px; color: var(--muted); font-size: 13px; }
.submit-button { width: 100%; margin-top: 4px; }
.back-link { margin-top: 20px; text-align: center; color: var(--muted); font-size: 14px; }
.back-link a { color: var(--brand); font-weight: 700; }
</style>
