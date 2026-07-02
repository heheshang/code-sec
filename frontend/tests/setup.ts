import { vi, beforeAll } from 'vitest'

// Polyfill matchMedia for components that read it
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

// CodeMirror 6 calls Range.getClientRects() internally, which jsdom doesn't
// implement. Polyfill to suppress unhandled errors in tests that mount CM.
if (typeof Range !== 'undefined' && !Range.prototype.getClientRects) {
  Range.prototype.getClientRects = (() => []) as unknown as () => DOMRectList
}

beforeAll(() => {
  // no-op
})
