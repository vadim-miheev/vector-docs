.PHONY: build build-once run clean restart stop

#Infrastructure start
run: build
	docker compose up -d --build

#Infrastructure and project rebuild
restart: stop clean run

#Gradle bootJar task
build-once:
	docker compose -f docker-compose.gradle.yml run --rm gradle gradle bootJar --no-daemon

build:
	@if [ -z "$$(docker ps -q -f name=gradle-daemon)" ]; then \
		docker compose -f docker-compose.gradle.yml up -d gradle-daemon --build; \
	fi
	docker exec -it gradle-daemon gradle bootJar

#Gradle clean task
clean:
	docker compose -f docker-compose.gradle.yml run --rm gradle gradle clean --no-daemon

stop:
	docker compose down
	docker compose -f docker-compose.gradle.yml down
