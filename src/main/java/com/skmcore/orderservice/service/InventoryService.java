package com.skmcore.orderservice.service;

import com.skmcore.orderservice.dto.InventoryCheckResponse;

public interface InventoryService {

    InventoryCheckResponse checkInventory(String productId, int quantity);
}
