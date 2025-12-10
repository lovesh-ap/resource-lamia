#!/bin/bash

################################################################################
# Traffic Generator Script for Resource Demo Application
# 
# This script generates HTTP traffic to the Spring Boot application with
# configurable modes and endpoint targets.
#
# Usage:
#   ./traffic-generator.sh --mode=<MODE> --targets=<TARGETS> [--duration=<SECONDS>]
#
# Modes:
#   slow      - 20 req/min for 12 hours (43200 seconds)
#   moderate  - 300 req/min for 3 hours (10800 seconds)
#   fast      - 1000 req/min for 30 minutes (1800 seconds)
#   stable    - 1000 req/min to stable endpoints only
#
# Targets (comma-separated):
#   cpu-load, cpu-stable, mem-load, mem-stable, db-slow, db-fast, all
#
# Examples:
#   ./traffic-generator.sh --mode=fast --targets=cpu-load,mem-load
#   ./traffic-generator.sh --mode=slow --targets=all --duration=7200
#   ./traffic-generator.sh --mode=stable
################################################################################

# Default values
MODE=""
TARGETS=""
DURATION=""
BASE_URL="http://localhost:8080"

# Parse arguments
for arg in "$@"; do
    case $arg in
        --mode=*)
            MODE="${arg#*=}"
            shift
            ;;
        --targets=*)
            TARGETS="${arg#*=}"
            shift
            ;;
        --duration=*)
            DURATION="${arg#*=}"
            shift
            ;;
        --url=*)
            BASE_URL="${arg#*=}"
            shift
            ;;
        --help)
            echo "Usage: $0 --mode=<MODE> --targets=<TARGETS> [--duration=<SECONDS>] [--url=<URL>]"
            echo ""
            echo "Modes: slow, moderate, fast, stable"
            echo "Targets: cpu-load, cpu-stable, mem-load, mem-stable, db-slow, db-fast, all"
            echo ""
            echo "Examples:"
            echo "  $0 --mode=fast --targets=cpu-load,mem-load"
            echo "  $0 --mode=slow --targets=all"
            echo "  $0 --mode=stable"
            exit 0
            ;;
        *)
            echo "Unknown argument: $arg"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Validate mode
if [[ -z "$MODE" ]]; then
    echo "Error: --mode is required"
    echo "Use --help for usage information"
    exit 1
fi

if [[ ! "$MODE" =~ ^(slow|moderate|fast|stable)$ ]]; then
    echo "Error: Invalid mode '$MODE'. Must be: slow, moderate, fast, or stable"
    exit 1
fi

# Set defaults for stable mode
if [[ "$MODE" == "stable" ]]; then
    if [[ -z "$TARGETS" ]]; then
        TARGETS="cpu-stable,mem-stable,db-fast"
    fi
fi

# Validate targets
if [[ -z "$TARGETS" ]]; then
    echo "Error: --targets is required"
    echo "Use --help for usage information"
    exit 1
fi

# Configure mode-specific settings
case "$MODE" in
    slow)
        REQUESTS_PER_MIN=20
        DEFAULT_DURATION=43200  # 12 hours
        INTERVAL=3.0  # seconds between requests (60/20)
        ;;
    moderate)
        REQUESTS_PER_MIN=300
        DEFAULT_DURATION=10800  # 3 hours
        INTERVAL=0.2  # seconds between requests (60/300)
        ;;
    fast)
        REQUESTS_PER_MIN=1000
        DEFAULT_DURATION=1800  # 30 minutes
        INTERVAL=0.06  # seconds between requests (60/1000)
        ;;
    stable)
        REQUESTS_PER_MIN=1000
        DEFAULT_DURATION=0  # Run indefinitely
        INTERVAL=0.06  # seconds between requests (60/1000)
        ;;
esac

# Use provided duration or default
if [[ -n "$DURATION" ]]; then
    TOTAL_DURATION=$DURATION
else
    TOTAL_DURATION=$DEFAULT_DURATION
fi

# Parse targets
IFS=',' read -ra TARGET_ARRAY <<< "$TARGETS"

# Expand 'all' to all endpoint types
if [[ " ${TARGET_ARRAY[@]} " =~ " all " ]]; then
    TARGET_ARRAY=("cpu-load" "cpu-stable" "mem-load" "mem-stable" "db-slow" "db-fast")
fi

# Helper functions to get endpoint properties (bash 3.2 compatible)
get_endpoint_weight() {
    case "$1" in
        cpu-load) echo 30 ;;
        cpu-stable) echo 10 ;;
        mem-load) echo 10 ;;
        mem-stable) echo 5 ;;
        db-slow) echo 30 ;;
        db-fast) echo 15 ;;
        *) echo 0 ;;
    esac
}

get_endpoint_url() {
    case "$1" in
        cpu-load) echo "$BASE_URL/api/cpu/load" ;;
        cpu-stable) echo "$BASE_URL/api/cpu/stable" ;;
        mem-load) echo "$BASE_URL/api/mem/load" ;;
        mem-stable) echo "$BASE_URL/api/mem/stable" ;;
        db-slow) echo "$BASE_URL/api/db/slow" ;;
        db-fast) echo "$BASE_URL/api/db/fast" ;;
        *) echo "" ;;
    esac
}

get_endpoint_method() {
    case "$1" in
        cpu-load) echo "POST" ;;
        cpu-stable) echo "GET" ;;
        mem-load) echo "POST" ;;
        mem-stable) echo "GET" ;;
        db-slow) echo "POST" ;;
        db-fast) echo "POST" ;;
        *) echo "GET" ;;
    esac
}

# Calculate total weight for selected targets
TOTAL_WEIGHT=0
for target in "${TARGET_ARRAY[@]}"; do
    weight=$(get_endpoint_weight "$target")
    TOTAL_WEIGHT=$((TOTAL_WEIGHT + weight))
done

if [[ $TOTAL_WEIGHT -eq 0 ]]; then
    echo "Error: No valid targets specified"
    exit 1
fi

# Initialize counters using simple variables with prefixes
for target in "${TARGET_ARRAY[@]}"; do
    eval "REQUEST_COUNT_${target//-/_}=0"
    eval "SUCCESS_COUNT_${target//-/_}=0"
    eval "ERROR_COUNT_${target//-/_}=0"
done

TOTAL_REQUESTS=0
START_TIME=$(date +%s)

# Log file
LOG_FILE="traffic-generator-$(date +%Y%m%d-%H%M%S).log"

# Cleanup function
cleanup() {
    echo ""
    echo "========================================="
    echo "Traffic Generator Summary"
    echo "========================================="
    echo "Mode: $MODE"
    echo "Targets: ${TARGET_ARRAY[*]}"
    echo "Duration: $(($(date +%s) - START_TIME)) seconds"
    echo ""
    echo "Request Statistics:"
    echo "-----------------------------------------"
    
    for target in "${TARGET_ARRAY[@]}"; do
        local var_name="${target//-/_}"
        local req_count=$(eval "echo \$REQUEST_COUNT_${var_name}")
        local succ_count=$(eval "echo \$SUCCESS_COUNT_${var_name}")
        local err_count=$(eval "echo \$ERROR_COUNT_${var_name}")
        echo "  $target:"
        echo "    Total: ${req_count:-0}"
        echo "    Success: ${succ_count:-0}"
        echo "    Errors: ${err_count:-0}"
    done
    
    echo "-----------------------------------------"
    echo "Total Requests: $TOTAL_REQUESTS"
    echo ""
    echo "Log file: $LOG_FILE"
    echo "========================================="
    
    # Kill all background jobs
    jobs -p | xargs kill 2>/dev/null || true
    exit 0
}

# Trap signals for cleanup
trap cleanup SIGINT SIGTERM EXIT

# Function to make HTTP request
make_request() {
    local target=$1
    local url=$(get_endpoint_url "$target")
    local method=$(get_endpoint_method "$target")
    local var_name="${target//-/_}"
    
    local params=""
    case "$target" in
        cpu-load)
            local iterations=$((RANDOM % 10 + 5))
            local dataSizeMB=$((RANDOM % 3 + 1))
            params="?iterations=$iterations&dataSizeMB=$dataSizeMB"
            ;;
        mem-load)
            local objectCount=$((RANDOM % 5 + 3))
            local sizeMB=$((RANDOM % 3 + 1))
            params="?objectCount=$objectCount&sizeMB=$sizeMB"
            ;;
    esac
    
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Add timeouts: --connect-timeout (connection) and --max-time (total operation)
    if [[ "$method" == "POST" ]]; then
        response=$(curl -s -w "\n%{http_code}" --connect-timeout 5 --max-time 30 -X POST "${url}${params}" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" --connect-timeout 5 --max-time 30 "${url}${params}" 2>&1)
    fi
    
    http_code=$(echo "$response" | tail -n1)
    
    eval "REQUEST_COUNT_${var_name}=\$((REQUEST_COUNT_${var_name} + 1))"
    TOTAL_REQUESTS=$((TOTAL_REQUESTS + 1))
    
    if [[ "$http_code" -ge 200 && "$http_code" -lt 300 ]]; then
        eval "SUCCESS_COUNT_${var_name}=\$((SUCCESS_COUNT_${var_name} + 1))"
        echo "[$timestamp] SUCCESS: $target (HTTP $http_code)" | tee -a "$LOG_FILE"
    else
        eval "ERROR_COUNT_${var_name}=\$((ERROR_COUNT_${var_name} + 1))"
        echo "[$timestamp] ERROR: $target (HTTP $http_code)" | tee -a "$LOG_FILE"
    fi
}

# Function to select target based on weights
select_target() {
    local rand=$((RANDOM % 100))
    local cumulative=0
    
    for target in "${TARGET_ARRAY[@]}"; do
        local weight=$(get_endpoint_weight "$target")
        local normalized=$((weight * 100 / TOTAL_WEIGHT))
        cumulative=$((cumulative + normalized))
        if [[ $rand -lt $cumulative ]]; then
            echo "$target"
            return
        fi
    done
    
    # Fallback to first target
    echo "${TARGET_ARRAY[0]}"
}

# Main execution
echo "========================================="
echo "Resource Demo Traffic Generator"
echo "========================================="
echo "Mode: $MODE"
echo "Requests per minute: $REQUESTS_PER_MIN"
echo "Interval: $INTERVAL seconds"
echo "Duration: $([ $TOTAL_DURATION -eq 0 ] && echo 'indefinite' || echo "$TOTAL_DURATION seconds")"
echo "Targets: ${TARGET_ARRAY[*]}"
echo ""
echo "Weights:"
for target in "${TARGET_ARRAY[@]}"; do
    weight=$(get_endpoint_weight "$target")
    normalized=$((weight * 100 / TOTAL_WEIGHT))
    echo "  $target: ${normalized}%"
done
echo ""
echo "Base URL: $BASE_URL"
echo "Log file: $LOG_FILE"
echo "========================================="
echo ""
echo "Starting traffic generation... (Press Ctrl+C to stop)"
echo ""

# Check if server is reachable
if ! curl -s -f -o /dev/null --connect-timeout 5 --max-time 10 "$BASE_URL/actuator/health"; then
    echo "Warning: Server at $BASE_URL may not be reachable"
    echo "Continuing anyway..."
    echo ""
fi

# Main loop
ELAPSED=0
while true; do
    # Select target endpoint based on weights
    target=$(select_target)
    
    # Make request in background to allow concurrent requests
    make_request "$target" &
    
    # Control concurrency - wait if too many background jobs
    while [[ $(jobs -r | wc -l) -gt 10 ]]; do
        sleep 0.1
    done
    
    # Sleep for interval
    sleep "$INTERVAL"
    
    # Check duration
    if [[ $TOTAL_DURATION -gt 0 ]]; then
        ELAPSED=$(($(date +%s) - START_TIME))
        if [[ $ELAPSED -ge $TOTAL_DURATION ]]; then
            echo ""
            echo "Duration completed. Stopping..."
            break
        fi
    fi
    
    # Print progress every 100 requests
    if [[ $((TOTAL_REQUESTS % 100)) -eq 0 && $TOTAL_REQUESTS -gt 0 ]]; then
        echo "Progress: $TOTAL_REQUESTS requests sent (Elapsed: $ELAPSED seconds)"
    fi
done

# Wait for remaining background jobs
wait

# Cleanup will be called by trap
