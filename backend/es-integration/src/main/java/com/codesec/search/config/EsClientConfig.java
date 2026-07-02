package com.codesec.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import jakarta.annotation.PostConstruct;
import org.apache.hc.core5.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * ES Java Client 9.x configuration + index auto-initialization.
 * Reads vuln.json / file_snippet.json mapping files and creates indices on startup if they don't exist.
 */
@Configuration
public class EsClientConfig {

    private static final Logger log = LoggerFactory.getLogger(EsClientConfig.class);

    @Value("${es.host:localhost}")
    private String esHost;

    @Value("${es.port:9200}")
    private int esPort;

    @Value("${es.index.prefix:codesec}")
    private String indexPrefix;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        Rest5Client restClient = Rest5Client.builder(
                new HttpHost("http", esHost, esPort)
        ).build();

        Rest5ClientTransport transport = new Rest5ClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }

    @Bean
    public EsIndexInitializer esIndexInitializer(ElasticsearchClient client) {
        return new EsIndexInitializer(client, indexPrefix);
    }

    /**
     * Auto-initializes vuln and file_snippet indices on startup if they don't exist.
     */
    public static class EsIndexInitializer {

        private final ElasticsearchClient client;
        private final String prefix;

        public EsIndexInitializer(ElasticsearchClient client, String prefix) {
            this.client = client;
            this.prefix = prefix;
        }

        @PostConstruct
        public void initIndices() {
            createIndexIfNotExists("vuln", "elasticsearch/vuln.json");
            createIndexIfNotExists("file_snippet", "elasticsearch/file_snippet.json");
        }

        private void createIndexIfNotExists(String indexName, String mappingResource) {
            String fullName = prefix + "_" + indexName;
            try {
                boolean exists = client.indices().exists(
                        ExistsRequest.of(e -> e.index(fullName))
                ).value();

                if (exists) {
                    log.info("ES index '{}' already exists, skipping creation", fullName);
                    return;
                }

                String mappingJson = loadResource(mappingResource);
                client.indices().create(
                        CreateIndexRequest.of(c -> c
                                .index(fullName)
                                .withJson(new java.io.StringReader(mappingJson))
                        )
                );
                log.info("ES index '{}' created successfully from {}", fullName, mappingResource);
            } catch (Exception e) {
                log.warn("ES index '{}' init skipped — ES unavailable: {}", fullName, e.getMessage());
            }
        }

        private String loadResource(String path) throws IOException {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + path);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        public String getVulnIndex() { return prefix + "_vuln"; }
        public String getSnippetIndex() { return prefix + "_file_snippet"; }
    }
}
