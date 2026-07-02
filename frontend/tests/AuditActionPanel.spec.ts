import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus from 'element-plus'
import AuditActionPanel from '@/components/audit/AuditActionPanel.vue'
import type { AuditAction } from '@/types/audit'

describe('AuditActionPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  function mountPanel(modelValue: AuditAction) {
    return mount(AuditActionPanel, {
      props: { modelValue },
      global: { plugins: [ElementPlus] },
    })
  }

  it('renders all three decision options', () => {
    const w = mountPanel('confirm')
    const cards = w.findAll('.cs-decision-card')
    expect(cards).toHaveLength(3)
  })

  it('marks the modelValue card as selected', () => {
    const w = mountPanel('false_positive')
    const selected = w.findAll('.cs-decision-card.is-selected')
    expect(selected).toHaveLength(1)
    expect(selected[0]?.text()).toContain('false positive')
  })

  it('emits update:modelValue when a card is clicked', async () => {
    const w = mountPanel('confirm')
    const cards = w.findAll('.cs-decision-card')
    const retest = cards[2]
    if (retest === undefined) throw new Error('expected 3 cards')
    await retest.trigger('click')
    const events = w.emitted('update:modelValue')
    expect(events).toBeTruthy()
    expect(events?.[0]).toEqual(['need_retest'])
  })

  it('shows the descriptive copy for each option', () => {
    const w = mountPanel('confirm')
    const html = w.html()
    expect(html).toContain('Confirm vulnerability')
    expect(html).toContain('Mark as false positive')
    expect(html).toContain('Request retest')
  })
})
