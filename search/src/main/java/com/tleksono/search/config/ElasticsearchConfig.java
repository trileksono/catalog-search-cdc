package com.tleksono.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.tleksono.search.repository.ElasticsearchProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;

import java.io.IOException;

@Slf4j
@Configuration
public class ElasticsearchConfig {

    @Value("${app.elasticsearch.host}")
    private String host;

    @Value("${app.elasticsearch.port}")
    private int port;

    private final ElasticsearchProductRepository repository;

    public ElasticsearchConfig(@Lazy ElasticsearchProductRepository repository) {
        this.repository = repository;
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(new HttpHost(host, port, "http")).build();
        JacksonJsonpMapper mapper = new JacksonJsonpMapper();
        RestClientTransport transport = new RestClientTransport(restClient, mapper);
        return new ElasticsearchClient(transport);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            repository.ensureIndexExists();
            log.info("Elasticsearch index verified at {}:{}", host, port);
        } catch (IOException e) {
            log.error("Failed to ensure ES index", e);
        }
    }
}
