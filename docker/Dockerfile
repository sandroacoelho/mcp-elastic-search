# syntax=docker/dockerfile:1
###############################################################################
# mcp-elastic-search — distribution image (HARDENED, multi-stage)
#
# Builds the Spring Boot (Java 25 + Spring AI) MCP server from ./server and ships
# it on a minimal JRE. Embodies the same controls as the reference container:
#   • ADR-0001 §9 (Runtime Isolation): non-root, read-only-rootfs friendly,
#     minimal base image, health endpoint for liveness/readiness probes.
#   • ADR-0002 §2 (Supply Chain): every base image pinned by SHA256 digest,
#     never a mutable tag. Re-pin deliberately on upgrade (see README).
#
# BUILD CONTEXT IS THE REPO ROOT (the build stage needs ./server):
#   docker build -f docker/Dockerfile -t mcp-elastic-search:0.1.0 .
# or via compose (context: .., dockerfile: docker/Dockerfile):
#   docker compose -f docker/docker-compose.yml up --build
###############################################################################

# ---- build stage -------------------------------------------------------------
# Maven + Temurin JDK 25, pinned by digest. Tag at pin time: maven:3.9-eclipse-temurin-25-alpine.
FROM maven@sha256:5e8229c2aed320c85d2b3851585dd2e7f7c481e366ce051238e802fd4cfb74aa AS build
WORKDIR /src

# Resolve dependencies first so this layer caches unless the POM changes.
COPY server/pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

# Compile + package the executable jar. Tests (incl. Testcontainers IT) run in CI,
# not in the image build — keep the build hermetic and Docker-in-Docker-free.
COPY server/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests clean package \
 && cp target/*.jar /src/app.jar

# ---- runtime stage -----------------------------------------------------------
# Temurin JRE 25 on Alpine, pinned by digest. Tag at pin time: eclipse-temurin:25-jre-alpine.
# Alpine's BusyBox provides `wget` for the HEALTHCHECK without adding packages.
FROM eclipse-temurin@sha256:c707c0d18cb9e8556380719f80d96a7529d0746fbb42143893949b98ed2f8943

LABEL org.opencontainers.image.title="mcp-elastic-search" \
      org.opencontainers.image.description="Read-only Elasticsearch MCP server (Java 25 + Spring AI, HTTP/SSE)" \
      org.opencontainers.image.source="https://github.com/your-org/mcp-elastic-search" \
      org.opencontainers.image.licenses="Apache-2.0"

# Non-root user (ADR-0001 §9). Fixed UID/GID so mounted-volume perms are predictable.
ARG APP_UID=10001
ARG APP_GID=10001
RUN addgroup -g ${APP_GID} -S app \
 && adduser  -u ${APP_UID} -G app -S -H -s /sbin/nologin app

# Port the MCP server listens on (Spring `server.port`, relaxed-bound from SERVER_PORT).
ARG APP_PORT=9090
ENV SERVER_PORT=${APP_PORT}
# Size the heap to the container's cgroup limit instead of the host's RAM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

WORKDIR /app
COPY --from=build --chown=app:app /src/app.jar /app/app.jar

USER app:app
EXPOSE ${APP_PORT}

# Liveness probe — app-state only, NOT gated on Elasticsearch (Actuator's ES health
# indicator would otherwise flap the container whenever ES is briefly unreachable).
# Orchestrators should additionally gate traffic on /actuator/health/readiness.
# Shell form so ${SERVER_PORT} expands at runtime.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT}/actuator/health/liveness" || exit 1

# Exec form via sh so the JVM is PID-but-signal-forwarded; `exec` replaces the shell.
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar"]
