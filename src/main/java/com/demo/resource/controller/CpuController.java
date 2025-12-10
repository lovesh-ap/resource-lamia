package com.demo.resource.controller;

import com.demo.resource.service.CpuService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cpu")
public class CpuController {

    private final CpuService cpuService;

    public CpuController(CpuService cpuService) {
        this.cpuService = cpuService;
    }

    /**
     * POST /api/cpu/load
     * CPU-intensive operation that also accumulates memory
     */
    @PostMapping("/load")
    public ResponseEntity<Map<String, Object>> cpuLoad(
            @RequestParam(defaultValue = "10") int iterations,
            @RequestParam(defaultValue = "1") int dataSizeMB) {
        
        Map<String, Object> result = cpuService.performCpuAndMemoryLoad(iterations, dataSizeMB);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/cpu/stable
     * Minimal CPU operation (stable baseline)
     */
    @GetMapping("/stable")
    public ResponseEntity<Map<String, Object>> cpuStable() {
        Map<String, Object> result = cpuService.performStableCpuOperation();
        return ResponseEntity.ok(result);
    }
}
