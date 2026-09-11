# coreIb

业务系统研发基座（Business Application Foundation）。

当前版本提供：

- 可供独立产品导入的 `coreib-bom` 版本清单和 `coreib-starter-web` 平台 API Starter
- 产品/领域模块元数据、租户上下文、领域事件、持久化任务和私有对象存储扩展契约
- Microsoft Build of OpenJDK 17（开源 LTS）/ Spring Boot 多模块后端骨架
- Vue 3 + TypeScript 完整管理端基座
- 基于芋道 Vue3 管理端官方前端（固定提交 `a58e6de223b616b9dc14c95551d9d10faf5a280b`）的 coreIb 适配实现：经典/顶部布局、递归动态菜单、面包屑、TagsView、主题设置和权限指令；不引入芋道 Java 后端
- `DISABLED`、JWT Resource Server、OIDC Authorization Code 三种认证模式
- SQL Server、Oracle、PostgreSQL 数据库适配契约、JDBC 连接池 starter 与 Liquibase 基线
- 权限元数据外键/唯一约束/索引，以及参数化 JDBC 行权限谓词编译器
- 可审计的权限配置中心：角色动作、行范围、字段权限、用户角色、上下级关系和记录归属
- 系统管理：用户、角色、部门、菜单、岗位、字典、参数和通知公告
- 基础设施：操作日志、登录日志、本地文件存储和 JDBC 元数据代码生成
- 代码生成器输出 Spring JDBC、REST API、Vue3、TypeScript 和 Liquibase 草稿，并支持 ZIP 下载
- 统一 API、健康检查和模块边界

## 目录

```text
coreIb/
├─ coreib-bom/         # 面向独立产品发布的依赖版本清单
├─ coreib-kernel/      # 与业务无关的基础契约和产品扩展 SPI
├─ coreib-data/        # 数据库供应商与方言适配
├─ coreib-security/    # RBAC、行权限、列权限和管理关系策略内核
├─ coreib-platform/    # 用户、组织、角色、菜单、字典、参数、公告和审计契约
├─ coreib-starter-jdbc/ # 按需启用的 Hikari/JDBC 自动配置
├─ coreib-starter-web/ # 可复用的平台管理 API 与 JDBC 实现
├─ coreib-server/      # coreIb 独立演示/验收应用
└─ coreib-web/         # 芋道 Vue3 管理端的 coreIb API 适配实现（不引入芋道后端）
```

## 独立产品接入

产品工程导入 BOM 后只依赖所需 Starter，不依赖可执行的 `coreib-server`：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.coreib</groupId>
      <artifactId>coreib-bom</artifactId>
      <version>1.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependency>
  <groupId>com.coreib</groupId>
  <artifactId>coreib-starter-web</artifactId>
</dependency>
```

产品启动类需要扫描自身与 `com.coreib` 包，并排除 Spring Boot 默认数据源自动配置，统一由
`CoreIbJdbcAutoConfiguration` 根据 `coreib.datasource.*` 创建数据源。产品 Liquibase master
先包含 coreIb 基线，再包含自己的领域变更集。

## 本地运行

后端：

```powershell
mvn -pl coreib-server -am package
java -jar coreib-server\target\coreib-server-1.0.1.jar
```

前端：

```powershell
pnpm install
pnpm --dir coreib-web dev
```

前端工程要求 Node.js `>=22.12.0`。开发环境访问 `http://localhost:5173`；Vite 代理将 `/api`、`/actuator`、`/login`、`/logout` 和 `/oauth2` 转发到 coreIb 服务端。

芋道前端来源、MIT 许可、固定提交和裁剪边界见 [`coreib-web/YUDao-UPSTREAM.md`](coreib-web/YUDao-UPSTREAM.md) 与 [`coreib-web/LICENSE.yudao`](coreib-web/LICENSE.yudao)。前端只消费 coreIb 的会话、权限和平台 API，行权限、列权限和审计仍由后端执行。

启动后端后访问 `GET /api/v1/system/info` 查看基座信息，访问
`GET /api/v1/permissions/effective` 获取当前用户的前端权限快照。

平台目录模块提供组织、用户和角色的增改删查与启停。查询要求 `platform-directory:READ`，
写操作要求 `UPDATE`，删除要求 `DELETE`；数据库模式下命令结果写入 `coreib_audit_event`。

平台管理 API 位于 `/api/v1/platform/management`，包含菜单、岗位、字典、参数、公告、操作日志和
登录日志。查看要求 `platform-management:READ`，保存要求 `UPDATE`，删除要求 `DELETE`。
文件 API 位于 `/api/v1/files`，使用独立的 `file-management` 权限资源；文件目录由
`COREIB_FILE_DIRECTORY` 设置，数据库模式只在业务数据库保存元数据。代码生成 API 位于
`/api/v1/code-generation`，通过标准 JDBC `DatabaseMetaData` 读取表结构，所以同一实现适用于
SQL Server、Oracle 和 PostgreSQL；预览要求 `READ`，下载要求 `EXPORT`。

权限配置中心使用独立的 `security-administration` 资源。查看要求 `READ`，所有策略和关系变更要求
`UPDATE`；管理 API 位于 `/api/v1/platform/security`。角色策略与用户角色采用事务化整体替换，
上下级关系和记录归属采用幂等保存并可停用。变更在下一次请求中生效，不依赖应用重启。

本地验证权限场景时可启用隔离的 `demo` Profile。默认认证模式为 `DISABLED`，通过请求头
`X-CoreIb-Demo-Subject` 选择 `nurse`、`head-nurse`、`information-clerk` 或
`information-leader`，使用 `platform-admin` 可联调全部系统管理、文件和代码生成功能；该适配器只用于开发联调，
部署环境应替换为 JWT、OIDC 或院内统一身份适配器：

```powershell
java -jar coreib-server\target\coreib-server-1.0.1.jar --spring.profiles.active=demo
Invoke-WebRequest http://localhost:8080/api/v1/permissions/effective `
  -Headers @{ 'X-CoreIb-Demo-Subject' = 'head-nurse' }
```

默认不连接数据库，适合启动前端和接口骨架。接入数据库时选择一个 Spring Profile，
并通过环境变量提供密码：

```powershell
$env:COREIB_DB_PASSWORD = 'change-me'
java -jar coreib-server\target\coreib-server-1.0.1.jar --spring.profiles.active=postgresql
```

## 生产认证

JWT API 模式使用无状态 Bearer Token，CSRF 关闭，主体名从可配置 claim 提取：

```powershell
$env:COREIB_AUTH_MODE = 'JWT'
$env:COREIB_AUTH_PRINCIPAL_CLAIM = 'preferred_username'
$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI = 'https://identity.example/realms/coreib'
java -jar coreib-server\target\coreib-server-1.0.1.jar --spring.profiles.active=postgresql
```

OIDC 管理端模式使用 Authorization Code、服务端 Session 和 Cookie CSRF。Spring Security 的
Client Registration 必须由部署环境提供，registration id 与 `COREIB_OIDC_REGISTRATION_ID` 一致：

```yaml
coreib:
  security:
    authentication:
      mode: OIDC
      oidc-registration-id: coreib
      login-success-url: /
      logout-success-url: /
spring:
  security:
    oauth2:
      client:
        registration:
          coreib:
            provider: hospital-sso
            client-id: ${COREIB_OIDC_CLIENT_ID}
            client-secret: ${COREIB_OIDC_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: [openid, profile]
        provider:
          hospital-sso:
            issuer-uri: https://identity.example/realms/coreib
```

实际 IdP 必须登记回调地址并由密钥管理系统注入客户端密钥；示例地址不是可直接用于生产的
身份服务。`GET /api/v1/auth/session` 返回身份状态、平台用户映射状态、登录/注销地址和 CSRF 契约。

认证建立 Servlet `Principal` 后，生产环境应启用数据库动态权限：

```yaml
coreib:
  security:
    jdbc:
      enabled: true
      principal-lookup: EXTERNAL_IDENTITY
```

`principal-lookup` 支持 `ID`、`LOGIN_NAME`、`EXTERNAL_IDENTITY`。映射成功后始终使用
`coreib_sys_user.id` 查询角色和数据关系；未知、停用或非唯一映射均按匿名无权限处理。JWT/OIDC
只负责证明身份，业务角色不从 Token 直接信任。JDBC 权限提供器会从角色、动作、行范围、
字段权限、组织树、管理关系和记录分配表生成当前请求的有效权限；没有认证主体、用户被停用或
用户不存在时均按匿名无权限处理。

后端业务模块统一使用 `CoreIbAuthorizationService`：列表、统计和导出先校验动作，再将
`RowAccessCriteria` 交给仓储生成参数化行条件；详情和写操作按 `PermissionRow` 逐行校验。
多角色的行范围按 OR 合并，字段权限可按动作计算。前端 `PermissionSnapshot` 只用于界面展示，
不能作为后端数据授权依据。完整约束见 [`docs/permission-model.md`](docs/permission-model.md)。

首次数据库部署可显式执行一次受控管理员引导。它不会创建本地默认密码，而是把医院 OIDC/JWT
主体映射到平台管理员；完成首次登录后必须关闭引导开关：

```powershell
$env:COREIB_BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:COREIB_BOOTSTRAP_ADMIN_EXTERNAL_IDENTITY = 'hospital-idp-subject-or-login'
$env:COREIB_AUTH_PRINCIPAL_LOOKUP = 'EXTERNAL_IDENTITY'
java -jar coreib-server\target\coreib-server-1.0.1.jar --spring.profiles.active=postgresql
```

引导过程幂等创建根组织、`platform-admin` 角色、管理员用户和平台所需全部权限。确认管理员能够登录后，
将 `COREIB_BOOTSTRAP_ADMIN_ENABLED` 改回 `false`。通用 Liquibase 基线不包含默认用户或弱口令。

可用 Profile：`postgresql`、`sqlserver`、`oracle`。URL、用户名和密码分别支持
`COREIB_DB_URL`、`COREIB_DB_USERNAME`、`COREIB_DB_PASSWORD` 覆盖，完整连接池和迁移参数见
[`docs/database-compatibility.md`](docs/database-compatibility.md)。

三数据库真实集成测试由 GitHub Actions 矩阵执行；本地已有数据库时可以按同样方式运行：

```powershell
mvn -Pintegration -pl coreib-starter-jdbc -am verify `
  "-Dcoreib.it.vendor=postgresql" `
  "-Dcoreib.it.url=jdbc:postgresql://localhost:5432/coreib" `
  "-Dcoreib.it.username=coreib" `
  "-Dcoreib.it.password=change-me"
```
