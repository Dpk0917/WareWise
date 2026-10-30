package com.app.warewise.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class purchaseOrdersResponse {

    private Long id;
    private Long supplierId;
    private String supplierName;
    private Long productId;
    private String productName;
    private UUID userId;
    private String userName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String status;
    private LocalDateTime createdAt;
}
