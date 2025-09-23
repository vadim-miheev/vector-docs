FROM openjdk:24-jdk-slim

# tesseract OCR
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-rus tesseract-ocr-eng libtesseract-dev && \
    rm -rf /var/lib/apt/lists/*