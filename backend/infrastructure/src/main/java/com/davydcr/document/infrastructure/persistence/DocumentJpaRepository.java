package com.davydcr.document.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, String> {
    
    List<DocumentJpaEntity> findByUserId(String userId);
    
    List<DocumentJpaEntity> findByStatusAndUserId(String status, String userId);
}
