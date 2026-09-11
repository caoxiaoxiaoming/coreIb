package com.coreib.server.api;

public record SystemInfoResponse(
        String name,
        String version,
        String javaVersion,
        String[] supportedDatabases,
        String activeDatabase) {
}
