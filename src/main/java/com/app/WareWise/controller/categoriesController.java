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

import com.app.warewise.entity.Categories;
import com.app.warewise.repository.categoriesRepository;
import com.app.warewise.response.categoriesResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class categoriesController {

    private final categoriesRepository categoriesRepository;

    @GetMapping
    public ResponseEntity<List<categoriesResponse>> getAllCategories() {
        List<categoriesResponse> categories = categoriesRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<categoriesResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findCategory(id)));
    }

    @PostMapping
    public ResponseEntity<categoriesResponse> createCategory(@RequestBody categoriesResponse request) {
        validateName(request.getName());

        Categories category = new Categories();
        category.setName(request.getName().trim());

        Categories saved = categoriesRepository.save(category);
        return ResponseEntity
                .created(URI.create("/api/categories/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<categoriesResponse> patchCategory(
            @PathVariable Long id,
            @RequestBody categoriesResponse request) {
        Categories category = findCategory(id);

        if (request.getName() != null) {
            validateName(request.getName());
            category.setName(request.getName().trim());
        }

        return ResponseEntity.ok(toResponse(categoriesRepository.save(category)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Categories category = findCategory(id);
        try {
            categoriesRepository.delete(category);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category is used by products and cannot be deleted");
        }
        return ResponseEntity.noContent().build();
    }


    private Categories findCategory(Long id) {
        return categoriesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is required");
        }
    }

    private categoriesResponse toResponse(Categories category) {
        return new categoriesResponse(category.getId(), category.getName());
    }
}