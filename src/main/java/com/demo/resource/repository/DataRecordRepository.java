package com.demo.resource.repository;

import com.demo.resource.entity.DataRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, Long> {

    // Slow query - causes N+1 problem with lazy loading
    List<DataRecord> findAll();

    // Slow query - unindexed LIKE search
    @Query("SELECT d FROM DataRecord d WHERE d.payload LIKE %?1%")
    List<DataRecord> findByPayloadContaining(String keyword);

    // Slow query - complex join without proper indexing
    @Query("SELECT DISTINCT d FROM DataRecord d " +
           "LEFT JOIN d.relatedEntities r " +
           "WHERE d.category = ?1 OR r.status = ?2")
    List<DataRecord> findByComplexCriteria(String category, String status);

    // Fast query - indexed lookup
    @Query("SELECT d FROM DataRecord d WHERE d.id = ?1")
    DataRecord findByIdFast(Long id);

    // Fast query - indexed category with limit
    @Query(value = "SELECT * FROM data_record WHERE category = ?1 LIMIT 100", nativeQuery = true)
    List<DataRecord> findByCategoryFast(String category);
}
