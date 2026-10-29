package com.app.warewise.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.Products;

@Repository
public interface productsRepository extends JpaRepository<Products, Long> {

    List<Products> findByCategoryId(Long categoryId);

    List<Products> findByNameContainingIgnoreCase(String name);

    List<Products> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String name);
}