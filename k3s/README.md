# k3s Deployment — vector-docs

Конфигурация для запуска vector-docs в кластере **K3s** (2 worker-ноды).

## Архитектура

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

## Быстрый старт

### 1. Соберите JAR-файлы

```bash
cd /home/vadim/java/vector-docs
make dev-build
```

### 2. Соберите Docker-образы

```bash
./k3s/build-images.sh
```

Загрузите образы в кластер (выберите один из вариантов):

#### Вариант A — registry (рекомендуется)

```bash
REGISTRY=myregistry.example.com ./k3s/build-images.sh
# Затем поправить imagePullPolicy и image в манифестах
```

#### Вариант B — загрузка напрямую на ноды

```bash
# На каждой ноде:
docker save vector-docs/gateway:latest | bzip2 | \
  ssh <user>@<node-ip> sudo k3s ctr images import -
# Повторить для каждого образа
```

#### Вариант C — собрать прямо на control-plane ноде

Скопировать проект на ноду и запустить `./k3s/build-images.sh`.

### 3. Запустите миграцию БД

```bash
kubectl apply -f k3s/flyway.yaml
# Проверьте статус:
kubectl -n vector-docs logs job/flyway-migration -f
```

### 4. Разверните сервисы

```bash
kubectl apply -k k3s/
```

### 5. Проверьте статус

```bash
kubectl -n vector-docs get pods -w
kubectl -n vector-docs get services
```

### 6. Откройте приложение

```
http://<node-ip>:30080
```

## Управление

### Логи

```bash
# Все поды сервиса
kubectl -n vector-docs logs -l app=gateway -f

# Конкретный под
kubectl -n vector-docs logs deployment/gateway -f
```

### Рестарт

```bash
kubectl -n vector-docs rollout restart deployment/gateway
```

### Обновление после изменений кода

```bash
# 1. Пересобрать JAR
make rebuild-gateway

# 2. Пересобрать образ
docker build -f k3s/images/Dockerfile.service \
  -t vector-docs/gateway:latest \
  --build-arg JAR_FILE=build/libs/gateway.jar \
  ./gateway

# 3. Загрузить в кластер (повторить на всех нодах)
docker save vector-docs/gateway:latest | bzip2 | \
  ssh <node> sudo k3s ctr images import -

# 4. Рестарт
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

## Структура файлов

```
k3s/
├── build-images.sh                   # Сборка всех образов
├── kustomization.yaml                # Kustomize root
├── namespace.yaml                    # namespace: vector-docs
├── configmap.yaml                    # Нечувствительные переменные
├── secret.yaml                       # Чувствительные переменные
├── postgres.yaml                     # PostgreSQL с pgvector + PVC
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
    ├── Dockerfile.service            # Generic для Java-сервисов
    ├── Dockerfile.document-processor # + tesseract OCR
    ├── Dockerfile.frontend           # Multi-stage React → nginx
    └── Dockerfile.flyway             # Flyway + встроенные миграции
```

## Переменные окружения

Основные настройки в `configmap.yaml` и `secret.yaml`:

| ConfigMap | Secret |
|---|---|
| `POSTGRES_DB`, `POSTGRES_PORT` | `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| `EMBEDDING_*` | `JWT_SECRET` |
| `APP_CHAT_BASE_URL`, `APP_CHAT_MODEL_NAME` | `APP_CHAT_API_KEY` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, ... | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` |
| `KAFKA_BOOTSTRAP_SERVERS` | |
| `SITE_ROOT`, `INTERNAL_UI_URL` | |

Измените под своё окружение перед деплоем.
