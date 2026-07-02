<script setup lang="ts">
import { computed } from 'vue'
import { User, Bell, Sunny } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()

const profile = {
  name: 'You (current auditor)',
  email: 'auditor@code-sec.io',
  role: 'Security auditor',
  team: 'Application security',
  joined: '2025-09-14',
}

const isDark = computed<boolean>(() => ui.theme === 'dark')
</script>

<template>
  <div class="cs-settings">
    <PageHeader
      title="Settings"
      subtitle="Profile, appearance, and notification preferences"
    />

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="cs-settings__card">
          <template #header>
            <el-space :size="6">
              <el-icon><User /></el-icon>
              <span>Profile</span>
            </el-space>
          </template>
          <div class="cs-settings__profile">
            <el-avatar :size="64" class="cs-settings__avatar">Y</el-avatar>
            <div class="cs-settings__profileText">
              <h4 style="margin: 0">{{ profile.name }}</h4>
              <el-space :size="6" class="cs-settings__tags">
                <el-tag type="info" effect="plain">{{ profile.role }}</el-tag>
                <el-tag effect="plain">{{ profile.team }}</el-tag>
                <el-tag type="success" effect="plain">
                  SSO active
                </el-tag>
              </el-space>
            </div>
          </div>
          <el-divider />
          <el-form label-position="top" disabled class="cs-settings__form">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="Display name">
                  <el-input :model-value="profile.name" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Email">
                  <el-input :model-value="profile.email" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Role">
                  <el-input :model-value="profile.role" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="Joined">
                  <el-input :model-value="profile.joined" />
                </el-form-item>
              </el-col>
            </el-row>
            <span class="cs-settings__hint">
              Profile fields are read-only in this prototype.
            </span>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="cs-settings__card">
          <template #header>
            <el-space :size="6">
              <el-icon><Sunny /></el-icon>
              <span>Appearance</span>
            </el-space>
          </template>
          <div class="cs-settings__row">
            <div>
              <strong>Dark mode</strong>
              <div class="cs-settings__rowHint">Use a low-light palette for late shifts.</div>
            </div>
            <el-switch :model-value="isDark" @change="ui.toggleTheme" />
          </div>
          <el-divider />
          <div class="cs-settings__row">
            <div>
              <strong>Compact tables</strong>
              <div class="cs-settings__rowHint">Reduce row height in the queue and history views.</div>
            </div>
            <el-switch :model-value="true" disabled />
          </div>
        </el-card>

        <el-card shadow="never" class="cs-settings__card" style="margin-top: var(--cs-space-4)">
          <template #header>
            <el-space :size="6">
              <el-icon><Bell /></el-icon>
              <span>Notifications</span>
            </el-space>
          </template>
          <div class="cs-settings__row">
            <div>
              <strong>Critical findings</strong>
              <div class="cs-settings__rowHint">Real-time toast + email when a critical lands in your queue.</div>
            </div>
            <el-switch
              :model-value="ui.prefs.notifyOnCritical"
              @change="(v: boolean) => ui.setPref('notifyOnCritical', v)"
            />
          </div>
          <el-divider />
          <div class="cs-settings__row">
            <div>
              <strong>Retest requested</strong>
              <div class="cs-settings__rowHint">When a fix you confirmed needs another look.</div>
            </div>
            <el-switch
              :model-value="ui.prefs.notifyOnRetest"
              @change="(v: boolean) => ui.setPref('notifyOnRetest', v)"
            />
          </div>
          <el-divider />
          <div class="cs-settings__row">
            <div>
              <strong>Daily digest</strong>
              <div class="cs-settings__rowHint">Morning email with yesterday's audit numbers.</div>
            </div>
            <el-switch
              :model-value="ui.prefs.dailyDigest"
              @change="(v: boolean) => ui.setPref('dailyDigest', v)"
            />
          </div>
          <el-divider />
          <div class="cs-settings__row">
            <div>
              <strong>Scan completion toast</strong>
              <div class="cs-settings__rowHint">Show a one-off toast when a watched project finishes a scan.</div>
            </div>
            <el-switch
              :model-value="ui.prefs.scanCompletionToast"
              @change="(v: boolean) => ui.setPref('scanCompletionToast', v)"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="cs-settings__foot">
      <el-button text @click="$router.push('/dashboard')">← Back to dashboard</el-button>
    </div>
  </div>
</template>

<style scoped>
.cs-settings__card {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
  margin-bottom: 16px;
}
.cs-settings__profile {
  display: flex;
  align-items: center;
  gap: var(--cs-space-4);
}
.cs-settings__avatar {
  background: linear-gradient(135deg, var(--cs-color-primary) 0%, var(--cs-color-accent) 100%) !important;
  font-size: 28px;
  font-weight: 600;
}
.cs-settings__tags {
  margin-top: 4px;
}
.cs-settings__form {
  margin-top: 0;
}
.cs-settings__hint {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
}
.cs-settings__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cs-space-3);
}
.cs-settings__rowHint {
  font-size: var(--cs-font-size-xs);
  color: var(--cs-text-tertiary);
  margin-top: 2px;
}
.cs-settings__foot {
  text-align: center;
  margin-top: var(--cs-space-6);
}
</style>
