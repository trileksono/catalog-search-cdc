package com.tleksono.search.domain;

import java.time.Instant;

public record ProductEvent(
        String eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        ProductPayload payload,
        Instant timestamp
) {
}
