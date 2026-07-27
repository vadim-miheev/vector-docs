FROM openjdk:24-jdk-slim

ARG JAR_FILE=build/libs/service.jar
COPY ${JAR_FILE} /app/service.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/service.jar"]
