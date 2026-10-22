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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.app.warewise.entity.Suppliers;
import com.app.warewise.repository.suppliersRepository;
import com.app.warewise.response.suppliersResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class suppliersController {

    private final suppliersRepository suppliersRepository;

    @GetMapping
    public ResponseEntity<List<suppliersResponse>> getAllSuppliers() {
        List<suppliersResponse> suppliers = suppliersRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<suppliersResponse> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findSupplier(id)));
    }

    @PostMapping
    public ResponseEntity<suppliersResponse> createSupplier(@RequestBody suppliersResponse request) {
        validateName(request.getName());
        validateContactNumber(request.getContact());

        Suppliers supplier = new Suppliers();
        supplier.setName(request.getName().trim());
        supplier.setContact(request.getContact());

        Suppliers saved = suppliersRepository.save(supplier);
        return ResponseEntity
                .created(URI.create("/api/suppliers/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<suppliersResponse> patchSupplier(@PathVariable Long id,
            @RequestBody suppliersResponse request) {
        Suppliers supplier = findSupplier(id);

        if (request.getName() != null) {
            validateName(request.getName());
            supplier.setName(request.getName().trim());
        }
        if (request.getContact() != null) {
            validateContactNumber(request.getContact());
            supplier.setContact(request.getContact());
        }

        return ResponseEntity.ok(toResponse(suppliersRepository.save(supplier)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        Suppliers supplier = findSupplier(id);
        try {
            suppliersRepository.delete(supplier);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Supplier is linked to products or purchase orders and cannot be deleted");
        }
        return ResponseEntity.noContent().build();
    }

    private Suppliers findSupplier(Long id) {
        return suppliersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier name is required");
        }
    }

    private void validateContactNumber(String number) {
        if (number == null || number.isBlank() || number.length() != 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier contact number is invalid");
        }
    }

    private suppliersResponse toResponse(Suppliers supplier) {
        return new suppliersResponse(supplier.getId(), supplier.getName(), supplier.getContact());
    }
}