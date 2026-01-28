package com.davydcr.document.infrastructure.repository;

import com.davydcr.document.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserAccountEntity, UUID> {

    @Query("SELECT u FROM UserAccountEntity u WHERE u.id = :id")
    Optional<UserAccountEntity> findByUserId(@Param("id") String id);

    @Query("SELECT u FROM UserAccountEntity u WHERE u.email = :email AND u.active = true")
    Optional<UserAccountEntity> findByEmailAndActive(@Param("email") String email);

    Optional<UserAccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
