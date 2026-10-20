# LabCore

LabCore 是面向课程学习、在线实验和能力考核的学习平台。当前本地版本保留完整业务代码和静态资源，但初始化方式向整理版收敛。

## 知识点掌握度计算与薄弱知识点识别

- 知识点掌握度计算：学习记录与考核成绩换算为掌握度分值（0~100），区分"未学"与"学得差"
- 薄弱知识点识别：按预警阈值分级预警，管理员端全员风险总览与知识点平均掌握度图，学生端优先补强建议

## 当前约定

- 数据库初始化以 `src/main/resources/db/labcore_full_init.sql` 为主入口。
- `labcore_full_init.sql` 会创建 `labcore` 数据库、导入学习卡片数据，并初始化一条管理员账号。
- 应用启动时不再自动创建管理员账号。
- 在线练习模板放在 `src/main/resources/learning-templates`，默认通过 `classpath:learning-templates` 读取。
- 作业提交文件和作业材料属于运行期数据，保存在 `data/assignment-submissions` 和 `data/assignment-materials`。
- 本地静态资源暂不按 `D:\Code\Java\labcore-code` 裁剪。

## 目录

```text
src/main/java/                         后端源码
src/main/resources/application.properties.example
                                        后端配置示例
src/main/resources/db/labcore_full_init.sql
                                        当前推荐的一体化初始化脚本
src/main/resources/db/verify_initialization.sql
                                        初始化结果只读校验
src/main/resources/learning-templates/  在线练习 Notebook 模板
src/main/resources/static/lite/         JupyterLite 静态运行环境
src/main/resources/static/experiments/  打包进后端的 Notebook 示例或备用资源
data/assignment-submissions/            学生作业提交文件
data/assignment-materials/              作业材料文件
web-study-1.0.1/package/                前端 Vue/Vite 项目
ops/                                    部署脚本和说明
```

## 数据库重建

先备份当前库，再导入初始化脚本：

```bash
mysqldump -uroot -p --databases labcore > backup_labcore.sql
mysql --binary-mode=1 -uroot -p < src/main/resources/db/labcore_full_init.sql
mysql -uroot -p labcore < src/main/resources/db/verify_initialization.sql
```

`verify_initialization.sql` 只读取数据并输出 `PASS/FAIL`，不会修改数据库。

## 本地启动

后端：

```powershell
mvn spring-boot:run
```

前端：

```powershell
cd web-study-1.0.1\package
npm install
npm run dev -- --host 127.0.0.1 --port 5173
```

默认访问地址：

```text
前端：http://127.0.0.1:5173
后端：http://127.0.0.1:8080
```

## 构建

```powershell
cd web-study-1.0.1\package
npm run build
cd ..\..
mvn -DskipTests package
```
