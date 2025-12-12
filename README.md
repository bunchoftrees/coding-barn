# Coding Barn
## From Barns to Bytes: A Farm Guide to Modern System Architecture

A narrative-driven technical series using farm and rural community analogies to teach modern architecture patterns, security principles, and integration strategies to enterprise developers.

Read the series at: [your-substack-url]

## Structure

Each chapter has its own directory with working code examples:

- `chapter-01/` - **The Barn That Burned Down** - Polling vs. Event-Driven Architecture
- `chapter-02/` - **The Harvest Party** - OAuth, Resources, and Access Control *(coming soon)*
- `chapter-03/` - **The Fence Line** - API Boundaries and Contracts *(coming soon)*

## Running the Examples

Each chapter includes a `docker-compose.yml` for easy setup:

```bash
cd chapter-01
docker compose up --build
```

Or run services individually with Maven:

```bash
cd chapter-01/barn-service
./mvnw spring-boot:run
```

## Requirements

- Java 21+
- Maven 3.8+
- Docker & Docker Compose (optional, for easy multi-service setup)

## Philosophy

These examples are intentionally simple. They're **toys**—designed to illustrate concepts, not to be production-ready. The goal is to *feel* the difference between architectural patterns, not to build the next enterprise platform.

Break them. Experiment. Adjust the polling intervals and watch things fail. That's the point.

## License

MIT - Use these examples however you'd like. Attribution appreciated but not required.
