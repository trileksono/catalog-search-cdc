package com.tleksono.search.domain;

import java.time.Instant;

public record CategoryEvent(
        String eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        CategoryPayload payload,
        Instant timestamp
) {
}
