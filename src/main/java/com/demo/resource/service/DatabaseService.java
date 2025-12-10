package com.demo.resource.service;

import com.demo.resource.entity.AuditLog;
import com.demo.resource.entity.DataRecord;
import com.demo.resource.entity.RelatedEntity;
import com.demo.resource.repository.AuditLogRepository;
import com.demo.resource.repository.DataRecordRepository;
import com.demo.resource.repository.RelatedEntityRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;

@Service
public class DatabaseService {

    private final DataRecordRepository dataRecordRepository;
    private final RelatedEntityRepository relatedEntityRepository;
    private final AuditLogRepository auditLogRepository;
    private final MeterRegistry meterRegistry;

    private Counter slowCounter;
    private Counter fastCounter;

    public DatabaseService(DataRecordRepository dataRecordRepository,
                          RelatedEntityRepository relatedEntityRepository,
                          AuditLogRepository auditLogRepository,
                          MeterRegistry meterRegistry) {
        this.dataRecordRepository = dataRecordRepository;
        this.relatedEntityRepository = relatedEntityRepository;
        this.auditLogRepository = auditLogRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        slowCounter = Counter.builder("database.slow.calls")
                .description("Number of slow database endpoint calls")
                .register(meterRegistry);
        fastCounter = Counter.builder("database.fast.calls")
                .description("Number of fast database endpoint calls")
                .register(meterRegistry);
    }

    /**
     * Slow database operations with N+1 queries and artificial delays
     */
    @Transactional
    public Map<String, Object> performSlowDatabaseOperations() {
        slowCounter.increment();
        
        long startTime = System.currentTimeMillis();
        
        // Slow query 1: N+1 problem - fetch all records then iterate lazy relationships
        List<DataRecord> allRecords = dataRecordRepository.findAll();
        int relatedCount = 0;
        for (DataRecord record : allRecords) {
            // This triggers N+1 queries due to lazy loading
            relatedCount += record.getRelatedEntities().size();
        }
        
        // Artificial delay to simulate slow processing
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Slow query 2: Unindexed LIKE search
        List<DataRecord> searchResults = dataRecordRepository.findByPayloadContaining("data");
        
        // Artificial delay
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Slow query 3: Complex join without proper indexing
        Random random = new Random();
        String category = "category_" + random.nextInt(10);
        String status = "status_" + random.nextInt(5);
        List<DataRecord> complexResults = dataRecordRepository.findByComplexCriteria(category, status);
        
        // Artificial delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Slow query 4: Audit log search with subquery
        List<AuditLog> auditResults = auditLogRepository.findByRecordCategoryContaining("category");
        
        // Final artificial delay to hold connection longer
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "database-slow");
        response.put("recordsFetched", allRecords.size());
        response.put("relatedEntitiesCount", relatedCount);
        response.put("searchResults", searchResults.size());
        response.put("complexResults", complexResults.size());
        response.put("auditResults", auditResults.size());
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * Fast database operations with optimized queries
     */
    @Transactional(readOnly = true)
    public Map<String, Object> performFastDatabaseOperations() {
        fastCounter.increment();
        
        long startTime = System.currentTimeMillis();
        
        Random random = new Random();
        
        // Fast query 1: Direct ID lookup
        Long randomId = (long) (random.nextInt(70000) + 1);
        DataRecord record = dataRecordRepository.findByIdFast(randomId);
        
        // Fast query 2: Indexed category search with limit
        String category = "category_" + random.nextInt(10);
        List<DataRecord> categoryResults = dataRecordRepository.findByCategoryFast(category);
        
        // Fast query 3: Indexed status search with limit
        String status = "status_" + random.nextInt(5);
        List<RelatedEntity> statusResults = relatedEntityRepository.findByStatusFast(status);
        
        // Fast query 4: Simple audit log query with limit
        Long afterId = (long) (random.nextInt(150000) + 1);
        List<AuditLog> auditResults = auditLogRepository.findRecentFast(afterId);
        
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "database-fast");
        response.put("recordFound", record != null);
        response.put("categoryResults", categoryResults.size());
        response.put("statusResults", statusResults.size());
        response.put("auditResults", auditResults.size());
        response.put("durationMs", duration);
        
        return response;
    }

    /**
     * Reset database - truncate and reseed
     */
    @Transactional
    public Map<String, Object> resetDatabase() {
        // This will be called via SQL script for better performance
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "database-reset");
        response.put("status", "completed");
        response.put("message", "Database reset via SQL script");
        
        return response;
    }

    /**
     * Get database statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDatabaseStats() {
        long recordCount = dataRecordRepository.count();
        long relatedCount = relatedEntityRepository.count();
        long auditCount = auditLogRepository.count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("dataRecordCount", recordCount);
        stats.put("relatedEntityCount", relatedCount);
        stats.put("auditLogCount", auditCount);
        
        return stats;
    }
}
