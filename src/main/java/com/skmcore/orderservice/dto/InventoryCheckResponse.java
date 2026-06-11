package com.skmcore.orderservice.dto;

public record InventoryCheckResponse(
        String productId,
        boolean available,
        int availableQuantity,
        String message
) {
    public static InventoryCheckResponse available(String productId, int quantity) {
        return new InventoryCheckResponse(productId, true, quantity, "In stock");
    }

    public static InventoryCheckResponse unavailable(String productId) {
        return new InventoryCheckResponse(productId, false, 0,
                "Inventory service unavailable — assuming out of stock");
    }
}
