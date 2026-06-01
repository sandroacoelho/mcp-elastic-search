# mcp-elastic-search — Read-Only Elasticsearch MCP Server

A secure, **read-only Elasticsearch** [Model Context Protocol (MCP)](https://modelcontextprotocol.io)
server, built on the secure-MCP blueprint for highly regulated environments. Binding
regimes: **GDPR, LGPD, HIPAA, PCI-DSS, SOC2**, with **FedRAMP Moderate** alignment and
AI-governance overlays (**EU AI Act · NIST AI RMF · ISO/IEC 42001**) — see ADR-0001 §13/§26.

The design follows the platform's core principle ([ADR-0001](docs/adrs/0001-secure-mcp-service-design.md)):

> **Primary security controls do not live inside the MCP servers.** They live in a
> hardened gateway every server sits behind. This server implements business logic plus a
> small mandatory **safety contract** — so the platform is language-agnostic *and* high-security.

The server's own design is **[ADR-0003](docs/adrs/0003-elasticsearch-readonly-mcp-server.md)**:
a closed, read-only tool surface with hard guard rails, behind the gateway.

## The server (`server/`)

Java 25 + Spring Boot (Spring AI MCP server, HTTP/SSE). Base package `ai.enterprise.mcp.elastic`.

| Aspect | Detail |
|---|---|
| **Stack** | Java 25 · Spring Boot 3.5.4 · Spring AI 1.1.2 (`spring-ai-starter-mcp-server-webmvc`) · Elasticsearch low-level REST client |
| **Tools** (closed set, read-only) | `listIndices` · `describeIndex` · `search` · `getDocument` · `count` |
| **Query guard** | `QueryGuard` rejects scripting (`script`, `script_score`, `runtime_mappings`, …), `_msearch`/`_sql`, and any non-search body shape |
| **Index allowlist** | logical→concrete mapping; unlisted and `.`-prefixed system indices **fail closed** |
| **Result bounding** | page-size cap, `max_result_window` ceiling, request timeout, `_source` projection |
| **Architecture** | Hexagonal `tool → service → port → adapter`; ES client confined to `adapter` (enforced by ArchUnit A1–A7) |
| **Quality gates** | JaCoCo **≥90% line+branch** (build-failing) · ArchUnit · server-contract suite (QA-10) · Testcontainers ES integration test |

### Build & test

```bash
cd server

# Unit tests + ArchUnit + server-contract + ≥90% coverage gate (no Docker needed)
mvn test

# Adds the Testcontainers Elasticsearch adapter integration test (needs Docker)
mvn verify
```

> **Docker note:** the integration test pins the Docker Engine API version
> (`-Dapi.version=1.40`, wired in `pom.xml`) so it works on Docker Engine 25+,
> which rejects the docker-java default of 1.32.

### Configure (`server/src/main/resources/application.yml`)

```yaml
mcpes:
  connection:
    host: ${ES_HOST:https://localhost:9200}
    api-key: ${ES_API_KEY:}        # least-privilege: read + view_index_metadata on allowlisted indices only
  allowlist:
    sample: sample-v1              # logicalName -> concrete index/alias
  limits: { default-page-size: 10, max-page-size: 100, max-result-window: 10000, request-timeout-seconds: 10 }
```

Transport is Streamable HTTP/SSE (`GET /elastic`), default port `9090` — reachable **only**
from the gateway by network policy (ADR-0001 §4).

## What's in here

```
.
├── server/                   # The Java 25 / Spring Boot MCP server (ADR-0003)
│   ├── pom.xml               #   JaCoCo ≥90% gate · ArchUnit · failsafe Testcontainers IT
│   └── src/main/java/ai/enterprise/mcp/elastic/   tool · service · domain · port · adapter · config
├── docs/
│   ├── adrs/
│   │   ├── 0001-secure-mcp-service-design.md   # canonical reference design
│   │   ├── 0002-supply-chain-security.md       # supply-chain security (accepted)
│   │   ├── 0003-elasticsearch-readonly-mcp-server.md   # THIS server's design
│   │   └── README.md           # ADR index
│   └── process/adr-process.md  # how ADRs are written/versioned/reviewed
├── policy/                   # OPA/Rego PDP (ADR-0001 §5): deny-by-default + read-tier obligations
│   ├── authz.rego · authz_test.rego · authz_elastic_test.rego · data.json
│   └── README.md
├── tests/server-contract/    # Language-neutral safety-contract spec (§4 / QA-10)
├── docker/                   # Hardened, digest-pinned reference container (ADR-0001 §9)
├── .github/workflows/
│   ├── ci.yml                # Build & release gates: mvn verify + OPA
│   ├── supply-chain.yml      # gitleaks · Trivy · SBOM · image scan (ADR-0002)
│   └── README.md
├── skills/                   # Reusable, tool-neutral agent playbooks
├── AGENTS.md · CLAUDE.md     # Agent guide (any AI CLI) + Claude Code entry point
└── .gitleaks.toml · .pre-commit-config.yaml · .gitignore
```

## Authorization policy (`policy/`)

The runnable PDP from ADR-0001 §5, extended for this server: read-tier obligations
(`cap_rows` from `read_row_cap`, `project_source` for sensitive reads) and ES-specific
authorization tests.

```bash
opa check policy/ && opa fmt --fail policy/ && opa test policy/ -v   # 26/26
```

## CI — release gates

| Workflow | Jobs |
|---|---|
| [`ci.yml`](.github/workflows/ci.yml) | `server-verify` (`mvn verify`: tests + ArchUnit + coverage + Testcontainers IT) · `policy-gate` (`opa check/fmt/test`) |
| [`supply-chain.yml`](.github/workflows/supply-chain.yml) | gitleaks · Trivy fs · CycloneDX SBOM · image build + scan (ADR-0002) |

All actions are pinned by full commit SHA. See [`.github/workflows/README.md`](.github/workflows/README.md).

## Decisions (ADRs)

| ADR | Title | Status |
|---|---|---|
| [0001](docs/adrs/0001-secure-mcp-service-design.md) | Secure, Language-Agnostic MCP Service — Architecture & Security Design | draft |
| [0002](docs/adrs/0002-supply-chain-security.md) | Software Supply Chain Security | accepted |
| [0003](docs/adrs/0003-elasticsearch-readonly-mcp-server.md) | Elasticsearch Read-Only MCP Server — Java 25 + Spring Boot | draft |

Full list and process: [`docs/adrs/README.md`](docs/adrs/README.md) ·
[`docs/process/adr-process.md`](docs/process/adr-process.md).

## Agent harness & skills

[`AGENTS.md`](AGENTS.md) is the tool-neutral source of truth for any AI CLI; Claude Code
reads [`CLAUDE.md`](CLAUDE.md) and discovers the [`skills/`](skills/) via `.claude/skills`.
Reusable playbooks: `new-adr`, `review-design-doc`, `compliance-check`, `init-project`,
`software-modernization`.

## Status & next steps

ADR-0003 is **draft**. Implemented: server + guard rails, ArchUnit, ≥90% coverage gate,
server-contract suite, Testcontainers IT, read-tier OPA policy, CI. Open (ADR-0003 §7):
per-environment index allowlist + least-privilege ES role, gateway route/audience binding,
page-size defaults, and an assigned owner before the ADR advances past `draft`.
