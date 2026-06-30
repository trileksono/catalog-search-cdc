package com.tleksono.product.entity;

public final class OutboxEventType {
    public static final String PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String PRODUCT_DELETED = "PRODUCT_DELETED";

    public static final String AGGREGATE_TYPE = "Product";

    private OutboxEventType() {
    }
}
