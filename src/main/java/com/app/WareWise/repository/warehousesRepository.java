package com.app.warewise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.Warehouses;

@Repository
public interface warehousesRepository extends JpaRepository<Warehouses, Long> {

    List<Warehouses> findByNameContainingIgnoreCase(String name);

    List<Warehouses> findByCityIgnoreCase(String city);

    List<Warehouses> findByCityIgnoreCaseAndNameContainingIgnoreCase(String city, String name);
}
