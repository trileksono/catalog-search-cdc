package com.tleksono.category.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tleksono.category.dto.CategoryRequest;
import com.tleksono.category.dto.CategoryResponse;
import com.tleksono.category.dto.event.CategoryEvent;
import com.tleksono.category.entity.CategoryEntity;
import com.tleksono.category.entity.OutboxEventEntity;
import com.tleksono.category.entity.OutboxEventType;
import com.tleksono.category.repository.CategoryRepository;
import com.tleksono.category.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String CACHE_NAME = "category";

    private final CategoryRepository categoryRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Instant now = Instant.now();
        CategoryEntity category = CategoryEntity.builder()
                .name(request.name())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        CategoryEntity saved = categoryRepository.save(category);
        appendOutbox(OutboxEventType.CATEGORY_CREATED, saved);
        return toResponse(saved);
    }

    @Transactional
    @CachePut(value = CACHE_NAME, key = "#id")
    public CategoryResponse update(Long id, CategoryRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        category.setName(request.name());
        category.setUpdatedAt(Instant.now());
        CategoryEntity saved = categoryRepository.save(category);
        appendOutbox(OutboxEventType.CATEGORY_UPDATED, saved);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void delete(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        category.setActive(false);
        category.setUpdatedAt(Instant.now());
        categoryRepository.save(category);
        appendOutbox(OutboxEventType.CATEGORY_DELETED, category);
    }

    @Cacheable(value = CACHE_NAME, key = "#id")
    public CategoryResponse findById(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        return toResponse(category);
    }

    private CategoryResponse toResponse(CategoryEntity c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getActive()
        );
    }

    private void appendOutbox(String eventType, CategoryEntity category) {
        try {
            CategoryEvent.CategoryPayload payload = new CategoryEvent.CategoryPayload(
                    category.getId(),
                    category.getName(),
                    category.getActive()
            );
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEventEntity event = OutboxEventEntity.builder()
                    .aggregateId(String.valueOf(category.getId()))
                    .aggregateType(OutboxEventType.AGGREGATE_TYPE)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .createdAt(Instant.now())
                    .build();
            outboxRepository.save(event);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize category payload", e);
        }
    }
}
