# Chapter 01: The Barn That Burned Down

**Polling vs. Event-Driven Architecture**

This chapter accompanies the blog post at [your-substack-url]. Here you'll find working code to experiment with both polling and event-driven architectures.

## The Story

A historical barn burned down three doors from the fire station because there was no way for the barn to tell anyone it was on fire. This is what happens when systems can't communicate in real-time.

## What's Inside

```
chapter-01/
├── barn-service/           # Polling version - a barn that waits to be asked
├── firehouse-polling/      # A firehouse that checks periodically
├── barn-service-events/    # Event version - a barn that screams for help
├── firehouse-subscriber/   # A firehouse that listens for events
├── docker-compose.yml      # Run everything with Docker
└── README.md              # You are here
```

## Prerequisites

- Java 21+
- Maven 3.6+
- (Optional) Docker and Docker Compose

## Quick Start

### Option 1: Run Locally with Maven

**Terminal 1 - Start the Barn (Polling Version)**
```bash
cd barn-service
./mvnw spring-boot:run
```

**Terminal 2 - Start the Firehouse (Polling)**
```bash
cd firehouse-polling
./mvnw spring-boot:run
```

**Terminal 3 - Start a Fire**
```bash
curl -X POST http://localhost:8080/barn/ignite
```

Watch Terminal 2. Depending on timing, you'll wait up to 10 seconds before seeing:
```
🔥 FIRE DETECTED! Response time: 7 seconds
```

### Option 2: Run with Docker Compose

```bash
docker compose up --build
```

## Experiments

### Experiment 1: Adjust Polling Interval

See how response time changes with different polling frequencies:

```bash
# Fast polling (2 seconds)
cd firehouse-polling
./mvnw spring-boot:run -Dspring-boot.run.arguments="--polling.interval.ms=2000"

# Slow polling (30 seconds)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--polling.interval.ms=30000"
```

Start fires at random times and record the response times.

### Experiment 2: Break the Fragile System

The barn service includes a rate-limited endpoint that simulates a legacy system:

```bash
# Start the firehouse pointing at the fragile endpoint with aggressive polling
cd firehouse-polling
./mvnw spring-boot:run -Dspring-boot.run.arguments="--polling.interval.ms=2000 --barn.endpoint=/fragile-barn/status"
```

Watch as the system starts returning `503 Service Unavailable` errors. Your aggressive polling is breaking the thing you're trying to monitor.

### Experiment 3: Event-Driven Response Time

Now try the event-driven version:

**Terminal 1 - Event-Emitting Barn**
```bash
cd barn-service-events
./mvnw spring-boot:run
```

**Terminal 2 - Subscribing Firehouse**
```bash
cd firehouse-subscriber
./mvnw spring-boot:run
```

**Terminal 3 - Start a Fire**
```bash
curl -X POST http://localhost:8080/barn/ignite
```

Watch Terminal 2. Response time will be measured in **milliseconds**, not seconds:
```
🔥 FIRE EVENT RECEIVED!
   Response time: 4 milliseconds
```

### Experiment 4: Multiple Subscribers

The event-driven barn supports multiple subscribers. Start additional firehouses on different ports:

```bash
# Second firehouse
cd firehouse-subscriber
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"

# Third firehouse
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

Start a fire and watch all subscribers receive the event simultaneously.

## API Reference

### Barn Service (Polling)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/barn/status` | GET | Get current barn status |
| `/barn/ignite` | POST | Start a fire |
| `/barn/extinguish` | POST | Put out the fire |
| `/fragile-barn/status` | GET | Rate-limited status (max 6/min) |
| `/fragile-barn/ignite` | POST | Start fire in fragile barn |
| `/fragile-barn/extinguish` | POST | Extinguish fragile barn |

### Barn Service (Events)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/barn/status` | GET | Get current barn status |
| `/barn/ignite` | POST | Start fire and notify subscribers |
| `/barn/extinguish` | POST | Extinguish and notify subscribers |
| `/barn/subscribe?callbackUrl=URL` | POST | Register for events |
| `/barn/subscribe?callbackUrl=URL` | DELETE | Unregister |
| `/barn/subscribers` | GET | List all subscribers |

### Firehouse Subscriber

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/events` | POST | Receive barn events (webhook) |
| `/events/log` | GET | View received events |
| `/events/log` | DELETE | Clear event log |
| `/events/stats` | GET | Response time statistics |

## Configuration

### Firehouse Polling

| Property | Default | Description |
|----------|---------|-------------|
| `polling.interval.ms` | 10000 | Milliseconds between polls |
| `barn.service.url` | http://localhost:8080 | Barn service URL |
| `barn.endpoint` | /barn/status | Status endpoint path |

### Firehouse Subscriber

| Property | Default | Description |
|----------|---------|-------------|
| `barn.service.url` | http://localhost:8080 | Barn service URL |
| `firehouse.callback.host` | localhost | Hostname for callback URL |

## Key Takeaways

1. **Polling has inherent latency** - You can never respond faster than your polling interval
2. **Polling wastes resources** - Most checks find nothing wrong
3. **Some systems can't handle aggressive polling** - The fragile barn demonstrates this
4. **Events enable instant response** - Milliseconds vs. seconds
5. **Events scale better** - Adding subscribers doesn't increase load on the barn
6. **Use polling when you have no choice** - Legacy systems, third-party APIs

## Next Chapter

Chapter 2: The Harvest Party - OAuth, Resources, and Access Control

The town rebuilt the barn and threw a party. But they needed a way to let the DJ control the music without giving them the keys to the whole building...

## License

MIT - Use this code however you want.
