# k3s Deployment — vector-docs

Configuration for running vector-docs in a **K3s** cluster (2 worker nodes).

## Architecture

```
               NodePort:30080
                     │
                ┌────┴────┐
                │  Gateway │
                └────┬────┘
                     │
      ┌──────────────┼──────────────┐
      │              │              │
  storage-     document-      search-     answer-
  service      processor       service     generator
      │              │              │              │
      └──────────────┴──────┬───────┴──────┬───────┘
                            │              │
                        postgres        kafka
                          (pgvector)    └── zookeeper

frontend (nginx :80)
```

## Quick start

### 1. Build the JAR files

```bash
cd /home/vadim/java/vector-docs
make dev-build
```

### 2. Build the Docker images

```bash
./k3s/scripts/build-images.sh
```

Load the images into the cluster (choose one of the options):

#### Option A — registry (recommended)

```bash
REGISTRY=myregistry.example.com ./k3s/scripts/build-images.sh
# Then adjust imagePullPolicy and image in the manifests
```

#### Option B — load directly onto the nodes

```bash
# On each node:
docker save vector-docs/gateway:latest | bzip2 | \
  ssh <user>@<node-ip> sudo k3s ctr images import -
# Repeat for each image
```

#### Option C — build directly on the control-plane node

Copy the project to the node and run `./k3s/scripts/build-images.sh`.

#### Option D — Docker Hub

```bash
docker login
./k3s/scripts/push-images.sh          # push all images
./k3s/scripts/push-images.sh gateway  # push a single image
```

Images are pushed as `vadimmiheev/vector-docs-<service>:latest` (Docker Hub does not support nested repository names, so the `vector-docs` prefix is added with a hyphen). The k3s manifests already reference these Docker Hub images with `imagePullPolicy: Always`. After a new push, redeploy:

```bash
kubectl apply -k k3s/
kubectl -n vector-docs rollout restart deploy/gateway deploy/search-service \
  deploy/answer-generator deploy/notification-service deploy/storage-service \
  deploy/document-processor deploy/frontend
```

### 3. Run the DB migration

```bash
kubectl apply -f k3s/flyway.yaml
# Check the status:
kubectl -n vector-docs logs job/flyway-migration -f
```

### 4. Deploy the services

```bash
kubectl apply -k k3s/
```

### 5. Check the status

```bash
kubectl -n vector-docs get pods -w
kubectl -n vector-docs get services
```

### 6. Open the application

```
http://<node-ip>:30080
```

## Management

### Logs

```bash
# All pods of a service
kubectl -n vector-docs logs -l app=gateway -f

# A specific pod
kubectl -n vector-docs logs deployment/gateway -f
```

### Restart

```bash
kubectl -n vector-docs rollout restart deployment/gateway
```

### Updating after code changes

```bash
# 1. Rebuild the JAR
make rebuild-gateway

# 2. Rebuild the image
docker build -f k3s/images/service.Dockerfile \
  -t vector-docs/gateway:latest \
  --build-arg JAR_FILE=build/libs/gateway.jar \
  ./gateway

# 3. Load into the cluster (repeat on all nodes)
docker save vector-docs/gateway:latest | bzip2 | \
  ssh <node> sudo k3s ctr images import -

# 4. Restart
kubectl -n vector-docs rollout restart deployment/gateway
```

### Port forwarding

```bash
# Gateway
kubectl -n vector-docs port-forward service/gateway 8080:8080

# PostgreSQL
kubectl -n vector-docs port-forward service/postgres 5432:5432

# Kafka
kubectl -n vector-docs port-forward service/kafka 9092:9092

# Frontend
kubectl -n vector-docs port-forward service/frontend 3000:80
```

## File structure

```
k3s/
├── scripts/
│   ├── build-images.sh               # Build all images
│   ├── load-images.sh                # Load images into the cluster
│   └── push-images.sh                # Push images to Docker Hub
├── kustomization.yaml                # Kustomize root
├── namespace.yaml                    # namespace: vector-docs
├── configmap.yaml                    # Non-sensitive variables
├── secret.yaml                       # Sensitive variables
├── postgres.yaml                     # PostgreSQL with pgvector + PVC
├── zookeeper.yaml                    # ZooKeeper
├── kafka.yaml                        # Kafka broker
├── storage-service.yaml              # Storage Service + PVC
├── document-processor.yaml           # Document Processor
├── search-service.yaml               # Search Service
├── answer-generator.yaml             # Answer Generator
├── notification-service.yaml         # Notification Service
├── gateway.yaml                      # API Gateway (NodePort 30080)
├── frontend.yaml                     # Frontend (nginx)
├── flyway.yaml                       # Flyway migration Job
└── images/
    ├── service.Dockerfile            # Generic for Java services
    ├── document-processor.Dockerfile # + tesseract OCR
    ├── frontend.Dockerfile           # Multi-stage React → nginx
    └── flyway.Dockerfile             # Flyway + bundled migrations
```

## Environment variables

The main settings are in `configmap.yaml` and `secret.yaml`:

| ConfigMap | Secret |
|---|---|
| `POSTGRES_DB`, `POSTGRES_PORT` | `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| `EMBEDDING_*` | `JWT_SECRET` |
| `APP_CHAT_BASE_URL`, `APP_CHAT_MODEL_NAME` | `APP_CHAT_API_KEY` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, ... | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` |
| `KAFKA_BOOTSTRAP_SERVERS` | |
| `SITE_ROOT`, `INTERNAL_UI_URL` | |

Adjust these to your environment before deploying.

### Frontend runtime config

The frontend does not bake `REACT_APP_*` values into the image: the `API_BASE_URL`, `WS_URL` and
`DEMO_USER_ID` values are set **at runtime** via a `config.js` block in `configmap.yaml` and
read by the app from `window.APP_CONFIG`. You can change the environment configuration
without rebuilding the image:

```bash
# 1. Edit the config.js block in k3s/configmap.yaml
# 2. Apply the manifests and restart the frontend
kubectl apply -k k3s/
kubectl -n vector-docs rollout restart deployment/frontend
```

nginx serves `/config.js` without caching and mounts it from a ConfigMap
(see `frontend/react-app/src/config/runtimeConfig.js`). The frontend `DEMO_USER_ID`
must match the backend `DEMO_USER_ID`.