package com.coreib.server.api;

import com.coreib.platform.OrganizationSummary;
import com.coreib.platform.PlatformDirectory;
import com.coreib.platform.RoleSummary;
import com.coreib.platform.UserSummary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Portable JDBC adapter for the platform directory tables. SQL stays deliberately simple so
 * the same adapter works with SQL Server, Oracle and PostgreSQL through their JDBC drivers.
 */
public final class JdbcPlatformDirectory implements PlatformDirectory {
    private static final String ORGANIZATIONS_SQL = """
            SELECT id, parent_id, org_code, display_name, org_type, enabled
            FROM coreib_sys_organization
            ORDER BY sort_order, display_name
            """;
    private static final String USERS_SQL = """
            SELECT u.id, u.login_name, u.display_name, u.organization_id,
                   o.display_name AS organization_name, u.enabled, r.role_code
            FROM coreib_sys_user u
            LEFT JOIN coreib_sys_organization o ON o.id = u.organization_id
            LEFT JOIN coreib_sec_user_role ur ON ur.user_id = u.id
            LEFT JOIN coreib_sec_role r ON r.role_code = ur.role_code
            ORDER BY u.display_name, r.role_code
            """;
    private static final String ROLES_SQL = """
            SELECT role_code, display_name, description, enabled
            FROM coreib_sec_role
            ORDER BY display_name, role_code
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcPlatformDirectory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OrganizationSummary> organizations() {
        return jdbcTemplate.query(ORGANIZATIONS_SQL, JdbcPlatformDirectory::mapOrganization);
    }

    @Override
    public List<UserSummary> users() {
        Map<String, UserAccumulator> users = new LinkedHashMap<>();
        jdbcTemplate.query(USERS_SQL, resultSet -> {
            String id = resultSet.getString("id");
            UserAccumulator user = users.computeIfAbsent(id, ignored -> mapUser(resultSet));
            String roleCode = resultSet.getString("role_code");
            if (roleCode != null && !roleCode.isBlank()) user.roleCodes.add(roleCode);
        });
        return users.values().stream().map(UserAccumulator::toSummary).toList();
    }

    @Override
    public List<RoleSummary> roles() {
        return jdbcTemplate.query(ROLES_SQL, (resultSet, rowNumber) -> new RoleSummary(
                resultSet.getString("role_code"),
                resultSet.getString("display_name"),
                resultSet.getString("description"),
                resultSet.getBoolean("enabled")));
    }

    private static OrganizationSummary mapOrganization(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OrganizationSummary(
                resultSet.getString("id"),
                resultSet.getString("parent_id"),
                resultSet.getString("org_code"),
                resultSet.getString("display_name"),
                resultSet.getString("org_type"),
                resultSet.getBoolean("enabled"));
    }

    private static UserAccumulator mapUser(ResultSet resultSet) {
        try {
            return new UserAccumulator(
                    resultSet.getString("id"),
                    resultSet.getString("login_name"),
                    resultSet.getString("display_name"),
                    resultSet.getString("organization_id"),
                    resultSet.getString("organization_name"),
                    resultSet.getBoolean("enabled"));
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to map platform user", exception);
        }
    }

    private static final class UserAccumulator {
        private final String id;
        private final String loginName;
        private final String displayName;
        private final String organizationId;
        private final String organizationName;
        private final boolean enabled;
        private final Set<String> roleCodes = new TreeSet<>();

        private UserAccumulator(String id, String loginName, String displayName,
                                String organizationId, String organizationName, boolean enabled) {
            this.id = id;
            this.loginName = loginName;
            this.displayName = displayName;
            this.organizationId = organizationId;
            this.organizationName = organizationName;
            this.enabled = enabled;
        }

        private UserSummary toSummary() {
            return new UserSummary(id, loginName, displayName, organizationId, organizationName, enabled, roleCodes);
        }
    }
}
