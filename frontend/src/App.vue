<script setup lang="ts">
import { ConfigProvider } from 'ant-design-vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()

const themeConfig = {
  token: {
    colorPrimary: '#5B47E0',
    colorSuccess: '#00B96B',
    colorError: '#CF1322',
    colorWarning: '#FAAD14',
    colorInfo: '#1890FF',
    colorTextBase: '#1F1F1F',
    colorBgBase: ui.theme === 'dark' ? '#0F1117' : '#FFFFFF',
    colorBgContainer: ui.theme === 'dark' ? '#181A23' : '#FFFFFF',
    colorBgLayout: ui.theme === 'dark' ? '#0F1117' : '#F5F6FA',
    colorBorder: ui.theme === 'dark' ? '#2A2D3A' : '#E5E7EB',
    colorBorderSecondary: ui.theme === 'dark' ? '#21242F' : '#F0F0F0',
    fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", sans-serif',
    fontSize: 14,
    borderRadius: 6,
    wireframe: false,
  },
  components: {
    Button: { controlHeight: 32 },
    Input: { controlHeight: 32 },
    Select: { controlHeight: 32 },
    Table: { headerBg: '#FAFAFC', headerColor: '#8C8C8C' },
    Tag: { defaultBg: '#F0F0F0' },
    Card: { borderRadiusLG: 8 },
  },
}
</script>

<template>
  <ConfigProvider :theme="themeConfig">
    <AppLayout />
  </ConfigProvider>
</template>
