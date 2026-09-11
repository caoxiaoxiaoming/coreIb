package com.coreib.server.api;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** JDBC metadata driven generator; no database-specific catalog SQL is used. */
final class MetadataCodeGenerator implements CoreIbCodeGenerator {
    private final DataSource dataSource;

    MetadataCodeGenerator(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public List<TableSummary> tables() {
        if (dataSource == null) return List.of(new TableSummary("DEMO", "coreib_patient", "代码生成演示表", 6));
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            List<TableSummary> result = new ArrayList<>();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), schema(connection), "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String schema = tables.getString("TABLE_SCHEM");
                    String name = tables.getString("TABLE_NAME");
                    if (systemTable(schema, name)) continue;
                    result.add(new TableSummary(schema, name, tables.getString("REMARKS"), columns(metadata, connection.getCatalog(), schema, name).size()));
                }
            }
            result.sort(java.util.Comparator.comparing(TableSummary::tableName, String.CASE_INSENSITIVE_ORDER));
            return result;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to read database metadata", exception);
        }
    }

    @Override public CodePreview preview(String tableName, String moduleName) {
        requireName(tableName, "tableName");
        String module = normalizeModule(moduleName == null || moduleName.isBlank() ? stripPrefix(tableName) : moduleName);
        List<ColumnSummary> columns = readColumns(tableName);
        if (columns.isEmpty()) throw new IllegalArgumentException("Unknown table or table has no columns: " + tableName);
        String className = pascal(module);
        String resource = kebab(module);
        String selectColumns = columns.stream().map(ColumnSummary::name).reduce((a,b) -> a + ", " + b).orElse("*");
        String recordFields = columns.stream().map(c -> javaType(c) + " " + camel(c.name())).reduce((a,b) -> a + ",\n        " + b).orElse("");
        String tsFields = columns.stream().map(c -> "  " + camel(c.name()) + (c.nullable() ? "?" : "") + ": " + tsType(c.javaType())).reduce((a,b) -> a + "\n" + b).orElse("");
        List<GeneratedFile> files = List.of(
                new GeneratedFile("backend/" + className + ".java", "java", entityTemplate(className, recordFields)),
                new GeneratedFile("backend/" + className + "Repository.java", "java", repositoryTemplate(className, tableName, selectColumns)),
                new GeneratedFile("backend/" + className + "Controller.java", "java", controllerTemplate(className, resource)),
                new GeneratedFile("frontend/" + resource + ".ts", "typescript", apiTemplate(className, resource, tsFields)),
                new GeneratedFile("frontend/" + className + "View.vue", "vue", viewTemplate(className, resource, columns)),
                new GeneratedFile("database/" + resource + ".yaml", "yaml", changelogTemplate(tableName, columns)));
        return new CodePreview(tableName, module, columns, files);
    }

    @Override public byte[] archive(String tableName, String moduleName) {
        CodePreview preview = preview(tableName, moduleName);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (GeneratedFile file : preview.files()) {
                zip.putNextEntry(new ZipEntry(file.path()));
                zip.write(file.content().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create generated source archive", exception);
        }
    }

    private List<ColumnSummary> readColumns(String tableName) {
        if (dataSource == null) return List.of(
                new ColumnSummary("id", "VARCHAR", "String", false, true),
                new ColumnSummary("patient_name", "VARCHAR", "String", false, false),
                new ColumnSummary("organization_id", "VARCHAR", "String", false, false),
                new ColumnSummary("assigned_user_id", "VARCHAR", "String", true, false),
                new ColumnSummary("enabled", "BOOLEAN", "Boolean", false, false),
                new ColumnSummary("created_at", "TIMESTAMP", "Instant", false, false));
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            for (TableSummary table : tables()) {
                if (table.tableName().equalsIgnoreCase(tableName)) return columns(metadata, connection.getCatalog(), table.schema(), table.tableName());
            }
            return List.of();
        } catch (SQLException exception) { throw new IllegalStateException("Unable to read table columns", exception); }
    }

    private static List<ColumnSummary> columns(DatabaseMetaData metadata, String catalog, String schema, String table) throws SQLException {
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet keys = metadata.getPrimaryKeys(catalog, schema, table)) {
            while (keys.next()) primaryKeys.add(keys.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        List<ColumnSummary> result = new ArrayList<>();
        try (ResultSet columns = metadata.getColumns(catalog, schema, table, "%")) {
            while (columns.next()) {
                int jdbcType = columns.getInt("DATA_TYPE");
                String name = columns.getString("COLUMN_NAME");
                result.add(new ColumnSummary(name, columns.getString("TYPE_NAME"), javaType(jdbcType),
                        columns.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                        primaryKeys.contains(name.toLowerCase(Locale.ROOT))));
            }
        }
        return result;
    }

    private static String entityTemplate(String className, String fields) {
        return """
                package com.coreib.generated;

                /** Generated by coreIb. Review domain rules before production use. */
                public record %s(
                        %s
                ) { }
                """.formatted(className, fields);
    }
    private static String repositoryTemplate(String className, String table, String columns) {
        return """
                package com.coreib.generated;

                import org.springframework.jdbc.core.JdbcTemplate;
                import java.util.List;

                public final class %sRepository {
                    private static final String SELECT_ALL = "SELECT %s FROM %s";
                    private final JdbcTemplate jdbc;
                    public %sRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
                    public List<java.util.Map<String,Object>> findAll() { return jdbc.queryForList(SELECT_ALL); }
                }
                """.formatted(className, columns, table, className);
    }
    private static String controllerTemplate(String className, String resource) {
        return """
                package com.coreib.generated;

                import org.springframework.web.bind.annotation.*;
                import java.util.List;
                import java.util.Map;

                @RestController
                @RequestMapping("/api/v1/%s")
                public class %sController {
                    private final %sRepository repository;
                    public %sController(%sRepository repository) { this.repository = repository; }
                    @GetMapping public List<Map<String,Object>> list() { return repository.findAll(); }
                }
                """.formatted(resource, className, className, className, className);
    }
    private static String apiTemplate(String className, String resource, String fields) {
        return """
                import { coreIbFetch } from '@/auth-client'

                export interface %s {
                %s
                }
                export async function fetch%s(): Promise<%s[]> {
                  const response = await coreIbFetch('/api/v1/%s')
                  if (!response.ok) throw new Error(`HTTP ${response.status}`)
                  return response.json()
                }
                """.formatted(className, fields, className, className, resource);
    }
    private static String viewTemplate(String className, String resource, List<ColumnSummary> columns) {
        String tableColumns = columns.stream().limit(8).map(c -> "    <el-table-column prop=\"" + camel(c.name()) + "\" label=\"" + c.name() + "\" min-width=\"140\" />").reduce((a,b) -> a + "\n" + b).orElse("");
        return """
                <script setup lang="ts">
                import { onMounted, ref } from 'vue'
                import { fetch%s, type %s } from './%s'
                const rows = ref<%s[]>([])
                onMounted(async () => { rows.value = await fetch%s() })
                </script>
                <template>
                  <section class="page-view"><div class="page-heading"><h1>%s</h1></div>
                    <section class="surface-panel"><el-table :data="rows" stripe>
                %s
                    </el-table></section>
                  </section>
                </template>
                """.formatted(className,className,resource,className,className,className,tableColumns);
    }
    private static String changelogTemplate(String table, List<ColumnSummary> columns) {
        String lines = columns.stream().map(c -> "              - column: { name: " + c.name() + ", type: " + liquibaseType(c) + " }").reduce((a,b) -> a + "\n" + b).orElse("");
        return """
                databaseChangeLog:
                  - changeSet:
                      id: generated-%s
                      author: coreib
                      changes:
                        - createTable:
                            tableName: %s
                            columns:
                %s
                """.formatted(kebab(table), table, lines);
    }

    private static String schema(Connection connection) { try { return connection.getSchema(); } catch (SQLException ignored) { return null; } }
    private static boolean systemTable(String schema, String name) {
        String s = schema == null ? "" : schema.toUpperCase(Locale.ROOT);
        String n = name.toUpperCase(Locale.ROOT);
        return s.startsWith("SYS") || s.equals("INFORMATION_SCHEMA") || n.startsWith("DATABASECHANGELOG");
    }
    private static String javaType(int type) { return switch (type) {
        case Types.BIGINT -> "Long"; case Types.INTEGER,Types.SMALLINT,Types.TINYINT -> "Integer";
        case Types.NUMERIC,Types.DECIMAL,Types.FLOAT,Types.DOUBLE,Types.REAL -> "BigDecimal";
        case Types.BOOLEAN,Types.BIT -> "Boolean"; case Types.DATE -> "LocalDate";
        case Types.TIME,Types.TIME_WITH_TIMEZONE -> "LocalTime"; case Types.TIMESTAMP,Types.TIMESTAMP_WITH_TIMEZONE -> "Instant";
        case Types.BINARY,Types.VARBINARY,Types.LONGVARBINARY,Types.BLOB -> "byte[]"; default -> "String"; };
    }
    private static String javaType(ColumnSummary c) {
        return switch (c.javaType()) { case "BigDecimal" -> "java.math.BigDecimal"; case "LocalDate" -> "java.time.LocalDate"; case "LocalTime" -> "java.time.LocalTime"; case "Instant" -> "java.time.Instant"; default -> c.javaType(); };
    }
    private static String tsType(String javaType) { return switch (javaType) { case "Integer","Long","BigDecimal" -> "number"; case "Boolean" -> "boolean"; case "byte[]" -> "string"; default -> "string"; }; }
    private static String liquibaseType(ColumnSummary c) { return switch (c.javaType()) { case "Integer" -> "int"; case "Long" -> "bigint"; case "BigDecimal" -> "decimal(19,4)"; case "Boolean" -> "boolean"; case "Instant","LocalDate","LocalTime" -> "datetime"; case "byte[]" -> "blob"; default -> "varchar(255)"; }; }
    private static String stripPrefix(String value) { return value.replaceFirst("(?i)^(coreib_|sys_|biz_)", ""); }
    private static String normalizeModule(String value) { requireName(value,"moduleName"); return value.replaceAll("[^A-Za-z0-9_-]", "-"); }
    private static void requireName(String value, String name) { if (value == null || value.isBlank() || !value.matches("[A-Za-z0-9_.-]+")) throw new IllegalArgumentException(name + " contains unsupported characters"); }
    private static String pascal(String value) { String result = pascalWords(value); return Character.isDigit(result.charAt(0)) ? "Generated" + result : result; }
    private static String pascalWords(String value) { StringBuilder result = new StringBuilder(); for (String part : value.split("[^A-Za-z0-9]+")) if (!part.isEmpty()) result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT)); return result.length() == 0 ? "Generated" : result.toString(); }
    private static String camel(String value) { String p = pascal(value); return Character.toLowerCase(p.charAt(0)) + p.substring(1); }
    private static String kebab(String value) { return value.replace('_','-').replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT); }
}
