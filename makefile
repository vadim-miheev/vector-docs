.PHONY: gradle-bootjar gradle-bootjar-once db-migration build-dev build-prod start-prod stop

# Docker compose vars
export UID := $(shell id -u)
export GID := $(shell id -g)

db-migration:
	docker compose run --rm flyway

# Builds the project using gradle daemon
gradle-bootjar:
	@if [ -z "$$(docker ps -q -f name=gradle-daemon)" ]; then \
		docker compose up -d gradle-daemon --build; \
	fi
	docker exec gradle-daemon gradle bootJar

# No gradle daemon (for prod)
gradle-bootjar-once:
	docker compose run --rm gradle gradle bootJar --no-daemon

build-dev: gradle-bootjar db-migration

build-prod: gradle-bootjar-once db-migration

#Builds the project and starts the infrastructure
start-prod: build-prod
	docker compose --profile prod up -d --build

# Java services rebuilding
rebuild-dev: build-dev
	# services restarting
	docker compose restart gateway notification-service answer-generator search-service storage-service document-processor
	# up all stopped
	docker compose up -d --build

stop:
	docker compose down gradle-daemon
	docker compose --profile prod down

rebuild-prod: stop start-prod

# Cleans the project build artifacts
gradle-clean:
	docker compose run --rm gradle gradle clean --no-daemon

dp-rebuild: build-dev
	docker compose up document-processor -d --build
	docker compose restart document-processor

storage-rebuild: build-dev
	docker compose up storage-service -d --build
	docker compose restart storage-service

search-rebuild: build-dev
	docker compose up search-service -d --build
	docker compose restart search-service

ag-rebuild: build-dev
	docker compose up answer-generator -d --build
	docker compose restart answer-generator

ns-rebuild: build-dev
	docker compose up notification-service -d --build
	docker compose restart notification-service

gateway-rebuild: build-dev
	docker compose up gateway -d --build
	docker compose restart gateway