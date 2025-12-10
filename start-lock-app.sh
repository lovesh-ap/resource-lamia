#!/bin/bash

################################################################################
# Start Application Script - Lock Contention Mode
# Starts the Spring Boot application with lock contention profile (no database)
################################################################################

set -e

echo "========================================="
echo "Starting Resource Demo - Lock Mode"
echo "========================================="
echo ""

# Check if JAR exists
JAR_FILE="target/resource-demo-1.0.0.jar"

if [[ ! -f "$JAR_FILE" ]]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run ./setup.sh first to build the application"
    exit 1
fi

# Check if port 8585 is already in use
if lsof -Pi :8585 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "Error: Port 8585 is already in use"
    echo "Please stop the application using that port first"
    exit 1
fi

# Start application with locks profile (no database required)
echo "Starting Spring Boot application in lock contention mode..."
echo "Profile: locks (database disabled)"
echo "Port: 8585"
echo "Log output will be written to application-locks.log"
echo ""

nohup java -jar "$JAR_FILE" \
    --spring.profiles.active=locks \
    --server.port=8585 \
    > application-locks.log 2>&1 &
APP_PID=$!

echo "Application started with PID: $APP_PID"
echo $APP_PID > app-locks.pid

echo ""
echo "Waiting for application to be ready..."
sleep 5

# Check if application is running
max_attempts=30
attempt=0
while ! curl -s -f -o /dev/null http://localhost:8585/actuator/health; do
    attempt=$((attempt + 1))
    if [[ $attempt -ge $max_attempts ]]; then
        echo "Error: Application did not become ready in time"
        echo "Check application-locks.log for details"
        exit 1
    fi
    
    # Check if process is still running
    if ! ps -p $APP_PID > /dev/null; then
        echo "Error: Application process died"
        echo "Check application-locks.log for details"
        exit 1
    fi
    
    echo "  Waiting... (attempt $attempt/$max_attempts)"
    sleep 2
done

echo ""
echo "========================================="
echo "Application is ready! (Lock Mode)"
echo "========================================="
echo ""
echo "Available endpoints:"
echo "  Lock Contention:"
echo "    POST   http://localhost:8585/api/contention/load"
echo "    GET    http://localhost:8585/api/contention/metrics"
echo "    DELETE http://localhost:8585/api/contention/clear"
echo ""
echo "  CPU & Memory (still available):"
echo "    POST http://localhost:8585/api/cpu/load"
echo "    GET  http://localhost:8585/api/cpu/stable"
echo "    POST http://localhost:8585/api/mem/load"
echo "    GET  http://localhost:8585/api/mem/stable"
echo ""
echo "  Monitoring:"
echo "    GET  http://localhost:8585/api/metrics/system"
echo "    GET  http://localhost:8585/api/metrics/endpoints"
echo "    GET  http://localhost:8585/actuator/health"
echo "    GET  http://localhost:8585/actuator/metrics"
echo ""
echo "  Control:"
echo "    DELETE http://localhost:8585/api/data/clear"
echo ""
echo "Note: Database endpoints are NOT available in lock mode"
echo "      Use ./start-app.sh for full database functionality"
echo ""
echo "Application PID: $APP_PID (saved to app-locks.pid)"
echo "Port: 8585"
echo "Log file: application-locks.log"
echo ""
