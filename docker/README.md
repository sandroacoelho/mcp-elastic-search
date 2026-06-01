# Docker — Distribution Image

**Buildable and runnable** Docker artifacts that package the `mcp-elastic-search`
server (Spring Boot, Java 25 + Spring AI; read-only Elasticsearch MCP over HTTP/SSE)
while keeping the container controls decided in the ADRs. A multi-stage build
compiles `./server` and ships the jar on a minimal Temurin JRE.

## Files

| File | Purpose |
|---|---|
| `Dockerfile` | Hardened, digest-pinned, non-root multi-stage image. **Build context is the repo root** (the build stage needs `./server`). |
| `docker-compose.yml` | Runs it locally with read-only rootfs, dropped capabilities, no-new-privileges, loopback-only ingress. |
| `../.dockerignore` | Keeps the (repo-root) build context minimal; keeps secrets/cruft out of images. |

## Quick start

```bash
# Build + run with all hardening flags (compose context is the repo root)
docker compose -f docker/docker-compose.yml up --build

# Verify liveness (app state, independent of Elasticsearch)
curl -s http://127.0.0.1:9090/actuator/health/liveness
# {"status":"UP"}
```

Or with plain Docker — note the `-f` flag and the `.` (repo-root) context:

```bash
docker build -f docker/Dockerfile -t mcp-elastic-search:0.1.0 .
docker run --rm -p 127.0.0.1:9090:9090 \
  --read-only --tmpfs /tmp --cap-drop ALL \
  --security-opt no-new-privileges --user 10001:10001 \
  -e ES_HOST=https://your-es:9200 -e ES_API_KEY=*** \
  mcp-elastic-search:0.1.0
```

## Configuration

The server is configured entirely through environment (never bake secrets into the
image):

| Env var | Default | Purpose |
|---|---|---|
| `ES_HOST` | `https://localhost:9200` | Elasticsearch endpoint. |
| `ES_API_KEY` | _(empty)_ | Least-privilege API key (read + `view_index_metadata` on allowlisted indices). |
| `SERVER_PORT` | `9090` | HTTP/SSE listen port. |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=75.0 …` | Heap sized to the container's cgroup memory limit. |
| `EXTRA_VAR_1` | `default-value` | _Rename:_ non-secret runtime config (falls back to its default if unset). |
| `EXTRA_VAR_2` | _(required)_ | _Rename:_ secret token/key; the run fails fast if unset (never baked into the image). |

## Health / probes

Spring Boot Actuator exposes **only** the health endpoint (ADR-0001 §9 minimal
surface):

- `…/actuator/health/liveness` — app state only. Used by the image `HEALTHCHECK`;
  does **not** flap when Elasticsearch is briefly unreachable.
- `…/actuator/health/readiness` — gate ingress traffic on this in an orchestrator.

## How this maps to the ADRs

| Control | Where | ADR |
|---|---|---|
| Base images pinned by **SHA256 digest** (build + runtime) | `Dockerfile` `FROM maven@sha256:…` / `eclipse-temurin@sha256:…` | [0002 §2](../docs/adrs/0002-supply-chain-security.md) |
| **Non-root** user (UID/GID 10001) | `Dockerfile` `adduser` + `USER` | [0001 §9](../docs/adrs/0001-secure-mcp-service-design.md) |
| **Read-only root filesystem** | `docker-compose.yml` `read_only: true` (+ `/tmp` tmpfs) | 0001 §9 |
| **Drop all capabilities** | `docker-compose.yml` `cap_drop: [ALL]` | 0001 §9 |
| **no-new-privileges** | `docker-compose.yml` `security_opt` | 0001 §9 |
| **Minimal base image** | Temurin JRE on Alpine; no extra packages | 0001 §9 |
| **Loopback-only ingress** (gateway fronts it in prod) | `docker-compose.yml` `127.0.0.1:9090:…` | 0001 §4/§9 |
| **Health/liveness probe** | `Dockerfile` `HEALTHCHECK` → `/actuator/health/liveness` | — |

## What this does NOT cover (deliberately out of scope here)

These are real production requirements from the ADRs that live outside a single
Dockerfile/compose:

- **SBOM generation** and **vulnerability scanning** (Trivy) — wired in CI:
  [`.github/workflows/supply-chain.yml`](../.github/workflows/supply-chain.yml).
- **Image signing + admission control** (Sigstore/Cosign) — still to wire up;
  needs a signing identity — [ADR-0002 §2](../docs/adrs/0002-supply-chain-security.md).
- **Publishing to a registry** — build locally and push by hand, or add a release
  job (`docker push ghcr.io/<owner>/mcp-elastic-search:<tag>`) when ready.
- **Gateway-only ingress + egress allowlist** via NetworkPolicy / service mesh —
  [ADR-0001 §4/§9](../docs/adrs/0001-secure-mcp-service-design.md).
- **seccomp/AppArmor/SELinux** profiles at the host/orchestrator — ADR-0001 §9.

## Re-pinning a base image

Digests are immutable; that's the point. When you intentionally upgrade (e.g. a new
JRE patch), re-resolve and paste the new digest into the `Dockerfile` `FROM` line:

```bash
docker pull eclipse-temurin:25-jre-alpine
docker inspect --format '{{index .RepoDigests 0}}' eclipse-temurin:25-jre-alpine
# copy the eclipse-temurin@sha256:… value into the Dockerfile FROM line
# (do the same for maven:3.9-eclipse-temurin-25-alpine on the build stage)
```
