package com.codesec.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.codesec.worker", "com.codesec.domain", "com.codesec.common", "com.codesec.engineadapter"})
@EnableJpaRepositories(basePackages = {"com.codesec.domain.repository"})
@EntityScan(basePackages = {"com.codesec.domain.entity"})
public class WorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
