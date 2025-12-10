package com.demo.resource.controller;

import com.demo.resource.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mem")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * POST /api/mem/load
     * Memory accumulation operation with minimal CPU usage
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> memoryLoad(
            @RequestParam(defaultValue = "5") int objectCount,
            @RequestParam(defaultValue = "2") int sizeMB) {
        
        Map<String, Object> result = memoryService.accumulateMemory(objectCount, sizeMB);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/mem/stable
     * Stable memory operation - no accumulation
     */
    @GetMapping("/stable")
    public ResponseEntity<Map<String, Object>> memoryStable() {
        Map<String, Object> result = memoryService.performStableMemoryOperation();
        return ResponseEntity.ok(result);
    }
}
