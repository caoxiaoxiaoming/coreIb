package com.coreib.starter.jdbc;

import com.coreib.security.RowAccessCriteria;
import com.coreib.security.RowScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcRowAccessPredicateCompilerTest {
    private static final JdbcRowColumns COLUMNS = new JdbcRowColumns(
            "br.record_id", "br.owner_user_id", "br.organization_id");

    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        jdbc = new JdbcTemplate(database);
        jdbc.execute("""
                CREATE TABLE business_record (
                    record_id VARCHAR(64) PRIMARY KEY,
                    owner_user_id VARCHAR(64),
                    organization_id VARCHAR(64)
                )
                """);
        jdbc.execute("""
                CREATE TABLE coreib_sec_record_assignment (
                    resource VARCHAR(128),
                    record_id VARCHAR(128),
                    user_id VARCHAR(64),
                    active BOOLEAN,
                    effective_from TIMESTAMP,
                    effective_to TIMESTAMP
                )
                """);
        insertRecord("r1", "nurse-1", "ward-a");
        insertRecord("r2", "nurse-2", "ward-b");
        insertRecord("r3", "other", "ward-a");
        insertRecord("r4", "other", "ward-b");
        insertRecord("r5", "other", "ward-b");
        insertRecord("r6", "other", "ward-b");
        jdbc.update("INSERT INTO coreib_sec_record_assignment VALUES ('patient', 'r2', 'nurse-1', TRUE, NULL, NULL)");
        jdbc.update("INSERT INTO coreib_sec_record_assignment VALUES ('patient', 'r5', 'nurse-2', TRUE, NULL, NULL)");
        jdbc.update("INSERT INTO coreib_sec_record_assignment VALUES ('encounter', 'r4', 'nurse-1', TRUE, NULL, NULL)");
        jdbc.update("INSERT INTO coreib_sec_record_assignment VALUES ('patient', 'r6', 'nurse-2', TRUE, NULL, ?)",
                Timestamp.from(Instant.now().minusSeconds(3600)));
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void combinesOrganizationAndManagedUserScopesWithOrSemantics() {
        RowAccessCriteria criteria = criteria(
                Set.of(RowScope.ORGANIZATION, RowScope.MANAGED_USERS),
                "head-nurse", "ward-a", Set.of("ward-a"),
                Set.of("nurse-1", "nurse-2"), Set.of());

        assertThat(find(criteria)).containsExactly("r1", "r2", "r3", "r5");
    }

    @Test
    void scopesAssignmentsByResourceAndEffectiveTime() {
        RowAccessCriteria criteria = criteria(
                Set.of(RowScope.RELATED_RECORDS),
                "nurse-1", "ward-a", Set.of("ward-a"), Set.of(), Set.of("r4"));

        assertThat(find(criteria)).containsExactly("r2", "r4");
    }

    @Test
    void denyAllAndAllowAllAreExplicitPredicates() {
        RowAccessCriteria denied = criteria(
                Set.of(), "user-1", null, Set.of(), Set.of(), Set.of());
        RowAccessCriteria allowed = criteria(
                Set.of(RowScope.ALL), "user-1", null, Set.of(), Set.of(), Set.of());

        assertThat(find(denied)).isEmpty();
        assertThat(find(allowed)).containsExactly("r1", "r2", "r3", "r4", "r5", "r6");
    }

    @Test
    void rejectsUntrustedSqlExpressions() {
        assertThatThrownBy(() -> new JdbcRowColumns(
                "br.record_id; DELETE FROM business_record", "br.owner_user_id", "br.organization_id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("simple SQL identifier");
    }

    @Test
    void rejectsOversizedInListsBeforeTheyReachVendorLimits() {
        Set<String> organizationIds = IntStream.rangeClosed(
                        1, JdbcRowAccessPredicateCompiler.MAX_IN_VALUES + 1)
                .mapToObj(index -> "org-" + index)
                .collect(Collectors.toSet());
        RowAccessCriteria criteria = criteria(
                Set.of(RowScope.ORGANIZATION_TREE),
                "user-1", "org-1", organizationIds, Set.of(), Set.of());

        assertThatThrownBy(() -> JdbcRowAccessPredicateCompiler.compile("patient", criteria, COLUMNS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("join-based repository strategy");
    }

    private List<String> find(RowAccessCriteria criteria) {
        JdbcRowAccessPredicate predicate = JdbcRowAccessPredicateCompiler.compile(
                "patient", criteria, COLUMNS);
        return jdbc.query(
                "SELECT br.record_id FROM business_record br WHERE " + predicate.sql() + " ORDER BY br.record_id",
                (result, row) -> result.getString(1),
                predicate.parameters().toArray());
    }

    private void insertRecord(String id, String ownerId, String organizationId) {
        jdbc.update("INSERT INTO business_record VALUES (?, ?, ?)", id, ownerId, organizationId);
    }

    private static RowAccessCriteria criteria(
            Set<RowScope> scopes,
            String subjectId,
            String organizationId,
            Set<String> visibleOrganizationIds,
            Set<String> managedUserIds,
            Set<String> relatedRecordIds) {
        return new RowAccessCriteria(
                scopes,
                Set.of(),
                subjectId,
                organizationId,
                visibleOrganizationIds,
                managedUserIds,
                relatedRecordIds);
    }
}
