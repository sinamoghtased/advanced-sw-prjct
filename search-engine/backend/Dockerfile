# Builds the Quickdex backend jar, then runs it on a slim JRE.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/quickdex-engine.jar ./quickdex-engine.jar
COPY --from=build /app/src/main/resources/corpus.txt ./corpus.txt
ENV QUICKDEX_CORPUS=/app/corpus.txt

# The host (e.g. Render) sets PORT; EngineServer reads it at startup.
CMD ["java", "-jar", "quickdex-engine.jar"]
