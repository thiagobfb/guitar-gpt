#!/bin/bash

# GuitarGPT API Testing Script
# Usage: ./scripts/test-api.sh

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

API_URL="${API_URL:-http://localhost:8080/api/v1}"
VERBOSE="${VERBOSE:-false}"

# Helper functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

pretty_json() {
    if command -v jq &> /dev/null; then
        jq '.'
    else
        cat
    fi
}

extract_id() {
    if command -v jq &> /dev/null; then
        jq -r '.id'
    else
        grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4
    fi
}

# Check if server is running
check_server() {
    log_info "Checking if API is running at $API_URL..."
    if curl -s "$API_URL/users" > /dev/null 2>&1; then
        log_success "API is running!"
    else
        log_warning "API is not reachable. Start with: docker-compose up -d"
        exit 1
    fi
}

# Test User endpoints
test_users() {
    echo ""
    log_info "=== Testing User Endpoints ==="

    # Create user
    log_info "Creating user..."
    USER_RESPONSE=$(curl -s -X POST "$API_URL/users" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Test User",
            "email": "test@example.com"
        }')

    if [ "$VERBOSE" = "true" ]; then
        echo "$USER_RESPONSE" | pretty_json
    fi

    USER_ID=$(echo "$USER_RESPONSE" | extract_id)
    log_success "Created user: $USER_ID"

    # Get all users
    log_info "Fetching all users..."
    curl -s -X GET "$API_URL/users" | pretty_json
    log_success "Users fetched"

    # Get user by ID
    log_info "Fetching user: $USER_ID"
    curl -s -X GET "$API_URL/users/$USER_ID" | pretty_json
    log_success "User fetched"

    echo "$USER_ID"
}

# Test Project endpoints
test_projects() {
    echo ""
    log_info "=== Testing Project Endpoints ==="

    # Create project
    log_info "Creating project..."
    PROJECT_RESPONSE=$(curl -s -X POST "$API_URL/projects" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Test Project",
            "description": "A project for testing"
        }')

    if [ "$VERBOSE" = "true" ]; then
        echo "$PROJECT_RESPONSE" | pretty_json
    fi

    PROJECT_ID=$(echo "$PROJECT_RESPONSE" | extract_id)
    log_success "Created project: $PROJECT_ID"

    # Get all projects
    log_info "Fetching all projects..."
    curl -s -X GET "$API_URL/projects" | pretty_json
    log_success "Projects fetched"

    echo "$PROJECT_ID"
}

# Test Track endpoints
test_tracks() {
    PROJECT_ID=$1

    echo ""
    log_info "=== Testing Track Endpoints ==="

    # Create track
    log_info "Creating track..."
    TRACK_RESPONSE=$(curl -s -X POST "$API_URL/projects/$PROJECT_ID/tracks" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Test Track",
            "type": "GUITAR",
            "description": "A test guitar track"
        }')

    if [ "$VERBOSE" = "true" ]; then
        echo "$TRACK_RESPONSE" | pretty_json
    fi

    TRACK_ID=$(echo "$TRACK_RESPONSE" | extract_id)
    log_success "Created track: $TRACK_ID"

    # Get all tracks
    log_info "Fetching all tracks for project..."
    curl -s -X GET "$API_URL/projects/$PROJECT_ID/tracks" | pretty_json
    log_success "Tracks fetched"

    echo "$TRACK_ID"
}

# Test Prompt Template endpoints
test_prompt_templates() {
    echo ""
    log_info "=== Testing Prompt Template Endpoints ==="

    # Create template
    log_info "Creating prompt template..."
    TEMPLATE_RESPONSE=$(curl -s -X POST "$API_URL/prompt-templates" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Test Template",
            "templateText": "Generate a {style} solo in {key}",
            "category": "SOLO",
            "description": "Test template"
        }')

    if [ "$VERBOSE" = "true" ]; then
        echo "$TEMPLATE_RESPONSE" | pretty_json
    fi

    TEMPLATE_ID=$(echo "$TEMPLATE_RESPONSE" | extract_id)
    log_success "Created template: $TEMPLATE_ID"

    # Get all templates
    log_info "Fetching all templates..."
    curl -s -X GET "$API_URL/prompt-templates" | pretty_json
    log_success "Templates fetched"

    echo "$TEMPLATE_ID"
}

# Test Generation Request endpoints
test_generation_requests() {
    TEMPLATE_ID=$1

    echo ""
    log_info "=== Testing Generation Request Endpoints ==="

    if [ -z "$TEMPLATE_ID" ]; then
        log_warning "Skipping generation tests (no template ID)"
        return
    fi

    # Create generation request
    log_info "Creating generation request..."
    GEN_RESPONSE=$(curl -s -X POST "$API_URL/generation-requests" \
        -H "Content-Type: application/json" \
        -d "{
            \"promptTemplateId\": \"$TEMPLATE_ID\",
            \"userPrompt\": \"Generate a test solo\"
        }")

    if [ "$VERBOSE" = "true" ]; then
        echo "$GEN_RESPONSE" | pretty_json
    fi

    GEN_ID=$(echo "$GEN_RESPONSE" | extract_id)
    log_success "Created generation request: $GEN_ID"

    # Get all requests
    log_info "Fetching all generation requests..."
    curl -s -X GET "$API_URL/generation-requests" | pretty_json
    log_success "Generation requests fetched"
}

# Main test flow
main() {
    log_info "Starting API tests..."
    log_info "API URL: $API_URL"
    echo ""

    check_server

    USER_ID=$(test_users)
    PROJECT_ID=$(test_projects)
    TRACK_ID=$(test_tracks "$PROJECT_ID")
    TEMPLATE_ID=$(test_prompt_templates)
    test_generation_requests "$TEMPLATE_ID"

    echo ""
    log_success "All tests completed!"
    echo ""
    log_info "Summary:"
    log_info "  User ID: $USER_ID"
    log_info "  Project ID: $PROJECT_ID"
    log_info "  Track ID: $TRACK_ID"
    log_info "  Template ID: $TEMPLATE_ID"
}

# Run if not sourced
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
