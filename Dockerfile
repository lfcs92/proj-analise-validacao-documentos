FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-por libgomp1 \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/docvalidator-1.0.0.jar app.jar
ENV TESSDATA_PATH=/usr/share/tesseract-ocr/5/tessdata
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
