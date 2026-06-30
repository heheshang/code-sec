import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import App from './App.vue'
import { router } from './router'

import './styles/global.css'

async function enableMocking(): Promise<void> {
  const { worker } = await import('./api/mock/browser')
  await worker.start({
    onUnhandledRequest: 'bypass',
    serviceWorker: { url: '/mockServiceWorker.js' },
  })
}

async function bootstrap(): Promise<void> {
  // Always enable MSW; there is no real backend.
  await enableMocking()

  const app = createApp(App)
  app.use(createPinia())
  app.use(router)
  app.use(Antd)
  app.mount('#app')
}

void bootstrap()
