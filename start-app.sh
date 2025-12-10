#!/bin/bash

################################################################################
# Start Application Script
# Starts the Spring Boot application
################################################################################

set -e

echo "========================================="
echo "Starting Resource Demo Application"
echo "========================================="
echo ""

# Check if PostgreSQL is running
if ! docker ps | grep resource-demo-postgres &> /dev/null; then
    echo "PostgreSQL is not running. Starting it now..."
    docker-compose up -d postgres
    
    echo "Waiting for PostgreSQL..."
    sleep 5
fi

# Check if JAR exists
JAR_FILE="target/resource-demo-1.0.0.jar"

if [[ ! -f "$JAR_FILE" ]]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run ./setup.sh first to build the application"
    exit 1
fi

# Start application
echo "Starting Spring Boot application..."
echo "Log output will be written to application.log"
echo ""

nohup java -jar "$JAR_FILE" > application.log 2>&1 &
APP_PID=$!

echo "Application started with PID: $APP_PID"
echo $APP_PID > app.pid

echo ""
echo "Waiting for application to be ready..."
sleep 5

# Check if application is running
max_attempts=30
attempt=0
while ! curl -s -f -o /dev/null http://localhost:8080/actuator/health; do
    attempt=$((attempt + 1))
    if [[ $attempt -ge $max_attempts ]]; then
        echo "Error: Application did not become ready in time"
        echo "Check application.log for details"
        exit 1
    fi
    
    # Check if process is still running
    if ! ps -p $APP_PID > /dev/null; then
        echo "Error: Application process died"
        echo "Check application.log for details"
        exit 1
    fi
    
    echo "  Waiting... (attempt $attempt/$max_attempts)"
    sleep 2
done

echo ""
echo "========================================="
echo "Application is ready!"
echo "========================================="
echo ""
echo "Available endpoints:"
echo "  Load Endpoints:"
echo "    POST http://localhost:8080/api/cpu/load"
echo "    GET  http://localhost:8080/api/cpu/stable"
echo "    POST http://localhost:8080/api/mem/load"
echo "    GET  http://localhost:8080/api/mem/stable"
echo "    POST http://localhost:8080/api/db/slow"
echo "    POST http://localhost:8080/api/db/fast"
echo ""
echo "  Monitoring:"
echo "    GET  http://localhost:8080/api/metrics/system"
echo "    GET  http://localhost:8080/api/metrics/endpoints"
echo "    GET  http://localhost:8080/api/health/db"
echo "    GET  http://localhost:8080/actuator/health"
echo ""
echo "  Control:"
echo "    DELETE http://localhost:8080/api/data/clear"
echo "    POST   http://localhost:8080/api/db/reset"
echo ""
echo "Application PID: $APP_PID (saved to app.pid)"
echo "Log file: application.log"
echo ""
