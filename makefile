.PHONY: run restart restart-hard build build-once clean stop

export UID := $(shell id -u)
export GID := $(shell id -g)

#Starts the infrastructure and builds the project
run: build
	docker compose up -d --build

#Restarts the infrastructure
restart: run
	docker compose restart

#Full infrastructure restart with cleanup and rebuild
restart-hard: stop clean run

#Builds the project using gradle daemon container
build:
	@if [ -z "$$(docker ps -q -f name=gradle-daemon)" ]; then \
		docker compose -f docker-compose.gradle.yml up -d gradle-daemon --build; \
	fi
	docker exec gradle-daemon gradle bootJar

#Builds the project once without using gradle daemon
build-once:
	docker compose -f docker-compose.gradle.yml run --rm gradle gradle bootJar --no-daemon

#Cleans the project build artifacts
clean:
	docker compose -f docker-compose.gradle.yml run --rm gradle gradle clean --no-daemon

#Stops all running containers and removes them
stop:
	docker compose down
	docker compose -f docker-compose.gradle.yml down
