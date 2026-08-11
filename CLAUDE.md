# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**vector-docs** is a distributed microservices application for document processing and AI-powered search.

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

### Database Migrations
```bash
make db-migration   # Run Flyway database migrations
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