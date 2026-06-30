package com.tleksono.search.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record OutboxRow(
        @JsonProperty("id") Long id,
        @JsonProperty("aggregate_id") String aggregateId,
        @JsonProperty("aggregate_type") String aggregateType,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("payload") String payload,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("__op") String op,
        @JsonProperty("__source_ts_ms") Long sourceTsMs
) {
}
