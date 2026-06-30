package com.tleksono.product.dto.event;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductEvent(
        String eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        ProductPayload payload,
        Instant timestamp
) {
    public record ProductPayload(
            Long id,
            String name,
            BigDecimal price,
            Long categoryId,
            Integer stock,
            Boolean active
    ) {
    }
}
