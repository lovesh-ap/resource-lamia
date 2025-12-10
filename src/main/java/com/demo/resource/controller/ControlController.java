package com.demo.resource.controller;

import com.demo.resource.service.CpuService;
import com.demo.resource.service.DatabaseService;
import com.demo.resource.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ControlController {

    private final CpuService cpuService;
    private final MemoryService memoryService;
    private final DatabaseService databaseService;
    private final JdbcTemplate jdbcTemplate;

    public ControlController(CpuService cpuService,
                            MemoryService memoryService,
                            DatabaseService databaseService,
                            JdbcTemplate jdbcTemplate) {
        this.cpuService = cpuService;
        this.memoryService = memoryService;
        this.databaseService = databaseService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * DELETE /api/data/clear
     * Clear all accumulated in-memory data
     */
    @DeleteMapping("/data/clear")
    public ResponseEntity<Map<String, Object>> clearData() {
        int cpuStoreSize = cpuService.getDataStoreSize();
        int memoryStoreSize = memoryService.getMemoryStoreSize();
        
        cpuService.clearData();
        memoryService.clearMemory();
        
        // Suggest garbage collection
        System.gc();
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "data-clear");
        response.put("cpuKeysCleared", cpuStoreSize);
        response.put("memoryKeysCleared", memoryStoreSize);
        response.put("gcSuggested", true);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/db/reset
     * Truncate and reseed database
     */
    @PostMapping("/db/reset")
    public ResponseEntity<Map<String, Object>> resetDatabase() {
        try {
            // Truncate tables
            jdbcTemplate.execute("TRUNCATE TABLE audit_log CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE related_entity CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE data_record CASCADE");
            
            Map<String, Object> response = new HashMap<>();
            response.put("operation", "database-reset");
            response.put("status", "completed");
            response.put("message", "Database truncated. Restart application to reseed data.");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("operation", "database-reset");
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * GET /api/health/db
     * Database connectivity check
     */
    @GetMapping("/health/db")
    public ResponseEntity<Map<String, Object>> checkDatabaseHealth() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("database", "connected");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "unhealthy");
            response.put("database", "disconnected");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(response);
        }
    }
}
