package com.app.warewise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.Alerts;

@Repository
public interface alertsRepository extends JpaRepository<Alerts, Long> {

    List<Alerts> findByWarehouseId(Long warehouseId);

    List<Alerts> findByProductId(Long productId);

    List<Alerts> findByAlertTypeIgnoreCase(String alertType);

    List<Alerts> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<Alerts> findByWarehouseIdAndAlertTypeIgnoreCase(Long warehouseId, String alertType);

    List<Alerts> findByProductIdAndAlertTypeIgnoreCase(Long productId, String alertType);

    List<Alerts> findByWarehouseIdAndProductIdAndAlertTypeIgnoreCase(Long warehouseId, Long productId, String alertType);
}
