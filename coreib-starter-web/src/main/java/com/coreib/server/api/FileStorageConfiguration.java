package com.coreib.server.api;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class FileStorageConfiguration {
    @Bean
    CoreIbFileStorage coreIbFileStorage(
            @Value("${coreib.files.directory:${java.io.tmpdir}/coreib-files}") String directory,
            ObjectProvider<DataSource> dataSource) {
        DataSource source = dataSource.getIfAvailable();
        return new LocalFileStorage(Path.of(directory), source == null ? null : new JdbcTemplate(source));
    }
}
