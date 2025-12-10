#!/bin/bash

################################################################################
# Setup Script for Resource Demo Application
# Prepares the environment and builds the application
################################################################################

set -e

echo "========================================="
echo "Resource Demo Application - Setup"
echo "========================================="
echo ""

# Check for required tools
echo "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "Error: Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed. Please install Maven first."
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed. Please install Java 8 or higher."
    exit 1
fi

echo "✓ All prerequisites found"
echo ""

# Check Java version
JAVA_VERSION_STRING=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
# Handle both old (1.8.x) and new (9+) version formats
if [[ "$JAVA_VERSION_STRING" == 1.* ]]; then
    # Java 8 or earlier: version format is 1.8.x
    JAVA_VERSION=$(echo "$JAVA_VERSION_STRING" | cut -d'.' -f2)
else
    # Java 9+: version format is 9.x, 11.x, etc.
    JAVA_VERSION=$(echo "$JAVA_VERSION_STRING" | cut -d'.' -f1)
fi

if [[ "$JAVA_VERSION" -lt 8 ]]; then
    echo "Error: Java 8 or higher is required (found Java $JAVA_VERSION)"
    exit 1
fi
echo "✓ Java version: $(java -version 2>&1 | head -n1)"
echo ""

# Start PostgreSQL
echo "Starting PostgreSQL container..."
docker-compose up -d postgres

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
max_attempts=30
attempt=0
while ! docker exec resource-demo-postgres pg_isready -U demouser -d resourcedb &> /dev/null; do
    attempt=$((attempt + 1))
    if [[ $attempt -ge $max_attempts ]]; then
        echo "Error: PostgreSQL did not become ready in time"
        exit 1
    fi
    echo "  Waiting... (attempt $attempt/$max_attempts)"
    sleep 2
done

echo "✓ PostgreSQL is ready"
echo ""

# Build application
echo "Building Spring Boot application..."
mvn clean package -DskipTests

if [[ $? -ne 0 ]]; then
    echo "Error: Maven build failed"
    exit 1
fi

echo "✓ Application built successfully"
echo ""

# Make scripts executable
echo "Setting script permissions..."
chmod +x traffic-generator.sh
chmod +x start-app.sh
chmod +x stop-all.sh

echo "✓ Scripts are executable"
echo ""

echo "========================================="
echo "Setup completed successfully!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Start the application: ./start-app.sh"
echo "  2. Generate traffic: ./traffic-generator.sh --mode=fast --targets=all"
echo "  3. Monitor metrics: curl http://localhost:8080/api/metrics/system"
echo "  4. Stop everything: ./stop-all.sh"
echo ""
