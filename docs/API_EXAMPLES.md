# GuitarGPT API Examples

## Base URL
- **Development**: `http://localhost:8080/api/v1`
- **Docker**: `http://localhost:8080/api/v1`

## 1. User Management

### Create User
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@example.com"
  }'
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "João Silva",
  "email": "joao@example.com",
  "createdAt": "2026-03-22T20:45:00Z"
}
```

### Get All Users
```bash
curl -X GET http://localhost:8080/api/v1/users
```

### Get User by ID
```bash
curl -X GET http://localhost:8080/api/v1/users/{userId}
```

### Update User
```bash
curl -X PUT http://localhost:8080/api/v1/users/{userId} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva Updated",
    "email": "joao.updated@example.com"
  }'
```

### Delete User
```bash
curl -X DELETE http://localhost:8080/api/v1/users/{userId}
```

---

## 2. Musical Projects

### Create Musical Project
```bash
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My First Songwriting",
    "description": "Composição original em estilo rock progressivo"
  }'
```

**JSON Example:**
```json
{
  "name": "Smooth Jazz Fusion",
  "description": "Exploring jazz scales and fusion techniques"
}
```

**Response (201 Created):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440000",
  "name": "Smooth Jazz Fusion",
  "description": "Exploring jazz scales and fusion techniques",
  "createdAt": "2026-03-22T20:45:00Z"
}
```

### Get All Projects
```bash
curl -X GET http://localhost:8080/api/v1/projects
```

### Get Project by ID
```bash
curl -X GET http://localhost:8080/api/v1/projects/{projectId}
```

### Update Project
```bash
curl -X PUT http://localhost:8080/api/v1/projects/{projectId} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Smooth Jazz Fusion (Updated)",
    "description": "Exploring jazz scales and fusion techniques - v2"
  }'
```

### Delete Project
```bash
curl -X DELETE http://localhost:8080/api/v1/projects/{projectId}
```

---

## 3. Tracks

### Create Track (Nested under Project)
```bash
curl -X POST http://localhost:8080/api/v1/projects/{projectId}/tracks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lead Guitar Solo",
    "type": "GUITAR",
    "description": "Pentatonic scale solo over blues progression"
  }'
```

**JSON Examples by Type:**
```json
{
  "name": "Lead Guitar Solo",
  "type": "GUITAR",
  "description": "Pentatonic scale solo over blues progression"
}
```

```json
{
  "name": "Bass Line",
  "type": "BASS",
  "description": "Walking bass line in G major"
}
```

```json
{
  "name": "Drum Pattern",
  "type": "DRUMS",
  "description": "4/4 rock beat at 120 BPM"
}
```

```json
{
  "name": "Backing Track",
  "type": "BACKING_TRACK",
  "description": "Chords: Gm - Bb - D7 progression"
}
```

```json
{
  "name": "Vocal Line",
  "type": "VOCAL",
  "description": "Melody in A minor pentatonic"
}
```

### Get All Tracks for Project
```bash
curl -X GET http://localhost:8080/api/v1/projects/{projectId}/tracks
```

### Get Track by ID
```bash
curl -X GET http://localhost:8080/api/v1/projects/{projectId}/tracks/{trackId}
```

### Update Track
```bash
curl -X PUT http://localhost:8080/api/v1/projects/{projectId}/tracks/{trackId} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lead Guitar Solo (Updated)",
    "type": "GUITAR",
    "description": "Updated pentatonic scale solo"
  }'
```

### Delete Track
```bash
curl -X DELETE http://localhost:8080/api/v1/projects/{projectId}/tracks/{trackId}
```

---

## 4. Prompt Templates

### Create Prompt Template
```bash
curl -X POST http://localhost:8080/api/v1/prompt-templates \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Blues Solo Generator",
    "templateText": "Generate a blues guitar solo using {technique} in the key of {key}",
    "category": "SOLO",
    "description": "Template for generating blues solos with AI"
  }'
```

**JSON Examples by Category:**
```json
{
  "name": "Blues Solo Generator",
  "templateText": "Generate a blues guitar solo using {technique} in the key of {key}",
  "category": "SOLO",
  "description": "Template for generating blues solos with AI"
}
```

```json
{
  "name": "Song Composer",
  "templateText": "Compose a {genre} song with the following chords: {chords}",
  "category": "COMPOSITION",
  "description": "Full composition from chord progression"
}
```

```json
{
  "name": "Practice Routine",
  "templateText": "Create a {duration} minute practice routine focusing on {skill}",
  "category": "PRACTICE",
  "description": "Generate personalized practice routines"
}
```

```json
{
  "name": "Riff Creator",
  "templateText": "Generate a {style} riff in {key} with emphasis on {notes}",
  "category": "RIFF",
  "description": "Create memorable guitar riffs"
}
```

```json
{
  "name": "Song Arranger",
  "templateText": "Arrange this song: {song_title} with instrumentation: {instruments}",
  "category": "ARRANGEMENT",
  "description": "Arrange existing songs for different instruments"
}
```

### Get All Prompt Templates
```bash
curl -X GET http://localhost:8080/api/v1/prompt-templates
```

### Get Prompt Template by ID
```bash
curl -X GET http://localhost:8080/api/v1/prompt-templates/{templateId}
```

### Update Prompt Template
```bash
curl -X PUT http://localhost:8080/api/v1/prompt-templates/{templateId} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Blues Solo Generator (v2)",
    "templateText": "Generate a blues guitar solo using {technique} in the key of {key} with {bends} bends",
    "category": "SOLO",
    "description": "Updated template for generating blues solos"
  }'
```

### Delete Prompt Template
```bash
curl -X DELETE http://localhost:8080/api/v1/prompt-templates/{templateId}
```

---

## 5. Generation Requests (AI Generation)

### Create Generation Request
```bash
curl -X POST http://localhost:8080/api/v1/generation-requests \
  -H "Content-Type: application/json" \
  -d '{
    "promptTemplateId": "770e8400-e29b-41d4-a716-446655440000",
    "userPrompt": "Generate a pentatonic blues solo in E minor at 120 BPM"
  }'
```

**JSON Examples:**
```json
{
  "promptTemplateId": "770e8400-e29b-41d4-a716-446655440000",
  "userPrompt": "Generate a pentatonic blues solo in E minor at 120 BPM"
}
```

```json
{
  "promptTemplateId": "880e8400-e29b-41d4-a716-446655440000",
  "userPrompt": "Compose a 32-bar jazz standard in Bb major with walking bass"
}
```

```json
{
  "promptTemplateId": "990e8400-e29b-41d4-a716-446655440000",
  "userPrompt": "Create a 15-minute practice routine for improving sweep picking"
}
```

**Response (202 Accepted):**
```json
{
  "id": "aa0e8400-e29b-41d4-a716-446655440000",
  "promptTemplateId": "770e8400-e29b-41d4-a716-446655440000",
  "userPrompt": "Generate a pentatonic blues solo in E minor at 120 BPM",
  "status": "PENDING",
  "result": null,
  "createdAt": "2026-03-22T20:45:00Z"
}
```

### Get All Generation Requests
```bash
curl -X GET http://localhost:8080/api/v1/generation-requests
```

### Get Generation Request by ID
```bash
curl -X GET http://localhost:8080/api/v1/generation-requests/{requestId}
```

**Response (after processing):**
```json
{
  "id": "aa0e8400-e29b-41d4-a716-446655440000",
  "promptTemplateId": "770e8400-e29b-41d4-a716-446655440000",
  "userPrompt": "Generate a pentatonic blues solo in E minor at 120 BPM",
  "status": "COMPLETED",
  "result": "e|---12-15-12---15-17-15-12-----12-15-12---15-17-15-12---...",
  "createdAt": "2026-03-22T20:45:00Z",
  "completedAt": "2026-03-22T20:45:15Z"
}
```

### Update Generation Request
```bash
curl -X PUT http://localhost:8080/api/v1/generation-requests/{requestId} \
  -H "Content-Type: application/json" \
  -d '{
    "promptTemplateId": "770e8400-e29b-41d4-a716-446655440000",
    "userPrompt": "Generate a faster pentatonic blues solo in E minor at 150 BPM"
  }'
```

### Delete Generation Request
```bash
curl -X DELETE http://localhost:8080/api/v1/generation-requests/{requestId}
```

---

## Testing Workflow Example

### 1. Create a User
```bash
USER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Guitar","email":"alice@example.com"}')

USER_ID=$(echo $USER_RESPONSE | jq -r '.id')
echo "Created user: $USER_ID"
```

### 2. Create a Musical Project
```bash
PROJECT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"Rock Opera","description":"A full rock concept album"}')

PROJECT_ID=$(echo $PROJECT_RESPONSE | jq -r '.id')
echo "Created project: $PROJECT_ID"
```

### 3. Create a Track
```bash
TRACK_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/projects/$PROJECT_ID/tracks \
  -H "Content-Type: application/json" \
  -d '{"name":"Main Solo","type":"GUITAR","description":"Epic guitar solo"}')

TRACK_ID=$(echo $TRACK_RESPONSE | jq -r '.id')
echo "Created track: $TRACK_ID"
```

### 4. Create a Prompt Template
```bash
TEMPLATE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/prompt-templates \
  -H "Content-Type: application/json" \
  -d '{"name":"Rock Solo","templateText":"Generate a {style} rock solo","category":"SOLO"}')

TEMPLATE_ID=$(echo $TEMPLATE_RESPONSE | jq -r '.id')
echo "Created template: $TEMPLATE_ID"
```

### 5. Create a Generation Request
```bash
curl -s -X POST http://localhost:8080/api/v1/generation-requests \
  -H "Content-Type: application/json" \
  -d "{\"promptTemplateId\":\"$TEMPLATE_ID\",\"userPrompt\":\"Create an epic rock solo\"}"
```

---

## Error Examples

### Missing Required Field
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice"}'
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-03-22T20:45:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "email": "Email is required"
  }
}
```

### Resource Not Found
```bash
curl -X GET http://localhost:8080/api/v1/users/00000000-0000-0000-0000-000000000000
```

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-03-22T20:45:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found"
}
```

---

## Tips for Testing

1. **Use jq to parse JSON:**
   ```bash
   curl -s http://localhost:8080/api/v1/users | jq '.[0].id'
   ```

2. **Save IDs for chained requests:**
   ```bash
   PROJECT_ID=$(curl -s ... | jq -r '.id')
   ```

3. **Pretty print JSON:**
   ```bash
   curl http://localhost:8080/api/v1/users | jq '.'
   ```

4. **View response headers:**
   ```bash
   curl -i http://localhost:8080/api/v1/users
   ```

5. **Test with verbose output:**
   ```bash
   curl -v http://localhost:8080/api/v1/users
   ```
