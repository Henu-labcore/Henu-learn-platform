import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    watch: {
      // Windows 下编辑器/工具写入时会生成临时文件，监听它会导致 EBUSY 崩溃
      ignored: ['**/.*.tmpdir/**', '**/.*.tmp']
    },
    proxy: {
      '/api': {
        // 开发环境代理到本地后端 (本地开发用)
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/lite': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false
  }
})

