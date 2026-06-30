package com.tleksono.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tleksono.product.dto.ProductRequest;
import com.tleksono.product.dto.ProductResponse;
import com.tleksono.product.dto.event.ProductEvent;
import com.tleksono.product.entity.OutboxEventEntity;
import com.tleksono.product.entity.OutboxEventType;
import com.tleksono.product.entity.ProductEntity;
import com.tleksono.product.repository.OutboxRepository;
import com.tleksono.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String CACHE_NAME = "product";

    private final ProductRepository productRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Instant now = Instant.now();
        ProductEntity product = ProductEntity.builder()
                .name(request.name())
                .price(request.price())
                .categoryId(request.categoryId())
                .stock(request.stock())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        ProductEntity saved = productRepository.save(product);
        appendOutbox(OutboxEventType.PRODUCT_CREATED, saved);
        return toResponse(saved);
    }

    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setName(request.name());
        product.setPrice(request.price());
        product.setCategoryId(request.categoryId());
        product.setStock(request.stock());
        product.setUpdatedAt(Instant.now());
        ProductEntity saved = productRepository.save(product);
        appendOutbox(OutboxEventType.PRODUCT_UPDATED, saved);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void delete(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setActive(false);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
        appendOutbox(OutboxEventType.PRODUCT_DELETED, product);
    }

    @Cacheable(value = CACHE_NAME, key = "#id")
    public ProductResponse findById(Long id) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toResponse(product);
    }

    private ProductResponse toResponse(ProductEntity p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getCategoryId(),
                p.getStock(),
                p.getActive()
        );
    }

    private void appendOutbox(String eventType, ProductEntity product) {
        try {
            ProductEvent.ProductPayload payload = new ProductEvent.ProductPayload(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getCategoryId(),
                    product.getStock(),
                    product.getActive()
            );
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .aggregateId(String.valueOf(product.getId()))
                    .aggregateType(OutboxEventType.AGGREGATE_TYPE)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize product payload", e);
        }
    }
}
