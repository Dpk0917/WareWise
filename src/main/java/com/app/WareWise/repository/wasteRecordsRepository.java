package com.app.warewise.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.WasteRecords;

@Repository
public interface wasteRecordsRepository extends JpaRepository<WasteRecords, Long> {

    List<WasteRecords> findByProductId(Long productId);

    List<WasteRecords> findByRecordedById(UUID recordedById);

    List<WasteRecords> findByReasonContainingIgnoreCase(String reason);
}
