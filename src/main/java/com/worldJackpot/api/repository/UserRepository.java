package com.worldJackpot.api.repository;

import com.worldJackpot.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.role = 'USER' ORDER BY COALESCE(u.totalPoints, 0) DESC")
    org.springframework.data.domain.Page<User> findAllOrderByTotalPointsDesc(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM User u WHERE u.role != :role")
    void deleteByRoleNot(@org.springframework.data.repository.query.Param("role") com.worldJackpot.api.model.enums.UserRole role);

    java.util.List<User> findByRole(com.worldJackpot.api.model.enums.UserRole role);
}
