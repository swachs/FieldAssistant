# syntax=docker/dockerfile:1
FROM openjdk:16-alpine3.13

WORKDIR /app

COPY target/fieldAssistant-1.0-SNAPSHOT.jar ./fieldAssistant-1.0-SNAPSHOT.jar
COPY target/lib ./lib

CMD ["java", "-jar", "fieldAssistant-1.0-SNAPSHOT.jar"]
