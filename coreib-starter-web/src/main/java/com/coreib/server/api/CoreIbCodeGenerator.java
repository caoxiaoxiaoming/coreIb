package com.coreib.server.api;

import java.util.List;

interface CoreIbCodeGenerator {
    List<TableSummary> tables();
    CodePreview preview(String tableName, String moduleName);
    byte[] archive(String tableName, String moduleName);

    record TableSummary(String schema, String tableName, String remarks, int columnCount) { }
    record ColumnSummary(String name, String jdbcType, String javaType, boolean nullable, boolean primaryKey) { }
    record GeneratedFile(String path, String language, String content) { }
    record CodePreview(String tableName, String moduleName, List<ColumnSummary> columns, List<GeneratedFile> files) { }
}
