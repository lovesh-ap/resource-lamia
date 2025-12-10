package com.demo.resource.controller;

import com.demo.resource.service.LockContentionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for lock contention endpoints.
 * Demonstrates real lock contention causing performance degradation.
 */
@RestController
@RequestMapping("/api/contention")
public class ContentionController {

    private final LockContentionService lockContentionService;

    public ContentionController(LockContentionService lockContentionService) {
        this.lockContentionService = lockContentionService;
    }

    /**
     * POST /api/contention/load
     * Trigger lock contention - ONE API CALL = ONE THREAD
     * Contention grows naturally as multiple concurrent API requests compete for locks
     * 
     * @param holdTimeMs Time to hold lock per operation in ms (10-500, default: 50)
     * @param operationCount Operations for this thread (10-1000, default: 100)
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> contentionLoad(
            @RequestParam(defaultValue = "50") int holdTimeMs,
            @RequestParam(defaultValue = "100") int operationCount) {
        
        Map<String, Object> result = lockContentionService.performContentionOperation(
                holdTimeMs, operationCount);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/contention/metrics
     * Get current lock contention metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = lockContentionService.getMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * DELETE /api/contention/clear
     * Clear accumulated contention data and reset metrics
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearData() {
        Map<String, Object> result = lockContentionService.clearData();
        return ResponseEntity.ok(result);
    }
}
