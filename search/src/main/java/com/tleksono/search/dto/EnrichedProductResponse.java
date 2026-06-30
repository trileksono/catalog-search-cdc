package com.tleksono.search.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record EnrichedProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        Integer stock,
        Boolean active
) implements Serializable {
}
