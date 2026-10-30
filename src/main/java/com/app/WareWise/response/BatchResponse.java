package com.app.warewise.response;

import java.time.LocalDate;

public record BatchResponse(
        Long id,
        String batchNumber,
        String productName,
        String warehouseName,
        Integer quantityAvailable,
        LocalDate expiryDate,
        Long daysToExpiry,
        String status
) {
}
