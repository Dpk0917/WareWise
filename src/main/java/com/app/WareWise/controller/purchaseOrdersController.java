package com.app.warewise.controller;

import java.math.BigDecimal;
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
import com.app.warewise.entity.PurchaseOrders;
import com.app.warewise.entity.Suppliers;
import com.app.warewise.entity.Users;
import com.app.warewise.repository.productsRepository;
import com.app.warewise.repository.purchaseOrdersRepository;
import com.app.warewise.repository.suppliersRepository;
import com.app.warewise.repository.usersRepository;
import com.app.warewise.response.purchaseOrdersRequest;
import com.app.warewise.response.purchaseOrdersResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class purchaseOrdersController {

    private final purchaseOrdersRepository purchaseOrdersRepository;
    private final suppliersRepository suppliersRepository;
    private final productsRepository productsRepository;
    private final usersRepository usersRepository;

    @GetMapping
    public ResponseEntity<List<purchaseOrdersResponse>> getAllPurchaseOrders(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String status) {

        List<PurchaseOrders> orders;

        if (supplierId != null && productId != null) {
            orders = purchaseOrdersRepository.findBySupplierId(supplierId).stream()
                    .filter(order -> order.getProduct() != null && order.getProduct().getId().equals(productId))
                    .toList();
        } else if (supplierId != null) {
            orders = purchaseOrdersRepository.findBySupplierId(supplierId);
        } else if (productId != null) {
            orders = purchaseOrdersRepository.findByProductId(productId);
        } else if (userId != null) {
            orders = purchaseOrdersRepository.findByCreatedById(userId);
        } else if (status != null && !status.isBlank()) {
            orders = purchaseOrdersRepository.findByStatusContainingIgnoreCase(status.trim());
        } else {
            orders = purchaseOrdersRepository.findAll();
        }

        return ResponseEntity.ok(orders.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<purchaseOrdersResponse> getPurchaseOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findPurchaseOrder(id)));
    }

    @PostMapping
    public ResponseEntity<purchaseOrdersResponse> createPurchaseOrder(@RequestBody purchaseOrdersRequest request) {
        Long supplierId = parseLongId(request.getSupplierId(), "supplierId");
        Long productId = parseLongId(request.getProductId(), "productId");
        UUID userId = parseUserId(request.getUserId());
        validateQuantity(request.getQuantity());
        validateUnitPrice(request.getUnitPrice());

        PurchaseOrders order = new PurchaseOrders();
        order.setSupplier(findSupplier(supplierId));
        order.setProduct(findProduct(productId));
        order.setCreatedBy(findUser(userId));
        order.setQuantity(request.getQuantity());
        order.setUnitPrice(request.getUnitPrice());
        order.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "PENDING" : request.getStatus().trim());

        PurchaseOrders saved = purchaseOrdersRepository.save(order);
        return ResponseEntity
                .created(URI.create("/api/purchase-orders/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<purchaseOrdersResponse> patchPurchaseOrder(
            @PathVariable Long id,
            @RequestBody purchaseOrdersRequest request) {
        PurchaseOrders order = findPurchaseOrder(id);

        if (request.getSupplierId() != null) {
            order.setSupplier(findSupplier(parseLongId(request.getSupplierId(), "supplierId")));
        }
        if (request.getProductId() != null) {
            order.setProduct(findProduct(parseLongId(request.getProductId(), "productId")));
        }
        if (request.getUserId() != null) {
            order.setCreatedBy(findUser(parseUserId(request.getUserId())));
        }
        if (request.getQuantity() != null) {
            validateQuantity(request.getQuantity());
            order.setQuantity(request.getQuantity());
        }
        if (request.getUnitPrice() != null) {
            validateUnitPrice(request.getUnitPrice());
            order.setUnitPrice(request.getUnitPrice());
        }
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus().trim());
        }

        return ResponseEntity.ok(toResponse(purchaseOrdersRepository.save(order)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable Long id) {
        PurchaseOrders order = findPurchaseOrder(id);
        try {
            purchaseOrdersRepository.delete(order);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Purchase order cannot be deleted because it is linked to other records");
        }
        return ResponseEntity.noContent().build();
    }

    private PurchaseOrders findPurchaseOrder(Long id) {
        return purchaseOrdersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));
    }

    private Suppliers findSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierId is required");
        }
        return suppliersRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier not found"));
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

    private Long parseLongId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be a numeric ID");
        }
    }

    private UUID parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        try {
            return UUID.fromString(userId.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must be a UUID");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }
    }

    private void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit price must be greater than zero");
        }
    }

    private purchaseOrdersResponse toResponse(PurchaseOrders order) {
        return new purchaseOrdersResponse(
                order.getId(),
                order.getSupplier() != null ? order.getSupplier().getId() : null,
                order.getSupplier() != null ? order.getSupplier().getName() : null,
                order.getProduct() != null ? order.getProduct().getId() : null,
                order.getProduct() != null ? order.getProduct().getName() : null,
                order.getCreatedBy() != null ? order.getCreatedBy().getId() : null,
                order.getCreatedBy() != null ? order.getCreatedBy().getName() : null,
                order.getQuantity(),
                order.getUnitPrice(),
                order.getStatus(),
                order.getCreatedAt());
    }
}
