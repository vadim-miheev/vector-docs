# Vector Docs

Distributed microservices application for document organization and AI-powered search using vector embeddings.

## About the Project

Vector Docs is a full-featured platform for:
- Uploading and processing documents (PDF, TXT) with OCR support
- Generating vector embeddings for semantic search
- AI-powered answer generation based on document context
- User access management and authentication

**Technology Stack:**
- **Backend**: Java 24, Spring Boot, Kafka, Langchain4j, PostgreSQL (pgvector), Apache PDFBox, Tesseract OCR
- **Frontend**: React, Tailwind
- **Infrastructure**: Docker, Docker Compose, Nginx
- **AI/ML**: Integration with LLM APIs (OpenAI-compatible), vector embeddings

## Architecture

### Microservices
1. **Gateway** (`/gateway`) - API gateway with authentication (JWT) on port 8080
2. **Document Processor** (`/services/document-processor`) - document processing, OCR, embedding generation
3. **Search Service** (`/services/search-service`) - vector search across documents
4. **Answer Generator** (`/services/answer-generator`) - answer generation using LLMs
5. **Storage Service** (`/services/storage-service`) - file storage management
6. **Notification Service** (`/services/notification-service`) - user notification handling

### Infrastructure
- **PostgreSQL** - primary database with Flyway migrations
- **Kafka** - message broker for async communication
- **Nginx** - reverse proxy for frontend in production

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 24 (for local development)
- Node.js 22.18+ (for frontend)

### Installation and Setup

1. **Clone the repository**
```bash
git clone https://github.com/vadim-miheev/vector-docs
cd vector-docs
```

2. **Configure environment**
```bash
cp .env.example .env
# Edit .env file with your settings
```

3. **Start in development mode**
```bash
make dev-start
```

4. **Access the application**
- API Gateway & UI: http://localhost:8080
- Database: localhost:5432
- Kafka: localhost:9092

### Production deployment
```bash
make prod-start
```
In production mode, frontend is served through Nginx.

## Configuration

### Key Environment Variables

**Database:**
```env
POSTGRES_DB=vector_docs
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
```

**AI Services:**
```env
# Embedding service (e.g., local Ollama)
EMBEDDING_BASE_URL=http://host.docker.internal:11434/v1
EMBEDDING_MODEL_NAME=nomic-embed-text

# LLM API (e.g., DeepSeek, OpenAI)
APP_CHAT_BASE_URL=https://api.deepseek.com/v1
APP_CHAT_MODEL_NAME=deepseek-chat
APP_CHAT_API_KEY=your-api-key

# Path to UI for gateway proxy (change to host.docker.internal:3000 for dev)
INTERNAL_UI_URL=http://react-nginx:80
```

**Authentication:**
```env
JWT_SECRET=your-secret-key
JWT_TTL_SECONDS=86400
```

**OCR:**
```env
TESSDATA_LANG=eng  # Language for Tesseract OCR
```

## Development Commands

### Main Commands
```bash
make dev-start      # Start in development mode
make prod-start     # Start in production mode
make stop           # Stop all containers
make gradle-clean   # Clean build artifacts
```

### Individual Service Rebuild
```bash
make rebuild-dp      # Rebuild document-processor
make rebuild-storage # Rebuild storage-service
make rebuild-search  # Rebuild search-service
make rebuild-ag      # Rebuild answer-generator
make rebuild-ns      # Rebuild notification-service
make rebuild-gateway # Rebuild gateway
```

### Frontend Development
```bash
cd frontend/react-app
npm start           # Start dev server
npm test            # Run tests
npm run build       # Build for production
```

## Project Structure

```
vector-docs/
├── gateway/                    # API Gateway
├── services/                   # Microservices
│   ├── answer-generator/       # Answer generator
│   ├── document-processor/     # Document processor
│   ├── search-service/         # Search service
│   ├── storage-service/        # File storage
│   └── notification-service/   # Notifications
├── frontend/                   # Frontend application
│   ├── react-app/              # React application
│   └── nginx/                  # Nginx configuration
├── db-migration/               # Database migrations
├── kafka/                      # Kafka configuration
├── docker-compose.yml          # Main Docker Compose
├── docker-compose.gradle.yml   # Build with Gradle
├── docker-compose.db.yml       # Database
├── docker-compose.fe.yml       # Frontend
├── makefile                    # Development commands
└── .env.example                # Configuration template
```

## Development

### Backend Development
1. Start infrastructure: `make dev-start`
2. Services auto-reload on changes via volume mounts
3. View logs: `docker compose logs -f <service-name>`

### Frontend Development
1. Navigate to frontend: `cd frontend/react-app`
2. Install dependencies: `npm install`
3. Start dev server: `npm start`
4. Application available at http://localhost:3000 (or http://localhost:8080 if INTERNAL_UI_URL changed to host.docker.internal:3000)

### Debugging
For Java service debugging, uncomment `JAVA_TOOL_OPTIONS` lines in `docker-compose.yml` and restart the service.

## License

MIT

## Contact
vadim.miheev.dev@gmail.com
