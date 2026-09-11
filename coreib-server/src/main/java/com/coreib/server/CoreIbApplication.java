package com.coreib.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
public class CoreIbApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreIbApplication.class, args);
    }
}
