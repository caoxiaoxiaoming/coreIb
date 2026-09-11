# 数据库兼容策略

coreIb 将 SQL Server、Oracle、PostgreSQL 作为首批一等支持数据库。当前基座已经提供统一的
`DatabaseVendor`、方言契约和 `coreib-starter-jdbc` 连接池自动配置。兼容的定义不是 JDBC
能连接，而是以下能力都通过自动化回归：

| 数据库 | JDBC 驱动 | Spring Profile | 默认 URL 示例 |
| --- | --- | --- | --- |
| PostgreSQL | `org.postgresql:postgresql` | `postgresql` | `jdbc:postgresql://localhost:5432/coreib` |
| SQL Server | `com.microsoft.sqlserver:mssql-jdbc` | `sqlserver` | `jdbc:sqlserver://localhost:1433;databaseName=coreib` |
| Oracle | `com.oracle.database.jdbc:ojdbc11` | `oracle` | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |

## 启用方式

默认配置 `coreib.datasource.enabled=false`，因此没有数据库时服务器仍可启动。启用某个
Profile 后，starter 会校验供应商与 JDBC URL 是否匹配，并创建 HikariCP 连接池：

```powershell
$env:COREIB_DB_USERNAME = 'coreib'
$env:COREIB_DB_PASSWORD = 'change-me'
java -jar coreib-server\target\coreib-server-0.1.0-SNAPSHOT.jar --spring.profiles.active=postgresql
```

连接池的默认值是最大连接 20、最小空闲 2、连接超时 30 秒。生产环境应通过配置文件或环境
变量调整，密码不应写入仓库。启用 Profile 时默认快速失败，数据库不可达会阻止应用启动，
避免服务以错误配置对外提供业务接口。

## 兼容边界

1. 迁移脚本和回滚策略
2. 事务隔离、锁和并发更新
3. 分页、排序和空值语义
4. 时间、精度、LOB、JSON 和序列/自增
5. 批量写入、连接池和超时
6. 备份恢复与升级演练

业务模块只依赖 `coreib-data` 的数据库契约，不直接拼接供应商 SQL。供应商差异放在数据库适配模块和迁移脚本中，CI 至少跑三数据库矩阵；H2 只能用于快速单元测试，不能作为兼容性证明。

当前阶段已经覆盖 URL/供应商识别、标识符引用、分页契约、连接池配置、Liquibase 基线资源、
迁移完整回滚、权限外键约束和参数化行权限谓词。真实数据库集成测试通过 Maven `integration`
profile 执行，CI 矩阵配置在 `.github/workflows/database-integration.yml`，分别启动 PostgreSQL、
SQL Server 和 Oracle，并验证无效用户角色关系会被数据库拒绝。

## 迁移约定

`coreib-starter-jdbc` 内置 `db/changelog/db.changelog-master.yaml`，默认通过
`spring.liquibase.change-log` 注册。每个变更集必须包含稳定的 `id`、作者、跨数据库类型和
显式 rollback；业务模块应在自己的模块资源目录中维护 changelog，再由部署组合入口文件。

基线表 `coreib_platform_metadata` 仅属于平台，不代表任何具体业务领域。启用数据库 Profile
后，Liquibase 会在应用启动阶段执行待处理变更；连接失败或迁移失败时应用应启动失败，避免
业务服务运行在不完整的数据库结构上。

`coreib-005-security-integrity` 以追加变更集补充安全表外键、唯一约束和常用查询索引，不修改
既有迁移历史。业务模块的行过滤优先使用 `JdbcRowAccessPredicateCompiler` 或等价的参数化仓储
适配器；禁止把前端传入的主体、SQL 片段或列名直接拼入查询。
