<script setup lang="ts">
import { ref } from 'vue'
import { Upload, Input, Typography, Space, Button } from 'ant-design-vue'
import { InboxOutlined, LinkOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import type { PocAttachment } from '@/types/audit'

interface Props {
  modelValue: PocAttachment[]
  content: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: PocAttachment[]): void
  (e: 'update:content', value: string): void
}>()

const urlInput = ref<string>('')

function makeId(): string {
  return `poc-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result))
    reader.onerror = () => reject(new Error('read failed'))
    reader.readAsDataURL(file)
  })
}

const handleBeforeUpload: NonNullable<UploadProps['beforeUpload']> = async (file) => {
  if (file.size > 4 * 1024 * 1024) {
    return Upload.LIST_IGNORE
  }
  const preview = await fileToDataUrl(file)
  const att: PocAttachment = {
    id: makeId(),
    name: file.name,
    type: 'image',
    preview,
  }
  emit('update:modelValue', [...props.modelValue, att])
  return false
}

function addUrlAttachment(): void {
  const value = urlInput.value.trim()
  if (value.length === 0) return
  const att: PocAttachment = {
    id: makeId(),
    name: value,
    type: 'url',
    preview: value,
  }
  emit('update:modelValue', [...props.modelValue, att])
  urlInput.value = ''
}

function removeAttachment(id: string): void {
  emit('update:modelValue', props.modelValue.filter((a) => a.id !== id))
}

function updateContent(e: Event): void {
  emit('update:content', (e.target as HTMLTextAreaElement).value)
}
</script>

<template>
  <div class="cs-poc-uploader">
    <Typography.Text class="cs-poc-uploader__label">Proof of concept</Typography.Text>
    <Input.TextArea
      :value="content"
      :rows="5"
      placeholder="Describe the steps to reproduce, paste a curl request, or list the payload that triggers the bug."
      class="cs-poc-uploader__textarea"
      @change="updateContent"
    />

    <div class="cs-poc-uploader__row">
      <Upload.Dragger
        :before-upload="handleBeforeUpload"
        :show-upload-list="false"
        :multiple="true"
        accept="image/*"
        class="cs-poc-uploader__drop"
      >
        <p class="cs-poc-uploader__dropInner">
          <InboxOutlined class="cs-poc-uploader__dropIcon" />
        </p>
        <p class="cs-poc-uploader__dropText">Drop a screenshot or click to upload</p>
        <p class="cs-poc-uploader__dropHint">PNG, JPG up to 4 MB</p>
      </Upload.Dragger>

      <div class="cs-poc-uploader__urlBlock">
        <Typography.Text class="cs-poc-uploader__label">Or attach a URL</Typography.Text>
        <Space.Compact style="display: flex">
          <Input
            v-model:value="urlInput"
            :prefix="LinkOutlined"
            placeholder="https://paste.example/poc"
            @press-enter="addUrlAttachment"
          />
          <Button @click="addUrlAttachment" :disabled="urlInput.trim().length === 0">Attach</Button>
        </Space.Compact>
      </div>
    </div>

    <div v-if="modelValue.length > 0" class="cs-poc-uploader__list">
      <div v-for="att in modelValue" :key="att.id" class="cs-poc-uploader__item">
        <img v-if="att.type === 'image'" :src="att.preview" :alt="att.name" class="cs-poc-uploader__thumb" />
        <div v-else class="cs-poc-uploader__urlChip">
          <LinkOutlined />
          <span class="cs-poc-uploader__urlText">{{ att.name }}</span>
        </div>
        <Button type="text" size="small" @click="removeAttachment(att.id)">
          <DeleteOutlined />
        </Button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-poc-uploader {
  display: flex;
  flex-direction: column;
  gap: var(--cs-space-2);
}
.cs-poc-uploader__label {
  font-size: var(--cs-font-size-sm);
  font-weight: 600;
  color: var(--cs-text-primary);
  display: block;
}
.cs-poc-uploader__textarea :deep(textarea) {
  font-family: var(--cs-font-mono);
  font-size: var(--cs-font-size-sm);
}
.cs-poc-uploader__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--cs-space-3);
}
.cs-poc-uploader__drop {
  background: var(--cs-bg-sunken);
  border-color: var(--cs-border) !important;
  padding: var(--cs-space-3) !important;
}
.cs-poc-uploader__drop :deep(.ant-upload-drag-icon) {
  display: none;
}
.cs-poc-uploader__dropInner {
  margin: 0 0 4px;
  font-size: 22px;
}
.cs-poc-uploader__dropIcon {
  color: var(--cs-color-primary);
}
.cs-poc-uploader__dropText {
  font-size: var(--cs-font-size-sm);
  color: var(--cs-text-primary);
  margin: 0;
}
.cs-poc-uploader__dropHint {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  margin: 2px 0 0;
}
.cs-poc-uploader__urlBlock {
  display: flex;
  flex-direction: column;
  gap: var(--cs-space-1);
  justify-content: center;
}
.cs-poc-uploader__list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cs-space-2);
  margin-top: var(--cs-space-2);
}
.cs-poc-uploader__item {
  display: inline-flex;
  align-items: center;
  gap: var(--cs-space-1);
  padding: 4px 4px 4px 6px;
  background: var(--cs-bg-sunken);
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
}
.cs-poc-uploader__thumb {
  width: 36px;
  height: 36px;
  object-fit: cover;
  border-radius: var(--cs-radius-sm);
}
.cs-poc-uploader__urlChip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: var(--cs-font-mono);
  font-size: 11px;
  color: var(--cs-color-primary);
  max-width: 200px;
  padding: 0 4px;
}
.cs-poc-uploader__urlText {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
