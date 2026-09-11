package com.coreib.server.api;

import com.coreib.data.DatabaseVendor;
import com.coreib.kernel.PlatformModule;
import com.coreib.starter.jdbc.CoreIbDataSourceProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final CoreIbDataSourceProperties dataSourceProperties;

    public SystemController(CoreIbDataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @GetMapping("/info")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(
                PlatformModule.NAME,
                PlatformModule.VERSION,
                Runtime.version().toString(),
                new String[]{
                        DatabaseVendor.SQL_SERVER.name(),
                        DatabaseVendor.ORACLE.name(),
                        DatabaseVendor.POSTGRESQL.name()
                }, dataSourceProperties.isEnabled()
                        ? dataSourceProperties.getVendor().name()
                        : "DISABLED");
    }
}
