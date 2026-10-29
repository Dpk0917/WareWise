
package com.app.WareWise.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "inventory_batches")
public class InventoryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long productId;

    @NotNull
    @Column(nullable = false)
    private Long warehouseId;

    private Long purchaseOrderId;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String batchNumber;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer quantityAvailable = 0;

    private LocalDate manufactureDate;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.ACTIVE;
}
