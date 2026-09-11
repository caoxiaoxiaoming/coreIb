# coreIb 前端基座

`coreib-web` 采用 Vue 3 + TypeScript + Vite，基于芋道 Vue3 管理端官方前端固定提交
`a58e6de223b616b9dc14c95551d9d10faf5a280b` 的 MIT 许可基础设施进行裁剪适配。保留经典侧边栏/顶部导航、递归菜单、面包屑、TagsView、主题设置、暗色模式、组件尺寸、全屏和权限指令，但不引入芋道业务页面、Token/用户/角色 API 或 Java 后端。

当前提供工作台、当前权限、权限配置、平台目录、系统信息基础路由，权限快照从
`GET /api/v1/permissions/effective` 加载。后续替换为真实认证提供器时，菜单、表格和字段组件
继续复用同一份权限契约；行条件不会下发到浏览器，最终由后端仓储执行。

芋道上游来源与裁剪边界记录在 `coreib-web/YUDao-UPSTREAM.md`，许可证副本为
`coreib-web/LICENSE.yudao`。Element Plus 使用直接组件导入和按组件样式加载，生产构建按
`element-plus`、`vue-vendor`、图标和业务路由分包。

导航采用 `src/navigation.ts` 的数据模型，平台页使用 `platformOnly` 放行；业务菜单必须同时声明
`resource` 和 `action`，并由当前权限快照过滤。路由元数据使用相同的资源标识，路由守卫在进入业务页
前调用 Pinia 权限状态校验，无权访问统一进入 `/forbidden`。菜单隐藏只是体验层控制，接口权限仍由后端最终裁决。

新增业务域时，在 `navigation.ts` 增加菜单项，在路由中增加 `resource`、`action` 元数据，在后端返回
动作、行范围和字段策略；前端复用 Pinia 权限状态，后端服务调用 `CoreIbAuthorizationService` 对列表、
详情、导出和写入统一校验。多角色行范围不会在浏览器中执行，后端仓储使用结构化
`RowAccessCriteria` 生成参数化查询条件。

平台目录使用 `platform-directory:READ` 控制查看，`UPDATE` 控制新增和启停；后端命令接口为
`POST /api/v1/platform/directory/organizations`、`/users`、`/roles` 以及
`PATCH /api/v1/platform/directory/{entity}/{id}/enabled`。生产数据库适配器会将成功命令写入
`coreib_audit_event`，命令异常会追加 `FAILED` 事件；审计写入异常不会覆盖原始业务异常。
`demo` Profile 提供内存态写操作联调，数据不会持久化；写按钮只在获得
`platform-directory:UPDATE` 后显示。

权限配置页使用 `security-administration:READ` 控制路由和菜单，使用 `UPDATE` 控制保存按钮。页面可维护
角色资源动作及行范围、字段访问模式、用户角色、上下级关系和记录归属；请求统一通过 `coreIbFetch`
发送，OIDC 模式下继续自动携带 CSRF 请求头。后端仍是最终权限边界，页面提交的资源名、范围和关系
只作为待校验命令，不会直接参与当前请求的数据过滤。

## 认证适配

前端启动时先读取 `GET /api/v1/auth/session`，再加载有效权限。OIDC 模式下未认证用户由路由守卫
跳转到后端生成的 `/oauth2/authorization/{registrationId}`；写请求通过统一 `coreIbFetch`
自动把 `XSRF-TOKEN` Cookie 放入 `X-XSRF-TOKEN` 请求头，注销后跳转到后端下发的本地地址。

JWT 模式不绑定某个厂商 SDK。院内门户或 IdP SDK 在应用启动时调用
`configureAccessTokenProvider`，统一客户端会给 API 请求附加 Bearer Token。Token 只建立身份，
菜单、按钮、行范围和字段权限仍以 `/api/v1/permissions/effective` 和后端数据库策略为准。

Vite 开发代理覆盖 `/api`、`/actuator`、`/login`、`/logout`、`/oauth2`，确保本地 OIDC 回调和
Session Cookie 与管理端保持同源。生产部署也应采用同源反向代理；跨域部署需要额外设计 Cookie、
CORS 和 CSRF 策略，不能直接沿用默认配置。
