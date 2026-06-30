package com.tleksono.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        Boolean active
) {
}
