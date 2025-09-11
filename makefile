.PHONY: up rebuild restart restart-hard bootjar bootjar-once gradle-clean down db-migration

export UID := $(shell id -u)
export GID := $(shell id -g)

#Builds the project and starts the infrastructure
up: bootjar db-migration
	docker compose up -d --build

rebuild: bootjar db-migration restart

#Restarts the infrastructure
restart:
	docker compose restart

#Full infrastructure restart with cleanup and rebuild
restart-hard: down gradle-clean db-migration up

#Builds the project using gradle daemon container
bootjar:
	@if [ -z "$$(docker ps -q -f name=gradle-daemon)" ]; then \
		docker compose -f docker-compose.gradle.yml up -d gradle-daemon --build; \
	fi
	docker exec gradle-daemon gradle bootJar

#Builds the project without using gradle daemon
bootjar-once:
	docker compose -f docker-compose.gradle.yml run --rm gradle gradle bootJar --no-daemon

#Cleans the project build artifacts
gradle-clean:
	docker compose -f docker-compose.gradle.yml run --rm gradle gradle clean --no-daemon

#Stops all running containers and removes them
down:
	docker compose down
	docker compose -f docker-compose.gradle.yml down

db-migration:
	docker compose -f docker-compose.db.yml run --rm flyway