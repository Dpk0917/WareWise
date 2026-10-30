package com.app.warewise.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class wasteRecordsResponse {

    private Long id;
    private Long productId;
    private String productName;
    private UUID userId;
    private String userName;
    private Integer quantity;
    private String reason;
    private LocalDateTime createdAt;
}
