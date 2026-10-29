package com.app.warewise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.Suppliers;

@Repository 
public interface suppliersRepository extends JpaRepository<Suppliers, Long>{
}