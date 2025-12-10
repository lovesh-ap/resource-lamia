# Resource Consumption Demo Application

A Java 8 Spring Boot application designed to demonstrate gradual resource consumption through CPU-intensive operations, memory accumulation, and database query patterns. Includes a configurable traffic generation script for testing application behavior under different load scenarios.

## Overview

This application provides six distinct endpoints with different resource consumption characteristics:

1. **POST /api/cpu/load** - CPU-intensive operations with memory accumulation
2. **GET /api/cpu/stable** - Minimal CPU usage (baseline)
3. **POST /api/mem/load** - Memory accumulation with minimal CPU
4. **GET /api/mem/stable** - No memory accumulation (baseline)
5. **POST /api/db/slow** - Slow database queries with N+1 problems and artificial delays
6. **POST /api/db/fast** - Optimized database queries with proper indexing

## Features

- **Resource Consumption Patterns**: Demonstrates CPU load, memory accumulation, and database performance issues
- **Configurable Traffic Generator**: Shell script with 4 modes (slow, moderate, fast, stable)
- **PostgreSQL with Resource Constraints**: Docker-based database with memory and CPU limits
- **Comprehensive Monitoring**: Metrics endpoints exposing JVM, connection pool, and endpoint statistics
- **Actuator Integration**: Spring Boot Actuator for health checks and metrics

## Prerequisites

- **Java**: JDK 8 or higher
- **Maven**: 3.6+
- **Docker**: For PostgreSQL container
- **Docker Compose**: For container orchestration
- **curl**: For traffic generation script

## Quick Start

### 1. Setup

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

### 2. Start Application

```bash
./start-app.sh
```

The application will start on `http://localhost:8080`. Wait for the "Application is ready!" message.

### 3. Generate Traffic

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

### 4. Monitor Application

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
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

### 5. Control Operations

**Clear In-Memory Data**:
```bash
curl -X DELETE http://localhost:8080/api/data/clear
```

**Reset Database**:
```bash
curl -X POST http://localhost:8080/api/db/reset
```

### 6. Stop Everything

```bash
./stop-all.sh
```

## Traffic Generator Modes

| Mode     | Requests/Min | Duration  | Use Case                                    |
|----------|--------------|-----------|---------------------------------------------|
| slow     | 20           | 12 hours  | Gradual resource degradation over long period |
| moderate | 300          | 3 hours   | Medium-paced resource consumption           |
| fast     | 1000         | 30 mins   | Rapid resource exhaustion for quick testing |
| stable   | 1000         | Unlimited | High traffic with no resource accumulation  |

## Endpoint Distribution

When using `--targets=all`, requests are distributed as:

- **cpu-load**: 30%
- **cpu-stable**: 10%
- **mem-load**: 10%
- **mem-stable**: 5%
- **db-slow**: 30%
- **db-fast**: 15%

Weights are automatically renormalized when targeting a subset of endpoints.

## Traffic Generator Options

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

## Database Configuration

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
- In-memory collection sizes
- HikariCP connection pool metrics
- Database record counts

## Project Structure

```
resource-demo/
├── src/main/java/com/demo/resource/
│   ├── ResourceDemoApplication.java
│   ├── controller/
│   │   ├── CpuController.java
│   │   ├── MemoryController.java
│   │   ├── DatabaseController.java
│   │   ├── MetricsController.java
│   │   └── ControlController.java
│   ├── service/
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
│   ├── application.properties
│   └── data.sql
├── docker-compose.yml
├── traffic-generator.sh
├── setup.sh
├── start-app.sh
├── stop-all.sh
├── pom.xml
└── README.md
```

## Use Cases

### Testing Performance Monitoring Tools
Use different modes to simulate various load patterns and test monitoring/alerting systems.

### Demonstrating Memory Leaks
Run `--mode=fast --targets=cpu-load,mem-load` to quickly accumulate memory and observe JVM behavior.

### Database Performance Analysis
Use `--targets=db-slow` to demonstrate connection pool exhaustion and slow query impacts.

### Baseline Performance
Use `--mode=stable` to generate high traffic without resource degradation for baseline metrics.

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
