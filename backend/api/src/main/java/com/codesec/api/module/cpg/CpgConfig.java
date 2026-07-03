package com.codesec.api.module.cpg;

import com.codesec.engine.config.CpgConfiguration;
import com.codesec.engine.judge.CpgService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CpgConfig {

    @Value("${codesec.cpg.enabled:true}")
    private boolean enabled;

    @Value("${codesec.cpg.uri:bolt://localhost:7687}")
    private String uri;

    @Value("${codesec.cpg.username:neo4j}")
    private String username;

    @Value("${codesec.cpg.password:neo4j}")
    private String password;

    @Bean(destroyMethod = "close")
    public CpgService cpgService() {
        return new CpgService(new CpgConfiguration(uri, username, password, enabled));
    }
}
