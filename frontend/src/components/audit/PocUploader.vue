<script setup lang="ts">
import { ref } from 'vue'
import { Upload, Link, Delete } from '@element-plus/icons-vue'
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

function handleBeforeUpload(rawFile: File): boolean {
  if (rawFile.size > 4 * 1024 * 1024) {
    return false
  }
  fileToDataUrl(rawFile).then((preview) => {
    const att: PocAttachment = {
      id: makeId(),
      name: rawFile.name,
      type: 'image',
      preview,
    }
    emit('update:modelValue', [...props.modelValue, att])
  })
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
</script>

<template>
  <div class="cs-poc-uploader">
    <span class="cs-poc-uploader__label">Proof of concept</span>
    <el-input
      :model-value="content"
      type="textarea"
      :rows="5"
      placeholder="Describe the steps to reproduce, paste a curl request, or list the payload that triggers the bug."
      class="cs-poc-uploader__textarea"
      @input="(v: string) => emit('update:content', v)"
    />

    <div class="cs-poc-uploader__row">
      <el-upload
        drag
        :multiple="true"
        accept="image/*"
        :auto-upload="false"
        :before-upload="handleBeforeUpload"
        :show-file-list="false"
        class="cs-poc-uploader__drop"
      >
        <div class="cs-poc-uploader__dropInner">
          <el-icon :size="22" color="var(--cs-color-primary)"><Upload /></el-icon>
        </div>
        <p class="cs-poc-uploader__dropText">Drop a screenshot or click to upload</p>
        <p class="cs-poc-uploader__dropHint">PNG, JPG up to 4 MB</p>
      </el-upload>

      <div class="cs-poc-uploader__urlBlock">
        <span class="cs-poc-uploader__label">Or attach a URL</span>
        <div class="cs-poc-uploader__urlRow">
          <el-input
            v-model="urlInput"
            placeholder="https://paste.example/poc"
            @keyup.enter="addUrlAttachment"
          >
            <template #prefix>
              <el-icon><Link /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" :disabled="urlInput.trim().length === 0" @click="addUrlAttachment">Attach</el-button>
        </div>
      </div>
    </div>

    <div v-if="modelValue.length > 0" class="cs-poc-uploader__list">
      <div v-for="att in modelValue" :key="att.id" class="cs-poc-uploader__item">
        <img v-if="att.type === 'image'" :src="att.preview" :alt="att.name" class="cs-poc-uploader__thumb" />
        <div v-else class="cs-poc-uploader__urlChip">
          <el-icon><Link /></el-icon>
          <span class="cs-poc-uploader__urlText">{{ att.name }}</span>
        </div>
        <el-button text size="small" @click="removeAttachment(att.id)">
          <el-icon><Delete /></el-icon>
        </el-button>
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
  width: 100%;
}
.cs-poc-uploader__drop :deep(.el-upload-dragger) {
  background: var(--cs-bg-sunken);
  border-color: var(--cs-border);
  padding: var(--cs-space-3);
}
.cs-poc-uploader__dropInner {
  margin: 0 0 4px;
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
.cs-poc-uploader__urlRow {
  display: flex;
  gap: var(--cs-space-1);
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
