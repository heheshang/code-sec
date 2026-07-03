import { ref, onMounted, onUnmounted } from 'vue'

export interface StompMessage {
  body: string
  headers?: Record<string, string>
}

export type MessageHandler = (msg: StompMessage) => void

const SOCKJS_URL = '/ws'
const RECONNECT_DELAY_MS = 5000
const HEARTBEAT_MS = 10000

const topics = [
  '/topic/scan/progress',
  '/topic/vuln/new',
  '/topic/audit/update',
  '/topic/ticket/update',
] as const

export type Topic = (typeof topics)[number]

export function useWebSocket() {
  const connected = ref(false)
  const lastMessage = ref<StompMessage | null>(null)
  const error = ref<string | null>(null)
  const reconnecting = ref(false)

  const handlers = new Map<Topic, Set<MessageHandler>>()
  let stompClient: any = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  function subscribe(topic: Topic, handler: MessageHandler) {
    if (!handlers.has(topic)) {
      handlers.set(topic, new Set())
    }
    handlers.get(topic)!.add(handler)
    if (stompClient && connected.value) {
      stompClient.subscribe(topic, (msg: StompMessage) => {
        handler(msg)
      })
    }
  }

  function unsubscribe(topic: Topic, handler: MessageHandler) {
    handlers.get(topic)?.delete(handler)
  }

  function connect() {
    error.value = null
    reconnecting.value = false

    try {
      const SockJS = (window as any).SockJS
      const Stomp = (window as any).Stomp

      if (!SockJS || !Stomp) {
        error.value = 'SockJS/Stomp not loaded. Install: npm install @stomp/stompjs sockjs-client'
        return
      }

      const socket = new SockJS(SOCKJS_URL)
      stompClient = Stomp.over(socket)

      stompClient.heartbeat.outgoing = HEARTBEAT_MS
      stompClient.heartbeat.incoming = HEARTBEAT_MS

      stompClient.connect({}, () => {
        connected.value = true
        error.value = null

        for (const [topic, topicHandlers] of handlers.entries()) {
          stompClient.subscribe(topic, (msg: StompMessage) => {
            lastMessage.value = msg
            topicHandlers.forEach((h) => h(msg))
          })
        }
      }, (err: any) => {
        connected.value = false
        error.value = err?.message || 'Connection lost'
        scheduleReconnect()
      })
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Connection failed'
      connected.value = false
      scheduleReconnect()
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) return
    reconnecting.value = true
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, RECONNECT_DELAY_MS)
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (stompClient) {
      try {
        stompClient.disconnect()
      } catch { /* ignore */ }
      stompClient = null
    }
    connected.value = false
    reconnecting.value = false
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    lastMessage,
    error,
    reconnecting,
    subscribe,
    unsubscribe,
  }
}
