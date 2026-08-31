# 验证记录

验证环境：Windows 11、Temurin Java 17.0.18、Docker Desktop 29.2.1、Docker Compose 5.0.2、Node.js 24.18.0。

## 已通过项目

| 检查项 | 命令 | 结果 |
|---|---|---|
| 后端编译与测试 | `./mvnw.cmd clean test` | 通过；订单状态机 2 项、MySQL + Redis Testcontainers 库存并发测试 1 项 |
| 前端生产构建 | `npm run build` | 通过；Vue 3 生产资源成功生成 |
| Compose 语法 | `docker compose config --quiet` | 通过 |
| 容器健康检查 | `./scripts/wait-health.ps1` | Gateway、Order、Inventory、Fulfillment、Web 全部就绪 |
| 端到端履约 | `./scripts/smoke-test.ps1` | 通过；订单完成审核、预占、支付、VIN 分配和交付，实测 VIN 为 `LAF00000000000001` |
| 运行态库存并发 | `./scripts/concurrency-test.ps1 -Requests 20` | 通过；初始可用 4，20 个并发请求中成功 4，最终可用 0，无超卖 |

验证日期：2026-08-31（Asia/Shanghai）。

## 边界

- 上述结果用于证明本地演示环境可复现，不代表生产环境容量。
- 项目没有虚构 QPS、P95、可用性或线上业务量；需要性能数字时，应在固定硬件和固定数据规模下另行压测并保存原始结果。
- Nacos 在本地单机 Compose 中关闭鉴权，业务入口仍由 JWT + RBAC 保护。生产部署应启用 Nacos 鉴权、使用集群并限制为内网访问。
- 第一次启动需要下载较大的基础设施镜像，耗时取决于 Docker Hub 网络状况；后续启动复用本地镜像。
