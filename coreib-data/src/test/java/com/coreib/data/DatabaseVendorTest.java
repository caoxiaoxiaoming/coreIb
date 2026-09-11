package com.coreib.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseVendorTest {
    @Test
    void detectsSupportedJdbcUrls() {
        assertEquals(DatabaseVendor.POSTGRESQL,
                DatabaseVendor.fromJdbcUrl("jdbc:postgresql://localhost:5432/coreib"));
        assertEquals(DatabaseVendor.SQL_SERVER,
                DatabaseVendor.fromJdbcUrl("jdbc:sqlserver://localhost;databaseName=coreib"));
        assertEquals(DatabaseVendor.ORACLE,
                DatabaseVendor.fromJdbcUrl("jdbc:oracle:thin:@//localhost:1521/FREEPDB1"));
    }

    @Test
    void acceptsHumanFriendlyVendorIds() {
        assertEquals(DatabaseVendor.SQL_SERVER, DatabaseVendor.fromId("sql_server"));
        assertEquals(DatabaseVendor.SQL_SERVER, DatabaseVendor.fromId("sql-server"));
        assertEquals(DatabaseVendor.POSTGRESQL, DatabaseVendor.fromId("POSTGRESQL"));
    }

    @Test
    void rejectsUnknownJdbcUrls() {
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseVendor.fromJdbcUrl("jdbc:h2:mem:coreib"));
    }
}
