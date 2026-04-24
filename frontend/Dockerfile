# 构建阶段
FROM node:23 AS build

WORKDIR /app
COPY . .
RUN npm install
RUN npm run build

# 运行阶段
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80