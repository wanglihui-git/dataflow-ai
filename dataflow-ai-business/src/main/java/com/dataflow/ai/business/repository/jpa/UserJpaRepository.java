package com.dataflow.ai.business.repository.jpa;

import com.dataflow.ai.domain.entity.User;
import com.dataflow.ai.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :time WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") String userId, @Param("time") LocalDateTime time);
}
