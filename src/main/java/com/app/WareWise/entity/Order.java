
package com.app.WareWise.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime placedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @PrePersist
    protected void onCreate() {
        placedAt = LocalDateTime.now();
    }
}
