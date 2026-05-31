# syntax=docker/dockerfile:1
###############################################################################
# Secure MCP Service — reference container (LANGUAGE-AGNOSTIC, HARDENED)
#
# Embodies the runtime-isolation + supply-chain controls from the ADRs:
#   • ADR-0001 §9 (Runtime Isolation & Egress): non-root, read-only root FS,
#     minimal base image, no host network (the last enforced via compose/orchestrator).
#   • ADR-0002 (Software Supply Chain Security): base image pinned by SHA256 digest.
#
# It BUILDS and RUNS as-is. The placeholder "service" is BusyBox httpd (already in
# the base image — no language runtime added) serving a health document, so the
# hardening is demonstrably real. Replace the placeholder at the ">>> REPLACE"
# markers with your Go / Java / Rust MCP server binary.
###############################################################################

# ---- (optional) build stage --------------------------------------------------
# >>> REPLACE: compile your MCP server here, then COPY the artifact into runtime.
#     Pin THIS base by digest too (ADR-0002 §2). Example (Go):
#
#       FROM golang@sha256:<digest> AS build
#       WORKDIR /src
#       COPY . .
#       RUN CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/mcp-server ./cmd/server
#
#     Then in the runtime stage below:  COPY --from=build /out/mcp-server /app/mcp-server

# ---- runtime stage -----------------------------------------------------------
# Base image pinned by digest (ADR-0002 §2). Tag at pin time: alpine:3.21.
# Re-pin deliberately when upgrading — never float to a mutable tag.
FROM alpine@sha256:48b0309ca019d89d40f670aa1bc06e426dc0931948452e8491e3d65087abc07d

LABEL org.opencontainers.image.title="secure-mcp-service" \
      org.opencontainers.image.description="Hardened, language-agnostic MCP service container (reference)" \
      org.opencontainers.image.source="https://example.com/your-org/your-repo"

# Non-root user (ADR-0001 §9). Fixed UID/GID so mounted-volume perms are predictable.
ARG APP_UID=10001
ARG APP_GID=10001
RUN addgroup -g ${APP_GID} -S app \
 && adduser  -u ${APP_UID} -G app -S -H -s /sbin/nologin app

# Port the MCP server listens on (Streamable HTTP / SSE). >1024 so no root needed.
ARG MCP_PORT=8080
ENV MCP_PORT=${MCP_PORT}

# ---- placeholder application -------------------------------------------------
# >>> REPLACE this whole block with your MCP server binary + ENTRYPOINT.
#     The placeholder serves a static health document via BusyBox httpd so the
#     container is verifiably runnable and hardened out of the box. httpd lives
#     in busybox-extras (the default Alpine BusyBox compiles it out); the package
#     version is pinned per ADR-0002 §2 ("pin tool versions").
RUN apk add --no-cache busybox-extras=1.37.0-r14
WORKDIR /app
RUN mkdir -p /app/www \
 && printf '{"status":"ok","service":"secure-mcp-service","note":"placeholder — replace with your MCP server"}\n' \
      > /app/www/healthz \
 && chown -R app:app /app
# ------------------------------------------------------------------------------

USER app:app
EXPOSE ${MCP_PORT}

# Liveness probe (an orchestrator may override). Shell form so ${MCP_PORT} expands at runtime.
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null "http://127.0.0.1:${MCP_PORT}/healthz" || exit 1

# >>> REPLACE: exec your server. Placeholder = BusyBox httpd in the foreground (-f),
#     serving the read-only /app/www document root.
ENTRYPOINT ["sh", "-c", "exec /usr/sbin/httpd -f -p \"${MCP_PORT}\" -h /app/www"]
