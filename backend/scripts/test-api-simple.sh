#!/bin/bash

# GuitarGPT API Testing Script (Simplified)
# Usage: ./scripts/test-api-simple.sh

API_URL="http://localhost:8080/api/v1"

echo "🎸 GuitarGPT API Test"
echo "===================="
echo ""

# 1. Create User
echo "1️⃣  Creating user..."
USER_RESPONSE=$(curl -s -X POST "$API_URL/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com"}')

USER_ID=$(echo "$USER_RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4 | head -1)
echo "   User ID: $USER_ID"
echo ""

# 2. Create Project
echo "2️⃣  Creating project..."
PROJECT_RESPONSE=$(curl -s -X POST "$API_URL/users/$USER_ID/projects" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Project","description":"Test"}')

PROJECT_ID=$(echo "$PROJECT_RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4 | head -1)
echo "   Project ID: $PROJECT_ID"
echo ""

# 3. Create Track
echo "3️⃣  Creating track..."
TRACK_RESPONSE=$(curl -s -X POST "$API_URL/projects/$PROJECT_ID/tracks" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Track","type":"GUITAR","description":"Test"}')

TRACK_ID=$(echo "$TRACK_RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4 | head -1)
echo "   Track ID: $TRACK_ID"
echo ""

# 4. Create Prompt Template
echo "4️⃣  Creating prompt template..."
TEMPLATE_RESPONSE=$(curl -s -X POST "$API_URL/prompt-templates" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Template","templateText":"Test","category":"SOLO"}')

TEMPLATE_ID=$(echo "$TEMPLATE_RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4 | head -1)
echo "   Template ID: $TEMPLATE_ID"
echo ""

# 5. Create Generation Request
echo "5️⃣  Creating generation request..."
GEN_RESPONSE=$(curl -s -X POST "$API_URL/projects/$PROJECT_ID/generation-requests" \
  -H "Content-Type: application/json" \
  -d "{\"promptTemplateId\":\"$TEMPLATE_ID\",\"userPrompt\":\"Generate a test solo\"}")

GEN_ID=$(echo "$GEN_RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4 | head -1)
echo "   Request ID: $GEN_ID"
echo ""

# 6. Get all users
echo "6️⃣  Fetching all users..."
curl -s -X GET "$API_URL/users" | grep -o '"id":"[^"]*' | wc -l
echo "   ✅ Users fetched"
echo ""

# 7. Get all projects
echo "7️⃣  Fetching projects for user..."
curl -s -X GET "$API_URL/users/$USER_ID/projects" | grep -o '"id":"[^"]*' | wc -l
echo "   ✅ Projects fetched"
echo ""

# 8. Get all templates
echo "8️⃣  Fetching all templates..."
curl -s -X GET "$API_URL/prompt-templates" | grep -o '"id":"[^"]*' | wc -l
echo "   ✅ Templates fetched"
echo ""

# 9. Get all generation requests
echo "9️⃣  Fetching generation requests..."
curl -s -X GET "$API_URL/projects/$PROJECT_ID/generation-requests" | grep -o '"id":"[^"]*' | wc -l
echo "   ✅ Generation requests fetched"
echo ""

echo "✅ All tests completed!"
echo ""
echo "Summary:"
echo "  User ID: $USER_ID"
echo "  Project ID: $PROJECT_ID"
echo "  Track ID: $TRACK_ID"
echo "  Template ID: $TEMPLATE_ID"
echo "  Generation Request ID: $GEN_ID"
