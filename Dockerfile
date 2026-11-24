# syntax=docker/dockerfile:1

# ---- build stage ---------------------------------------------------------
# Uses the Maven wrapper committed to the repo, so the build does not depend
# on a Maven version installed anywhere — the server needs only Docker.
FROM eclipse-temurin:17-jdk AS build

WORKDIR /build

# Copy the wrapper and the POM first so dependency resolution is cached as its
# own layer; source changes then rebuild without re-downloading the world.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests \
    && mv target/*.jar /build/app.jar

# ---- runtime stage -------------------------------------------------------
# JRE only: no compiler, no Maven, no source. Smaller and less to attack.
FROM eclipse-temurin:17-jre AS runtime

# curl is used by the container HEALTHCHECK below. Installed deliberately and
# kept minimal; nothing else is added to the runtime image.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as a non-root user. The app never needs to write to the filesystem.
RUN groupadd --system resibo && useradd --system --gid resibo --no-create-home resibo

WORKDIR /app
COPY --from=build --chown=resibo:resibo /build/app.jar /app/app.jar

USER resibo

# The container always listens on 8080 internally. Which host port that maps to
# is decided in docker-compose.yml, so the image is identical in every
# environment.
ENV SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
