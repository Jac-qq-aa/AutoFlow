# 验证记录

验证环境：Windows 11、Temurin Java 17.0.18、Docker Desktop 29.2.1、Docker Compose 5.0.2、Node.js 24.18.0。

## 已通过项目

| 检查项 | 命令或方式 | 结果 |
|---|---|---|
| 后端测试 | `.\mvnw.cmd clean test` | 通过，共 19 项：状态机、事务 Outbox、MySQL + Redis 集成、跨门店隔离、取消/支付/库存释放乱序、重复消费、库存并发、权限与死信重放 |
| 后端打包 | `.\mvnw.cmd package -DskipTests` | 通过，4 个服务镜像所需 JAR 均生成 |
| 前端测试 | `npm test -- --run` | 通过，共 3 项工作台交互测试 |
| 前端生产构建 | `npm run build` | 通过，Vue 3 生产资源成功生成 |
| Compose 语法 | `docker compose config --quiet` | 通过 |
| 容器健康检查 | `.\scripts\wait-health.ps1` | Gateway、Order、Inventory、Fulfillment、Web 全部就绪 |
| 端到端履约 | `.\scripts\smoke-test.ps1` | 连续两次通过：每次创建隔离车型/VIN 夹具，再完成登录、建单、审核、库存预占、支付、VIN 分配和交付 |
| 运行态库存并发 | `.\scripts\concurrency-test.ps1 -Requests 10` | 通过：并发前可用 6，成功 6，耗尽为 0；清理后恢复为 6，无超卖 |
| JWT + RBAC | 经 Gateway 使用签名 JWT 请求 | 销售访问事件监控为 403、跨门店库存为 403；管理员事件查询为 200 |
| 内部服务隔离 | 主机端口探测 | 8082、8083、8084 均不可从宿主机访问，仅 Gateway 8080 对外 |
| 可扩展子页面记录 | 经 Gateway 创建并查询 `INSURANCE` 记录 | 创建成功，按订单查询返回 1 条；跨门店访问由集成测试拒绝 |
| Prometheus | 查询 `up` 与业务指标 | 4 个后端目标均为 1；3 个业务服务均暴露 Outbox 和死信积压指标 |
| Prometheus 配置/规则 | `promtool check config`、`promtool check rules` | 通过，共 3 条告警规则 |
| Grafana | `/api/health` 与 dashboard search | 数据库状态 `ok`，`AutoFlow Overview` 已自动装载 |
| CodeGraph | `codegraph sync .`、`codegraph status . --json` | 全量同步完成，无待处理变更或引用 |

验证日期：2026-09-01（Asia/Shanghai）。

## 关键故障场景证据

- 支付成功晚于取消请求时，订单进入 `REFUNDING` 并产生退款请求，不再出现“已收款但无退款”的终态漏洞。
- 订单进入 `REFUNDING` 后才收到库存释放事件，仍会记录释放事实，并在退款成功后收敛到 `CANCELLED`。
- VIN 分配早于支付成功时，通过事实字段重算状态，最终仍收敛到 `PENDING_DELIVERY`。
- 重复事件通过业务事件 ID 唯一约束与同一数据库事务实现幂等。
- Outbox 发送失败会保留待重试记录；死信可由管理员重放，并记录操作人。
- Redisson 锁缩小并发冲突，MySQL 条件更新与唯一约束作为防超卖的最终正确性兜底。
- 冒烟脚本每次创建唯一车型和 VIN 测试夹具，避免持久化演示库中的实体车辆被重复验收耗尽；该脚本已连续运行两次通过。

## 边界

- 真实 Broker 长时间宕机、跨机房网络分区和死信堆积压测未作为本地验收项；当前证据覆盖发布失败重试、重复消费幂等、死信重放及审计。
- 本项目没有虚构 QPS、P95、可用性或线上业务量。Grafana 提供运行态 P95 面板，HTTP 直方图已显式启用，但性能结论必须在固定硬件和固定数据规模下另行压测。
- Nacos 在本地单机 Compose 中关闭鉴权。生产部署应启用鉴权、使用集群并限制为内网访问。
- 第一次启动需要下载较大的基础设施镜像，耗时取决于 Docker Hub 网络状况；后续启动复用本地镜像。
