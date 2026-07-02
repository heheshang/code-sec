<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const visible = ref(false)
const loadingCount = ref(0)

router.beforeEach(() => {
  loadingCount.value++
  visible.value = true
})

router.afterEach(() => {
  loadingCount.value--
  if (loadingCount.value <= 0) {
    loadingCount.value = 0
    visible.value = false
  }
})

router.onError(() => {
  loadingCount.value = 0
  visible.value = false
})
</script>

<template>
  <div v-if="visible" class="cs-top-progress" />
</template>

<style scoped>
.cs-top-progress {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 99999;
  width: 100%;
  height: 2px;
  background: transparent;
  overflow: hidden;
}
.cs-top-progress::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 30%;
  background: var(--el-color-primary);
  animation: cs-progress-indeterminate 1.5s ease-in-out infinite;
}
@keyframes cs-progress-indeterminate {
  0% { left: -30%; width: 30%; }
  50% { width: 50%; }
  100% { left: 100%; width: 30%; }
}
</style>
