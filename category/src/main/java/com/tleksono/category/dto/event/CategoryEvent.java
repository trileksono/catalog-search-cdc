package com.tleksono.category.dto.event;

import java.time.Instant;

public record CategoryEvent(
        String eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        CategoryPayload payload,
        Instant timestamp
) {
    public record CategoryPayload(
            Long id,
            String name,
            Boolean active
    ) {
    }
}
