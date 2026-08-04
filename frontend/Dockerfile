# ============ 知联汇学 前端镜像（多阶段构建）============
# 阶段 1：Node 构建
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

# 阶段 2：Nginx 托管静态文件 + 反向代理 /api
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80