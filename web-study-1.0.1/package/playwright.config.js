import { defineConfig } from '@playwright/test'

/** AI 浏览器联调测试配置：复用电脑已安装的 Chrome，不额外下载浏览器文件。 */
export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30_000,
  use: {
    baseURL: 'http://127.0.0.1:4175',
    channel: 'chrome',
    headless: true,
    trace: 'retain-on-failure'
  },
  webServer: {
    command: 'npm.cmd run dev -- --host 127.0.0.1 --port 4175',
    url: 'http://127.0.0.1:4175',
    reuseExistingServer: true,
    timeout: 60_000
  }
})
