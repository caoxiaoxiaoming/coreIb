# Yudao UI Admin Vue3 upstream

coreIb-web uses an adapted Yudao UI Admin Vue3 administration foundation and implements the platform pages against coreIb-owned APIs. It does not include or depend on the Yudao Java backend.

- Upstream repository: https://github.com/yudaocode/yudao-ui-admin-vue3
- Pinned commit: `a58e6de223b616b9dc14c95551d9d10faf5a280b`
- Upstream version at integration time: `2026.07-snapshot`
- Retrieved: 2026-08-21
- License: MIT, see `LICENSE.yudao`

Included frontend capabilities:

- classic sidebar and top navigation layouts;
- recursive permission-aware menus;
- breadcrumbs and multi-page tags;
- theme, dark mode, component size and full-screen controls;
- Pinia shell state and Vue permission directives.
- system administration pages for users, roles, departments, menus, posts, dictionaries, parameters and notices;
- infrastructure pages for audit/login logs, files and database metadata code generation;
- coreIb authentication, dynamic permission, row/field security and platform API adapters.

Not reused from upstream:

- Yudao token, user, role, menu and dictionary API contracts;
- Yudao Java backend and its database model.

The administration pages are coreIb implementations following the upstream interaction and layout conventions. They are not thin copies of pages that still require Yudao backend endpoints.

Authentication, effective permissions, row restrictions, field restrictions, audit and platform APIs continue to come exclusively from the coreIb backend.
