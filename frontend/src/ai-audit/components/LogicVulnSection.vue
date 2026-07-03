<script setup lang="ts">
import type { LogicVulnResult, LogicVulnType } from '@/types/ai-audit'
import { LOGIC_VULN_TYPE_LABEL, LOGIC_VULN_COLOR } from '@/types/ai-audit'

defineProps<{
  results: LogicVulnResult[]
  loading?: boolean
}>()

const emit = defineEmits<{
  viewCode: [result: LogicVulnResult]
}>()

function isKnownType(t: string): t is LogicVulnType {
  return t in LOGIC_VULN_TYPE_LABEL
}

function typeLabel(t: string): string {
  return isKnownType(t) ? LOGIC_VULN_TYPE_LABEL[t] : t
}

function riskColor(level: string): string {
  return LOGIC_VULN_COLOR[level] || 'var(--cs-text-tertiary)'
}
</script>

<template>
  <div class="cs-logic-vuln">
    <div class="cs-logic-vuln-header">
      <span class="cs-logic-vuln-title">Logic Vulnerability Mining</span>
      <el-tag v-if="results.length > 0" type="warning" size="small">
        {{ results.length }} findings
      </el-tag>
      <el-tag v-else type="info" size="small">No logic issues found</el-tag>
    </div>

    <div v-if="loading" class="cs-logic-vuln-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="results.length === 0" class="cs-logic-vuln-empty">
      <el-empty description="No logic vulnerabilities detected" :image-size="80" />
    </div>

    <div v-else class="cs-logic-vuln-list">
      <div
        v-for="(result, idx) in results"
        :key="idx"
        class="cs-logic-vuln-card"
      >
        <div class="cs-logic-vuln-card-header">
          <div class="cs-logic-vuln-card-type">
            <span
              class="cs-logic-vuln-risk-dot"
              :style="{ background: riskColor(result.riskLevel) }"
            />
            <span class="cs-logic-vuln-type-label">{{ typeLabel(result.vulnType) }}</span>
          </div>
          <el-tag
            size="small"
            :style="{
              background: riskColor(result.riskLevel) + '18',
              borderColor: riskColor(result.riskLevel) + '40',
              color: riskColor(result.riskLevel),
            }"
          >
            {{ result.riskLevel }}
          </el-tag>
        </div>

        <div class="cs-logic-vuln-evidence">
          <span class="cs-logic-vuln-section-label">Evidence Chain</span>
          <ol class="cs-logic-vuln-evidence-list">
            <li v-for="(step, sIdx) in result.evidenceChain" :key="sIdx">
              {{ step }}
            </li>
          </ol>
        </div>

        <div class="cs-logic-vuln-condition">
          <span class="cs-logic-vuln-section-label">Exploit Condition</span>
          <p>{{ result.exploitCondition }}</p>
        </div>

        <div v-if="result.recommendedFix" class="cs-logic-vuln-fix">
          <span class="cs-logic-vuln-section-label">Recommended Fix</span>
          <p>{{ result.recommendedFix }}</p>
        </div>

        <div class="cs-logic-vuln-meta">
          <el-space :size="12">
            <span class="cs-logic-vuln-location">
              L{{ result.lineStart }}-{{ result.lineEnd }}
            </span>
            <el-button size="small" text type="primary" @click="emit('viewCode', result)">
              <el-icon><View /></el-icon>
              View code
            </el-button>
          </el-space>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cs-logic-vuln {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cs-logic-vuln-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cs-logic-vuln-title {
  font-size: 14px;
  font-weight: 600;
}

.cs-logic-vuln-loading {
  padding: 16px 0;
}

.cs-logic-vuln-empty {
  padding: 16px 0;
}

.cs-logic-vuln-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cs-logic-vuln-card {
  border: 1px solid var(--cs-border);
  border-radius: var(--cs-radius-md);
  padding: var(--cs-space-3);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cs-logic-vuln-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cs-logic-vuln-card-type {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cs-logic-vuln-risk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.cs-logic-vuln-type-label {
  font-size: 13px;
  font-weight: 600;
}

.cs-logic-vuln-section-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--cs-text-tertiary);
  margin-bottom: 4px;
}

.cs-logic-vuln-evidence-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--cs-text-primary);
}

.cs-logic-vuln-condition p,
.cs-logic-vuln-fix p {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--cs-text-secondary);
}

.cs-logic-vuln-meta {
  display: flex;
  align-items: center;
  padding-top: 4px;
  border-top: 1px solid var(--cs-border-light);
}

.cs-logic-vuln-location {
  font-family: var(--cs-font-mono);
  font-size: 11px;
  color: var(--cs-text-tertiary);
}
</style>
