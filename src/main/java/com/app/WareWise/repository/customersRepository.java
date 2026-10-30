package com.app.warewise.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.Customers;

@Repository
public interface customersRepository extends JpaRepository<Customers, Long> {

    List<Customers> findByNameContainingIgnoreCase(String name);

    Optional<Customers> findByEmailIgnoreCase(String email);

    Optional<Customers> findByPhone(String phone);
}
