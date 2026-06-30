package com.tleksono.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Long categoryId,
        Integer stock,
        Boolean active
) {
}
