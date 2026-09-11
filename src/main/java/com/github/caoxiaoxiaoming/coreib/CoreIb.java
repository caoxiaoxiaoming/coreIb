package com.github.caoxiaoxiaoming.coreib;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Entry point for coreIb metadata.
 */
public final class CoreIb {
    private static final String VERSION = loadVersion();

    private CoreIb() {
    }

    /**
     * Returns the version of the loaded coreIb artifact.
     *
     * @return the Maven project version
     */
    public static String version() {
        return VERSION;
    }

    private static String loadVersion() {
        Properties properties = new Properties();
        try (InputStream input = CoreIb.class.getResourceAsStream("/coreib-version.properties")) {
            if (input == null) {
                throw new IllegalStateException("Missing coreIb version resource");
            }
            properties.load(input);
            String version = properties.getProperty("version");
            if (version == null || version.trim().isEmpty()) {
                throw new IllegalStateException("Missing coreIb version value");
            }
            return version;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load coreIb version", exception);
        }
    }
}
