<script setup lang="ts">
import { ref, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Form, Input, Button, Card, message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { http, setToken } from '@/api/client'

const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) {
    message.warning('Please enter username and password')
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
    message.error(e instanceof Error ? e.message : 'Login failed')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="cs-login">
    <Card class="cs-login__card" :bordered="false">
      <div class="cs-login__head">
        <h1 class="cs-login__title">code-sec</h1>
        <p class="cs-login__subtitle">Code Security Audit Platform</p>
      </div>
      <Form layout="vertical" @submit.prevent="handleLogin">
        <Form.Item label="Username" name="username">
          <Input
            v-model:value="username"
            placeholder="Enter username"
            size="large"
            :prefix="() => h(UserOutlined)"
            autocomplete="username"
          />
        </Form.Item>
        <Form.Item label="Password" name="password">
          <Input.Password
            v-model:value="password"
            placeholder="Enter password"
            size="large"
            :prefix="() => h(LockOutlined)"
            autocomplete="current-password"
          />
        </Form.Item>
        <Form.Item>
          <Button
            type="primary"
            html-type="submit"
            :loading="loading"
            block
            size="large"
          >
            Sign in
          </Button>
        </Form.Item>
      </Form>
      <div class="cs-login__footer">
        <span class="cs-login__hint">Demo: admin / admin123</span>
      </div>
    </Card>
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
