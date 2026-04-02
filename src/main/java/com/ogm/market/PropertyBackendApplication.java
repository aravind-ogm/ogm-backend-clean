package com.ogm.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PropertyBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PropertyBackendApplication.class, args);
    }
}
