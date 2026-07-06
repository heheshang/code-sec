package com.codesec.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"com.codesec.api", "com.codesec.domain", "com.codesec.common"})
@EnableJpaRepositories(basePackages = {"com.codesec.domain.repository", "com.codesec.api"})
@EntityScan(basePackages = {"com.codesec.domain.entity", "com.codesec.api"})
@EnableAspectJAutoProxy
@EnableAsync
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
