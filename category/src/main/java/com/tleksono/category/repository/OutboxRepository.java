package com.tleksono.category.repository;

import com.tleksono.category.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEventEntity, Long> {
}
