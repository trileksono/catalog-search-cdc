package com.tleksono.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tleksono.search.domain.*;
import com.tleksono.search.dto.EnrichedProductResponse;
import com.tleksono.search.repository.ElasticsearchProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Repartitioned;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SearchStream {

    private final ElasticsearchProductRepository esRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.product-topic}")
    private String productTopic;

    @Value("${app.kafka.category-topic}")
    private String categoryTopic;

    @Value("${app.kafka.enriched-topic}")
    private String enrichedTopic;

    @Bean
    public KStream<String, ProductEvent> productStream(StreamsBuilder builder) {
        Serde<CategoryEvent> categorySerde = JsonSerdeFactory.categoryEventSerde();
        Serde<ProductEvent> productSerde = JsonSerdeFactory.productEventSerde();
        Serde<EnrichedProductResponse> enrichedSerde = JsonSerdeFactory.enrichedSerde();

        KTable<String, CategoryEvent> categoryTable = builder
                .stream(categoryTopic, Consumed.with(Serdes.String(), JsonSerdeFactory.outboxRowSerde()))
                .filter((k, row) -> row != null && row.payload() != null)
                .mapValues(this::toCategoryEvent)
                .selectKey((k, ev) -> String.valueOf(ev.payload().id()))
                .repartition(Repartitioned.with(Serdes.String(), categorySerde))
                .toTable(Materialized.<String, CategoryEvent, KeyValueStore<Bytes, byte[]>>with(Serdes.String(), categorySerde));

        KStream<String, ProductEvent> productStream = builder
                .stream(productTopic, Consumed.with(Serdes.String(), JsonSerdeFactory.outboxRowSerde()))
                .filter((k, row) -> row != null && row.payload() != null)
                .mapValues(this::toProductEvent);

        KStream<String, EnrichedProductResponse> enriched = productStream
                .filter((k, ev) -> ev.payload() != null && ev.payload().id() != null)
                .selectKey((k, ev) -> String.valueOf(ev.payload().categoryId()))
                .repartition(Repartitioned.with(Serdes.String(), productSerde))
                .join(categoryTable,
                        (product, category) -> {
                            ProductPayload p = product.payload();
                            CategoryPayload c = category == null ? null : category.payload();
                            String categoryName = c == null ? "Unknown" : c.name();
                            return new EnrichedProductResponse(
                                    p.id(),
                                    p.name(),
                                    p.price(),
                                    p.categoryId(),
                                    categoryName,
                                    p.stock(),
                                    p.active());
                        },
                        Joined.with(Serdes.String(), productSerde, categorySerde))
                .selectKey((k, ep) -> String.valueOf(ep.id()));

        enriched.peek((k, ep) -> {
            log.info("Enriched product {}: cat={}, price={}", ep.id(), ep.categoryName(), ep.price());
            try {
                if (Boolean.TRUE.equals(ep.active())) {
                    esRepository.index(ep);
                } else {
                    esRepository.delete(ep.id());
                }
            } catch (Exception e) {
                log.error("Failed to sink enriched product {}", ep.id(), e);
            }
        });

        enriched.to(enrichedTopic, Produced.with(Serdes.String(), enrichedSerde));

        return productStream;
    }

    private ProductEvent toProductEvent(OutboxRow row) {
        try {
            ProductPayload p = objectMapper.readValue(row.payload(), ProductPayload.class);
            return new ProductEvent(
                    UUID.randomUUID().toString(),
                    row.aggregateId(),
                    row.aggregateType(),
                    row.eventType(),
                    p,
                    row.createdAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse product payload from outbox row " + row.id(), e);
        }
    }

    private CategoryEvent toCategoryEvent(OutboxRow row) {
        try {
            CategoryPayload c = objectMapper.readValue(row.payload(), CategoryPayload.class);
            return new CategoryEvent(
                    UUID.randomUUID().toString(),
                    row.aggregateId(),
                    row.aggregateType(),
                    row.eventType(),
                    c,
                    row.createdAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse category payload from outbox row " + row.id(), e);
        }
    }

    static final class JsonSerdeFactory {
        static org.springframework.kafka.support.serializer.JsonSerde<OutboxRow> outboxRowSerde() {
            return new org.springframework.kafka.support.serializer.JsonSerde<>(OutboxRow.class);
        }

        static org.springframework.kafka.support.serializer.JsonSerde<CategoryEvent> categoryEventSerde() {
            return new org.springframework.kafka.support.serializer.JsonSerde<>(CategoryEvent.class);
        }

        static org.springframework.kafka.support.serializer.JsonSerde<ProductEvent> productEventSerde() {
            return new org.springframework.kafka.support.serializer.JsonSerde<>(ProductEvent.class);
        }

        static org.springframework.kafka.support.serializer.JsonSerde<EnrichedProductResponse> enrichedSerde() {
            return new org.springframework.kafka.support.serializer.JsonSerde<>(EnrichedProductResponse.class);
        }
    }
}
