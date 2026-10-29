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

import com.app.warewise.entity.Categories;
import com.app.warewise.entity.Products;
import com.app.warewise.entity.Suppliers;
import com.app.warewise.repository.categoriesRepository;
import com.app.warewise.repository.productsRepository;
import com.app.warewise.repository.suppliersRepository;
import com.app.warewise.response.productsResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class productsController {

    private final productsRepository productsRepository;
    private final categoriesRepository categoriesRepository;
    private final suppliersRepository suppliersRepository;

    @GetMapping
    public ResponseEntity<List<productsResponse>> getAllProducts(@RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId) {
        boolean hasName = name != null && !name.isBlank();
        List<Products> products;

        if (categoryId != null && hasName) {
            products = productsRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, name.trim());
        } else if (categoryId != null) {
            products = productsRepository.findByCategoryId(categoryId);
        } else if (hasName) {
            products = productsRepository.findByNameContainingIgnoreCase(name.trim());
        } else {
            products = productsRepository.findAll();
        }

        List<productsResponse> response = products.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<productsResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findProduct(id)));
    }

    @PostMapping
    public ResponseEntity<productsResponse> createProduct(@RequestBody productsResponse request) {
        validateName(request.getName());

        Products product = new Products();
        product.setName(request.getName().trim());
        product.setCategory(findCategory(request.getCategoryId()));
        product.setSupplier(findSupplier(request.getSupplierId()));
        product.setUnit(request.getUnit());
        if (request.getExpired() != null) {
            product.setExpired(request.getExpired());
        }

        Products saved = productsRepository.save(product);
        return ResponseEntity
                .created(URI.create("/api/products/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<productsResponse> patchProduct(
            @PathVariable Long id,
            @RequestBody productsResponse request) {
        Products product = findProduct(id);

        if (request.getName() != null) {
            validateName(request.getName());
            product.setName(request.getName().trim());
        }
        if (request.getCategoryId() != null) {
            product.setCategory(findCategory(request.getCategoryId()));
        }
        if (request.getSupplierId() != null) {
            product.setSupplier(findSupplier(request.getSupplierId()));
        }
        if (request.getUnit() != null) {
            product.setUnit(request.getUnit());
        }
        if (request.getExpired() != null) {
            product.setExpired(request.getExpired());
        }

        return ResponseEntity.ok(toResponse(productsRepository.save(product)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        Products product = findProduct(id);
        try {
            productsRepository.delete(product);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product is linked to batches, alerts or purchase orders and cannot be deleted");
        }
        return ResponseEntity.noContent().build();
    }


    private Products findProduct(Long id) {
        return productsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private Categories findCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId is required");
        }
        return categoriesRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

    private Suppliers findSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "supplierId is required");
        }
        return suppliersRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier not found"));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required");
        }
    }

    private productsResponse toResponse(Products product) {
        return new productsResponse(
                product.getId(),
                product.getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getSupplier().getId(),
                product.getSupplier().getName(),
                product.getUnit(),
                product.isExpired(),
                product.getCreatedAt());
    }
}