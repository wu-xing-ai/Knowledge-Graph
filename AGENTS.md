# AGENTS.md — 知联汇学知识图谱平台

> 本文件是 AI 与协作者修改本仓库的完整工作约定。项目面向高中师生交叉学科教学，采用 `backend`（Spring Boot）、`frontend`（Vue 3 + Vite）、`model-service`（FastAPI）、MySQL、Neo4j 组合；Nginx 只用于生产反向代理。

## 1. 项目概览与技术栈

### 核心能力
- 学生：浏览与检索知识点、按学科筛选、多关键词生成关联图谱、学习任务与班级互动。
- 教师：管理班级与学生小组、发布任务、维护知识点与交叉关系、导入文档并确认 AI 抽取结果。
- 管理员：维护用户、学科与系统基础数据。
- AI 能力：LLM 对话/抽取与图片 OCR，统一由独立 `model-service` 承接。

### 技术栈
| 层 | 技术 | 版本口径 |
| --- | --- | --- |
| 后端 | Spring Boot, Spring Security/JWT, Spring Data JPA, Spring Data Neo4j | Boot 3.2.5 / Java 17 |
| 前端 | Vue 3 Composition API, Vite, Element Plus, Pinia, Vue Router, Axios, AntV G6 | Node 18+ / G6 v4 |
| 模型服务 | FastAPI, Uvicorn, httpx | Python 3.12（CI 口径） |
| 存储 | MySQL 8, Neo4j 4.4.4 | MySQL 为关系主数据源 |
| 部署 | Maven, Nginx, systemd 模板, Docker Compose | 生产入口为 Nginx 80 |

## 2. 架构与数据流

```text
Browser (Vue SPA)
  -> Nginx(生产) 或 Vite Proxy(开发)
    -> backend:8080 /api/**
      -> MySQL: 业务事实
      -> Neo4j: 图谱事实与教学组织镜像
      -> model-service:8000
        -> ModelScope LLM
        -> OCR Provider
```

- 浏览器只访问前端与 `/api`；后端是唯一业务入口。
- 前端普通请求经 Axios 统一携带 JWT；SSE 使用 `fetch` 并显式带 Authorization。
- 后端调用模型服务时只读取 `MODEL_SERVICE_URL`；云端模型密钥只在 `model-service` 进程读取。
- MySQL 保存用户、学科、班级、任务等可审计关系事实；Neo4j 保存知识点节点、知识关系以及教学组织镜像。两者职责不同，不能互相替代。

## 3. 目录地图与入口

```text
.
├── backend/
│   └── src/main/java/com/zhiplatform/
│       ├── common/          # 统一响应、异常、工具
│       ├── config/          # Spring 配置
│       ├── controller/      # HTTP 边界，保持薄层
│       ├── dto/             # 请求/响应对象
│       ├── entity/          # JPA 实体
│       ├── repository/      # JPA / Neo4j Repository
│       ├── security/        # JWT、过滤器、用户上下文
│       └── service/         # 业务逻辑、跨库一致性
├── frontend/
│   └── src/
│       ├── api/             # 全部后端请求封装
│       ├── assets/          # 静态资源
│       ├── components/      # 可复用组件
│       ├── router/          # 路由与角色守卫
│       ├── store/           # Pinia
│       ├── utils/           # 配色、格式化等工具
│       └── views/           # 页面
├── model-service/
│   ├── app/main.py          # FastAPI 入口
│   ├── app/config.py        # 模型与环境配置
│   ├── app/llm.py           # OpenAI 兼容网关
│   └── app/ocr.py           # OCR 网关
├── scripts/                 # Windows/Linux 启动、测试、部署脚本
├── docs/                    # 设计与功能文档
└── docker-compose.yml
```

### 关键入口
- 后端应用：`backend/src/main/java/com/zhiplatform/ZhiPlatformApplication.java`
- 后端配置：`backend/src/main/resources/application.yml`
- 数据初始化：`backend/src/main/resources/schema.sql`、`neo4j-init.cypher`
- 前端应用：`frontend/src/main.js`
- 前端请求基座：`frontend/src/api/request.js`
- 路由守卫：`frontend/src/router/index.js`
- Vite 构建/代理：`frontend/vite.config.js`
- FastAPI 入口：`model-service/app/main.py`

## 4. 常用命令

### 一键与分模块命令
```powershell
# 根目录一键测试（Windows）
.\scripts\run-tests.ps1

# 后端
cd backend
mvn test
mvn clean package -DskipTests
$env:MAVEN_OPTS="-Dfile.encoding=UTF-8"
java -jar target/zhi-platform-1.0.0.jar

# 前端
cd frontend
npm install
npm test
npm run dev
npm run build
npm run preview

# 模型服务
cd model-service
python -m pip install -r requirements.txt
python -m pytest tests -q
python -m uvicorn app.main:app --port 8000
curl http://localhost:8000/health
```

### 本地全栈启动顺序
1. 启动 MySQL `3306`。
2. 启动 Neo4j Bolt `7687` / Browser `7474`。
3. 可选启动 model-service `8000`；不用 AI 功能时后端仍应能处理非模型接口。
4. 启动 backend `8080`。
5. 启动 frontend `3000`。
6. 打开 `http://localhost:3000`。

快捷脚本：
```powershell
.\scripts\start-neo4j.ps1     # 仅 Neo4j
.\scripts\start-backend.ps1   # 构建并启动后端
.\scripts\start-all.ps1       # 按顺序拉起全栈，自动 npm install
```

## 5. 环境变量与本地配置

- 先复制 `.env.example` 为根目录 `.env`；`.env` 已被 Git 忽略，禁止 `git add -f`。
- 必须重点检查：`SPRING_DATASOURCE_PASSWORD`、`SPRING_NEO4J_AUTHENTICATION_PASSWORD`、`JWT_SECRET`、`MODELSCOPE_API_KEY`、`PADDLE_OCR_TOKEN`、`MODEL_SERVICE_URL`。
- `JWT_SECRET` 至少 32 字符；为空时安全组件会 fail-fast，不要为了本地绕过而删除校验。
- `MODEL_SERVICE_URL` 默认指向 `http://localhost:8000`；生产由 `/etc/zhi-platform.env` 注入。
- Windows 下后端构建需要 UTF-8：`MAVEN_OPTS=-Dfile.encoding=UTF-8`。

## 6. JDK、数据库与服务端口

### JDK 分离要求
- Backend/Maven/CI：JDK 17。
- 本机 Neo4j 4.4.4：JDK 11。
- `scripts/start-neo4j.ps1` 和 `scripts/start-backend.ps1` 中包含当前机器的 JDK 绝对路径；换机器必须同步调整，不要混用 `JAVA_HOME`。
- 典型症状：Neo4j 用错 JDK 会报 `UseBiasedLocking` 相关错误；后端用错 JDK 会编译失败。

### 端口
| 服务 | 端口 | 说明 |
| --- | ---: | --- |
| MySQL | 3306 | 关系库 |
| Neo4j Browser | 7474 | 连接地址填 `bolt://localhost:7687` |
| Neo4j Bolt | 7687 | 不要在 4.x Browser 里填 `neo4j://` |
| Backend | 8080 | 业务 API |
| Model Service | 8000 | LLM/OCR |
| Frontend Dev | 3000 | `/api` 代理到 8080 |
| Nginx | 80 | 生产入口 |

Docker Compose 中除 Nginx 外的存储和服务端口仅绑定本机；不要随意改成公网监听。

## 7. API、响应与错误约定

- 统一响应使用 `Result<T>`，字段固定为 `{ code, message, data }`；成功码是 `200`。
- 禁止新增 `msg`、`success` 或其他平行响应字段。
- Controller 返回 `Result<T>`，不做复杂业务计算。
- 参数校验失败返回明确的中文 `message`；前端拦截器展示该字段。
- 新增或修改接口时同步更新对应 `src/api/*.js` 封装、页面调用和相关测试。
- 分页、筛选、排序参数命名沿用所在模块既有风格；不引入同一含义的第二套参数名。

## 8. 后端开发约定

### 分层与依赖
- 固定方向：`controller -> service -> repository`。
- Controller 只做路由、参数接收、登录身份提取和响应包装。
- 业务规则、归属校验、状态流转放在 Service。
- 优先构造器注入；避免 Service 直接依赖 HttpServletRequest 或在实体里写业务流程。
- 第三方 LLM/OCR 只通过 `ModelServiceClient` / `OcrService` 访问，不允许散落直连外部服务。

### MySQL 与 Neo4j 一致性
- MySQL 是业务事实源；Neo4j 是图查询视图和教学组织镜像。
- 新增、编辑、批量导入、AI 导入知识点时，必须解析并写入 `subjectId`，否则图谱学科筛选会静默失效。
- 知识点名称、学科、前置/相关关系变化时，确保 Neo4j 节点与关系同步更新。
- 班级、小组、任务等教学组织如果参与图谱查询，也要检查镜像是否需要更新。
- 可降级的图同步失败不能破坏 MySQL 主事务，但必须有日志、状态或补偿路径；不要静默吞掉异常。

### 异步与长任务
- 文档抽取等异步任务要有明确归属人、状态机和超时/失败态，例如 `PARSING / EXTRACTING / SAVING / DONE / FAILED`。
- 轮询接口必须校验当前用户是否有权读取该任务结果。
- 任务完成后进入教师确认环节才写最终图数据；不要把中间抽取结果直接当成已确认知识。

## 9. 安全与权限约定

- 密码一律 BCrypt 存储；任何用户列表、详情、VO 在返回前清空 `password`。
- JWT 校验逻辑集中在 `security` 包；不要在业务 Controller 自行解析 token。
- 新增受保护端点必须同步检查 `SecurityConfig` 显式规则：
  - 教师/管理员可用：`hasAnyRole("TEACHER", "ADMIN")`。
  - 仅管理员可用：`hasRole("ADMIN")`。
  - 学生可见接口要明确允许 `STUDENT` 或登录用户，而不是依赖“没写就是放行”的猜测。
- 所有写操作都要校验资源归属：教师只能操作自己班级/资料/任务，学生只能访问被授权内容。
- 文件上传限制类型、大小和用途；下载/预览接口要防止越权拼接路径。
- 日志不得输出密码、token、模型密钥或学生敏感信息。

## 10. 前端开发约定

### 结构与请求
- 页面放 `views`，复用组件放 `components`；所有后端请求封装到 `src/api/*.js`。
- 组件内禁止硬编码 `/api/...`；普通 Axios 请求统一走 `@/api/request`。
- 请求封装自动处理 JWT、`/api` 前缀、401 跳转和非 200 业务提示；已被拦截器提示的错误不要重复弹窗。
- SSE 不能交给普通 Axios 时，使用 `fetch` 并显式携带 Authorization。
- 后端错误字段是 `message`，不是 `msg`。

### Vue 与 UI
- Vue 3 使用 `<script setup>` + Composition API。
- 模板引用的状态、方法、computed 必须在 `<script setup>` 中定义；新增弹窗按钮时同时实现打开、保存、关闭、加载和错误分支。
- 复杂弹窗优先拆成独立组件；同一个领域的数据和方法集中在一个 API 模块中。
- 路由角色守卫继续放在 `router/index.js`；管理员可访问教师页，系统管理仅 ADMIN。
- 学科配色统一引用 `utils/subjectColors.js`，不在页面里重复硬编码颜色。
- AntV G6 会把节点 ID 转成字符串；跨组件比较、选中、高亮时注意类型一致。

## 11. 模型服务契约

- `model-service` 是唯一读取云端模型密钥的服务；Java 与浏览器都不应拿到密钥。
- 保持以下最小契约：
  - `POST /v1/chat/completions`
  - `POST /v1/ocr`
  - `GET /health`
- `/v1/chat/completions` 保持 OpenAI 兼容语义；修改请求、响应、流式事件结构时，必须同步 Java client、SSE 解析、前端渲染和三端测试。
- 模型服务失败时，Java 端要给出可理解的业务错误；不要把上游堆栈原样透给浏览器。

## 12. 测试与 CI

### 必跑范围
```powershell
cd backend; mvn test
cd frontend; npm test
cd model-service; python -m pytest tests -q
```

一键：
```powershell
.\scripts\run-tests.ps1
./scripts/run-tests.sh
```

### 补测原则
- 新增/修改后端业务规则、权限判断、状态机、MySQL-Neo4j 同步时补 Service 单测。
- 新增/修改前端 API 封装、关键交互、角色跳转或图谱转换逻辑时补 Vitest 测试。
- 新增/修改模型契约时补 FastAPI 测试，覆盖成功、上游失败和非法输入。
- Bug 修复先复现失败，再修复并保留回归测试。

### CI
- `.github/workflows/ci.yml` 包含 `backend-test`、`model-service-test`、`frontend-test`、`frontend-build(needs frontend-test)`。
- 触发条件为 `push/PR -> main|master`。
- CI 版本：JDK 17、Node 20、Python 3.12。

## 13. 构建与部署

### 本地打包
```powershell
.\scripts\build-deploy.ps1
```

输出到 `deploy/`，包含 backend jar、schema、Neo4j 初始化、frontend dist、model-service app、Nginx/systemd 模板和 `install.sh`。

### 服务器安装与维护
```bash
sudo bash install.sh
sudo bash restart.sh restart|status|logs|backend
```

支持覆盖变量：
```bash
MYSQL_PASSWORD=... NEO4J_PASSWORD=... JWT_SECRET=... MODELSCOPE_API_KEY=... CLEAN_BUILD=1 SERVER_NAME=... bash install.sh
```

生产配置写入 `/etc/zhi-platform.env`，权限应为 `600`。公开仓库时也不能假设历史提交中的默认密码安全；上线前必须替换。

### Docker
服务器需安装 Docker Compose plugin：
```bash
sudo bash scripts/docker-deploy.sh up|status|logs|restart|down
```

## 14. 故障排查与已知坑

- **Neo4j 连接**：Browser 使用 `bolt://localhost:7687`，账号默认 `neo4j/neo4j`；`neo4j://` 在当前 4.4.4 本地环境可能连不上。
- **JDK 冲突**：Neo4j 用 JDK 11，Backend 用 JDK 17；脚本里的机器专用绝对路径换环境后要改。
- **编码乱码**：Windows Maven 必须保留 `-Dfile.encoding=UTF-8`。
- **旧进程占用**：`start-all.ps1` 会处理旧的 `zhi-platform-1.0.0.jar` 进程；手动重启前先确认 8080 是否仍被占用。
- **JPA 自动建表**：当前使用 `ddl-auto: update`；不要把临时字段随手加到实体里，避免污染表结构。
- **文档导入链路**：Word/PDF 本地解析，图片走 OCR；异步任务轮询成功后进入草稿确认，再写 Neo4j。
- **图谱筛选失效信号**：若新知识点不出现在某学科图谱，优先检查 Neo4j 节点是否有 `subjectId`，而不是只查 MySQL。
- **G6 ID 类型**：图谱交互异常时先打印比较双方的 ID 与类型，确认是否数字/字符串不一致。
- **不要提交产物**：`.env`、`deploy/`、`frontend/dist/`、`backend/target/`、`node_modules/` 都必须留在 Git 外。

## 15. 变更完成标准

一次功能或修复只有同时满足以下几点才算完成：
1. 明确了变更范围，没有无关重构、格式化或移动文件。
2. 后端有权限、归属和数据一致性处理；Controller 保持薄层。
3. 前端模板、状态、方法、API、加载态、空态和错误提示闭环。
4. MySQL 主数据与 Neo4j 图数据同步策略明确；必要时可降级但可观测。
5. 接口契约、前端 API 封装和页面调用一致。
6. 三端相关测试通过：后端 `mvn test`，前端 `npm test`，模型服务 `pytest tests -q`（未触及时说明理由）。
7. 生产构建通过：`mvn clean package` 与 `npm run build`（涉及对应端时）。
8. 文档口径变化已同步 README 或设计文档。
9. 没有新增硬编码密钥、机器专用路径泄漏、越权接口或未清理的调试代码。

## 16. 提交信息建议

- 使用简洁祈使句标题，例如 `fix: persist knowledge subjectId in Neo4j`。
- 正文说明动机、关键方案和影响面；跨 MySQL/Neo4j 的改动要写明兼容与补偿策略。
- 涉及 UI 时描述主要入口；涉及权限时列出角色矩阵变化。

