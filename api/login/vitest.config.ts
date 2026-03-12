/// <reference types="vitest" />
import {defineConfig} from 'vite'

export default defineConfig({
  test: {
    disableConsoleIntercept: true,
    testTimeout: 1_000_000,
  },
})