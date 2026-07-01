 import { createApp } from 'vue'
 import { createPinia } from 'pinia'
 import Antd from 'ant-design-vue'
 import 'ant-design-vue/dist/reset.css'
 
 import App from './App.vue'
 import { router } from './router'
 
 import './styles/global.css'
 
 /**
  * Enable MSW mock service worker in development only.
  * In production builds Vite replaces import.meta.env.DEV with false,
  * so the entire dynamic import is tree-shaken away — zero mock code in prod.
  */
async function enableMocking(): Promise<void> {
  if (!import.meta.env.DEV) return
  // Set VITE_MOCK_API=false to connect to real backend at localhost:8080
  if (import.meta.env.VITE_MOCK_API === 'false') return
  const { worker } = await import('./api/mock/browser')
  await worker.start({
    onUnhandledRequest: 'bypass',
    serviceWorker: { url: '/mockServiceWorker.js' },
  })
}
 
 async function bootstrap(): Promise<void> {
   await enableMocking()
 
   const app = createApp(App)
   app.use(createPinia())
   app.use(router)
   app.use(Antd)
   app.mount('#app')
 }
 
 void bootstrap()
