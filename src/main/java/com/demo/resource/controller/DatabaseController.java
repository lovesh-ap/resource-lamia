package com.demo.resource.controller;

import com.demo.resource.service.DatabaseService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.Map;

@RestController
@RequestMapping("/api/db")
@ConditionalOnBean(DataSource.class)
public class DatabaseController {

    private final DatabaseService databaseService;

    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * POST /api/db/slow
     * Slow database operations with N+1 queries and artificial delays
     */
    @PostMapping("/slow")
    public ResponseEntity<Map<String, Object>> slowDatabaseOperations() {
        Map<String, Object> result = databaseService.performSlowDatabaseOperations();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/db/fast
     * Fast database operations with optimized queries
     */
    @PostMapping("/fast")
    public ResponseEntity<Map<String, Object>> fastDatabaseOperations() {
        Map<String, Object> result = databaseService.performFastDatabaseOperations();
        return ResponseEntity.ok(result);
    }
}
