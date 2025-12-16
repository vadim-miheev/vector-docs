# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**vector-docs** is a distributed microservices application for document processing and AI-powered search with:
- **Backend**: Java Spring Boot services (Java 24) using Kafka for async communication
- **Frontend**: React application with nginx for production serving
- **Build**: Gradle with Docker-based build system using persistent gradle-daemon
- **Database**: PostgreSQL with Flyway migrations
- **Architecture**: Microservices with API Gateway pattern and event-driven communication

## Key Services

1. **Gateway** (`/gateway`): Spring Cloud Gateway - authentication and routing (port 8080)
2. **Answer Generator** (`/services/answer-generator`): AI response generation using LLM APIs
3. **Document Processor** (`/services/document-processor`): Document processing with OCR and embedding generation
4. **Search Service** (`/services/search-service`): Vector search and document retrieval
5. **Storage Service** (`/services/storage-service`): File storage management
6. **Notification Service** (`/services/notification-service`): Event notifications
7. **Common Service** (`/services/common`): Shared utilities and DTOs
8. **Frontend** (`/frontend/react-app`): React user interface with nginx proxy

## Infrastructure Components

- **PostgreSQL**: Primary database with Flyway migrations (`db-migration/`)
- **Kafka**: Message broker for async service communication (`kafka/`)
- **Nginx**: Frontend reverse proxy in production (`frontend/nginx/`)

## Build and Development Commands

### Primary Make Commands
```bash
make dev-build      # Build all services using gradle-daemon and run migrations
make dev-start      # Build and start all containers (development)
make prod-start     # Build without daemon and start production stack
make stop           # Stop all containers
make gradle-clean   # Clean build artifacts
make gradle-test    # Run all tests without gradle daemon (for CI/CD)
```

### Individual Service Rebuild Commands
```bash
make rebuild-dp      # Rebuild document-processor only
make rebuild-storage # Rebuild storage-service only
make rebuild-search  # Rebuild search-service only
make rebuild-ag      # Rebuild answer-generator only
make rebuild-ns      # Rebuild notification-service only
make rebuild-gateway # Rebuild gateway only
```

### Frontend Development
```bash
cd frontend/react-app
npm start           # Start React dev server (port 3000)
npm test            # Run frontend tests
npm run build       # Build for production
```

### Database Migrations
```bash
make db-migration   # Run Flyway database migrations
```

### Manual Gradle Commands (if needed)
```bash
# Build specific service
docker compose -f docker-compose.gradle.yml run --rm gradle gradle :gateway:bootJar

# Run tests for specific service
docker compose -f docker-compose.gradle.yml run --rm gradle gradle :services:search-service:test
```

## Architecture Notes

- **API Gateway**: Routes traffic to backend services on port 8080, handles JWT authentication
- **Service Communication**: Mix of direct HTTP and Kafka-based async messaging
- **Docker Setup**: Uses separate Docker Compose files for build (`docker-compose.gradle.yml`), database (`docker-compose.db.yml`), and frontend (`docker-compose.fe.yml`)
- **Gradle Daemon**: Persistent `gradle-daemon` container for faster incremental builds
- **Environment**: Requires `.env` file with configuration (see `.env.example`)

## Development Workflow

1. Copy `.env.example` to `.env` and configure environment variables
2. Use `make dev-start` to build and start the full stack
3. Frontend runs on React dev server (port 3000) during development
4. Backend services run in Docker containers with hot-reload via volume mounts
5. API Gateway exposes backend services on port 8080
6. For production: `make prod-start` (uses nginx instead of React dev server)

## Key Configuration

- **Embedding Service**: Configured via `EMBEDDING_BASE_URL` (default: local Ollama)
- **LLM API**: Configured via `APP_CHAT_BASE_URL` and `APP_CHAT_API_KEY`
- **JWT Authentication**: Configured via `JWT_SECRET` and `JWT_TTL_SECONDS`
- **Email**: SMTP configuration for notifications
- **OCR**: Tesseract language configuration via `TESSDATA_LANG`

## Important Files

- `docker-compose.yml`: Main runtime services configuration (includes others)
- `docker-compose.gradle.yml`: Build environment with gradle-daemon
- `docker-compose.db.yml`: Database and migration services
- `docker-compose.fe.yml`: Frontend nginx configuration
- `makefile`: Development workflow commands
- `settings.gradle`: Project module inclusion
- `.env.example`: Configuration template with all required environment variables