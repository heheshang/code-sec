<script setup lang="ts">
import { ref } from 'vue'

interface Notification {
  id: string
  type: 'info' | 'success' | 'warning' | 'error'
  title: string
  message: string
  timestamp: string
}

const notifications = ref<Notification[]>([])

function addNotification(n: Notification) {
  notifications.value.unshift(n)
  setTimeout(() => {
    const idx = notifications.value.findIndex((x) => x.id === n.id)
    if (idx >= 0) notifications.value.splice(idx, 1)
  }, 6000)
}

function removeNotification(id: string) {
  const idx = notifications.value.findIndex((n) => n.id === id)
  if (idx >= 0) notifications.value.splice(idx, 1)
}

defineExpose({ addNotification })
</script>

<template>
  <div class="cs-realtime-notifications">
    <TransitionGroup name="cs-notif">
      <el-alert
        v-for="n in notifications"
        :key="n.id"
        :title="n.title"
        :type="n.type"
        :description="n.message"
        :closable="true"
        show-icon
        class="cs-notif-item"
        @close="removeNotification(n.id)"
      />
    </TransitionGroup>
  </div>
</template>

<style scoped>
.cs-realtime-notifications {
  position: fixed;
  top: 60px;
  right: 16px;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 360px;
}

.cs-notif-item {
  margin: 0;
}

.cs-notif-enter-active,
.cs-notif-leave-active {
  transition: all 0.3s ease;
}

.cs-notif-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.cs-notif-leave-to {
  opacity: 0;
  transform: translateX(40px);
}
</style>
