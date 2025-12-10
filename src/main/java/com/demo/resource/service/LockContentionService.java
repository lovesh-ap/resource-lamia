package com.demo.resource.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service demonstrating real lock contention scenarios.
 * Uses synchronized blocks on shared data structures to cause real thread waiting.
 */
@Service
public class LockContentionService {

    private static final Logger logger = LoggerFactory.getLogger(LockContentionService.class);

    // Shared data structures that will be contended
    private final Map<String, Object> sharedMap = new HashMap<>();
    private final List<String> sharedList = new ArrayList<>();
    
    // Metrics tracking
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong totalWaitTimeMs = new AtomicLong(0);
    
    private Counter operationCounter;
    private Timer contentionTimer;
    
    private final MeterRegistry meterRegistry;

    public LockContentionService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        operationCounter = Counter.builder("contention.operations")
                .description("Number of contention operations performed")
                .register(meterRegistry);
        
        contentionTimer = Timer.builder("contention.wait.time")
                .description("Time spent waiting for locks")
                .register(meterRegistry);
    }

    /**
     * Perform lock contention operation - ONE API CALL = ONE THREAD
     * Multiple concurrent API calls naturally create contention as they compete for locks
     * 
     * @param holdTimeMs Time to hold lock in milliseconds (10-500)
     * @param operationCount Number of operations for this thread (10-1000)
     * @return Results map with timing and contention metrics
     */
    public Map<String, Object> performContentionOperation(int holdTimeMs, int operationCount) {
        long startTime = System.currentTimeMillis();
        
        // Validate and cap parameters
        final int finalHoldTimeMs = Math.max(10, Math.min(500, holdTimeMs));
        final int finalOperationCount = Math.max(10, Math.min(1000, operationCount));
        
        // Generate unique thread ID based on current thread
        final String threadId = "api_" + Thread.currentThread().getId() + "_" + System.currentTimeMillis();
        
        logger.debug("Starting single-thread contention: holdTime={}ms, operations={}, threadId={}", 
                     finalHoldTimeMs, finalOperationCount, threadId);
        
        long threadWaitTime = 0;
        activeThreads.incrementAndGet();
        
        try {
            for (int op = 0; op < finalOperationCount; op++) {
                long opStart = System.nanoTime();
                
                // Synchronized block - THIS IS WHERE REAL LOCK CONTENTION HAPPENS
                // Multiple concurrent API calls will wait here
                synchronized (sharedMap) {
                    long lockAcquiredTime = System.nanoTime();
                    long waitTime = (lockAcquiredTime - opStart) / 1_000_000; // Convert to ms
                    threadWaitTime += waitTime;
                    
                    // Perform operations while holding the lock
                    String key = threadId + "_op_" + op;
                    sharedMap.put(key, System.currentTimeMillis());
                    
                    // Also contend on the shared list
                    synchronized (sharedList) {
                        sharedList.add(key);
                        
                        // Hold the lock for specified time to increase contention
                        if (finalHoldTimeMs > 0) {
                            try {
                                Thread.sleep(finalHoldTimeMs);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                logger.warn("Thread interrupted during lock hold: {}", threadId);
                                break;
                            }
                        }
                        
                        // Perform some work while holding lock
                        if (sharedList.size() > 1000) {
                            sharedList.subList(0, 500).clear();
                        }
                    }
                    
                    operationCounter.increment();
                    totalOperations.incrementAndGet();
                }
            }
        } finally {
            activeThreads.decrementAndGet();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        totalWaitTimeMs.addAndGet(threadWaitTime);
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "lock-contention");
        response.put("threadId", threadId);
        response.put("holdTimeMsPerOperation", finalHoldTimeMs);
        response.put("operationsCompleted", finalOperationCount);
        response.put("durationMs", duration);
        response.put("waitTimeMs", threadWaitTime);
        response.put("activeThreadsNow", activeThreads.get());
        response.put("contentionRatio", duration > 0 ? (double) threadWaitTime / duration : 0.0);
        response.put("sharedMapSize", sharedMap.size());
        response.put("sharedListSize", sharedList.size());
        response.put("timestamp", System.currentTimeMillis());
        
        logger.debug("Contention operation completed: duration={}ms, wait={}ms, ratio={}, active={}", 
                     duration, threadWaitTime, response.get("contentionRatio"), activeThreads.get());
        
        return response;
    }

    /**
     * Get current contention metrics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeThreads", activeThreads.get());
        metrics.put("totalOperations", totalOperations.get());
        metrics.put("totalWaitTimeMs", totalWaitTimeMs.get());
        metrics.put("sharedMapSize", sharedMap.size());
        metrics.put("sharedListSize", sharedList.size());
        metrics.put("avgWaitTimePerOperation", 
                    totalOperations.get() > 0 ? totalWaitTimeMs.get() / totalOperations.get() : 0);
        
        return metrics;
    }

    /**
     * Clear all accumulated data and reset metrics
     */
    public Map<String, Object> clearData() {
        int mapSize = sharedMap.size();
        int listSize = sharedList.size();
        
        synchronized (sharedMap) {
            synchronized (sharedList) {
                sharedMap.clear();
                sharedList.clear();
            }
        }
        
        // Reset counters
        totalOperations.set(0);
        totalWaitTimeMs.set(0);
        
        // Suggest GC
        System.gc();
        
        Map<String, Object> response = new HashMap<>();
        response.put("operation", "contention-clear");
        response.put("mapEntriesCleared", mapSize);
        response.put("listEntriesCleared", listSize);
        response.put("metricsReset", true);
        response.put("gcSuggested", true);
        response.put("timestamp", System.currentTimeMillis());
        
        logger.info("Cleared contention data: map={}, list={}", mapSize, listSize);
        
        return response;
    }

    /**
     * Get current data store sizes
     */
    public int getSharedMapSize() {
        return sharedMap.size();
    }

    public int getSharedListSize() {
        return sharedList.size();
    }
}
