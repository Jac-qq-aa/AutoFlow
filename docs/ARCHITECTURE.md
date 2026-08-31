# 架构与一致性设计

## 服务边界

| 服务 | 拥有的数据 | 不负责的事情 |
|---|---|---|
| Gateway | 演示账号和 JWT 签发 | 不保存订单、不参与业务事务 |
| Order | 销售订单、渠道/客户快照、订单状态 | 不直接修改库存、支付或交付表 |
| Inventory | 车型配额、VIN 车辆、预占记录 | 不判断支付结果 |
| Fulfillment | 支付、退款、交付任务 | 不直接改变订单主状态 |

一个 MySQL 实例承载三个 schema，仅为了降低本地演示成本。服务禁止跨 schema 查询，因此可以在生产环境独立迁移数据库。

## 创建与履约时序

```mermaid
sequenceDiagram
    actor M as 门店经理
    participant O as Order
    participant MQ as RocketMQ
    participant I as Inventory
    participant F as Fulfillment
    M->>O: 审核订单
    O->>O: 更新状态 + 写 outbox（同一事务）
    O-->>MQ: InventoryReservationRequested
    MQ-->>I: 至少一次投递
    I->>I: processed_event + 条件扣减 + reservation
    I-->>MQ: InventoryReserved
    MQ-->>O: 更新为待支付
    M->>O: 发起支付模拟
    O-->>MQ: PaymentRequested
    MQ-->>F: 创建支付记录
    F-->>MQ: PaymentSucceeded
    par 状态推进
        MQ-->>O: 更新为待分配 VIN
    and VIN 分配
        MQ-->>I: 锁定唯一 VIN
        I-->>MQ: VinAllocated
    end
    MQ-->>O: 更新为待交付
    MQ-->>F: 创建交付任务
```

## 为什么采用 Transactional Outbox

订单状态更新与消息直接发送无法形成一个原子操作：数据库提交后发送失败会丢事件，消息先发后数据库回滚会产生幽灵事件。每个业务服务把业务修改和 `outbox_event` 写入放在同一个本地事务中，由后台任务持续发布。

发送失败时按指数退避重试。成功发送后更新 `SENT`。极端情况下可能重复发送，因此消费者仍必须幂等。

## 消费幂等

每条事件拥有全局唯一 `eventId`。消费者把 `processed_event` 插入与业务修改放在同一个本地事务中：

1. 首次插入成功，执行状态转换；
2. 重复投递触发主键冲突，直接返回成功；
3. 业务修改失败则整个事务回滚，Broker 下次重试仍可重新处理。

业务表还带有 `order_id`、`allocated_order_id`、VIN 等唯一约束，形成第二层幂等保护。

## 库存正确性

Redisson 锁键为 `inventory:{storeId}:{modelCode}`。它减少同一热点配额上的无效数据库竞争，但不是事实来源。

最终正确性由以下 SQL 保证：

```sql
UPDATE inventory_quota
SET available = available - 1,
    reserved = reserved + 1,
    version = version + 1
WHERE store_id = ?
  AND model_code = ?
  AND available > 0;
```

只有受影响行数为 1 才算成功。即使 Redis 故障、锁租约过期或两个实例同时进入，`available` 也不会小于零。数据库提交后才释放分布式锁，代码使用 `TransactionTemplate` 明确保证这一顺序。

## 取消与补偿

- 未支付：释放库存后进入 `CANCELLED`；
- 已支付：并行触发库存释放和退款；
- 两个事件可乱序、重复或延迟；
- Order 使用条件更新，只有 `inventory_status=RELEASED` 且 `payment_status=REFUNDED` 才结束取消；
- 已交付订单不允许普通取消。

## 缓存

订单详情采用 cache-aside，缓存键含业务前缀并设置 TTL。任何状态变化后主动淘汰详情缓存。库存扣减结果永远不以 Redis 为唯一依据，避免缓存丢失造成库存恢复或超卖。

## 可观测性

所有服务暴露 `/actuator/prometheus`。Prometheus 每 10 秒采集，Grafana 自动配置数据源。正式压测前需记录 CPU、内存、数据库连接池、HTTP P95/P99、MQ 重试和 Redis 命中率，不能脱离硬件环境宣称固定吞吐量。

