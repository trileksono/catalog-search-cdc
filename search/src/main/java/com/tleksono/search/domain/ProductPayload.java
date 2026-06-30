package com.tleksono.search.domain;

import java.math.BigDecimal;

public record ProductPayload(
        Long id,
        String name,
        BigDecimal price,
        Long categoryId,
        Integer stock,
        Boolean active
) {
}
