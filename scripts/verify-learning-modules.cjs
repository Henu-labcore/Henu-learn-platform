/** 独立测试库的真实浏览器验收；不使用日常账号密码，不写入日常数据库。 */
const path=require('node:path')
const fs=require('node:fs')
const assert=require('node:assert/strict')
const crypto=require('node:crypto')
const {spawn}=require('node:child_process')
const {createRequire}=require('node:module')
const {pathToFileURL}=require('node:url')
const {chromium}=require('playwright')
const root=path.resolve(__dirname,'..'),front=path.join(root,'web-study-1.0.1/package')
const requireFront=createRequire(path.join(front,'package.json'))
const database=process.env.LABCORE_MODULE_TEST_DB,userId=Number(process.env.LABCORE_MODULE_TEST_USER)
if(!/^labcore_module_check_[a-zA-Z0-9_]+$/.test(database||'')||!userId||!process.env.LABCORE_DB_PASSWORD)throw new Error('必须指定独立模块测试库和有效测试用户')
const secret=crypto.randomBytes(32).toString('hex')
const payload=Buffer.from(`${userId}:${Math.floor(Date.now()/1000)+3600}:${crypto.randomUUID()}`).toString('base64url')
const token=`token-${payload}.${crypto.createHmac('sha256',secret).update(payload).digest('base64url')}`
let backend,vite,browser,testFailed=false
// 每轮使用唯一标题，避免保留的历史测试计划干扰严格定位。
const completionPlanName='浏览器完成反馈验收-'+Date.now()
// Vite 清理过程可能影响退出状态；退出时仍须保留验收失败状态。
process.on('exit',()=>{if(testFailed)process.exitCode=1})
const output=path.join(root,'test-results/learning-modules');fs.mkdirSync(output,{recursive:true})
const wait=ms=>new Promise(resolve=>setTimeout(resolve,ms))
async function until(fn,label){for(let i=0;i<60;i++){if(await fn())return;await wait(500)}throw new Error(`等待失败：${label}`)}
async function api(method,url,body){const response=await fetch(`http://127.0.0.1:8081/api${url}`,{method,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:body===undefined?undefined:JSON.stringify(body)});const result=await response.json();assert(response.ok&&result.success,`${method} ${url}: ${response.status}`);return result.data}
;(async()=>{
  // 随机测试签名密钥只传给测试进程，不写入文件或输出。
  backend=spawn(process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin/java.exe'):'java',['-jar',path.join(root,'target/LabCore-0.0.1-SNAPSHOT.jar'),'--server.port=8081','--server.address=127.0.0.1'],{cwd:root,windowsHide:true,env:{...process.env,LABCORE_DB_URL:`jdbc:mysql://127.0.0.1:3306/${database}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`,LABCORE_ACCESS_TOKEN_SECRET:secret,LABCORE_LOG_FILE:path.join(output,'backend.log')}})
  const log=fs.createWriteStream(path.join(output,'backend-console.log'));backend.stdout.pipe(log);backend.stderr.pipe(log)
  await until(async()=>{try{return(await fetch('http://127.0.0.1:8081/actuator/health')).ok}catch{return false}},'测试后端')
  const viteExports=await import(pathToFileURL(requireFront.resolve('vite')).href)
  const viteModule=viteExports.createServer?viteExports:viteExports.default
  vite=await viteModule.createServer({root:front,configFile:path.join(front,'vite.config.js'),server:{host:'127.0.0.1',port:5174,strictPort:true,proxy:{'/api':{target:'http://127.0.0.1:8081',changeOrigin:true},'/lite':{target:'http://127.0.0.1:8081',changeOrigin:true}}}});await vite.listen()
  browser=await chromium.launch({headless:true,channel:'msedge'})
  const origin='http://127.0.0.1:5174'
  // 服务端签发格式的测试身份仅用于克隆库，跳过验证码，不修改原登录实现。
  const storageState={cookies:[],origins:[{origin,localStorage:[{name:'token',value:token},{name:'user',value:JSON.stringify({userId,username:'module-test',displayName:'模块测试账号',role:'USER'})}]}]}
  const context=await browser.newContext({storageState,viewport:{width:1440,height:1000}})
  const page=await context.newPage(),errors=[];page.on('pageerror',error=>errors.push(error.message))
  const items=await api('GET','/learning-items'),item=items[0];assert(item?.id)
  await api('DELETE',`/favorites/${item.id}`)
  await page.goto(origin+'/knowledge');await page.locator('.knowledge-card').first().waitFor()
  await page.locator('.knowledge-card').first().getByRole('button',{name:'收藏',exact:true}).click()
  await until(async()=>await page.locator('.knowledge-card').first().getByRole('button',{name:'取消收藏',exact:true}).count()>0,'星标收藏成功')
  await page.getByRole('link',{name:'我的收藏',exact:true}).click();await page.getByRole('heading',{name:'我的收藏',exact:true}).waitFor()
  await page.getByRole('button',{name:'分类 / 标签 / 备注',exact:true}).first().waitFor()
  await page.screenshot({path:path.join(output,'favorites.png'),fullPage:true})
  const categoryName='浏览器分类-'+Date.now()
  await page.getByRole('button',{name:'管理分类',exact:true}).click();await page.getByPlaceholder('新分类名称').fill(categoryName);await page.getByRole('button',{name:'新增',exact:true}).click()
  await page.getByText(categoryName,{exact:true}).first().waitFor();await page.keyboard.press('Escape')
  await page.getByRole('button',{name:'分类 / 标签 / 备注',exact:true}).first().click()
  await page.getByRole('dialog',{name:'整理收藏'}).getByRole('textbox').last().fill('浏览器检索备注验收')
  await page.getByRole('button',{name:'保存并同步',exact:true}).click();await page.getByText('备注：浏览器检索备注验收',{exact:true}).waitFor()
  await page.getByPlaceholder('检索标题、摘要或个人备注').fill('浏览器检索备注验收');await page.getByRole('button',{name:'检索',exact:true}).click()
  await until(async()=>await page.locator('.favorite-grid article').count()===1,'收藏检索')
  // 第二个独立浏览器上下文确认同一账号读取后端收藏，而不是第一个浏览器的本地缓存。
  const device=await browser.newContext({storageState,viewport:{width:1440,height:1000}}),other=await device.newPage()
  await other.goto(origin+'/favorites');await other.getByText('备注：浏览器检索备注验收',{exact:true}).waitFor();await device.close()
  const progress=await api('GET','/learning-progress/user'),completed=new Set(progress.filter(r=>r.completeTime).map(r=>Number(r.itemPk)))
  const pendingItem=items.find(i=>!completed.has(Number(i.id)));assert(pendingItem)
  const current=await api('GET','/learning-plans/current');if(current?.status==='ACTIVE')await api('POST',`/learning-plans/${current.planId}/cancel`)
  const today=new Date().toLocaleDateString('en-CA',{timeZone:'Asia/Shanghai'}),later=new Date(Date.now()+7*86400000).toLocaleDateString('en-CA',{timeZone:'Asia/Shanghai'})
  const created=await api('POST','/learning-plans',{planName:'浏览器计划验收',startDate:today,endDate:later,items:[{itemPk:pendingItem.id,targetDate:later,sortOrder:1}]})
  await page.getByRole('link',{name:'学习计划',exact:true}).click();await page.getByRole('heading',{name:'学习计划',exact:true}).waitFor()
  await page.getByRole('heading',{name:'浏览器计划验收',exact:true}).waitFor()
  await page.getByRole('button',{name:'调整计划',exact:true}).click();await page.getByPlaceholder('例如：密码学两周强化计划').fill('浏览器计划调整验收');await page.getByRole('button',{name:'保存计划',exact:true}).click();await page.getByRole('heading',{name:'浏览器计划调整验收',exact:true}).waitFor()
  await page.screenshot({path:path.join(output,'learning-plan.png'),fullPage:true})
  await page.getByRole('button',{name:'学习提醒',exact:true}).click();await page.getByText('截止提醒设置',{exact:true}).waitFor();await page.getByRole('button',{name:'保存设置',exact:true}).click();await page.getByText('提醒设置已同步',{exact:true}).waitFor()
  await page.screenshot({path:path.join(output,'reminders.png'),fullPage:true})
  await page.keyboard.press('Escape')
  // 全部条目早已完成时应拒绝创建，避免新计划直接归档；仅在克隆库验收。
  const learnedItem=items.find(i=>completed.has(Number(i.id)));assert(learnedItem)
  await api('POST',`/learning-plans/${created.planId}/cancel`)
  const rejected=await fetch('http://127.0.0.1:8081/api/learning-plans',{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({planName:'全部已学完应拦截',startDate:today,endDate:later,items:[{itemPk:learnedItem.id,targetDate:later,sortOrder:1}]})});assert.equal(rejected.status,409)
  // 先创建待完成任务，再通过原学习接口在测试库完成；不写入日常库。
  await api('POST','/learning-plans',{planName:completionPlanName,startDate:today,endDate:later,items:[{itemPk:pendingItem.id,targetDate:later,sortOrder:1}]})
  await api('POST','/learning-progress/enter',{itemPk:pendingItem.id})
  // 原接口 DTO 使用 snake_case 的 JSON 字段，不修改生产接口以迁就测试。
  await api('POST','/learning-progress/time',{item_pk:pendingItem.id,delta_sec:600})
  await api('POST','/learning-progress/complete',{itemPk:pendingItem.id,completeRemark:'独立测试库浏览器验收'})
  await page.goto(origin+'/learning-plan');await page.getByText(completionPlanName,{exact:true}).click()
  await page.getByRole('button',{name:'计划完成总结',exact:true}).first().click();await page.getByRole('dialog',{name:'计划完成总结'}).getByRole('textbox').first().fill('浏览器完成总结验收');await page.getByRole('button',{name:'保存并同步',exact:true}).click();await page.getByText('完成反馈已保存，可在其他设备查看',{exact:true}).waitFor()
  await page.screenshot({path:path.join(output,'completion-feedback.png'),fullPage:true})
  // 公共导航及原学习卡片行为仍存在。
  for(const name of ['学习数据面板','能力考核','项目贡献者']){await page.getByRole('link',{name,exact:true}).click();await until(async()=>await page.locator('.main-content').innerText().then(t=>t.trim().length>0),name)}
  await page.goto(origin+'/knowledge');await page.locator('.knowledge-card').first().waitFor()
  const card=page.locator('.knowledge-card').first();assert(await card.getByRole('button',{name:'开始学习',exact:true}).count());assert(await card.getByRole('button',{name:'开始练习',exact:true}).count())
  await card.getByRole('button',{name:'查看详情',exact:true}).click();await page.getByRole('dialog').waitFor()
  assert.deepEqual(errors,[],'浏览器不应出现页面异常')
  fs.writeFileSync(path.join(output,'browser-results.json'),JSON.stringify({passed:true,checks:['知识卡片收藏','收藏整理及检索','两个独立浏览器上下文同步','学习计划查看和修改','完成总结表单','提醒设置','原导航及详情弹窗'],pageErrors:errors},null,2))
  console.log('浏览器验收通过：收藏、检索、跨设备、计划、提醒、原导航和详情弹窗')
})().catch(async error=>{testFailed=true;console.error(error.message);const page=browser?.contexts()[0]?.pages()[0];if(page){await page.screenshot({path:path.join(output,'failure.png'),fullPage:true});console.error((await page.locator('body').innerText()).slice(-2500))}}).finally(async()=>{await browser?.close();await vite?.close();backend?.kill();if(testFailed)process.exitCode=1})
