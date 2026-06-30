package com.tleksono.product.dto;

import java.math.BigDecimal;

public record ProductRequest(
        String name,
        BigDecimal price,
        Long categoryId,
        Integer stock
) {
}
