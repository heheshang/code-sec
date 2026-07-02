<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { http, setToken } from '@/api/client'

const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) {
    ElMessage.warning('Please enter username and password')
    return
  }
  loading.value = true
  try {
    const resp = await http.post<{ token: string }>('/auth/login', {
      username: username.value,
      password: password.value,
    })
    setToken(resp.data.token)
    const redirect = (route.query.redirect as string) || '/dashboard'
    await router.push(redirect)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : 'Login failed')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="cs-login">
    <el-card class="cs-login__card" shadow="never">
      <div class="cs-login__head">
        <h1 class="cs-login__title">code-sec</h1>
        <p class="cs-login__subtitle">Code Security Audit Platform</p>
      </div>
      <el-form label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="Username">
          <el-input
            v-model="username"
            placeholder="Enter username"
            autocomplete="username"
          >
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="Password">
          <el-input
            v-model="password"
            type="password"
            show-password
            placeholder="Enter password"
            autocomplete="current-password"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            style="width: 100%"
          >
            Sign in
          </el-button>
        </el-form-item>
      </el-form>
      <div class="cs-login__footer">
        <span class="cs-login__hint">Enter your credentials to sign in</span>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.cs-login {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--cs-bg);
}
.cs-login__card {
  width: 380px;
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-lg);
}
.cs-login__head {
  text-align: center;
  margin-bottom: var(--cs-space-6);
}
.cs-login__title {
  font-size: 28px;
  font-weight: 700;
  color: var(--cs-text-primary);
  margin: 0;
}
.cs-login__subtitle {
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-tertiary);
  margin: 4px 0 0;
}
.cs-login__footer {
  text-align: center;
  margin-top: var(--cs-space-2);
}
.cs-login__hint {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
}
</style>
