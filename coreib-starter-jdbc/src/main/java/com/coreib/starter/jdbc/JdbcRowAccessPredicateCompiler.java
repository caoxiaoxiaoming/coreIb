package com.coreib.starter.jdbc;

import com.coreib.security.RowAccessCriteria;
import com.coreib.security.RowScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Compiles standard row scopes to portable, parameterized JDBC predicates. */
public final class JdbcRowAccessPredicateCompiler {
    public static final int MAX_IN_VALUES = 900;
    private static final JdbcRowAccessPredicate DENY_ALL =
            new JdbcRowAccessPredicate("(1 = 0)", List.of());
    private static final JdbcRowAccessPredicate ALLOW_ALL =
            new JdbcRowAccessPredicate("(1 = 1)", List.of());

    private JdbcRowAccessPredicateCompiler() {
    }

    public static JdbcRowAccessPredicate compile(
            String resource,
            RowAccessCriteria criteria,
            JdbcRowColumns columns) {
        requireText("resource", resource);
        Objects.requireNonNull(criteria, "criteria");
        Objects.requireNonNull(columns, "columns");
        if (criteria.unrestricted()) {
            return ALLOW_ALL;
        }
        if (criteria.denied()) {
            return DENY_ALL;
        }

        List<String> alternatives = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        criteria.scopes().stream().sorted().forEach(scope -> appendScope(
                scope, resource, criteria, columns, alternatives, parameters));
        return alternatives.isEmpty()
                ? DENY_ALL
                : new JdbcRowAccessPredicate(
                        "(" + String.join(" OR ", alternatives) + ")",
                        parameters);
    }

    private static void appendScope(
            RowScope scope,
            String resource,
            RowAccessCriteria criteria,
            JdbcRowColumns columns,
            List<String> alternatives,
            List<Object> parameters) {
        switch (scope) {
            case ALL -> alternatives.add("1 = 1");
            case ORGANIZATION -> appendEquals(
                    columns.organizationId(), criteria.organizationId(), alternatives, parameters);
            case ORGANIZATION_TREE -> appendIn(
                    columns.organizationId(), criteria.visibleOrganizationIds(), alternatives, parameters);
            case SELF -> appendEquals(
                    columns.ownerUserId(), criteria.subjectId(), alternatives, parameters);
            case MANAGED_USERS -> appendManagedUsers(
                    resource, criteria, columns, alternatives, parameters);
            case RELATED_RECORDS -> appendRelatedRecords(
                    resource, criteria, columns, alternatives, parameters);
            case CUSTOM -> throw new IllegalArgumentException("CUSTOM row scope is not executable");
        }
    }

    private static void appendManagedUsers(
            String resource,
            RowAccessCriteria criteria,
            JdbcRowColumns columns,
            List<String> alternatives,
            List<Object> parameters) {
        LinkedHashSet<String> userIds = sorted(criteria.managedUserIds());
        userIds.add(criteria.subjectId());
        List<Object> local = new ArrayList<>();
        String owner = inPredicate(columns.ownerUserId(), userIds, local);
        String assignment = assignmentExists(resource, columns.rowId(), userIds, local);
        alternatives.add("(" + owner + " OR " + assignment + ")");
        parameters.addAll(local);
    }

    private static void appendRelatedRecords(
            String resource,
            RowAccessCriteria criteria,
            JdbcRowColumns columns,
            List<String> alternatives,
            List<Object> parameters) {
        List<String> related = new ArrayList<>();
        List<Object> local = new ArrayList<>();
        if (!criteria.relatedRecordIds().isEmpty()) {
            related.add(inPredicate(columns.rowId(), sorted(criteria.relatedRecordIds()), local));
        }
        related.add(assignmentExists(
                resource,
                columns.rowId(),
                List.of(criteria.subjectId()),
                local));
        alternatives.add("(" + String.join(" OR ", related) + ")");
        parameters.addAll(local);
    }

    private static String assignmentExists(
            String resource,
            String rowId,
            Collection<String> userIds,
            List<Object> parameters) {
        parameters.add(resource);
        String userPredicate = inPredicate("cra.user_id", userIds, parameters);
        parameters.add(Boolean.TRUE);
        return "EXISTS (SELECT 1 FROM coreib_sec_record_assignment cra "
                + "WHERE cra.resource = ? AND cra.record_id = " + rowId
                + " AND " + userPredicate
                + " AND cra.active = ?"
                + " AND (cra.effective_from IS NULL OR cra.effective_from <= CURRENT_TIMESTAMP)"
                + " AND (cra.effective_to IS NULL OR cra.effective_to >= CURRENT_TIMESTAMP))";
    }

    private static void appendEquals(
            String column,
            String value,
            List<String> alternatives,
            List<Object> parameters) {
        if (value != null && !value.isBlank()) {
            alternatives.add(column + " = ?");
            parameters.add(value);
        }
    }

    private static void appendIn(
            String column,
            Collection<String> values,
            List<String> alternatives,
            List<Object> parameters) {
        if (values != null && !values.isEmpty()) {
            alternatives.add(inPredicate(column, sorted(values), parameters));
        }
    }

    private static String inPredicate(
            String column,
            Collection<String> values,
            List<Object> parameters) {
        if (values == null || values.isEmpty()) {
            return "1 = 0";
        }
        if (values.size() > MAX_IN_VALUES) {
            throw new IllegalArgumentException(
                    "row access set exceeds " + MAX_IN_VALUES
                            + " values; use a join-based repository strategy");
        }
        parameters.addAll(values);
        return column + " IN (" + String.join(",", values.stream().map(ignored -> "?").toList()) + ")";
    }

    private static LinkedHashSet<String> sorted(Collection<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .sorted()
                    .forEach(result::add);
        }
        return result;
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
