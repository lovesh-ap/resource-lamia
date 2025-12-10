#!/bin/bash

################################################################################
# Stop All Script
# Stops the application and Docker containers
################################################################################

echo "========================================="
echo "Stopping Resource Demo Application"
echo "========================================="
echo ""

# Stop Spring Boot application
if [[ -f app.pid ]]; then
    APP_PID=$(cat app.pid)
    if ps -p $APP_PID > /dev/null 2>&1; then
        echo "Stopping application (PID: $APP_PID)..."
        kill $APP_PID
        
        # Wait for graceful shutdown
        sleep 3
        
        # Force kill if still running
        if ps -p $APP_PID > /dev/null 2>&1; then
            echo "Force killing application..."
            kill -9 $APP_PID
        fi
        
        echo "✓ Application stopped"
    else
        echo "Application is not running"
    fi
    rm -f app.pid
else
    echo "No PID file found, checking for running process..."
    pkill -f "resource-demo-1.0.0.jar" && echo "✓ Application stopped" || echo "No running application found"
fi

echo ""

# Stop Docker containers
echo "Stopping Docker containers..."
docker-compose down

echo "✓ Docker containers stopped"
echo ""

echo "========================================="
echo "Cleanup completed"
echo "========================================="
echo ""
