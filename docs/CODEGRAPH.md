# CodeGraph 代码知识图谱

项目已使用 CodeGraph 1.4.1 完成全量索引，图谱数据库位于项目根目录的 `.codegraph/codegraph.db`。

## 图谱统计

| 指标 | 数量 |
|---|---:|
| 索引文件 | 111 |
| 节点 | 1,411 |
| 边 | 1,932 |
| 类 | 63 |
| 接口 | 17 |
| 方法 | 194 |
| 路由 | 19 |
| 导入关系 | 593 |

覆盖 Java、TypeScript 和 Vue 的代码符号及调用关系。YAML、XML、Properties、Markdown、SQL、CSS、Dockerfile 和 PowerShell 保留在项目中，但当前 CodeGraph 版本不会把所有配置与脚本内容都转换为代码符号节点。

## 常用查询

```powershell
# 查看图谱状态
codegraph status .

# 搜索库存服务符号
codegraph query InventoryService --path .

# 查看库存预占的源码、调用者和被调用符号
codegraph node "io.autoflow.inventory.application::InventoryService::reserve" --path .

# 联合探索 Outbox、库存预占、支付与 VIN 链路
codegraph explore "transactional outbox inventory reservation payment VIN" --path . --max-files 6

# 源码修改后增量同步
codegraph sync .
```

`InventoryService.reserve` 的图谱关系已验证：它连接库存预占事务、预占记录查询和业务异常，调用方包括 `InventoryController.reserve` 与库存并发测试。

## 使用说明

- `.codegraph/codegraph.db` 是本机生成的索引，可直接用于当前交付物。
- `.codegraph/.gitignore` 按 CodeGraph 默认规则不建议提交数据库；在其他电脑克隆项目后运行 `codegraph init .` 即可重建。
- 使用 `codegraph status . --json` 可检查索引版本、待同步文件及节点/边统计。
