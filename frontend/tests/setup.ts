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

// Avoid loading the actual MSW worker in unit tests; tests that need
// network behavior should spin up a node server explicitly.
beforeAll(() => {
  // no-op
})
