package com.app.warewise.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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

import com.app.warewise.entity.Products;
import com.app.warewise.entity.Users;
import com.app.warewise.entity.WasteRecords;
import com.app.warewise.repository.productsRepository;
import com.app.warewise.repository.usersRepository;
import com.app.warewise.repository.wasteRecordsRepository;
import com.app.warewise.response.wasteRecordsResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/waste-records")
@RequiredArgsConstructor
public class wasteRecordsController {

    private final wasteRecordsRepository wasteRecordsRepository;
    private final productsRepository productsRepository;
    private final usersRepository usersRepository;

    @GetMapping
    public ResponseEntity<List<wasteRecordsResponse>> getAllWasteRecords(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String reason) {

        List<WasteRecords> records;

        if (productId != null && userId != null) {
            records = wasteRecordsRepository.findByProductId(productId).stream()
                    .filter(record -> record.getRecordedBy() != null && record.getRecordedBy().getId().equals(userId))
                    .toList();
        } else if (productId != null) {
            records = wasteRecordsRepository.findByProductId(productId);
        } else if (userId != null) {
            records = wasteRecordsRepository.findByRecordedById(userId);
        } else if (reason != null && !reason.isBlank()) {
            records = wasteRecordsRepository.findByReasonContainingIgnoreCase(reason.trim());
        } else {
            records = wasteRecordsRepository.findAll();
        }

        return ResponseEntity.ok(records.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<wasteRecordsResponse> getWasteRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findWasteRecord(id)));
    }

    @PostMapping
    public ResponseEntity<wasteRecordsResponse> createWasteRecord(@RequestBody wasteRecordsResponse request) {
        validateProduct(request.getProductId());
        validateUser(request.getUserId());
        validateQuantity(request.getQuantity());
        validateReason(request.getReason());

        WasteRecords record = new WasteRecords();
        record.setProduct(findProduct(request.getProductId()));
        record.setRecordedBy(findUser(request.getUserId()));
        record.setQuantity(request.getQuantity());
        record.setReason(request.getReason().trim());

        WasteRecords saved = wasteRecordsRepository.save(record);
        return ResponseEntity
                .created(URI.create("/api/waste-records/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<wasteRecordsResponse> patchWasteRecord(
            @PathVariable Long id,
            @RequestBody wasteRecordsResponse request) {
        WasteRecords record = findWasteRecord(id);

        if (request.getProductId() != null) {
            validateProduct(request.getProductId());
            record.setProduct(findProduct(request.getProductId()));
        }
        if (request.getUserId() != null) {
            validateUser(request.getUserId());
            record.setRecordedBy(findUser(request.getUserId()));
        }
        if (request.getQuantity() != null) {
            validateQuantity(request.getQuantity());
            record.setQuantity(request.getQuantity());
        }
        if (request.getReason() != null) {
            validateReason(request.getReason());
            record.setReason(request.getReason().trim());
        }

        return ResponseEntity.ok(toResponse(wasteRecordsRepository.save(record)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWasteRecord(@PathVariable Long id) {
        WasteRecords record = findWasteRecord(id);
        try {
            wasteRecordsRepository.delete(record);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Waste record cannot be deleted because it is linked to another record");
        }
        return ResponseEntity.noContent().build();
    }

    private WasteRecords findWasteRecord(Long id) {
        return wasteRecordsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waste record not found"));
    }

    private Products findProduct(Long productId) {
        if (productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        return productsRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found"));
    }

    private Users findUser(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        return usersRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }

    private void validateProduct(Long productId) {
        findProduct(productId);
    }

    private void validateUser(UUID userId) {
        findUser(userId);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waste quantity must be greater than zero");
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waste reason is required");
        }
    }

    private wasteRecordsResponse toResponse(WasteRecords record) {
        return new wasteRecordsResponse(
                record.getId(),
                record.getProduct() != null ? record.getProduct().getId() : null,
                record.getProduct() != null ? record.getProduct().getName() : null,
                record.getRecordedBy() != null ? record.getRecordedBy().getId() : null,
                record.getRecordedBy() != null ? record.getRecordedBy().getName() : null,
                record.getQuantity(),
                record.getReason(),
                record.getCreatedAt());
    }
}
