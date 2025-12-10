package com.demo.resource.repository;

import com.demo.resource.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Slow query - unindexed foreign keys with multiple joins
    @Query("SELECT a FROM AuditLog a WHERE a.recordId IN " +
           "(SELECT d.id FROM DataRecord d WHERE d.category LIKE %?1%)")
    List<AuditLog> findByRecordCategoryContaining(String category);

    // Slow query - with limit to prevent OOM
    @Query(value = "SELECT a.* FROM audit_log a WHERE a.record_id IN " +
           "(SELECT d.id FROM data_record d WHERE d.category LIKE CONCAT('%', ?1, '%')) LIMIT ?2", 
           nativeQuery = true)
    List<AuditLog> findByRecordCategoryContainingLimit(String category, int limit);

    // Fast query - simple indexed lookup
    @Query(value = "SELECT * FROM audit_log WHERE id > ?1 LIMIT 100", nativeQuery = true)
    List<AuditLog> findRecentFast(Long afterId);
}
