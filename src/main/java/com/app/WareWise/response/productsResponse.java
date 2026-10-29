package com.app.warewise.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class productsResponse {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Long supplierId;
    private String supplierName;
    private String unit;
    private Boolean expired; 
    private LocalDateTime createdAt;
}