package com.tleksono.search.domain;

public record CategoryPayload(
        Long id,
        String name,
        Boolean active
) {
}
