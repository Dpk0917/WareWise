package com.app.warewise.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.Categories;

@Repository
public interface categoriesRepository extends JpaRepository<Categories, Long> {
}