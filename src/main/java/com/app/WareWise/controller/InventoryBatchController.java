
// package com.app.WareWise.controller;

// import com.app.WareWise.entity.*;
// import com.app.WareWise.repository.*;
// import com.app.WareWise.response.BatchResponse;

// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.time.temporal.ChronoUnit;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// @RestController
// @RequestMapping("/api/batches")
// public class InventoryBatchController {

//     private final InventoryBatchRepository repo;
//     private final ProductRepository productRepo;
//     private final WarehouseRepository warehouseRepo;
//     private final WasteRecordRepository wasteRepo;
//     private final AlertRepository alertRepo;

//     public InventoryBatchController(
//             InventoryBatchRepository repo,
//             ProductRepository productRepo,
//             WarehouseRepository warehouseRepo,
//             WasteRecordRepository wasteRepo,
//             AlertRepository alertRepo) {

//         this.repo = repo;
//         this.productRepo = productRepo;
//         this.warehouseRepo = warehouseRepo;
//         this.wasteRepo = wasteRepo;
//         this.alertRepo = alertRepo;
//     }

//     // Get batch details, optionally filtered by warehouse
//     @GetMapping("/details")
//     public ResponseEntity<List<BatchResponse>> details(
//             @RequestParam(required = false) Long warehouseId) {

//         List<InventoryBatch> batches = warehouseId == null
//                 ? repo.findAll()
//                 : repo.findByWarehouseId(warehouseId);

//         return ResponseEntity.ok(toResponses(batches));
//     }

//     // Get batches expiring within the specified number of days
//     @GetMapping("/expiring")
//     public ResponseEntity<List<BatchResponse>> expiring(
//             @RequestParam(defaultValue = "7") int days) {

//         if (days < 0) {
//             return ResponseEntity.badRequest().build();
//         }

//         LocalDate today = LocalDate.now();

//         List<BatchResponse> responses = toResponses(
//                 repo.findByExpiryDateBetween(today, today.plusDays(days)));

//         return ResponseEntity.ok(responses);
//     }

//     // Get expired batches
//     @GetMapping("/expired")
//     public ResponseEntity<List<BatchResponse>> expired() {

//         List<BatchResponse> responses = toResponses(
//                 repo.findByExpiryDateBefore(LocalDate.now()));

//         return ResponseEntity.ok(responses);
//     }

//     // Run the expiry check
//     @PostMapping("/run-expiry-check")
//     @Transactional
//     public ResponseEntity<String> runExpiryCheck() {

//         int count = 0;

//         for (InventoryBatch batch :
//                 repo.findByExpiryDateBefore(LocalDate.now())) {

//             if (batch.getStatus() == BatchStatus.EXPIRED) {
//                 continue;
//             }

//             batch.setStatus(BatchStatus.EXPIRED);

//             productRepo.findById(batch.getProductId()).ifPresent(product -> {
//                 product.setExpired(true);
//                 productRepo.save(product);
//             });

//             if (!alertRepo.existsByProductIdAndWarehouseIdAndAlertType(
//                     batch.getProductId(),
//                     batch.getWarehouseId(),
//                     "EXPIRED")) {

//                 Alert alert = new Alert();
//                 alert.setProductId(batch.getProductId());
//                 alert.setWarehouseId(batch.getWarehouseId());
//                 alert.setAlertType("EXPIRED");
//                 alert.setCreatedAt(LocalDateTime.now());

//                 alertRepo.save(alert);
//             }

//             count++;
//         }

//         return ResponseEntity.ok(
//                 count + " batch(es) marked as expired");
//     }

//     // Record disposed or wasted inventory
//     @PostMapping("/{id}/waste")
//     @Transactional
//     public ResponseEntity<?> dispose(
//             @PathVariable Long id,
//             @RequestBody WasteRecord body) {

//         InventoryBatch batch = repo.findById(id).orElse(null);

//         if (batch == null) {
//             return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                     .body("Batch not found");
//         }

//         if (body.getQuantity() == null
//                 || body.getQuantity() <= 0
//                 || batch.getQuantityAvailable() == null
//                 || body.getQuantity() > batch.getQuantityAvailable()) {

//             return ResponseEntity.badRequest()
//                     .body("Invalid quantity");
//         }

//         batch.setQuantityAvailable(
//                 batch.getQuantityAvailable() - body.getQuantity());

//         repo.save(batch);

//         body.setId(null);
//         body.setBatchId(id);
//         body.setDisposedAt(LocalDateTime.now());

//         WasteRecord savedRecord = wasteRepo.save(body);

//         return ResponseEntity.status(HttpStatus.CREATED)
//                 .body(savedRecord);
//     }

//     // Convert entities to response DTOs
//     private List<BatchResponse> toResponses(
//             List<InventoryBatch> batches) {

//         Map<Long, String> products = productRepo.findAll().stream()
//                 .collect(Collectors.toMap(
//                         Product::getId,
//                         Product::getName));

//         Map<Long, String> warehouses = warehouseRepo.findAll().stream()
//                 .collect(Collectors.toMap(
//                         Warehouse::getId,
//                         Warehouse::getName));

//         LocalDate today = LocalDate.now();

//         return batches.stream()
//                 .map(batch -> new BatchResponse(
//                         batch.getId(),
//                         batch.getBatchNumber(),
//                         products.getOrDefault(
//                                 batch.getProductId(), "-"),
//                         warehouses.getOrDefault(
//                                 batch.getWarehouseId(), "-"),
//                         batch.getQuantityAvailable(),
//                         batch.getExpiryDate(),
//                         batch.getExpiryDate() == null
//                                 ? null
//                                 : ChronoUnit.DAYS.between(
//                                         today,
//                                         batch.getExpiryDate()),
//                         batch.getStatus() == null
//                                 ? null
//                                 : batch.getStatus().name()))
//                 .toList();
//     }
// }
