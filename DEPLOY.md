# 部署指南（GitHub + 服务器）

本平台前端是 Vue 静态文件、后端是 Spring Boot jar。推荐部署方式：

```
GitHub（私有仓库）  --git pull-->  服务器  --构建-->  Nginx(前端) + systemd(后端)
```

## 一、本地推送到 GitHub

### 1. 创建私有仓库
在 GitHub 新建一个 **Private** 仓库（不要 Public），例如 `zhi-platform`。

### 2. 关联并推送
```bash
# 在项目根目录执行
git remote add origin https://github.com/<你的用户名>/zhi-platform.git
git add -A
git commit -m "init: 知联汇学知识图谱平台"
git push -u origin main
```

> 仓库已保证不包含敏感信息：
> - `.env`（真实 API Key）、`deploy/`、`frontend/dist/`、`backend/target/`、`node_modules/` 均被 `.gitignore` 忽略
> - `application.yml` 中的数据库/Neo4j/JWT 密钥已改为**环境变量优先 + 本地默认值**，生产环境用服务器的 `/etc/zhi-platform.env` 覆盖

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
git clone https://github.com/<你的用户名>/zhi-platform.git /opt/zhi-platform-src
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