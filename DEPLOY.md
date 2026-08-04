# 部署指南（GitHub + 服务器）

本平台前端是 Vue 静态文件、后端是 Spring Boot jar。推荐部署方式：

```
GitHub（私有仓库）  --git pull-->  服务器  --构建-->  Nginx(前端) + systemd(后端)
```

## 零、一键部署（推荐，Ubuntu 22.04/24.04 / Debian 12）

提供 `install.sh` 一键脚本：自动安装 JDK21、Maven、Node.js、MySQL 8、Neo4j、Nginx，自动建库、生成随机密钥（JWT/数据库/Neo4j）、配置 systemd 服务与 Nginx 反代，全程无需手动干预。

```bash
# 方式 A：全新服务器（自动 clone 源码并构建）
sudo bash install.sh

# 方式 B：已 clone 源码（推荐）
cd ~/zhi-platform-src && sudo bash install.sh

# 方式 C：已上传 deploy 部署包
cd deploy && sudo bash install.sh
```

可选：用环境变量覆盖默认值（不设则自动生成随机密码并写入 `/etc/zhi-platform.env`）：

```bash
sudo MYSQL_PASSWORD=你的密码 NEO4J_PASSWORD=你的密码 \
     MODELSCOPE_API_KEY=xxx PADDLE_OCR_TOKEN=xxx bash install.sh
```

> 脚本可重复执行（幂等）：已安装的软件自动跳过、已存在的 `/etc/zhi-platform.env` 沿用、Neo4j 已有数据时不重复初始化。完成后访问 `http://服务器IP/`，管理员 `admin / 123456`（首次登录后请立即修改密码）。
## 一、本地推送到 GitHub

### 1. 创建仓库（公开或私有均可，公开仓库必须在服务器上用环境变量覆盖密钥）
在 GitHub 新建一个 **Private** 仓库（不要 Public），例如 `zhi-platform`。

### 2. 关联并推送
```bash
# 在项目根目录执行
git remote add origin https://github.com/wu-xing-ai/Knowledge-Graph.git
git add -A
git commit -m "init: 知联汇学知识图谱平台"
git push -u origin main
```

> 仓库已保证不包含敏感信息：
> - `.env`（真实 API Key）、`deploy/`、`frontend/dist/`、`backend/target/`、`node_modules/` 均被 `.gitignore` 忽略
> - `application.yml` 中的数据库/Neo4j/JWT 密钥已改为**必须从环境变量读取（默认值为空）**：本地用 `.env`、生产用 `/etc/zhi-platform.env` 提供。`JWT_SECRET` 未设置时登录会报错（故意 fail-fast，防止无密钥运行）
> - ⚠️ 仓库如果是公开的：虽然代码里已不再包含默认密钥，但 **git 历史里仍有旧默认值**，因此生产环境的数据库/Neo4j 密码请改成与旧值不同的新密码，并设置独立的 `JWT_SECRET`

### 3.（可选）CI 自动测试
仓库已带 `.github/workflows/ci.yml`，推送到 `main` 会自动跑前后端测试。

## 二、服务器部署

### 1. 服务器需要安装
| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行 |
| Node.js | 18+ | 构建前端 |
| Maven | 3.6+ | 构建后端 |
| MySQL | 8.0+ | 关系库 |
| Neo4j | 4.x | 图库 |
| Nginx | 任意 | 托管前端 + 反代 API |

### 2. 拉取代码并构建
```bash
git clone https://github.com/wu-xing-ai/Knowledge-Graph.git /opt/zhi-platform-src
cd /opt/zhi-platform-src

# 后端
cd backend
mvn clean package -DskipTests          # 生成 target/zhi-platform-1.0.0.jar
cd ..

# 前端
cd frontend
npm install
npm run build                          # 生成 dist/
cd ..
```

### 3. 准备生产配置
```bash
mkdir -p /opt/zhi-platform/backend /opt/zhi-platform/frontend
cp backend/target/zhi-platform-1.0.0.jar /opt/zhi-platform/backend/
cp -r frontend/dist/. /opt/zhi-platform/frontend/

# 生产密钥文件（每个值都要改！）
sudo tee /etc/zhi-platform.env > /dev/null <<'EOF'
JWT_SECRET=请设置一个足够长的随机字符串(32位以上)
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/zhi_platform?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=你的mysql密码
SPRING_NEO4J_URI=bolt://localhost:7687
SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j
SPRING_NEO4J_AUTHENTICATION_PASSWORD=你的neo4j密码
MODELSCOPE_API_KEY=你的魔搭key
PADDLE_OCR_TOKEN=你的PaddleOCR token
EOF
sudo chmod 600 /etc/zhi-platform.env
```

### 4. 初始化数据库
```bash
# MySQL：执行 schema.sql（自动建库建表 + 初始账号/学科）
mysql -uroot -p < backend/src/main/resources/schema.sql

# Neo4j：首次设置密码后，执行示例数据脚本
#   neo4j-admin dbms set-initial-password <你的密码>
cypher-shell -u neo4j -p <你的密码> < backend/src/main/resources/neo4j-init.cypher
```

### 5. 后端 systemd 服务
`/etc/systemd/system/zhi-platform.service`：
```ini
[Unit]
Description=ZhiPlatform Backend
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/zhi-platform/backend
ExecStart=/usr/bin/java -Dfile.encoding=UTF-8 -jar /opt/zhi-platform/backend/zhi-platform-1.0.0.jar
EnvironmentFile=/etc/zhi-platform.env
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now zhi-platform
sudo systemctl status zhi-platform
```

### 6. Nginx 配置
`/etc/nginx/conf.d/zhi-platform.conf`：
```nginx
server {
    listen 80;
    server_name 你的域名或IP;

    root /opt/zhi-platform/frontend;
    index index.html;

    location / { try_files $uri $uri/ /index.html; }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        client_max_body_size 50m;
        proxy_read_timeout 300s;
    }
}
```
```bash
sudo nginx -t && sudo systemctl reload nginx
```

### 7. 访问
- 浏览器打开 `http://服务器IP/`（管理员：admin / 123456）

## 三、以后更新
```bash
# 本地改完推送
git add -A && git commit -m "xxx" && git push

# 服务器拉取并重新构建
cd /opt/zhi-platform-src && git pull
cd backend && mvn clean package -DskipTests && cd ..
cp backend/target/zhi-platform-1.0.0.jar /opt/zhi-platform/backend/
cd frontend && npm run build && cd ..
cp -r frontend/dist/. /opt/zhi-platform/frontend/
sudo systemctl restart zhi-platform
```

> 提示：也可以用项目自带的 `scripts/build-deploy.ps1` 在本地生成 `deploy/` 部署包，直接上传服务器，跳过在服务器上构建（服务器只需 JDK17 + MySQL + Neo4j + Nginx）。

## 四、日常维护（无需构建环境）

项目自带 `restart.sh`（仓库根目录），只管理系统服务，不需要 JDK/Maven/Node：

```bash
sudo bash restart.sh restart    # 重启全部服务（MySQL/Neo4j/Nginx/后端）
sudo bash restart.sh backend    # 只重启后端（改配置/换包后最快生效）
sudo bash restart.sh start      # 启动全部
sudo bash restart.sh stop       # 停止全部
sudo bash restart.sh status     # 查看各服务状态
sudo bash restart.sh logs       # 查看后端最近日志
```