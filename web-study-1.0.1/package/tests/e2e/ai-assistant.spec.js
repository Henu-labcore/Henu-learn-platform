import { expect, test } from '@playwright/test'

const success = data => ({ success: true, data })

/**
 * 用浏览器路由模拟本地后端和 JupyterLite，只验证 AI 新功能，不依赖测试数据库和模型密钥。
 * fake notebook 会响应 Shift+Enter，便于确认实际执行结果被放进 AI 请求上下文。
 */
test('多会话管理和运行分析闭环可用', async ({ page }) => {
  let streamPayload = null
  let conversationCreated = false

  await page.addInitScript(() => {
    localStorage.setItem('token', 'e2e-token')
    localStorage.setItem('user', JSON.stringify({ userId: 7, username: 'student', displayName: '测试学生', role: 'USER' }))
  })

  await page.route('**/api/**', async route => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    // “/src/api/*.js” 也是前端模块路径，不能被后端接口模拟规则误拦截。
    if (!path.startsWith('/api/')) return route.continue()
    if (path === '/api/learning-items/1') {
      return route.fulfill({ json: success({
        id: 1,
        title: 'Tool Use',
        summary: 'AI 工具调用学习',
        category: '智能体',
        difficulty: '中级',
        duration: '30 分钟',
        prerequisites: 'Python',
        objectives: '掌握工具调用',
        features: [],
        steps: [{ title: '定义工具', description: '编写并运行一个工具函数', code: 'print("tool-ready")' }]
      }) })
    }
    if (path === '/api/learning-items') return route.fulfill({ json: success([]) })
    if (path === '/api/learning-progress') return route.fulfill({ json: success({ status: 'IN_PROGRESS' }) })
    if (path.startsWith('/api/learning-progress/')) return route.fulfill({ json: success({}) })
    if (path === '/api/experiment/1/launch') return route.fulfill({ json: success({ launchUrl: '/fake-notebook' }) })
    if (path === '/api/ai-assistant/history') {
      const selected = url.searchParams.get('conversationId')
      return route.fulfill({ json: success({
        conversationId: Number(selected || 11),
        conversationTitle: selected === '12' ? '报错排查' : '默认会话',
        demoMode: true,
        messages: selected === '12' ? [{ messageId: 31, role: 'USER', content: '旧问题' }] : []
      }) })
    }
    if (path === '/api/ai-assistant/conversations' && request.method() === 'GET') {
      const rows = [
        { conversationId: 11, title: '默认会话', messageCount: 0, updatedAt: '2026-07-19T09:00:00' },
        { conversationId: 12, title: '报错排查', messageCount: 1, updatedAt: '2026-07-19T08:00:00' }
      ]
      if (conversationCreated) rows.unshift({ conversationId: 13, title: '新会话 3', messageCount: 0 })
      return route.fulfill({ json: success(rows) })
    }
    if (path === '/api/ai-assistant/conversations' && request.method() === 'POST') {
      conversationCreated = true
      return route.fulfill({ json: success({ conversationId: 13, title: '新会话 3', messageCount: 0 }) })
    }
    if (path === '/api/ai-assistant/chat/stream') {
      streamPayload = request.postDataJSON()
      // 保留短暂的首包等待，验证请求处理中已经出现流式生成状态，而不是只检查最终文本。
      await new Promise(resolve => setTimeout(resolve, 150))
      const body = [
        'event:meta\ndata:{"demoMode":true,"sourceLabel":"当前实验"}\n\n',
        'event:delta\ndata:{"content":"已结合运行输出完成分析"}\n\n',
        'event:done\ndata:{"conversationId":13,"messageId":99}\n\n'
      ].join('')
      return route.fulfill({ status: 200, contentType: 'text/event-stream', body })
    }
    return route.fulfill({ json: success({}) })
  })

  // 同源的最小 notebook 夹具：Shift+Enter 后产生真实 DOM 输出。
  await page.route('**/fake-notebook**', route => route.fulfill({
    contentType: 'text/html',
    body: `<div class="jp-Cell jp-mod-active"><div class="cm-content" tabindex="0">print("tool-ready")</div><div class="jp-OutputArea-output"></div></div>
      <script>document.querySelector('.cm-content').addEventListener('keydown', event => { if (event.shiftKey && event.key === 'Enter') setTimeout(() => { document.querySelector('.jp-OutputArea-output').textContent = 'tool-ready' }, 30) })<\/script>`
  }))

  await page.goto('/learning/item/1')
  await expect(page.getByRole('heading', { name: /Tool Use/ }).first()).toBeVisible()

  const learningAssistant = page.locator('.ai-study-assistant:not(.is-experiment)')
  await learningAssistant.getByRole('button', { name: '打开 AI 助学助手' }).click()
  await learningAssistant.getByRole('button', { name: '记录' }).click()
  await expect(learningAssistant.getByText('报错排查')).toBeVisible()
  await learningAssistant.getByRole('button', { name: '＋ 新建会话' }).click()
  await expect(learningAssistant.getByText(/新会话 3/).first()).toBeVisible()
  await learningAssistant.getByRole('button', { name: '关闭助手' }).click()

  await page.getByRole('button', { name: '在线练习' }).click()
  const experimentAssistant = page.locator('.ai-study-assistant.is-experiment')
  await experimentAssistant.getByRole('button', { name: '打开 AI 助学助手' }).click()
  // 点击前记录回答DOM的每次变化，避免用某一个瞬间是否抓到光标来判断流式效果。
  await page.evaluate(() => {
    window.__aiStreamState = { cursorObserved: false, snapshots: [] }
    const observer = new MutationObserver(() => {
      const root = document.querySelector('.ai-study-assistant.is-experiment')
      if (!root) return
      if (root.querySelector('.stream-cursor')) window.__aiStreamState.cursorObserved = true
      const answers = root.querySelectorAll('.message-row.assistant pre')
      const text = answers.length ? answers[answers.length - 1].textContent : ''
      const previous = window.__aiStreamState.snapshots.at(-1)
      if (text && text !== previous) window.__aiStreamState.snapshots.push(text)
    })
    observer.observe(document.body, { childList: true, characterData: true, subtree: true })
    window.__aiStreamObserver = observer
  })
  await experimentAssistant.getByRole('button', { name: '运行并分析' }).click()
  await expect(experimentAssistant.getByText('已结合运行输出完成分析')).toBeVisible()
  await expect(experimentAssistant.locator('.stream-cursor')).toBeHidden()
  const streamState = await page.evaluate(() => {
    window.__aiStreamObserver?.disconnect()
    return window.__aiStreamState
  })
  expect(streamState.cursorObserved).toBe(true)
  expect(streamState.snapshots.length).toBeGreaterThan(1)
  expect(streamState.snapshots.some(text => text !== '已结合运行输出完成分析')).toBe(true)
  expect(streamPayload.additionalContext).toContain('print("tool-ready")')
  expect(streamPayload.additionalContext).toContain('tool-ready')
})
