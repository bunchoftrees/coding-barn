# Chapter 2: The Harvest Party - OAuth, Resources, and Access Control

Code for Chapter 2 of "From Barns to Bytes: A Farm Guide to Modern System Architecture."

## The Story

Remember that party where my equipment got "relocated" to the barn and came back broken? This chapter demonstrates why sharing master keys is a bad idea, and how OAuth provides scoped, time-limited access instead.

## The Services

### auth-server (Port 8081)
The OAuth authorization server. Issues JWT tokens with specific scopes.

**Pre-registered clients:**
- `harvest-service`: Can only read "now playing" info (`read:nowplaying`)
- `party-guest-app`: Can read and control music (`read:nowplaying`, `write:music`)
- `admin-app`: Can do everything including delete equipment (all scopes)

### shed-service (Port 8080)
The protected resource server with music equipment. All endpoints require valid Bearer tokens with appropriate scopes.

**Endpoints:**
- `GET /music/nowplaying` - Requires `read:nowplaying`
- `GET /music/playlist` - Requires `read:nowplaying`
- `POST /music/play` - Requires `write:music`
- `POST /music/next` - Requires `write:music`
- `GET /music/equipment` - Requires `admin:equipment`
- `DELETE /music/equipment` - Requires `admin:equipment` (⚠️ deletes everything!)

### harvest-service (Port 8082)
Public-facing service that uses OAuth internally. Guests can access these endpoints without authentication, but the service authenticates with shed-service behind the scenes.

**Endpoints:**
- `GET /harvest/food` - Public, no auth needed
- `GET /harvest/nowplaying` - Public, but fetches data from shed-service using OAuth

This demonstrates the key insight: **public endpoints can aggregate protected data using service credentials**.

### party-guest-app (Port 8083)
Demo app that shows how to request and use tokens. Has read and write scopes but cannot delete equipment.

**Endpoints:**
- `POST /guest/token` - Request a token with specific scopes
- `GET /guest/nowplaying` - View now playing (requires token)
- `POST /guest/play/{songId}` - Change the song (requires token)
- `DELETE /guest/equipment` - Try to delete equipment (should fail!)

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Start all services
docker compose up --build

# Wait for services to be healthy, then run experiments
```

### Running Individually

```bash
# Terminal 1 - Auth Server
cd auth-server
./mvnw spring-boot:run

# Terminal 2 - Shed Service
cd shed-service
./mvnw spring-boot:run

# Terminal 3 - Harvest Service
cd harvest-service
./mvnw spring-boot:run

# Terminal 4 - Party Guest App
cd party-guest-app
./mvnw spring-boot:run
```

## Experiments

### Experiment 1: The Public Party

Access harvest-service endpoints without any authentication:

```bash
# Get the food menu - no auth needed
curl http://localhost:8082/harvest/food

# Get what's playing - no auth needed
# But behind the scenes, harvest-service authenticates with shed-service
curl http://localhost:8082/harvest/nowplaying
```

**What this demonstrates:** Public endpoints can aggregate protected data using service credentials. Guests hear the music (metaphorically) without needing keys to the shed.

### Experiment 2: Request a Token

Get a token as the party-guest-app:

```bash
# Request token with read and write scopes
curl -X POST http://localhost:8083/guest/token \
  -H "Content-Type: application/json" \
  -d '{
    "scopes": ["read:nowplaying", "write:music"]
  }'
```

Save the `token` from the response for the next experiments.

### Experiment 3: Use the Token

Use your token to access shed-service:

```bash
# Set your token (replace with actual token from previous step)
TOKEN="your-token-here"

# View now playing
curl http://localhost:8080/music/nowplaying \
  -H "Authorization: Bearer $TOKEN"

# Change the song (song IDs: 1-5)
curl -X POST http://localhost:8080/music/play \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"songId": "3"}'

# Skip to next song
curl -X POST http://localhost:8080/music/next \
  -H "Authorization: Bearer $TOKEN"
```

### Experiment 4: The Scope Violation

Try to request a token with scopes you're not authorized for:

```bash
# Try to get admin:equipment scope (party-guest-app isn't allowed)
curl -X POST http://localhost:8081/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "party-guest-app",
    "clientSecret": "party-secret-key",
    "scopes": ["read:nowplaying", "admin:equipment"]
  }'
```

**Expected result:** `403 Forbidden - Client not authorized for scopes: [admin:equipment]`

**What this demonstrates:** The authorization server enforces what scopes each client can request. party-guest-app physically cannot get admin access.

### Experiment 5: Try to Delete Equipment

Even with a valid token, you can't delete equipment without the right scope:

```bash
# Try to delete equipment (should fail)
curl -X DELETE http://localhost:8083/guest/equipment

# Or try directly with your token
curl -X DELETE http://localhost:8080/music/equipment \
  -H "Authorization: Bearer $TOKEN"
```

**Expected result:** `403 Forbidden - Token missing required scope: admin:equipment`

**What this demonstrates:** Even if you have a valid token, scope enforcement prevents unauthorized actions.

### Experiment 6: Admin Access

Get a token as admin-app (which has all scopes):

```bash
# Request admin token
curl -X POST http://localhost:8081/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "admin-app",
    "clientSecret": "admin-secret-key",
    "scopes": ["admin:equipment"]
  }'

# Save the token
ADMIN_TOKEN="admin-token-here"

# View equipment
curl http://localhost:8080/music/equipment \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Delete all equipment (⚠️ this actually works!)
curl -X DELETE http://localhost:8080/music/equipment \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**What this demonstrates:** With the right scope, destructive operations are allowed. This is why we don't give `admin:equipment` to harvest-service.

### Experiment 7: harvest-service Can't Delete Equipment

Let's prove that harvest-service can't delete equipment even though it has valid credentials:

```bash
# Try to get a token with admin scope as harvest-service
curl -X POST http://localhost:8081/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "harvest-service",
    "clientSecret": "harvest-secret-key",
    "scopes": ["admin:equipment"]
  }'
```

**Expected result:** `403 Forbidden`

**What this demonstrates:** harvest-service is pre-registered with only `read:nowplaying` scope. Even though it has valid credentials, it cannot escalate its privileges.

### Experiment 8: Token Expiration

Tokens expire after 1 hour. To see this quickly, you can:

1. Request a token
2. Wait 1 hour (or modify the code to use shorter expiry for testing)
3. Try to use the token

```bash
# Use an expired token
curl http://localhost:8080/music/nowplaying \
  -H "Authorization: Bearer $EXPIRED_TOKEN"
```

**Expected result:** `401 Unauthorized - Token has expired`

### Experiment 9: Invalid Token

Try using a completely made-up token:

```bash
curl http://localhost:8080/music/nowplaying \
  -H "Authorization: Bearer invalid-fake-token"
```

**Expected result:** `401 Unauthorized - Invalid token`

### Experiment 10: Missing Authorization Header

Try accessing a protected endpoint without any token:

```bash
curl http://localhost:8080/music/nowplaying
```

**Expected result:** `401 Unauthorized - Missing or invalid Authorization header`

## The Key Insights

### 1. Scoped Permissions

Each client gets exactly the access it needs:
- harvest-service: read-only
- party-guest-app: read and write
- admin-app: everything

Compare this to a shared password where everyone has full access.

### 2. Service-to-Service Authentication

harvest-service is both:
- A public API (guests don't authenticate)
- An OAuth client (authenticates with shed-service)

This is the BFF pattern in action.

### 3. Scope Enforcement

The system has two layers of protection:
1. Authorization server won't issue tokens with unauthorized scopes
2. Resource server validates scopes on every request

### 4. Audit Trail

Check the logs:

```bash
# shed-service logs show WHO did WHAT
docker compose logs shed-service | grep "Client"
```

You'll see lines like:
```
Client harvest-service accessed now playing: Harvest Moon
Client party-guest-app changed song to: Autumn Leaves
```

With a shared password, all requests look identical.

## Common Patterns

### Requesting Tokens (Client Credentials Grant)

```bash
curl -X POST http://localhost:8081/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "your-client-id",
    "clientSecret": "your-secret",
    "scopes": ["scope1", "scope2"]
  }'
```

### Using Tokens

```bash
curl http://localhost:8080/resource \
  -H "Authorization: Bearer $TOKEN"
```

### Token Response Format

```json
{
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "scopes": ["read:nowplaying", "write:music"]
}
```

## Architecture

```
┌─────────────────┐
│  Party Guests   │ (no auth needed)
└────────┬────────┘
         │
         │ GET /harvest/food
         │ GET /harvest/nowplaying
         ↓
┌─────────────────┐
│ harvest-service │ (public endpoints)
│   Port 8082     │
│                 │ But internally:
│                 │ 1. Requests token from auth-server
│                 │ 2. Uses token to call shed-service
│                 │ 3. Returns data to guest
└────────┬────────┘
         │
         │ (authenticated with token)
         ↓
┌─────────────────┐        ┌─────────────────┐
│  shed-service   │◄───────│  auth-server    │
│   Port 8080     │ issues │   Port 8081     │
│                 │ tokens │                 │
│ Validates token │        │ Manages clients │
│ Checks scope    │        │ Issues JWT      │
│ Returns data    │        └─────────────────┘
└─────────────────┘
         ▲
         │ (authenticated with token)
         │
┌────────┴────────┐
│ party-guest-app │
│   Port 8083     │
│                 │
│ Gets tokens     │
│ Makes requests  │
└─────────────────┘
```

## Files Structure

```
chapter-02/
├── README.md (this file)
├── docker-compose.yml
├── auth-server/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/codingbarn/auth/
│       ├── AuthServerApplication.java
│       ├── AuthorizationController.java
│       ├── ClientRegistry.java
│       └── TokenService.java
├── shed-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/codingbarn/shed/
│       ├── ShedServiceApplication.java
│       ├── MusicController.java
│       ├── MusicService.java
│       ├── EquipmentService.java
│       └── TokenValidator.java
├── harvest-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/codingbarn/harvest/
│       ├── HarvestServiceApplication.java
│       ├── HarvestController.java
│       ├── MusicClient.java
│       └── FoodService.java
└── party-guest-app/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/codingbarn/guest/
        ├── PartyGuestAppApplication.java
        └── GuestController.java
```

## Production Considerations

**⚠️ This is teaching code, not production code.**

In production:
- Use a battle-tested OAuth provider (Auth0, Okta, Keycloak, AWS Cognito)
- Store secrets in a secure secret management system
- Use asymmetric keys (RS256) instead of symmetric (HS256)
- Implement token refresh flows
- Add rate limiting
- Use HTTPS everywhere
- Implement proper logging and monitoring
- Store client secrets hashed (bcrypt)
- Add token revocation lists
- Implement proper error handling

## The Lesson

OAuth isn't about trust—it's about appropriate boundaries.

I trust harvest-service to display "now playing" info. I don't need to trust it to delete my equipment, because the system physically prevents it.

When you build your next API, ask:
- Who needs access?
- What do they need to do?
- How long should that access last?
- What happens if they're compromised?

If your answer is "everyone gets the master password," you're dropping your keys in the grass.

Build better sheds. Use OAuth.
