package com.app.WareWise.repository;

import com.app.WareWise.entity.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
    List<InventoryBatch> findByWarehouseId(Long warehouseId);
    List<InventoryBatch> findByProductId(Long productId);
    List<InventoryBatch> findByExpiryDateBefore(LocalDate date);
    List<InventoryBatch> findByExpiryDateBetween(LocalDate from, LocalDate to);
}
