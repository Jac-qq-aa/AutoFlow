FROM node:24-alpine AS builder
WORKDIR /app
COPY web-admin/package*.json ./
RUN npm install
COPY web-admin/ ./
RUN npm run build
FROM nginx:1.29-alpine
COPY web-admin/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80

