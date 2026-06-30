package com.codesec.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application for es-integration module tests.
 * Does NOT scan config package — those beans (ElasticsearchClient, etc.)
 * are incompatible with Mockito on JDK 25 and are excluded from unit tests.
 */
@SpringBootApplication(scanBasePackages = "com.codesec.search.controller")
public class TestEsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestEsApplication.class, args);
    }
}
