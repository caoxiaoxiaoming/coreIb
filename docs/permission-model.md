# coreIb permission model

`coreib-security` provides the vendor-neutral authorization contract used by platform and
business modules. The Vue admin shell consumes an effective permission snapshot for menus,
buttons and fields, but the backend remains the security boundary.

## Decision layers

1. **Action**: `READ`, `CREATE`, `UPDATE`, `DELETE`, `EXPORT`.
2. **Row scope**: `ALL`, `ORGANIZATION`, `ORGANIZATION_TREE`, `SELF`, `MANAGED_USERS`,
   `RELATED_RECORDS` or a trusted backend `CUSTOM` policy.
3. **Field access**: `HIDDEN`, `READ`, `WRITE` or `MASKED`.

Every list, detail, export, aggregate, batch and write operation must apply the same action and
row decision. A browser-hidden button is not an authorization check.

## Backend authorization contract

`CurrentAuthorizationProvider` keeps every effective `PermissionPolicy` instead of flattening
multiple roles into one row scope. `CoreIbAuthorizationService` is the entry point for business
services:

- `require(resource, action)` checks the action before a list, aggregate or export query.
- `rowCriteria(resource, action)` returns `RowAccessCriteria` for repository filtering.
- `require(resource, action, row)` protects detail and mutation operations.
- `fieldAccess(resource, action, field)` protects response and command fields.

`RowAccessCriteria.scopes` has OR semantics. For example, grants from two roles may produce
`[ORGANIZATION, MANAGED_USERS]`; a repository must return rows in the current organization OR rows
owned/assigned to a managed user. The criteria also carries the subject ID, organization IDs,
managed user IDs and resource-specific related record IDs. Repositories translate those facts into
parameterized predicates for their own schema. The security module never returns or concatenates SQL.

For a list query the expected call sequence is:

```java
authorization.require("patient", PermissionAction.READ);
RowAccessCriteria criteria = authorization.rowCriteria("patient", PermissionAction.READ);
return patientRepository.find(criteria, query);
```

JDBC repositories can use `JdbcRowAccessPredicateCompiler` as the portable reference adapter:

```java
JdbcRowAccessPredicate predicate = JdbcRowAccessPredicateCompiler.compile(
        "patient",
        criteria,
        new JdbcRowColumns("p.id", "p.owner_user_id", "p.organization_id"));
return jdbc.query(
        "SELECT p.id, p.name FROM patient p WHERE " + predicate.sql(),
        rowMapper,
        predicate.parameters().toArray());
```

The column mapping must be backend-owned and accepts only simple identifiers; it must never be built
from request data. The compiler emits placeholders for all identity values, applies OR semantics, and
uses active/effective `coreib_sec_record_assignment` rows for managed-user and related-record scopes.
Domain relationships not represented by the central assignment table remain the responsibility of a
domain repository or trusted custom policy compiler.
Portable `IN` sets are capped at 900 values to stay below Oracle expression and SQL Server parameter
limits. Larger organization trees or management sets must use a join-based domain repository strategy;
the compiler throws before executing vendor-specific invalid SQL and never widens access.

The repository must treat `criteria.denied()` as no rows, may omit row predicates only when
`criteria.unrestricted()` is true, and must OR the remaining scopes. It must not accept row criteria
or identity facts supplied by a browser.

## Required relationships

- `coreib_sec_user_supervisor` stores active management relationships. It supports direct and
  transitive management scopes such as a head nurse and the nurses below them.
- `coreib_sec_record_assignment` stores resource-qualified domain relationships such as a patient
  and the nurses responsible for that patient. Assignments are never shared across resources merely
  because two records have the same ID.
- `coreib_sec_permission` grants resource actions and a row scope to a role.
- `coreib_sec_field_permission` controls the fields exposed or accepted for that role/resource.

The `coreib-005-security-integrity` migration adds foreign keys, duplicate-grant constraints and lookup
indexes. `external_identity` remains a non-unique nullable column at the database layer because nullable
unique semantics differ across SQL Server, Oracle and PostgreSQL; the provider requires exactly one
enabled match and fails closed otherwise.

Only active relationships inside `effective_from` and `effective_to` participate in a decision.
Organization-tree and transitive supervisor sets are assembled in Java using portable, parameterized
SQL, avoiding recursive SQL differences between SQL Server, Oracle and PostgreSQL.

## Confirmed scenarios

| Scenario | Resource policy |
| --- | --- |
| Nurse reads own patients | `patient:READ` + `RELATED_RECORDS` |
| Head nurse reads managed nurses and their patients | `nurse:READ` and `patient:READ` + `MANAGED_USERS` |
| Information clerk is read-only | `business-data:READ` + `ORGANIZATION` |
| Information leader can edit | `business-data:READ,UPDATE` + `ORGANIZATION` |

`CUSTOM` scopes fail closed in the pure policy evaluator and remain visible through
`RowAccessCriteria.unresolvedCustomPolicyKeys`. A trusted domain policy compiler must be registered
before such a policy can grant rows. An unresolved custom grant never widens a standard grant.

Business services should use `CoreIbAuthorizationService`; `CoreIbPermissionGuard` remains the
low-level single-policy primitive. A failed guard raises
`PermissionDeniedException`; the server maps it to HTTP 403 with `PERMISSION_DENIED`.

## API contract

`GET /api/v1/permissions/effective` returns the frontend-safe `PermissionSnapshot`. It contains
actions, a row-scope summary and field access, but never exposes identity sets or SQL predicates.
When several incomparable scopes apply, the UI summary is `CUSTOM`; this is only a display marker.
Backend decisions always use the original action-specific policy collection. Until an
authentication module supplies a `CurrentPermissionProvider`, the endpoint returns an anonymous
empty snapshot. This prevents the development fallback from being mistaken for production auth.

`GET /api/v1/platform/security` and its command endpoints form the administration boundary for the
stored model. The separate `security-administration:READ,UPDATE` grant controls access. The API covers
role policies, field grants, user-role membership, supervisor relationships and resource-qualified
record assignments. Role policies and user roles are replaced transactionally; relationship writes are
idempotent and can be deactivated without deleting their identity. Every command appends a success or
failure event to `coreib_audit_event` when the JDBC audit sink is active.

The administration API never bypasses runtime authorization. A policy change becomes visible to new
requests immediately; the request-scoped provider keeps a stable snapshot for the request already in
progress. A production deployment must bootstrap its first administrator through a controlled database
provisioning step and should not ship a universal default administrator in the shared migration.

When `coreib.security.jdbc.enabled=true`, `JdbcPermissionSnapshotProvider` resolves the already
authenticated Servlet `Principal` using `ID`, `LOGIN_NAME` or `EXTERNAL_IDENTITY`. A successful lookup is
normalized to the internal `coreib_sys_user.id` before roles, relationships and record assignments are
loaded. Unknown, disabled or non-unique mappings fail closed. The provider is request-scoped and caches
identity, policies, organization tree, management relationships and resource-qualified assignments for
the current request.

`coreib.security.authentication.mode` selects `DISABLED`, stateless `JWT`, or session-based `OIDC`.
JWT/OIDC validates identity only; token roles and arbitrary browser headers are not trusted as coreIb
business grants. The JDBC setting should be enabled in production so database-owned policies remain the
authorization source. `GET /api/v1/auth/session` deliberately reports identity authentication and
platform-subject resolution as separate states.

For local permission smoke tests, the server has a `demo`-profile-only `DemoPermissionProvider`.
The `X-CoreIb-Demo-Subject` request header selects the example nurse, head nurse, information
clerk, information leader or platform administrator. Its security administration fixture is mutable
in memory and resets on restart. It is not a production authentication mechanism; a deployed
installation should provide a JWT, OIDC or hospital identity adapter.
