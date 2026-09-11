package com.coreib.starter.jdbc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Migration settings kept compatible with the standard spring.liquibase namespace. */
@ConfigurationProperties(prefix = "spring.liquibase")
public class CoreIbLiquibaseProperties {
    private boolean enabled = true;
    private String changeLog = "classpath:db/changelog/db.changelog-master.yaml";
    private String contexts;
    private String labels;
    private String defaultSchema;
    private String liquibaseSchema;
    private String databaseChangeLogTable = "DATABASECHANGELOG";
    private String databaseChangeLogLockTable = "DATABASECHANGELOGLOCK";
    private boolean dropFirst;
    private boolean clearChecksums;
    private boolean testRollbackOnUpdate;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getChangeLog() { return changeLog; }
    public void setChangeLog(String changeLog) { this.changeLog = changeLog; }
    public String getContexts() { return contexts; }
    public void setContexts(String contexts) { this.contexts = contexts; }
    public String getLabels() { return labels; }
    public void setLabels(String labels) { this.labels = labels; }
    public String getDefaultSchema() { return defaultSchema; }
    public void setDefaultSchema(String defaultSchema) { this.defaultSchema = defaultSchema; }
    public String getLiquibaseSchema() { return liquibaseSchema; }
    public void setLiquibaseSchema(String liquibaseSchema) { this.liquibaseSchema = liquibaseSchema; }
    public String getDatabaseChangeLogTable() { return databaseChangeLogTable; }
    public void setDatabaseChangeLogTable(String databaseChangeLogTable) { this.databaseChangeLogTable = databaseChangeLogTable; }
    public String getDatabaseChangeLogLockTable() { return databaseChangeLogLockTable; }
    public void setDatabaseChangeLogLockTable(String databaseChangeLogLockTable) { this.databaseChangeLogLockTable = databaseChangeLogLockTable; }
    public boolean isDropFirst() { return dropFirst; }
    public void setDropFirst(boolean dropFirst) { this.dropFirst = dropFirst; }
    public boolean isClearChecksums() { return clearChecksums; }
    public void setClearChecksums(boolean clearChecksums) { this.clearChecksums = clearChecksums; }
    public boolean isTestRollbackOnUpdate() { return testRollbackOnUpdate; }
    public void setTestRollbackOnUpdate(boolean testRollbackOnUpdate) { this.testRollbackOnUpdate = testRollbackOnUpdate; }
}
