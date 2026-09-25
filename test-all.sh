#!/usr/bin/env bash
set -e

# Universal API Verification Script for TinyURL Polyglot Services
# Can be run against a specific port or scans 8001-8010.

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

test_service() {
    local port=$1
    local name=$2
    local base_url="http://localhost:${port}"

    echo -e "\n${BLUE}====================================================${NC}"
    echo -e "${BLUE}Testing Service: ${name} on port ${port}${NC}"
    echo -e "${BLUE}====================================================${NC}"

    # 1. Health check
    echo -n "1. Checking /api/health ... "
    health_resp=$(curl -s -w "\n%{http_code}" "${base_url}/api/health" || true)
    http_code=$(echo "$health_resp" | tail -n1)
    body=$(echo "$health_resp" | sed '$d')

    if [ "$http_code" != "200" ]; then
        echo -e "${RED}FAILED (HTTP $http_code)${NC}"
        echo "   Response: $body"
        return 1
    fi
    echo -e "${GREEN}OK${NC} ($body)"

    # 2. Shorten URL (auto-generated slug)
    echo -n "2. Shortening URL (random slug) ... "
    shorten_resp=$(curl -s -w "\n%{http_code}" -X POST "${base_url}/api/shorten" \
        -H "Content-Type: application/json" \
        -d '{"url": "https://news.ycombinator.com"}')
    http_code=$(echo "$shorten_resp" | tail -n1)
    body=$(echo "$shorten_resp" | sed '$d')

    if [ "$http_code" != "201" ]; then
        echo -e "${RED}FAILED (HTTP $http_code)${NC}"
        echo "   Response: $body"
        return 1
    fi
    echo -e "${GREEN}OK${NC}"

    # 3. Shorten with custom alias
    alias_name="custom-${port}-$$"
    echo -n "3. Shortening with custom alias '$alias_name' ... "
    alias_resp=$(curl -s -w "\n%{http_code}" -X POST "${base_url}/api/shorten" \
        -H "Content-Type: application/json" \
        -d "{\"url\": \"https://github.com\", \"custom_alias\": \"$alias_name\"}")
    http_code=$(echo "$alias_resp" | tail -n1)
    body=$(echo "$alias_resp" | sed '$d')

    if [ "$http_code" != "201" ]; then
        echo -e "${RED}FAILED (HTTP $http_code)${NC}"
        echo "   Response: $body"
        return 1
    fi
    echo -e "${GREEN}OK${NC}"

    # 4. Duplicate alias (collision handling)
    echo -n "4. Testing duplicate alias collision (expect 400) ... "
    dup_resp=$(curl -s -w "\n%{http_code}" -X POST "${base_url}/api/shorten" \
        -H "Content-Type: application/json" \
        -d "{\"url\": \"https://github.com\", \"custom_alias\": \"$alias_name\"}")
    http_code=$(echo "$dup_resp" | tail -n1)

    if [ "$http_code" != "400" ]; then
        echo -e "${RED}FAILED (Expected 400, got $http_code)${NC}"
        return 1
    fi
    echo -e "${GREEN}OK (Rejected with 400)${NC}"

    # 5. Redirect test (302)
    echo -n "5. Testing 302 Redirect for '/$alias_name' ... "
    headers=$(curl -s -I "${base_url}/${alias_name}")
    status_line=$(echo "$headers" | head -n1)
    location_line=$(echo "$headers" | grep -i "^location:" | tr -d '\r')

    if ! echo "$status_line" | grep -q "302"; then
        echo -e "${RED}FAILED (Expected 302 redirect)${NC}"
        echo "   Headers: $headers"
        return 1
    fi
    if ! echo "$location_line" | grep -qi "github.com"; then
        echo -e "${RED}FAILED (Incorrect Location header: $location_line)${NC}"
        return 1
    fi
    echo -e "${GREEN}OK (Redirected to $location_line)${NC}"

    # 6. Stats test (click count >= 1)
    echo -n "6. Testing /api/stats/$alias_name ... "
    stats_resp=$(curl -s -w "\n%{http_code}" "${base_url}/api/stats/${alias_name}")
    http_code=$(echo "$stats_resp" | tail -n1)
    body=$(echo "$stats_resp" | sed '$d')

    if [ "$http_code" != "200" ]; then
        echo -e "${RED}FAILED (HTTP $http_code)${NC}"
        return 1
    fi
    if ! echo "$body" | grep -q '"click_count":[ ]*[1-9]'; then
        echo -e "${RED}FAILED (Click count not incremented)${NC}"
        echo "   Body: $body"
        return 1
    fi
    echo -e "${GREEN}OK${NC} ($body)"

    echo -e "${GREEN}>>> All tests PASSED for ${name} (Port ${port}) <<<${NC}"
    return 0
}

TARGET_PORT="$1"

if [ -n "$TARGET_PORT" ]; then
    test_service "$TARGET_PORT" "Port $TARGET_PORT"
else
    declare -A STACKS=(
        [8001]="01-go"
        [8002]="02-typescript-fastify"
        [8003]="03-python-fastapi"
        [8004]="04-rust-axum"
        [8005]="05-java-spring"
        [8006]="06-dotnet-minimal"
        [8007]="07-ruby-sinatra"
        [8008]="08-php-slim"
        [8009]="09-kotlin-ktor"
        [8010]="10-bun-hono"
    )

    passed=0
    failed=0
    skipped=0

    for port in 8001 8002 8003 8004 8005 8006 8007 8008 8009 8010; do
        # Check if service is listening
        if nc -z localhost "$port" 2>/dev/null; then
            if test_service "$port" "${STACKS[$port]}"; then
                ((passed++))
            else
                ((failed++))
            fi
        else
            echo -e "${YELLOW}Port $port (${STACKS[$port]}) is not running - skipping.${NC}"
            ((skipped++))
        fi
    done

    echo -e "\n${BLUE}================ Summary ================${NC}"
    echo -e "Passed:  ${GREEN}${passed}${NC}"
    echo -e "Failed:  ${RED}${failed}${NC}"
    echo -e "Skipped: ${YELLOW}${skipped}${NC}"
fi
