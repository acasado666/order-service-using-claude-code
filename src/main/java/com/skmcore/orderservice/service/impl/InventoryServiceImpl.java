package com.skmcore.orderservice.service.impl;

import com.skmcore.orderservice.dto.InventoryCheckResponse;
import com.skmcore.orderservice.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Random RANDOM = new Random();

    @Override
    @Retryable(
            retryFor = RuntimeException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public InventoryCheckResponse checkInventory(String productId, int quantity) {
        log.info("Checking inventory for productId={} quantity={}", productId, quantity);

        // Simulates a flaky downstream HTTP call: 30% chance of transient failure
        if (RANDOM.nextDouble() < 0.3) {
            log.debug("Simulating inventory service failure for productId={}", productId);
            throw new RuntimeException("Simulated inventory service timeout for product: " + productId);
        }

        return InventoryCheckResponse.available(productId, quantity);
    }

    @Recover
    public InventoryCheckResponse recoverInventoryCheck(RuntimeException ex, String productId, int quantity) {
        log.warn("Inventory check exhausted all retries for productId={} quantity={}: {}",
                productId, quantity, ex.getMessage());
        return InventoryCheckResponse.unavailable(productId);
    }
}
