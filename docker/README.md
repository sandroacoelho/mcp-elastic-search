# Docker — Hardened Reference Container

Language-agnostic, **buildable and runnable** Docker artifacts that embody the
container controls decided in the ADRs. The image ships a **placeholder service**
(BusyBox httpd serving `/healthz`) so the hardening is real and verifiable out of
the box — you replace the placeholder with your MCP server binary.

## Files

| File | Purpose |
|---|---|
| `Dockerfile` | Hardened, digest-pinned, non-root image. `>>> REPLACE` markers show where to plug in your server. |
| `docker-compose.yml` | Runs it locally with read-only rootfs, dropped capabilities, no-new-privileges, loopback-only ingress. |
| `.dockerignore` | Minimal build context; keeps secrets/cruft out of images. |

## Quick start

```bash
# Build + run with all hardening flags
docker compose -f docker/docker-compose.yml up --build

# Verify the placeholder is serving
curl -s http://127.0.0.1:8080/healthz
# {"status":"ok","service":"secure-mcp-service","note":"placeholder — replace with your MCP server"}
```

Or with plain Docker:

```bash
docker build -t secure-mcp-service:dev docker/
docker run --rm -p 127.0.0.1:8080:8080 \
  --read-only --tmpfs /tmp --cap-drop ALL \
  --security-opt no-new-privileges secure-mcp-service:dev
```

## How this maps to the ADRs

| Control | Where | ADR |
|---|---|---|
| Base image pinned by **SHA256 digest** (not a mutable tag) | `Dockerfile` `FROM alpine@sha256:…` | [0002 §2](../docs/adrs/0002-supply-chain-security.md) |
| **Non-root** user (UID/GID 10001) | `Dockerfile` `adduser` + `USER` | [0001 §9](../docs/adrs/0001-secure-mcp-service-design.md) |
| **Read-only root filesystem** | `docker-compose.yml` `read_only: true` | 0001 §9 |
| **Drop all capabilities** | `docker-compose.yml` `cap_drop: [ALL]` | 0001 §9 |
| **no-new-privileges** | `docker-compose.yml` `security_opt` | 0001 §9 |
| **Minimal base image** | Alpine + BusyBox only; no extra packages | 0001 §9 |
| **Loopback-only ingress** (gateway fronts it in prod) | `docker-compose.yml` `127.0.0.1:8080:…` | 0001 §4/§9 |
| **Healthcheck** | `Dockerfile` `HEALTHCHECK` | — |

## Replacing the placeholder with your server

1. Add a **build stage** to the `Dockerfile` (commented template is included) that
   compiles your Go/Java/Rust MCP server. Pin that base image by digest too.
2. `COPY --from=build` the artifact into the runtime stage.
3. Replace the placeholder `ENTRYPOINT` with your server's exec command, listening
   on `${MCP_PORT}`.
4. Keep the non-root `USER`, the read-only rootfs, and the digest pin intact.

## What this does NOT cover (deliberately out of scope here)

These are real production requirements from the ADRs that live outside a single
Dockerfile/compose:

- **SBOM generation** and **vulnerability scanning** (Trivy) — now wired in CI:
  [`.github/workflows/supply-chain.yml`](../.github/workflows/supply-chain.yml).
- **Image signing + admission control** (Sigstore/Cosign) — still to wire up;
  needs a signing identity — [ADR-0002 §2](../docs/adrs/0002-supply-chain-security.md).
- **Gateway-only ingress + egress allowlist** via NetworkPolicy / service mesh —
  [ADR-0001 §4/§9](../docs/adrs/0001-secure-mcp-service-design.md).
- **seccomp/AppArmor/SELinux** profiles at the host/orchestrator — ADR-0001 §9.

## Re-pinning the base image

Digests are immutable; that's the point. When you intentionally upgrade:

```bash
docker pull alpine:3.21
docker inspect --format '{{index .RepoDigests 0}}' alpine:3.21
# copy the alpine@sha256:… value into the Dockerfile FROM line
```
