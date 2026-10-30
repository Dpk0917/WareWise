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

import com.app.warewise.entity.Customers;
import com.app.warewise.repository.customersRepository;
import com.app.warewise.response.customersResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class customersController {

    private final customersRepository customersRepository;

    @GetMapping
    public ResponseEntity<List<customersResponse>> getAllCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
        if (email != null && !email.isBlank()) {
            return ResponseEntity.ok(customersRepository.findByEmailIgnoreCase(email.trim())
                    .map(this::toResponse)
                    .map(List::of)
                    .orElseGet(List::of));
        }

        if (phone != null && !phone.isBlank()) {
            return ResponseEntity.ok(customersRepository.findByPhone(phone.trim())
                    .map(this::toResponse)
                    .map(List::of)
                    .orElseGet(List::of));
        }

        List<Customers> customers;
        if (name != null && !name.isBlank()) {
            customers = customersRepository.findByNameContainingIgnoreCase(name.trim());
        } else {
            customers = customersRepository.findAll();
        }

        List<customersResponse> response = customers.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<customersResponse> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(findCustomer(id)));
    }

    @PostMapping
    public ResponseEntity<customersResponse> createCustomer(@RequestBody customersResponse request) {
        validateName(request.getName());
        if (request.getPhone() != null) {
            validatePhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            validateEmail(request.getEmail());
        }

        Customers customer = new Customers();
        customer.setName(request.getName().trim());
        customer.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        customer.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);

        Customers saved = customersRepository.save(customer);
        return ResponseEntity
                .created(URI.create("/api/customers/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<customersResponse> patchCustomer(
            @PathVariable Long id,
            @RequestBody customersResponse request) {
        Customers customer = findCustomer(id);

        if (request.getName() != null) {
            validateName(request.getName());
            customer.setName(request.getName().trim());
        }
        if (request.getPhone() != null) {
            validatePhone(request.getPhone());
            customer.setPhone(request.getPhone().trim());
        }
        if (request.getEmail() != null) {
            validateEmail(request.getEmail());
            customer.setEmail(request.getEmail().trim());
        }

        return ResponseEntity.ok(toResponse(customersRepository.save(customer)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        Customers customer = findCustomer(id);
        try {
            customersRepository.delete(customer);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Customer is linked to orders and cannot be deleted");
        }
        return ResponseEntity.noContent().build();
    }

    private Customers findCustomer(Long id) {
        return customersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required");
        }
    }

    private void validatePhone(String phone) {
        if (phone != null && !phone.isBlank() && phone.trim().length() < 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer phone number is invalid");
        }
    }

    private void validateEmail(String email) {
        if (email != null && !email.isBlank() && !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer email format is invalid");
        }
    }

    private customersResponse toResponse(Customers customer) {
        return new customersResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail());
    }
}
