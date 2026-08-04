# 知联汇学 —— 面向高中师生的交叉学科知识图谱平台

> 基于 SpringBoot + Vue3 + Neo4j + MySQL 构建的交叉学科知识图谱教学辅助平台，服务于高中物理、化学、生物、航天拓展、环境（水污染治理）等学科的交叉关联教学。

---

## 目录

- [项目简介](#项目简介)
- [一键部署](#一键部署)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速启动](#快速启动)
- [Neo4j 图数据库使用指南](#neo4j-图数据库使用指南)
- [数据库初始化](#数据库初始化)
- [功能说明](#功能说明)
- [用户角色与权限](#用户角色与权限)
- [API 接口文档](#api-接口文档)
- [学科配色方案](#学科配色方案)
- [常见问题](#常见问题)

---

## 项目简介

本平台面向高中师生，围绕交叉学科知识体系，提供以下核心能力：

- **知识图谱可视化**：基于 AntV G6 实现交叉学科知识点的图谱展示，支持缩放、拖拽、节点交互、学科配色
- **智能问答**：KBQA 模式（语义解析 + Cypher 查询）与 GraphRAG 模式（魔搭 MiniMax 云端大模型 + 知识图谱检索增强问答）
- **知识库管理**：教师可管理知识点、学科分类、交叉关系，支持 Excel 批量导入导出
- **学习辅助**：知识溯源、笔记收藏、习题练习、学习记录
- **系统管理**：用户管理、权限管控、操作日志

---

## 一键部署（推荐）

> 适用于 **Ubuntu 22.04/24.04、Debian 12**（CentOS/RHEL 部分支持）。一条命令自动完成：安装 JDK21/Maven/Node/MySQL/Neo4j/Nginx → 生成随机密钥 → 建库 → 构建前后端 → 配置 systemd + Nginx → 启动。

```bash
# 方式 A：全新服务器（自动 clone 源码并构建）
sudo bash install.sh

# 方式 B：已 clone 源码（推荐）
cd ~/zhi-platform-src && sudo bash install.sh

# 方式 C：已有 deploy 部署包
cd deploy && sudo bash install.sh
```

### 参数示例

所有参数均为可选，**不传则自动生成随机安全密码**并写入 `/etc/zhi-platform.env`：

| 参数 | 作用 | 示例 |
|------|------|------|
| `MYSQL_PASSWORD` | MySQL root 密码 | `MYSQL_PASSWORD=wjx1314520` |
| `NEO4J_PASSWORD` | Neo4j neo4j 密码 | `NEO4J_PASSWORD=MyNeo4j2026!` |
| `JWT_SECRET` | 登录令牌签名密钥（≥32 字符） | `JWT_SECRET='MyJwtSecret2026ForZhiPlatform!@#'` |
| `MODELSCOPE_API_KEY` | 魔搭 AI key（问答/文档抽取） | `MODELSCOPE_API_KEY=ms-xxxxxxxx` |
| `PADDLE_OCR_TOKEN` | PaddleOCR token（扫描件 OCR） | `PADDLE_OCR_TOKEN=xxxxxxxx` |
| `SERVER_NAME` | Nginx 域名或 IP | `SERVER_NAME=kg.example.com` |

```bash
# 示例 1：最小部署（自动生成所有随机密码，最省事）
sudo bash install.sh

# 示例 2：指定数据库密码 + AI 密钥
sudo MYSQL_PASSWORD=wjx1314520 NEO4J_PASSWORD=MyNeo4j2026 \
     MODELSCOPE_API_KEY=ms-xxxxxxxx PADDLE_OCR_TOKEN=xxxxxxxx \
     bash install.sh

# 示例 3：指定 JWT 密钥 + 域名（正式上线推荐）
sudo JWT_SECRET='MyJwtSecret2026ForZhiPlatform!@#' SERVER_NAME=kg.example.com \
     bash install.sh
```

### 部署后

- 访问 `http://服务器IP/`，管理员账号 `admin / 123456`（**首次登录后请立即修改密码**）
- 密钥文件：`/etc/zhi-platform.env`（权限 600，含自动生成的随机密码）
- 日常维护：`sudo bash restart.sh restart|status|logs|backend`
- 手动分步部署：见 [`DEPLOY.md`](DEPLOY.md)

---

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **前端框架** | Vue 3 + Vite | 响应式 SPA 应用 |
| **UI 组件库** | Element Plus | 表格、表单、弹窗等 |
| **图谱可视化** | AntV G6 | 力导向图、节点交互 |
| **状态管理** | Pinia | 用户状态、图谱状态 |
| **路由** | Vue Router | 含权限守卫 |
| **HTTP 客户端** | Axios | 请求拦截、JWT 注入 |
| **后端框架** | Spring Boot 3.2.5 | Java 17 |
| **安全框架** | Spring Security + JWT | 角色权限控制 |
| **图数据库** | Neo4j 4.x 社区版 | 存储知识点节点与交叉关系 |
| **关系数据库** | MySQL 8.0 | 存储用户、习题、日志等 |
| **ORM** | Spring Data JPA + SDN | 双数据库访问 |
| **文件处理** | Apache POI | Excel 批量导入 |

---

## 项目结构

```
知识图谱/
├── frontend/                          # 前端 Vue3 项目
│   ├── src/
│   │   ├── api/                       # API 请求封装
│   │   │   ├── request.js             # Axios 实例（JWT 拦截器）
│   │   │   ├── user.js                # 用户相关接口
│   │   │   ├── knowledge.js           # 知识库接口
│   │   │   ├── graph.js               # 图谱数据接口
│   │   │   ├── qa.js                  # 智能问答接口
│   │   │   └── exercise.js            # 习题与学习记录接口
│   │   ├── assets/styles/global.scss  # 全局样式 + 学科配色变量
│   │   ├── components/
│   │   │   ├── layout/
│   │   │   │   ├── AppLayout.vue      # 主布局（侧边栏 + 导航栏 + 内容区）
│   │   │   │   ├── Sidebar.vue        # 侧边栏菜单（师生差异化）
│   │   │   │   └── Navbar.vue         # 顶部导航栏
│   │   │   ├── KnowledgeGraph.vue     # AntV G6 图谱核心组件
│   │   │   ├── NodeDetail.vue         # 节点详情弹窗
│   │   │   └── SearchFilter.vue       # 通用筛选组件
│   │   ├── router/index.js            # 路由配置 + 权限守卫
│   │   ├── store/
│   │   │   ├── user.js                # 用户状态（token / 角色 / 信息）
│   │   │   └── graph.js               # 图谱配置状态
│   │   └── views/                     # 10 个核心页面
│   │       ├── Login.vue              # 登录注册
│   │       ├── Dashboard.vue          # 首页仪表盘
│   │       ├── GraphView.vue          # 图谱可视化
│   │       ├── QA.vue                 # 智能问答
│   │       ├── Learning.vue           # 学习辅助（知识溯源）
│   │       ├── teacher/
│   │       │   ├── KnowledgeBase.vue  # 知识库管理
│   │       │   ├── GraphConfig.vue    # 图谱配置
│   │       │   ├── ExerciseManage.vue # 习题管理
│   │       │   └── SystemAdmin.vue    # 系统管理（用户 / 日志）
│   │       └── student/
│   │           └── StudyCenter.vue    # 个人学习中心
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
│
├── backend/                           # 后端 SpringBoot 项目
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/zhiplatform/
│       │   ├── ZhiPlatformApplication.java  # 启动类
│       │   ├── config/
│       │   │   ├── SecurityConfig.java      # Spring Security 配置
│       │   │   ├── Neo4jConfig.java         # Neo4j 配置
│       │   │   └── CorsConfig.java          # 跨域配置
│       │   ├── controller/                  # 6 个 Controller
│       │   ├── service/                     # 6 个 Service
│       │   ├── repository/
│       │   │   ├── mysql/                   # 8 个 JPA Repository
│       │   │   └── neo4j/                   # 2 个 Neo4j Repository（Cypher 查询）
│       │   ├── entity/
│       │   │   ├── mysql/                   # 8 个 MySQL 实体
│       │   │   └── neo4j/                   # 2 个 Neo4j 实体
│       │   ├── dto/                         # 6 个数据传输对象
│       │   ├── security/
│       │   │   ├── JwtUtil.java             # JWT 工具类
│       │   │   ├── JwtFilter.java           # JWT 过滤器
│       │   │   └── UserDetailsServiceImpl.java
│       │   └── common/
│       │       ├── Result.java              # 统一响应封装
│       │       └── PageResult.java          # 分页响应
│       └── resources/
│           └── application.yml              # 应用配置
│
├── docs/                              # 需求设计文档
│   ├── 面向高中师生的交叉学科知识图谱平台（功能.md
│   ├── 面向高中师生的交叉学科知识图谱平台（前端.md
│   └── 面向高中师生的交叉学科知识图谱平台（后端.md
└── README.md                          # 本文件
```

---

## 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+（推荐 21） | 后端编译目标 17；Neo4j 2025+/最新版需 21 |
| Maven | 3.6+ | 构建后端（或使用 `mvnw.cmd`） |
| Node.js | 18+ | 构建前端 |
| MySQL | 8.0+ | 关系数据库 |
| Neo4j | 4.x / 5.x 社区版 | 图数据库 |

---

## 快速启动

### 1. 启动 MySQL

确保 MySQL 服务运行中。默认配置（本地开发）：
- 地址：`localhost:3306`
- 用户名：`root`
- 密码：`wjx1314520`（来自项目根目录 `.env`）
- 数据库：`zhi_platform`（自动创建）

> 数据库/Neo4j/JWT 密钥均通过环境变量读取（`application.yml` 不再写死默认值）：本地在项目根目录 `.env` 配置，生产在 `/etc/zhi-platform.env` 配置。

### 2. 启动 Neo4j

Neo4j 解压版（社区版 4.4.4）位于 `D:\neo4j-community-4.4.4`。

首次启动需初始化密码（仅一次）：
```bash
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot
D:\neo4j-community-4.4.4\bin\neo4j-admin.bat set-initial-password neo4j
```

每次启动 Neo4j（**必须使用 JDK 11**，JDK 17/25 会报警告但也能用）：
```bash
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot
D:\neo4j-community-4.4.4\bin\neo4j.bat console
```

或用项目自带脚本：
```bash
.\scripts\start-neo4j.ps1
```

默认配置：
- Bolt 端口：`7687`（程序连接用）
- HTTP 端口：`7474`（浏览器访问用）
- 用户名/密码：`neo4j` / `neo4j`

> Neo4j 4.4.4 官方仅支持 JDK 11，用 JDK 17/25 会打印 "unsupported Java runtime" 警告但不影响功能。已在 `conf/neo4j.conf` 中注释了 `-XX:-UseBiasedLocking` 等不兼容参数。

### 3. 启动后端

JDK 17 和 Maven 已安装的情况下：

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

或使用已构建好的 jar：
```bash
java -jar target/zhi-platform-1.0.0.jar
```

后端启动成功后显示：
```
Started ZhiPlatformApplication in X.XXX seconds
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

启动成功后显示：
```
Local: http://localhost:3000/
```

### 5. 访问系统

| 地址 | 说明 |
|------|------|
| http://localhost:3000 | 前端页面 |
| http://localhost:8080 | 后端 API |
| http://localhost:7474 | Neo4j Browser 管理界面 |

### 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 (ADMIN) |
| 3060904182 | 123456 | 学生 |

---

## 环境变量（.env）

项目根目录提供 `.env`（已被 `.gitignore` 忽略，不会提交）与 `.env.example`（模板，可提交）。

| 变量 | 说明 |
|------|------|
| `PADDLE_OCR_TOKEN` | 云端 PaddleOCR（AI Studio）token，用于扫描件/图片 OCR |
| `MODELSCOPE_API_KEY` | 魔搭 ModelScope API key（云端 LLM 知识抽取，默认模型 MiniMax-M1-80k） |

使用方式：
1. 复制 `.env.example` 为 `.env` 并填入真实值；
2. 启动脚本（`scripts/start-all.ps1` / `scripts/start-backend.ps1`）会自动加载 `.env` 到环境变量；
3. 后端 `application.yml` 通过 `${PADDLE_OCR_TOKEN:}` 读取（未设置时为空，OCR 会提示失败，不会误用假 token）。

> ⚠️ 不要把 `.env` 或真实 token 提交到公开仓库；如需在 CI/服务器上运行，直接在环境中配置同名变量即可。
## Neo4j 图数据库使用指南

### 什么是图数据库？

Neo4j 是**图数据库**（Graph Database），它用**节点**（Node）和**关系**（Relationship）来存储数据，而非传统的行列表。非常适合知识点之间的交叉关联场景。

- **节点**：代表一个知识点（如"牛顿第二定律"、"光合作用"）
- **关系**：代表知识点之间的关联（如"基于原理"、"跨学科关联"）
- **标签**：节点的分类（如 `Knowledge` 代表知识节点）
- **属性**：节点的附加信息（如 `name`、`subject`、`difficulty`）

### 如何管理 Neo4j 数据

**方式一：Neo4j Browser 可视化界面**

1. 打开 http://localhost:7474
2. 在登录页顶部输入框填入连接地址 `bolt://localhost:7687`（默认填的 `neo4j://` 连不上 4.x）
3. 下方输入用户名 `neo4j`，密码 `neo4j`，点击 Connect
4. 连接后在顶部命令行输入 Cypher 查询（需完整语句，不要只打 "Cypher"）

常用查询示例：

```cypher
// 查看所有知识点
MATCH (n:Knowledge) RETURN n

// 查看某个学科的知识点
MATCH (n:Knowledge {subject:'高中物理'}) RETURN n

// 查看跨学科知识点
MATCH (n:Knowledge {isCross:true}) RETURN n

// 查看关联关系
MATCH (n)-[r]-(m) RETURN n, r, m LIMIT 30

// 查看两个知识点之间的关联路径
MATCH (a:Knowledge {name:'牛顿第二定律'}),(b:Knowledge {name:'光合作用'}),
p = shortestPath((a)-[*..6]-(b))
RETURN p
```

**方式二：Cypher Shell 命令行**

```bash
"D:\neo4j-community-4.4.4\bin\cypher-shell.bat" -u neo4j -p neo4j "MATCH (n:Knowledge) RETURN count(n)"
```

### 已导入的示例数据

当前已导入 **16 个知识点节点** 和 **19 条交叉关系**，覆盖：

- **高中物理**：牛顿运动定律、万有引力、电磁感应、能量守恒
- **高中化学**：化学键、氧化还原反应、化学反应速率、有机化学基础
- **高中生物**：细胞代谢、遗传与变异、生态系统、光合作用
- **航天拓展**：火箭推进原理、太空环境
- **环境科普**：碳循环、温室效应
- **交叉关系**：物理↔化学、物理↔生物、化学↔生物、航天↔物理、环境↔化学等

### 管理 Neo4j 服务

```bash
# 启动（需指定 JDK 11）
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot
"D:\neo4j-community-4.4.4\bin\neo4j.bat" console

# 或使用快捷脚本
.\scripts\start-neo4j.ps1

# 查看状态
"D:\neo4j-community-4.4.4\bin\neo4j.bat" status

# 停止：按 Ctrl+C（前台运行），或在任务管理器结束 java 进程
```

---

## 数据库初始化

### MySQL

JPA 配置了 `ddl-auto: update`，启动时会自动创建表并将数据插入。预置数据包括：

**用户表** `sys_user`：

| id | username | role | password |
|----|----------|------|----------|
| 1 | admin | TEACHER | Bcrypt(123456) |
| 3 | 3060904182 | STUDENT | Bcrypt(123456) |

**学科表** `subject`：5 个学科 + 3 个交叉专题

### Neo4j

示例数据已直接导入 Neo4j 数据库。如需重置，在 Neo4j Browser 中执行：

```cypher
MATCH (n) DETACH DELETE n
```

然后重新执行 `neo4j-init.cypher` 中的语句。

---

## 自动化测试

项目已配置前后端自动化测试，覆盖核心功能逻辑。

### 后端测试（Spring Boot + JUnit 5 + Mockito）

- **位置**：`backend/src/test/java/com/zhiplatform/`
- **覆盖**：Service 层单元测试、Controller 层 MockMvc 集成测试、JWT 工具测试
- **特点**：不依赖外部 MySQL/Neo4j，全部使用 Mock 隔离

运行方式：

```bash
cd backend
mvn test
```

### 前端测试（Vitest + Vue Test Utils）

- **位置**：`frontend/src/**/__tests__/`
- **覆盖**：Pinia Store、API 模块、核心组件、路由守卫
- **特点**：使用 Vitest + jsdom，Mock 后端接口

运行方式：

```bash
cd frontend
npm install
npm test
```

### 一键运行全部测试

Windows PowerShell：

```powershell
.\scripts\run-tests.ps1
```

Linux/macOS：

```bash
./scripts/run-tests.sh
```

### CI/CD

项目已配置 GitHub Actions 工作流（`.github/workflows/ci.yml`），在推送或提交 PR 时自动运行前后端测试与前端构建。

---

## 功能说明

### 1. 登录注册

- 支持教师/学生角色注册
- JWT Token 认证，24 小时有效期
- 登录后根据角色展示不同菜单

### 2. 首页仪表盘

- **教师视图**：快捷入口（知识库管理、图谱配置、习题管理、系统管理）
- **学生视图**：快捷入口（知识图谱、智能问答、学习辅助、个人中心）

### 3. 知识图谱可视化

- 基于 AntV G6 的力导向图布局
- 学科配色：物理蓝 `#2563EB`、化学橙 `#D97706`、生物绿 `#16A34A`、航天紫 `#7C3AED`、环境青 `#0D9488`、跨学科紫 `#4F46E5`
- 节点底色按学科区分；跨学科节点以紫色描边 + 更大尺寸标识
- 支持：缩放、拖拽、悬浮详情、双击展开关联节点
- 筛选：按学科、关键词多条件筛选
- 导出：PNG 图片、JSON 数据

### 4. 智能问答

- **KBQA 模式**：自然语言输入 → 名称直查 / 关键词模糊匹配（名称·描述·标签）→ 返回答案 + 关联图谱高亮；检索不到时自动升级为 AI 回答
- **GraphRAG 模式**：知识图谱检索（相关知识点/学科）→ 组装上下文 → 魔搭 MiniMax 生成带引用来源的跨学科答案（`.env` 配置 `MODELSCOPE_API_KEY`）
- 问答历史自动保存（教师/学生均可），可在个人学习中心查看
- 聊天式界面，问答结果联动右侧知识图谱高亮

### 5. 知识库管理（教师）

- 学科与交叉专题管理
- 知识点 CRUD（概念、公式、实验等类型）
- 交叉关系管理（新增/删除知识点间跨学科关系）
- Excel 批量导入知识点（知识库页可「下载模板」，含表头/示例行/填写说明；可选「关联知识点」「交叉关系」列批量建关系）
- 文档解析（Word / PDF / 图片 -> 文本）：文本型 PDF 本地 PDFBox 提取，扫描件/图片走云端 PaddleOCR（token 见 application.yml，可用环境变量 PADDLE_OCR_TOKEN 覆盖）
- 文档导入知识图谱：知识库管理页「文档导入」Tab 上传 Word/PDF/图片，后端异步任务 + 前端轮询（每 4 秒）展示真实进度（解析/OCR → AI 抽取 → 存草稿）；抽取结果自动存为「待确认导入」草稿（切换页面不丢失）→ 教师勾选确认 → 写入 Neo4j；「已导入的文档」支持一键删除该文档解析出的知识点与关系

### 6. 更多功能

- 图谱配置、习题管理、学习辅助、笔记收藏、学习中心、系统管理

---

## 用户角色与权限

| 功能模块 | 管理员 (ADMIN) | 教师 (TEACHER) | 学生 (STUDENT) |
|----------|:--------------:|:--------------:|:--------------:|
| 知识图谱查看 | ✅ | ✅ | ✅ |
| 智能问答 | ✅ | ✅ | ✅ |
| 学习辅助 | ✅ | ✅ | ✅ |
| 个人学习中心 | ✅ | ✅ | ✅ |
| 知识库管理 | ✅ | ✅ | ❌ |
| 图谱配置 | ✅ | ✅ | ❌ |
| 习题管理 | ✅ | ✅ | ❌ |
| 系统管理 | ✅ | ❌ | ❌ |
| 批量导入 | ✅ | ✅ | ❌ |

> 权限控制采用双重校验：前端路由守卫 + 后端 Spring Security 拦截器。
> 教师注册需管理员在「系统管理」页审核通过后方可登录；管理员可修改用户身份（角色）、重置密码、启用/禁用、删除账号（不显示密码明文或哈希）。

---

## 常见问题

### Q: Neo4j 启动报 "Unrecognized VM option 'UseBiasedLocking'"？

Neo4j 4.4.4 的默认 JVM 配置在新版 JDK（15+）上有不兼容参数。已在 `conf/neo4j.conf` 中注释了 `-XX:-UseBiasedLocking` 和 `-XX:+TrustFinalNonStaticFields`。使用 Java 11 或 17 启动可避免此问题。

### Q: Neo4j 连接失败？

确保 Neo4j 已启动，且 `application.yml` 中的配置正确：
```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: neo4j
```

### Q: MySQL 连接失败？

检查 MySQL 服务是否启动，密码是否正确：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhi_platform?createDatabaseIfNotExist=true&serverTimezone=Asia/Shanghai
    username: root
    password: wjx1314520
```

### Q: 前端页面空白或报错？

确保后端已启动。Vite 代理已配置 `/api` → `http://localhost:8080`。

### Q: 如何重置所有密码？

密码使用 BCrypt 加密存储，可注册新账号或通过数据库直接修改。

### Q: 如何添加新的知识点到图谱？

教师登录后进入「知识库管理」页面，点击新增知识点即可。新增的知识点会自动同步到 Neo4j 图数据库。也可以使用 Excel 批量导入。

### Q: 如何查看图数据库中的原始数据？

打开 http://localhost:7474（Neo4j Browser），用 `neo4j` / `neo4j` 登录，执行 `MATCH (n:Knowledge) RETURN n LIMIT 25`。













