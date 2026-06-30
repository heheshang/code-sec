<script setup lang="ts">
import { computed } from 'vue'
import { Card, Form, Input, Switch, Row, Col, Avatar, Divider, Tag, Space, Typography, Button } from 'ant-design-vue'
import { UserOutlined, BulbOutlined, BellOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
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

    <Row :gutter="[16, 16]">
      <Col :xs="24" :lg="14">
        <Card :bordered="false" class="cs-settings__card">
          <template #title>
            <Space :size="6">
              <UserOutlined />
              <span>Profile</span>
            </Space>
          </template>
          <div class="cs-settings__profile">
            <Avatar :size="64" class="cs-settings__avatar">Y</Avatar>
            <div class="cs-settings__profileText">
              <Typography.Title :level="4" style="margin: 0">{{ profile.name }}</Typography.Title>
              <Space :size="6" class="cs-settings__tags">
                <Tag color="purple" bordered>{{ profile.role }}</Tag>
                <Tag bordered>{{ profile.team }}</Tag>
                <Tag color="green" bordered>
                  <SafetyCertificateOutlined /> SSO active
                </Tag>
              </Space>
            </div>
          </div>
          <Divider />
          <Form layout="vertical" :disabled="true" class="cs-settings__form">
            <Row :gutter="16">
              <Col :span="12">
                <Form.Item label="Display name">
                  <Input :value="profile.name" />
                </Form.Item>
              </Col>
              <Col :span="12">
                <Form.Item label="Email">
                  <Input :value="profile.email" />
                </Form.Item>
              </Col>
              <Col :span="12">
                <Form.Item label="Role">
                  <Input :value="profile.role" />
                </Form.Item>
              </Col>
              <Col :span="12">
                <Form.Item label="Joined">
                  <Input :value="profile.joined" />
                </Form.Item>
              </Col>
            </Row>
            <Typography.Text type="secondary" class="cs-settings__hint">
              Profile fields are read-only in this prototype.
            </Typography.Text>
          </Form>
        </Card>
      </Col>

      <Col :xs="24" :lg="10">
        <Card :bordered="false" class="cs-settings__card">
          <template #title>
            <Space :size="6">
              <BulbOutlined />
              <span>Appearance</span>
            </Space>
          </template>
          <div class="cs-settings__row">
            <div>
              <Typography.Text strong>Dark mode</Typography.Text>
              <div class="cs-settings__rowHint">Use a low-light palette for late shifts.</div>
            </div>
            <Switch :checked="isDark" @change="ui.toggleTheme" />
          </div>
          <Divider />
          <div class="cs-settings__row">
            <div>
              <Typography.Text strong>Compact tables</Typography.Text>
              <div class="cs-settings__rowHint">Reduce row height in the queue and history views.</div>
            </div>
            <Switch :checked="true" disabled />
          </div>
        </Card>

        <Card :bordered="false" class="cs-settings__card" style="margin-top: 16px">
          <template #title>
            <Space :size="6">
              <BellOutlined />
              <span>Notifications</span>
            </Space>
          </template>
          <div class="cs-settings__row">
            <div>
              <Typography.Text strong>Critical findings</Typography.Text>
              <div class="cs-settings__rowHint">Real-time toast + email when a critical lands in your queue.</div>
            </div>
            <Switch
              :checked="ui.prefs.notifyOnCritical"
              @change="(v: unknown) => ui.setPref('notifyOnCritical', Boolean(v))"
            />
          </div>
          <Divider />
          <div class="cs-settings__row">
            <div>
              <Typography.Text strong>Retest requested</Typography.Text>
              <div class="cs-settings__rowHint">When a fix you confirmed needs another look.</div>
            </div>
            <Switch
              :checked="ui.prefs.notifyOnRetest"
              @change="(v: unknown) => ui.setPref('notifyOnRetest', Boolean(v))"
            />
          </div>
          <Divider />
          <div class="cs-settings__row">
            <div>
              <Typography.Text strong>Daily digest</Typography.Text>
              <div class="cs-settings__rowHint">Morning email with yesterday\u2019s audit numbers.</div>
            </div>
            <Switch
              :checked="ui.prefs.dailyDigest"
              @change="(v: unknown) => ui.setPref('dailyDigest', Boolean(v))"
            />
          </div>
          <Divider />
          <div class="cs-settings__row">
            <div>
              <Typography.Text strong>Scan completion toast</Typography.Text>
              <div class="cs-settings__rowHint">Show a one-off toast when a watched project finishes a scan.</div>
            </div>
            <Switch
              :checked="ui.prefs.scanCompletionToast"
              @change="(v: unknown) => ui.setPref('scanCompletionToast', Boolean(v))"
            />
          </div>
        </Card>
      </Col>
    </Row>

    <div class="cs-settings__foot">
      <Button type="link" @click="$router.push('/dashboard')">← Back to dashboard</Button>
    </div>
  </div>
</template>

<style scoped>
.cs-settings__card {
  background: var(--cs-bg-elevated);
  border: 1px solid var(--cs-border-light);
  border-radius: var(--cs-radius-lg);
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
