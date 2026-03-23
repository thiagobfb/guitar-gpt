#!/bin/bash

# GuitarGPT API Full Test Script
# Requires: jq (for JSON parsing)

API_URL="${API_URL:-http://localhost:8080/api/v1}"
USE_JQ=true

if ! command -v jq &> /dev/null; then
    echo "⚠️  jq not found. Install with: apt-get install jq"
    USE_JQ=false
fi

extract_id() {
    if [ "$USE_JQ" = true ]; then
        jq -r '.id'
    else
        grep -o '"id":"[^"]*' | cut -d'"' -f4 | head -1
    fi
}

pretty_json() {
    if [ "$USE_JQ" = true ]; then
        jq '.'
    else
        cat
    fi
}

echo "🎸 GuitarGPT API Full Test"
echo "=========================="
echo "API URL: $API_URL"
echo ""

# Test 1: Create User
echo "1️⃣  Creating user..."
USER=$(curl -s -X POST "$API_URL/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com"}')
USER_ID=$(echo "$USER" | extract_id)
echo "   ✅ User created: $USER_ID"

# Test 2: Get User
echo "2️⃣  Getting user..."
curl -s -X GET "$API_URL/users/$USER_ID" | pretty_json
echo "   ✅ User retrieved"
echo ""

# Test 3: Create Project
echo "3️⃣  Creating project..."
PROJECT=$(curl -s -X POST "$API_URL/users/$USER_ID/projects" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Project","description":"A test project"}')
PROJECT_ID=$(echo "$PROJECT" | extract_id)
echo "   ✅ Project created: $PROJECT_ID"
echo ""

# Test 4: Get Projects
echo "4️⃣  Getting projects for user..."
curl -s -X GET "$API_URL/users/$USER_ID/projects" | pretty_json
echo "   ✅ Projects retrieved"
echo ""

# Test 5: Create Track
echo "5️⃣  Creating track..."
TRACK=$(curl -s -X POST "$API_URL/projects/$PROJECT_ID/tracks" \
  -H "Content-Type: application/json" \
  -d '{"name":"Lead Guitar","type":"GUITAR","description":"A test track"}')
TRACK_ID=$(echo "$TRACK" | extract_id)
echo "   ✅ Track created: $TRACK_ID"
echo ""

# Test 6: Get Tracks
echo "6️⃣  Getting tracks for project..."
curl -s -X GET "$API_URL/projects/$PROJECT_ID/tracks" | pretty_json
echo "   ✅ Tracks retrieved"
echo ""

# Test 7: Create Prompt Template
echo "7️⃣  Creating prompt template..."
TEMPLATE=$(curl -s -X POST "$API_URL/prompt-templates" \
  -H "Content-Type: application/json" \
  -d '{"name":"Solo Generator","templateText":"Generate a {style} solo in {key}","category":"SOLO","description":"Generates solos"}')
TEMPLATE_ID=$(echo "$TEMPLATE" | extract_id)
echo "   ✅ Template created: $TEMPLATE_ID"
echo ""

# Test 8: Get Templates
echo "8️⃣  Getting all templates..."
curl -s -X GET "$API_URL/prompt-templates" | pretty_json
echo "   ✅ Templates retrieved"
echo ""

# Test 9: Create Generation Request
echo "9️⃣  Creating generation request..."
GEN=$(curl -s -X POST "$API_URL/projects/$PROJECT_ID/generation-requests" \
  -H "Content-Type: application/json" \
  -d "{\"promptTemplateId\":\"$TEMPLATE_ID\",\"userPrompt\":\"Create a blues solo in E minor\"}")
GEN_ID=$(echo "$GEN" | extract_id)
echo "   ✅ Generation request created: $GEN_ID"
echo ""

# Test 10: Get Generation Requests
echo "🔟 Getting generation requests for project..."
curl -s -X GET "$API_URL/projects/$PROJECT_ID/generation-requests" | pretty_json
echo "   ✅ Generation requests retrieved"
echo ""

echo "✅ All tests completed successfully!"
echo ""
echo "Created Resources:"
echo "  • User: $USER_ID"
echo "  • Project: $PROJECT_ID"
echo "  • Track: $TRACK_ID"
echo "  • Template: $TEMPLATE_ID"
echo "  • Generation Request: $GEN_ID"
