
package com.app.WareWise.controller;

import com.app.WareWise.entity.InventoryBatch;
import com.app.WareWise.entity.OrderItem;
import com.app.WareWise.repository.InventoryBatchRepository;
import com.app.WareWise.repository.OrderItemRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemRepository repo;
    private final InventoryBatchRepository batchRepo;

    public OrderItemController(
            OrderItemRepository repo,
            InventoryBatchRepository batchRepo) {
        this.repo = repo;
        this.batchRepo = batchRepo;
    }

    // Get order items by order ID
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<List<OrderItem>> byOrder(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(repo.findByOrderId(orderId));
    }

    // Create an order item and update available stock
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody OrderItem item) {

        if (item.getBatchId() == null) {
            return ResponseEntity.badRequest()
                    .body("Batch ID is required");
        }

        InventoryBatch batch = batchRepo.findById(item.getBatchId())
                .orElse(null);

        if (batch == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Batch not found");
        }

        if (batch.getStatus() == null
                || "EXPIRED".equals(batch.getStatus().toString())
                || (batch.getExpiryDate() != null
                && batch.getExpiryDate().isBefore(LocalDate.now()))) {
            return ResponseEntity.badRequest()
                    .body("Batch is expired or unavailable");
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            return ResponseEntity.badRequest()
                    .body("Quantity must be positive");
        }

        if (batch.getQuantityAvailable() == null
                || batch.getQuantityAvailable() < item.getQuantity()) {
            return ResponseEntity.badRequest()
                    .body("Insufficient stock");
        }

        batch.setQuantityAvailable(
                batch.getQuantityAvailable() - item.getQuantity());

        batchRepo.save(batch);

        // Ensure a new order item is created
        item.setId(null);

        OrderItem savedItem = repo.save(item);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
    }
}
