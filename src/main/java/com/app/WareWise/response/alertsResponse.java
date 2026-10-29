package com.app.warewise.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class alertsResponse {

    private Long id;
    private Long productId;
    private Long warehouseId;
    private String warehouseName;
    private String alertType;
    private LocalDateTime createdAt;

    public alertsResponse() {
    }

    public alertsResponse(Long id, Long productId, Long warehouseId, String warehouseName, String alertType, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.alertType = alertType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
