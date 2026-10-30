package com.app.warewise.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.PurchaseOrders;

@Repository
public interface purchaseOrdersRepository extends JpaRepository<PurchaseOrders, Long> {

    List<PurchaseOrders> findBySupplierId(Long supplierId);

    List<PurchaseOrders> findByProductId(Long productId);

    List<PurchaseOrders> findByCreatedById(UUID createdById);

    List<PurchaseOrders> findByStatusContainingIgnoreCase(String status);
}
