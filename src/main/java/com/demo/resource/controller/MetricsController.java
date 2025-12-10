package com.demo.resource.controller;

import com.demo.resource.service.CpuService;
import com.demo.resource.service.DatabaseService;
import com.demo.resource.service.LockContentionService;
import com.demo.resource.service.MemoryService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final CpuService cpuService;
    private final MemoryService memoryService;
    private final LockContentionService lockContentionService;
    
    @Autowired(required = false)
    private DatabaseService databaseService;
    
    @Autowired(required = false)
    private DataSource dataSource;

    public MetricsController(CpuService cpuService,
                            MemoryService memoryService,
                            LockContentionService lockContentionService) {
        this.cpuService = cpuService;
        this.memoryService = memoryService;
        this.lockContentionService = lockContentionService;
    }

    /**
     * GET /api/metrics/system
     * Comprehensive system metrics
     */
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // JVM Memory metrics
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> jvmMemory = new HashMap<>();
        jvmMemory.put("maxMB", maxMemory / (1024 * 1024));
        jvmMemory.put("totalMB", totalMemory / (1024 * 1024));
        jvmMemory.put("usedMB", usedMemory / (1024 * 1024));
        jvmMemory.put("freeMB", freeMemory / (1024 * 1024));
        jvmMemory.put("usedPercentage", (usedMemory * 100.0) / maxMemory);
        
        // Heap memory details
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        
        Map<String, Object> heapDetails = new HashMap<>();
        heapDetails.put("initMB", heapMemory.getInit() / (1024 * 1024));
        heapDetails.put("usedMB", heapMemory.getUsed() / (1024 * 1024));
        heapDetails.put("committedMB", heapMemory.getCommitted() / (1024 * 1024));
        heapDetails.put("maxMB", heapMemory.getMax() / (1024 * 1024));
        
        Map<String, Object> nonHeapDetails = new HashMap<>();
        nonHeapDetails.put("initMB", nonHeapMemory.getInit() / (1024 * 1024));
        nonHeapDetails.put("usedMB", nonHeapMemory.getUsed() / (1024 * 1024));
        nonHeapDetails.put("committedMB", nonHeapMemory.getCommitted() / (1024 * 1024));
        nonHeapDetails.put("maxMB", nonHeapMemory.getMax() / (1024 * 1024));
        
        // GC statistics
        Map<String, Object> gcStats = new HashMap<>();
        long totalGcCount = 0;
        long totalGcTime = 0;
        
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }
        
        gcStats.put("totalCollections", totalGcCount);
        gcStats.put("totalTimeMs", totalGcTime);
        
        // In-memory collection sizes
        Map<String, Object> collectionSizes = new HashMap<>();
        collectionSizes.put("cpuDataStoreKeys", cpuService.getDataStoreSize());
        collectionSizes.put("memoryStoreKeys", memoryService.getMemoryStoreSize());
        collectionSizes.put("lockSharedMapSize", lockContentionService.getSharedMapSize());
        collectionSizes.put("lockSharedListSize", lockContentionService.getSharedListSize());
        
        // Lock contention metrics
        Map<String, Object> contentionMetrics = lockContentionService.getMetrics();
        
        // HikariCP connection pool metrics
        Map<String, Object> connectionPool = new HashMap<>();
        if (dataSource != null && dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            connectionPool.put("activeConnections", poolMXBean.getActiveConnections());
            connectionPool.put("idleConnections", poolMXBean.getIdleConnections());
            connectionPool.put("totalConnections", poolMXBean.getTotalConnections());
            connectionPool.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
        }
        
        // Database stats
        Map<String, Object> dbStats = databaseService != null ? databaseService.getDatabaseStats() : new HashMap<>();
        
        metrics.put("jvmMemory", jvmMemory);
        metrics.put("heapMemory", heapDetails);
        metrics.put("nonHeapMemory", nonHeapDetails);
        metrics.put("garbageCollection", gcStats);
        metrics.put("inMemoryCollections", collectionSizes);
        metrics.put("lockContention", contentionMetrics);
        metrics.put("connectionPool", connectionPool);
        metrics.put("database", dbStats);
        metrics.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/metrics/endpoints
     * Get endpoint call counters (via Micrometer)
     */
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getEndpointMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("message", "Endpoint metrics available via /actuator/metrics");
        metrics.put("endpoints", new String[]{
            "/actuator/metrics/cpu.load.calls",
            "/actuator/metrics/cpu.stable.calls",
            "/actuator/metrics/memory.load.calls",
            "/actuator/metrics/memory.stable.calls",
            "/actuator/metrics/database.slow.calls",
            "/actuator/metrics/database.fast.calls",
            "/actuator/metrics/contention.operations",
            "/actuator/metrics/contention.wait.time"
        });
        
        return ResponseEntity.ok(metrics);
    }
}
