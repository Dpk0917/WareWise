package com.app.warewise.controller;

import java.net.URI;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.app.warewise.entity.Alerts;
import com.app.warewise.entity.Products;
import com.app.warewise.entity.Warehouses;
import com.app.warewise.repository.alertsRepository;
import com.app.warewise.repository.productsRepository;
import com.app.warewise.repository.warehousesRepository;
import com.app.warewise.response.alertsResponse;

@RestController
@RequestMapping("/api/alerts")
public class alertsController {

    private final alertsRepository alertsRepository;
    private final warehousesRepository warehousesRepository;
    private final productsRepository productsRepository;

    public alertsController(
            alertsRepository alertsRepository,
            warehousesRepository warehousesRepository,
            productsRepository productsRepository) {
        this.alertsRepository = alertsRepository;
        this.warehousesRepository = warehousesRepository;
        this.productsRepository = productsRepository;
    }

    @GetMapping
    public ResponseEntity<List<alertsResponse>> getAllAlerts(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String alertType) {

        boolean hasWarehouse = warehouseId != null;
        boolean hasProduct = productId != null;
        boolean hasType = alertType != null && !alertType.isBlank();
        String type = hasType ? alertType.trim() : null;

        List<Alerts> alerts;

        if (hasWarehouse && hasProduct && hasType) {
            alerts = alertsRepository.findByWarehouseIdAndProductIdAndAlertTypeIgnoreCase(
                    warehouseId, productId, type);
        } else if (hasWarehouse && hasProduct) {
            alerts = alertsRepository.findByWarehouseIdAndProductId(warehouseId, productId);
        } else if (hasWarehouse && hasType) {
            alerts = alertsRepository.findByWarehouseIdAndAlertTypeIgnoreCase(warehouseId, type);
        } else if (hasProduct && hasType) {
            alerts = alertsRepository.findByProductIdAndAlertTypeIgnoreCase(productId, type);
        } else if (hasWarehouse) {
            alerts = alertsRepository.findByWarehouseId(warehouseId);
        } else if (hasProduct) {
            alerts = alertsRepository.findByProductId(productId);
        } else if (hasType) {
            alerts = alertsRepository.findByAlertTypeIgnoreCase(type);
        } else {
            alerts = alertsRepository.findAll();
        }

        return ResponseEntity.ok(alerts.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<alertsResponse> getAlertById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findAlert(id)));
    }

    @PostMapping
    public ResponseEntity<alertsResponse> createAlert(@RequestBody alertsResponse request) {
        validateAlertType(request.getAlertType());

        Products product = findProduct(request.getProductId());
        Warehouses warehouse = findWarehouse(request.getWarehouseId());

        Alerts alert = new Alerts();
        alert.setProduct(product);
        alert.setWarehouse(warehouse);
        alert.setAlertType(request.getAlertType().trim());

        Alerts saved = alertsRepository.save(alert);

        return ResponseEntity
                .created(URI.create("/api/alerts/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<alertsResponse> patchAlert(
            @PathVariable Long id,
            @RequestBody alertsResponse request) {

        Alerts alert = findAlert(id);

        if (request.getProductId() != null) {
            alert.setProduct(findProduct(request.getProductId()));
        }

        if (request.getWarehouseId() != null) {
            alert.setWarehouse(findWarehouse(request.getWarehouseId()));
        }

        if (request.getAlertType() != null) {
            validateAlertType(request.getAlertType());
            alert.setAlertType(request.getAlertType().trim());
        }

        return ResponseEntity.ok(toResponse(alertsRepository.save(alert)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        Alerts alert = findAlert(id);

        try {
            alertsRepository.delete(alert);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Alert is referenced by other records and cannot be deleted");
        }

        return ResponseEntity.noContent().build();
    }

    private Alerts findAlert(Long id) {
        return alertsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alert not found"));
    }

    private Products findProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Valid productId is required");
        }

        return productsRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Product not found"));
    }

    private Warehouses findWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "warehouseId is required");
        }

        return warehousesRepository.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Warehouse not found"));
    }

    private void validateAlertType(String alertType) {
        if (alertType == null || alertType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "alertType is required");
        }
    }

    private alertsResponse toResponse(Alerts alert) {
        return new alertsResponse(
                alert.getId(),
                alert.getProduct().getId(),
                alert.getWarehouse().getId(),
                alert.getWarehouse().getName(),
                alert.getAlertType(),
                alert.getCreatedAt());
    }
}