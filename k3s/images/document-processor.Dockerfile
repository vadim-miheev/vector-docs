FROM openjdk:24-jdk-slim AS base

RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-rus tesseract-ocr-eng libtesseract-dev curl && \
    rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=build/libs/document-processor.jar
COPY ${JAR_FILE} /app/document-processor.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/document-processor.jar"]
