# Convertify — single Spring Boot container (backend + static frontend).
# Build context must be the repository root: the Maven build packages ../frontend into the jar.

# ---- Build stage: compile and package the executable jar ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached until pom.xml changes.
COPY backend/pom.xml backend/pom.xml
RUN mvn -B -q -f backend/pom.xml dependency:go-offline

COPY backend/src backend/src
COPY frontend frontend
RUN mvn -B -q -f backend/pom.xml -DskipTests package

# ---- Runtime stage: JRE only, non-root ----
FROM eclipse-temurin:25-jre
RUN groupadd --system convertify && useradd --system --gid convertify --home-dir /app convertify
WORKDIR /app
COPY --from=build /workspace/backend/target/convertify.jar app.jar
USER convertify

# prod: production settings. container: logs to stdout only (platforms collect them; the
# filesystem is ephemeral). PORT is overridden by Render/Railway; 8080 for `docker run`.
ENV SPRING_PROFILES_ACTIVE=prod,container \
    PORT=8080
EXPOSE 8080

# Tuned for small instances (Render free: 512 MB, 0.1 CPU). The heap gets 60% of the container
# limit so metaspace, code cache and thread stacks fit in the rest; SerialGC has the smallest
# footprint and suits a single slow core. Restart cleanly instead of limping on OOM.
# Set as JAVA_TOOL_OPTIONS so a platform environment variable can override them without a rebuild.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m \
-XX:ReservedCodeCacheSize=48m -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
