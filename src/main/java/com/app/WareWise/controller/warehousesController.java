package com.app.warewise.controller;

import java.net.URI;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.warewise.entity.Warehouses;
import com.app.warewise.repository.warehousesRepository;
import com.app.warewise.response.warehousesResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class warehousesController {

    private final warehousesRepository warehousesRepository;

    @GetMapping
    public ResponseEntity<List<warehousesResponse>> getAllWarehouses(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasCity = city != null && !city.isBlank();
        List<Warehouses> warehouses;

        if (hasName && hasCity) {
            warehouses = warehousesRepository.findByCityIgnoreCaseAndNameContainingIgnoreCase(city.trim(), name.trim());
        } else if (hasCity) {
            warehouses = warehousesRepository.findByCityIgnoreCase(city.trim());
        } else if (hasName) {
            warehouses = warehousesRepository.findByNameContainingIgnoreCase(name.trim());
        } else {
            warehouses = warehousesRepository.findAll();
        }

        List<warehousesResponse> response = warehouses.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<warehousesResponse> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findWarehouse(id)));
    }

    @PostMapping
    public ResponseEntity<warehousesResponse> createWarehouse(@RequestBody warehousesResponse request) {
        validateName(request.getName());
        validateCity(request.getCity());

        Warehouses warehouse = new Warehouses();
        warehouse.setName(request.getName().trim());
        warehouse.setCity(request.getCity().trim());
        warehouse.setPincode(request.getPincode() != null ? request.getPincode().trim() : null);

        Warehouses saved = warehousesRepository.save(warehouse);
        return ResponseEntity
                .created(URI.create("/api/warehouses/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<warehousesResponse> patchWarehouse(
            @PathVariable Long id,
            @RequestBody warehousesResponse request) {
        Warehouses warehouse = findWarehouse(id);

        if (request.getName() != null) {
            validateName(request.getName());
            warehouse.setName(request.getName().trim());
        }
        if (request.getCity() != null) {
            validateCity(request.getCity());
            warehouse.setCity(request.getCity().trim());
        }
        if (request.getPincode() != null) {
            warehouse.setPincode(request.getPincode().trim());
        }

        return ResponseEntity.ok(toResponse(warehousesRepository.save(warehouse)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
        Warehouses warehouse = findWarehouse(id);
        try {
            warehousesRepository.delete(warehouse);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Warehouse is linked to orders, inventory batches, alerts or purchase orders and cannot be deleted");
        }
        return ResponseEntity.noContent().build();
    }

    private Warehouses findWarehouse(Long id) {
        return warehousesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse name is required");
        }
    }

    private void validateCity(String city) {
        if (city == null || city.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warehouse city is required");
        }
    }

    private warehousesResponse toResponse(Warehouses warehouse) {
        return new warehousesResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getCity(),
                warehouse.getPincode());
    }
}
