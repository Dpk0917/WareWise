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

}
