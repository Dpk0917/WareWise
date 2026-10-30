package com.app.warewise.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class purchaseOrdersRequest {

    private String supplierId;
    private String productId;
    private String userId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String status;
}