package com.davydcr.document.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, String> {
    
    List<DocumentJpaEntity> findByUserId(String userId);
    
    List<DocumentJpaEntity> findByStatusAndUserId(String status, String userId);

    Page<DocumentJpaEntity> findByUserId(String userId, Pageable pageable);

    Page<DocumentJpaEntity> findByUserIdAndStatus(String userId, String status, Pageable pageable);

    Page<DocumentJpaEntity> findByUserIdAndType(String userId, String type, Pageable pageable);

    Page<DocumentJpaEntity> findByUserIdAndStatusAndType(String userId, String status, String type, Pageable pageable);
}
