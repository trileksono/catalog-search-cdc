package com.tleksono.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tleksono.product.dto.ProductRequest;
import com.tleksono.product.dto.ProductResponse;
import com.tleksono.product.entity.ProductEntity;
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
}
