package com.demo.resource.repository;

import com.demo.resource.entity.RelatedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelatedEntityRepository extends JpaRepository<RelatedEntity, Long> {

    // Slow query - unindexed foreign key search
    List<RelatedEntity> findByDataRecordId(Long dataRecordId);

    // Fast query - with limit
    @Query(value = "SELECT * FROM related_entity WHERE status = ?1 LIMIT 50", nativeQuery = true)
    List<RelatedEntity> findByStatusFast(String status);
}
