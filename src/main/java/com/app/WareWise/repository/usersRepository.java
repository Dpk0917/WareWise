package com.app.warewise.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.warewise.entity.Users;

@Repository
public interface usersRepository extends JpaRepository<Users, UUID> {

    List<Users> findByRoleContainingIgnoreCase(String role);

    List<Users> findByNameContainingIgnoreCase(String name);
}
