package com.codesec.engine.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public class CpgConfiguration {

    private final String uri;
    private final String username;
    private final String password;
    private final boolean enabled;

    private Driver driver;

    public CpgConfiguration() {
        this.uri = System.getenv().getOrDefault("NEO4J_URI", "bolt://localhost:7687");
        this.username = System.getenv().getOrDefault("NEO4J_USERNAME", "neo4j");
        this.password = System.getenv().getOrDefault("NEO4J_PASSWORD", "neo4j");
        String store = System.getenv().getOrDefault("CPG_STORE", "memory");
        this.enabled = "neo4j".equalsIgnoreCase(store);
    }

    public CpgConfiguration(String uri, String username, String password, boolean enabled) {
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
    }

    public Driver getDriver() {
        if (driver == null || !driver.isEncrypted()) {
            this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        }
        return driver;
    }

    public boolean isEnabled() { return enabled; }
    public String getUri() { return uri; }
    public String getUsername() { return username; }

    public void close() {
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception ignored) {}
        }
    }
}
