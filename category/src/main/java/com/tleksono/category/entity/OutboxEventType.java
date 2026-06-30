package com.tleksono.category.entity;

public final class OutboxEventType {
    public static final String CATEGORY_CREATED = "CATEGORY_CREATED";
    public static final String CATEGORY_UPDATED = "CATEGORY_UPDATED";
    public static final String CATEGORY_DELETED = "CATEGORY_DELETED";

    public static final String AGGREGATE_TYPE = "Category";

    private OutboxEventType() {
    }
}
