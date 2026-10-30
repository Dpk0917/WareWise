package com.app.warewise.controller;

import com.app.warewise.entity.AuditLog;
import com.app.warewise.repository.AuditLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Get all audit logs
    @GetMapping
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    // Get audit log by ID
    @GetMapping("/{id}")
    public Optional<AuditLog> getAuditLogById(@PathVariable Long id) {
        return auditLogRepository.findById(id);
    }

    // Create a new audit log
    @PostMapping
    public AuditLog createAuditLog(@RequestBody AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    // Update an existing audit log
    @PutMapping("/{id}")
    public AuditLog updateAuditLog(@PathVariable Long id, @RequestBody AuditLog auditLog) {
        auditLog.setId(id);
        return auditLogRepository.save(auditLog);
    }

    // Delete an audit log
    @DeleteMapping("/{id}")
    public void deleteAuditLog(@PathVariable Long id) {
        auditLogRepository.deleteById(id);
    }
}
