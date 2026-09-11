package com.coreib.server.api;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class CodeGenerationConfiguration {
    @Bean CoreIbCodeGenerator coreIbCodeGenerator(ObjectProvider<DataSource> source) {
        return new MetadataCodeGenerator(source.getIfAvailable());
    }
}
