# Resource Consumption Demo Application

A Java 8 Spring Boot application designed to demonstrate gradual resource consumption through CPU-intensive operations, memory accumulation, database query patterns, and **lock contention**. Includes configurable traffic generation scripts for testing application behavior under different load scenarios.

## Overview

This application provides **two operation modes** with different endpoint sets:

### Database Mode (Default)
Six endpoints demonstrating various resource consumption patterns:

1. **POST /api/cpu/load** - CPU-intensive operations with memory accumulation
2. **GET /api/cpu/stable** - Minimal CPU usage (baseline)
3. **POST /api/mem/load** - Memory accumulation with minimal CPU
4. **GET /api/mem/stable** - No memory accumulation (baseline)
5. **POST /api/db/slow** - Slow database queries with N+1 problems and artificial delays
6. **POST /api/db/fast** - Optimized database queries with proper indexing

### Lock Contention Mode (New!)
Three endpoints demonstrating real lock contention causing performance degradation:

1. **POST /api/contention/load** - Real lock contention with configurable thread count and hold time
2. **GET /api/contention/metrics** - Lock contention statistics and metrics
3. **DELETE /api/contention/clear** - Clear accumulated contention data

**Plus** CPU and Memory endpoints (available in both modes)

## Features

- **Multiple Operation Modes**: Database mode (full features) or Lock mode (no database required)
- **Real Lock Contention**: Synchronized blocks causing actual thread waiting, not simulated delays
- **Resource Consumption Patterns**: CPU load, memory accumulation, database performance issues
- **Configurable Traffic Generators**: Separate scripts for database and lock contention testing
- **PostgreSQL with Resource Constraints**: Docker-based database with memory and CPU limits (database mode only)
- **Comprehensive Monitoring**: Metrics endpoints exposing JVM, connection pool, lock contention, and endpoint statistics
- **Actuator Integration**: Spring Boot Actuator for health checks and metrics

## Prerequisites

### For Database Mode (Full Features)
- **Java**: JDK 8 or higher
- **Maven**: 3.6+
- **Docker**: For PostgreSQL container
- **Docker Compose**: For container orchestration
- **curl**: For traffic generation script

### For Lock Contention Mode (Database-Free)
- **Java**: JDK 8 or higher
- **Maven**: 3.6+
- **curl**: For traffic generation script

## Quick Start

### Option A: Database Mode (Full Features)

#### 1. Setup

Run the setup script to prepare the environment:

```bash
chmod +x setup.sh
./setup.sh
```

This will:
- Check prerequisites
- Start PostgreSQL container with resource constraints
- Build the Spring Boot application
- Make all scripts executable

#### 2. Start Application

#### 2. Start Application

```bash
./start-app.sh
```

The application will start on `http://localhost:8080`. Wait for the "Application is ready!" message.

#### 3. Generate Traffic

Use the traffic generator script with different modes:

**Fast Mode** (1000 req/min for 30 minutes):
```bash
./traffic-generator.sh --mode=fast --targets=all
```

**Moderate Mode** (300 req/min for 3 hours):
```bash
./traffic-generator.sh --mode=moderate --targets=all
```

**Slow Mode** (20 req/min for 12 hours):
```bash
./traffic-generator.sh --mode=slow --targets=all
```

**Stable Mode** (1000 req/min, stable endpoints only):
```bash
./traffic-generator.sh --mode=stable
```

**Custom Target Selection**:
```bash
# Only CPU and memory load
./traffic-generator.sh --mode=fast --targets=cpu-load,mem-load

# Only database slow queries
./traffic-generator.sh --mode=moderate --targets=db-slow

# CPU intensive without memory
./traffic-generator.sh --mode=fast --targets=cpu-stable
```

### Option B: Lock Contention Mode (No Database Required)

#### 1. Build Application

```bash
chmod +x setup.sh
# Run setup without Docker (just Maven build)
mvn clean package -DskipTests
chmod +x start-lock-app.sh traffic-generator-locks.sh
```

#### 2. Start Application in Lock Mode

```bash
./start-lock-app.sh
```

The application will start on `http://localhost:8080` **without** requiring PostgreSQL. Wait for the "Application is ready! (Lock Mode)" message.

#### 3. Generate Lock Contention Traffic

Use the dedicated lock contention traffic generator:

**Fast Mode** (1000 req/min for 30 minutes):
```bash
./traffic-generator-locks.sh --mode=fast --targets=all
```

**Moderate Mode** (300 req/min for 3 hours):
```bash
./traffic-generator-locks.sh --mode=moderate --targets=all
```

**Heavy Contention Only**:
```bash
./traffic-generator-locks.sh --mode=fast --targets=heavy-contention
```

**Mixed Contention Levels**:
```bash
./traffic-generator-locks.sh --mode=moderate --targets=light-contention,moderate-contention,heavy-contention
```

**Stable Mode** (metrics queries only, no contention):
```bash
./traffic-generator-locks.sh --mode=stable
```

### Common Operations (Both Modes)

#### 4. Monitor Application

**System Metrics**:
```bash
curl http://localhost:8080/api/metrics/system | jq
```

**Endpoint Call Counts**:
```bash
curl http://localhost:8080/api/metrics/endpoints
```

**Database Health**:
```bash
curl http://localhost:8080/api/health/db
```

**Actuator Endpoints**:
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active  # Database mode only
curl http://localhost:8080/actuator/metrics/contention.operations  # Lock mode
curl http://localhost:8080/actuator/metrics/contention.wait.time  # Lock mode
```

**Lock Contention Metrics** (Lock Mode):
```bash
curl http://localhost:8080/api/contention/metrics
```

#### 5. Control Operations

**Clear In-Memory Data**:
```bash
curl -X DELETE http://localhost:8080/api/data/clear
```

**Clear Lock Contention Data** (Lock Mode):
```bash
curl -X DELETE http://localhost:8080/api/contention/clear
```

**Reset Database** (Database Mode Only):
```bash
curl -X POST http://localhost:8080/api/db/reset
```

#### 6. Stop Everything

```bash
./stop-all.sh
```

**For Lock Mode**:
```bash
# Stop lock mode application
if [[ -f app-locks.pid ]]; then
    kill $(cat app-locks.pid)
    rm app-locks.pid
fi
```

## Lock Contention Mode Details

### How It Works

Lock contention mode demonstrates **real thread contention** causing actual performance degradation:

1. **Synchronized Blocks**: Multiple threads compete for locks on shared `HashMap` and `ArrayList`
2. **Real Waiting**: Threads actually wait (not `Thread.sleep()` simulation)
3. **Configurable Intensity**: Control thread count, hold time, and operation count
4. **Measurable Impact**: Track total wait time, contention ratio, and latency increase

### Contention Levels

| Level    | Threads | Hold Time (ms) | Operations | Expected Behavior |
|----------|---------|----------------|------------|-------------------|
| Light    | 2-5     | 10-30          | 50-100     | Minimal contention, fast responses |
| Moderate | 5-10    | 30-70          | 100-200    | Noticeable waiting, slower responses |
| Heavy    | 10-20   | 50-100         | 150-300    | Significant contention, degraded latency |

### Performance Degradation Timeline

When running heavy contention at 1000 req/min:

- **0-5 min**: Response time 50-200ms (baseline)
- **5-15 min**: Response time 200-500ms (moderate contention)
- **15-30 min**: Response time 500-1500ms (heavy contention)
- **30+ min**: Response time 1-3 seconds (severe contention)

This creates a **realistic latency increase** without HTTP 500 errors or crashes.

### Lock Contention Endpoints

**POST /api/contention/load**
```bash
# Light contention
curl -X POST "http://localhost:8080/api/contention/load?concurrentThreads=3&holdTimeMs=20&operationCount=50"

# Moderate contention
curl -X POST "http://localhost:8080/api/contention/load?concurrentThreads=7&holdTimeMs=50&operationCount=150"

# Heavy contention
curl -X POST "http://localhost:8080/api/contention/load?concurrentThreads=15&holdTimeMs=80&operationCount=250"
```

**Response Example**:
```json
{
  "operation": "lock-contention",
  "concurrentThreads": 15,
  "holdTimeMsPerOperation": 80,
  "operationsPerThread": 250,
  "totalOperations": 3750,
  "durationMs": 12456,
  "totalWaitTimeMs": 8234,
  "avgWaitTimePerThreadMs": 548,
  "maxThreadWaitTimeMs": 892,
  "contentionRatio": 0.661,
  "sharedMapSize": 3750,
  "sharedListSize": 3750,
  "timestamp": 1702234567890
}
```

**GET /api/contention/metrics**
```bash
curl http://localhost:8080/api/contention/metrics
```

**Response**:
```json
{
  "activeThreads": 0,
  "totalOperations": 15680,
  "totalWaitTimeMs": 34521,
  "sharedMapSize": 15680,
  "sharedListSize": 15680,
  "avgWaitTimePerOperation": 2
}
```

**DELETE /api/contention/clear**
```bash
curl -X DELETE http://localhost:8080/api/contention/clear
```

## Traffic Generator Comparison

### Database Mode Traffic Generator (`traffic-generator.sh`)

**Targets**: `cpu-load`, `cpu-stable`, `mem-load`, `mem-stable`, `db-slow`, `db-fast`, `all`

| Mode     | Requests/Min | Duration  | Use Case                                    |
|----------|--------------|-----------|---------------------------------------------|
| slow     | 20           | 12 hours  | Gradual resource degradation over long period |
| moderate | 300          | 3 hours   | Medium-paced resource consumption           |
| fast     | 1000         | 30 mins   | Rapid resource exhaustion for quick testing |
| stable   | 1000         | Unlimited | High traffic with no resource accumulation  |

**Endpoint Distribution** (when using `--targets=all`):
- **cpu-load**: 30%
- **cpu-stable**: 10%
- **mem-load**: 10%
- **mem-stable**: 5%
- **db-slow**: 30%
- **db-fast**: 15%

### Lock Contention Traffic Generator (`traffic-generator-locks.sh`)

**Targets**: `light-contention`, `moderate-contention`, `heavy-contention`, `stable`, `all`

| Mode     | Requests/Min | Duration  | Use Case                                    |
|----------|--------------|-----------|---------------------------------------------|
| slow     | 20           | 12 hours  | Gradual lock contention buildup             |
| moderate | 300          | 3 hours   | Medium-paced contention increase            |
| fast     | 1000         | 30 mins   | Rapid contention for testing alerts         |
| stable   | 1000         | Unlimited | High traffic to metrics endpoint only       |

**Endpoint Distribution** (when using `--targets=all`):
- **light-contention**: 20%
- **moderate-contention**: 30%
- **heavy-contention**: 40%
- **stable**: 10%

## Spring Profiles

The application uses Spring profiles to enable/disable database dependencies:

### Default Profile (Database Mode)
```bash
# Starts with PostgreSQL and all database endpoints
./start-app.sh
# or
java -jar target/resource-demo-1.0.0.jar
```

**Features**:
- All 6 database endpoints (cpu, mem, db)
- PostgreSQL connection
- HikariCP connection pool
- Full JPA/Hibernate
- Lock contention endpoints (also available)

### Locks Profile (Database-Free)
```bash
# Starts without PostgreSQL
./start-lock-app.sh
# or
java -jar target/resource-demo-1.0.0.jar --spring.profiles.active=locks
```

**Features**:
- Lock contention endpoints
- CPU and memory endpoints
- **No database** (PostgreSQL not required)
- **No Docker** needed
- Faster startup
- Lower resource footprint

## Traffic Generator Modes

When using `--targets=all`, requests are distributed as:

- **cpu-load**: 30%
- **cpu-stable**: 10%
- **mem-load**: 10%
- **mem-stable**: 5%
- **db-slow**: 30%
- **db-fast**: 15%

Weights are automatically renormalized when targeting a subset of endpoints.

## Traffic Generator Options

### Database Traffic Generator Options

```bash
./traffic-generator.sh [OPTIONS]

Options:
  --mode=<MODE>        Traffic mode: slow, moderate, fast, stable (required)
  --targets=<TARGETS>  Comma-separated endpoint targets (required unless mode=stable)
                       Values: cpu-load, cpu-stable, mem-load, mem-stable, db-slow, db-fast, all
  --duration=<SECONDS> Override default duration (optional)
  --url=<URL>          Base URL of application (default: http://localhost:8080)
  --help               Show help message

Examples:
  ./traffic-generator.sh --mode=fast --targets=all
  ./traffic-generator.sh --mode=slow --targets=cpu-load,db-slow --duration=3600
  ./traffic-generator.sh --mode=stable
```

### Lock Contention Traffic Generator Options

```bash
./traffic-generator-locks.sh [OPTIONS]

Options:
  --mode=<MODE>        Traffic mode: slow, moderate, fast, stable (required)
  --targets=<TARGETS>  Comma-separated contention levels (required unless mode=stable)
                       Values: light-contention, moderate-contention, heavy-contention, stable, all
  --duration=<SECONDS> Override default duration (optional)
  --url=<URL>          Base URL of application (default: http://localhost:8080)
  --help               Show help message

Examples:
  ./traffic-generator-locks.sh --mode=fast --targets=all
  ./traffic-generator-locks.sh --mode=moderate --targets=heavy-contention --duration=1800
  ./traffic-generator-locks.sh --mode=stable
```

## Database Configuration (Database Mode Only)

PostgreSQL runs in Docker with the following constraints:
- **Memory Limit**: 512MB
- **CPU Limit**: 1.0 core
- **Connection Pool**: Max 20 connections

The database is seeded with:
- 75,000 `data_record` entries
- 150,000 `related_entity` entries
- 200,000 `audit_log` entries

Indexes are deliberately missing on certain columns to demonstrate slow query performance.

## Application Configuration

Key configuration in `src/main/resources/application.properties`:

```properties
# Connection Pool
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/resourcedb
spring.datasource.username=demouser
spring.datasource.password=demopass
```

## Monitoring Metrics

The `/api/metrics/system` endpoint provides:
- JVM memory (heap, non-heap, used, free)
- Garbage collection statistics
- In-memory collection sizes (CPU, Memory, Lock contention stores)
- Lock contention metrics (total operations, wait time, contention ratio)
- HikariCP connection pool metrics (database mode only)
- Database record counts (database mode only)

## Project Structure

```
resource-demo/
├── src/main/java/com/demo/resource/
│   ├── ResourceDemoApplication.java
│   ├── controller/
│   │   ├── ContentionController.java       # NEW: Lock contention endpoints
│   │   ├── CpuController.java
│   │   ├── MemoryController.java
│   │   ├── DatabaseController.java
│   │   ├── MetricsController.java
│   │   └── ControlController.java
│   ├── service/
│   │   ├── LockContentionService.java      # NEW: Lock contention logic
│   │   ├── CpuService.java
│   │   ├── MemoryService.java
│   │   └── DatabaseService.java
│   ├── entity/
│   │   ├── DataRecord.java
│   │   ├── RelatedEntity.java
│   │   └── AuditLog.java
│   └── repository/
│       ├── DataRecordRepository.java
│       ├── RelatedEntityRepository.java
│       └── AuditLogRepository.java
├── src/main/resources/
│   ├── application.properties              # Default profile (database mode)
│   ├── application-locks.properties        # NEW: Locks profile (no database)
│   └── data.sql
├── docker-compose.yml
├── traffic-generator.sh                    # Database mode traffic
├── traffic-generator-locks.sh              # NEW: Lock contention traffic
├── setup.sh
├── start-app.sh                            # Database mode startup
├── start-lock-app.sh                       # NEW: Lock mode startup
├── stop-all.sh
├── pom.xml
└── README.md
```

## Use Cases

### Testing Performance Monitoring Tools
- **Database Mode**: Use different modes to simulate various load patterns and test monitoring/alerting systems
- **Lock Mode**: Test lock contention detection, thread dump analysis, and latency spike alerts

### Demonstrating Memory Leaks
Run `--mode=fast --targets=cpu-load,mem-load` to quickly accumulate memory and observe JVM behavior.

### Database Performance Analysis
Use `--targets=db-slow` to demonstrate connection pool exhaustion and slow query impacts.

### Lock Contention Scenarios
Use `--mode=fast --targets=heavy-contention` to create severe thread contention for testing:
- APM tools detecting lock contention
- Thread dump analysis
- Response time degradation alerts
- Performance profiler testing

### Baseline Performance
- **Database Mode**: Use `--mode=stable` to generate high traffic without resource degradation
- **Lock Mode**: Use `--mode=stable` to query metrics without creating contention

## Troubleshooting

**PostgreSQL not starting**:
```bash
docker-compose down
docker-compose up -d postgres
docker logs resource-demo-postgres
```

**Application fails to start**:
```bash
tail -f application.log
```

**High memory usage**:
```bash
curl -X DELETE http://localhost:8080/api/data/clear
```

**Database connection issues**:
```bash
curl http://localhost:8080/api/health/db
docker exec resource-demo-postgres pg_isready -U demouser
```

## Cleanup

To completely remove all data and containers:

```bash
./stop-all.sh
docker-compose down -v
rm -rf target/
rm -f application.log app.pid traffic-generator-*.log
```

## License

This is a demonstration application for educational and testing purposes.
