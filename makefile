.PHONY: gradle-bootjar gradle-bootjar-once db-migration dev-build dev-start prod-start prod-restart stop

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

# Cleans the project build artifacts
gradle-clean:
	docker compose run --rm gradle gradle clean --no-daemon


# Runs all tests without gradle daemon (for CI/CD or one-time test runs)
gradle-test:
	docker compose run --rm gradle gradle test --no-daemon

dev-build: gradle-bootjar db-migration

# Java services rebuilding
dev-start: dev-build
	# services restarting
	docker compose restart gateway notification-service answer-generator search-service storage-service document-processor
	# up all stopped
	docker compose up -d --build

#Builds the project and starts the infrastructure
prod-start: gradle-bootjar-once db-migration
	docker compose --profile prod up -d --build

prod-restart: stop prod-start

stop:
	docker compose down gradle-daemon
	docker compose --profile prod down

rebuild-dp: dev-build
	docker compose up document-processor -d --build
	docker compose restart document-processor

rebuild-storage: dev-build
	docker compose up storage-service -d --build
	docker compose restart storage-service

rebuild-search: dev-build
	docker compose up search-service -d --build
	docker compose restart search-service

rebuild-ag: dev-build
	docker compose up answer-generator -d --build
	docker compose restart answer-generator

rebuild-ns: dev-build
	docker compose up notification-service -d --build
	docker compose restart notification-service

rebuild-gateway: dev-build
	docker compose up gateway -d --build
	docker compose restart gateway