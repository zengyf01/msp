FROM node:18-alpine AS builder

WORKDIR /app

# 配置 npm 国内镜像
RUN npm config set registry https://registry.npmmirror.com && \
    npm config set fetch-retries 5 && \
    npm config set fetch-retry-mintimeout 20000 && \
    npm config set fetch-retry-maxtimeout 120000

COPY package.json .
RUN npm install --legacy-peer-deps

COPY . .

RUN NODE_OPTIONS=--max-old-space-size=2048 npx vite build

FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]