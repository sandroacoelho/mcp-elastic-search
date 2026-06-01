---
title: Elasticsearch Read-Only MCP Server — Java 25 + Spring Boot
doc_id: elasticsearch-readonly-mcp-server
version: 0.1
date: 2026-05-31
status: draft
owner:
audience: [architects, security, ai-assistants]
regimes: [GDPR, LGPD, HIPAA, PCI-DSS, SOC2]
tags: [mcp, elasticsearch, search, read-only, java, spring-boot, spring-ai, archunit, jacoco, server-contract]
summary: >
  Project-specific design for a read-only Elasticsearch MCP server built on
  Java 25 and Spring Boot (Spring AI MCP server, HTTP/SSE). It exposes search
  and metadata tools only — no write, update, delete, or scripting paths — and
  sits behind the platform security gateway, honoring the minimum server safety
  contract from ADR-0001 §4. Internal architecture is enforced with ArchUnit and
  release is gated on ≥90% test coverage.
---

# Elasticsearch Read-Only MCP Server — Java 25 + Spring Boot

> **Status:** Draft v0.1 · **Date:** 2026-05-31
> **Context:** A concrete `read`-tier MCP server for the secure platform — exposes
> Elasticsearch query/metadata as MCP tools, with all primary security controls in
> the gateway (`secure-mcp-service-design`, ADR-0001).

---

## 1. Context & Problem

The platform reference design (`secure-mcp-service-design`, ADR-0001) centralizes
authn, authz, DLP, tokenization, and audit in a hardened **gateway**, and requires
each MCP server to implement only business logic plus a small **server safety
contract** (ADR-0001 §4). This ADR specifies the first concrete server: a
**read-only Elasticsearch search server** — the "Foundation / read-only server"
in the delivery phasing (ADR-0001 §27, phase 1).

Forcing constraints:

- **Read-only by mandate.** The server must expose Elasticsearch *search and
  metadata* only. Any write, update, delete, bulk, reindex, ingest, or cluster-
  mutating capability is out of scope — those are `write`-tier concerns that
  belong to a separate server behind step-up + human approval (ADR-0001 §7).
- **Regulated indices.** Indices may contain Confidential or Restricted data with
  PII/PHI/PCI overlays (ADR-0001 §24). The server must fail closed on unlabeled
  data and never widen access beyond the gateway-scoped request.
- **Language/stack chosen up front:** **Java 25 + Spring Boot**, reusing the
  team's existing Spring AI MCP-server pattern (`brightflag-mcp-database-server`)
  so operational knowledge transfers.
- **Internal-architecture enforcement and high coverage are release gates:**
  **ArchUnit** must enforce the internal layering/conventions, and **test coverage
  must be ≥90%** to merge.
- "Good" = a server that is *boring at the boundary* (gateway does the hard
  security) and *rigid internally* (a SELECT-only-equivalent query guard, an index
  allowlist, ArchUnit-enforced layers, and a contract test suite that fails closed).

This is a **project-specific ADR** that adapts ADR-0001; where the two conflict,
ADR-0001 governs platform-wide controls and this ADR governs the server internals.

## 2. Decision

Build a **read-only Elasticsearch MCP server** on Java 25 / Spring Boot, exposing a
fixed set of `read`-tier tools, fronted by the platform gateway, with an
ArchUnit-enforced internal architecture and a ≥90% coverage release gate.

| Dimension | Decision |
|---|---|
| **Language / runtime** | **Java 25** (LTS), toolchain-pinned; preview features **off**. |
| **Framework** | **Spring Boot** (3.5.x line) with the **Spring AI MCP server** starter (`spring-ai-starter-mcp-server-webmvc`). Build with Maven, pinned to a known-good GA (`maven-mirror-newer-versions`). |
| **Transport** | Remote **HTTP / SSE** (`GET /elastic`, messages `POST /elastic/message?sessionId=…`, default port `9090`), per ADR-0001 transport decision. Reachable **only** from the gateway (ADR-0001 §4 invariant). |
| **Capability tier** | **`read` only.** No `write` / `bulk` / `external` / `file-write` tools exist in this server (ADR-0001 §7). |
| **Tools exposed** | `listIndices`, `describeIndex` (mappings + settings), `search` (query DSL, bounded), `getDocument` (by id), `count`. Closed set; adding one is a threat-model + review event (ADR-0001 §23/§25). |
| **Query guard** | A read-only **`QueryGuard`** (analogous to the DB server's `SqlGuard`): allow only search/read request shapes; **reject** `script`/`script_score`/`runtime_mappings` scripting, `_msearch`, `_sql`, `_painless_execute`, update-by-query, delete-by-query, and any mutating endpoint. |
| **Index allowlist** | Tools operate only on indices in a configured **allowlist** (logical name → concrete index/alias). Wildcards resolve **within** the allowlist; `.`-prefixed system/internal indices are denied. Unlisted index → fail closed. |
| **Result bounding** | Mandatory `size`/`from` caps and `max_result_window` ceiling; default and max page size; per-request `timeout`; `terminate_after` guard; `_source` field projection driven by gateway obligations. Deep pagination beyond the window is denied (use a bounded `search_after`, not unbounded scroll). |
| **ES connection** | Official Elasticsearch Java client over **TLS**, authenticated with a **least-privilege** API key/role: `read` + `view_index_metadata` on allowlisted indices **only**; no write, no cluster-admin, no `manage`. Credentials are dynamic/short-lived from Vault where available (ADR-0001 §11). |
| **Server safety contract** | Implements ADR-0001 §4 and the `tests/server-contract` suite (QA-10): gateway-only audience-bound tokens, obligation enforcement (row cap, field projection, redaction, tokenization), resource/purpose scoping, structured audit without raw payloads, fail-closed on missing classification. |
| **Internal architecture** | Enforced by **ArchUnit** (see §2.1). Hexagonal-ish layering: `tool/api → service (domain) → port → elasticsearch adapter`. The ES client is reachable **only** through the adapter package. |
| **Coverage gate** | **JaCoCo** with a build-failing threshold: **≥90%** (line + branch) on the coverage-relevant scope; CI fails below it. ArchUnit + contract + unit + Testcontainers integration tests all run in CI. |
| **Observability** | OpenTelemetry SDK + OTLP only — no vendor agent (ADR-0001 §10). Audit metadata carries query hash, index(es), tool+version, decision id; never raw documents. |
| **Optional local auth gate** | An optional bearer/API-key gate for non-gateway/dev contexts (off when blank), mirroring the DB server; **never** a substitute for the gateway in production. |

### 2.1 ArchUnit-enforced internal rules (normative)

ArchUnit tests run as part of the normal test suite and **must** be release-
blocking. The internal architecture **must** satisfy at least:

| # | Rule |
|---|---|
| A1 | **Layering:** `tool`/`api` may depend on `service`; `service` may depend on `port`; only the `elasticsearch` adapter may implement a `port`. No back-edges (adapter must not call `tool`/`service`). |
| A2 | **Datastore encapsulation:** classes from the Elasticsearch client packages (`co.elastic.clients..`, low-level REST) are referenced **only** inside the `..elasticsearch..` adapter package — nowhere in `tool`/`service`/`domain`. |
| A3 | **No raw mutation API surface:** no production code references update/delete/bulk/reindex/ingest client methods (defense in depth behind `QueryGuard`). |
| A4 | **Tool registration discipline:** every `@Tool`-annotated method lives in a `*Tools` class in the `tool` package and returns a typed result (no raw `Map`/`Object` leakage of the ES response). |
| A5 | **Logging hygiene:** use SLF4J only; `java.util.logging` and `System.out/err` are banned (supports "no raw payload in logs", ADR-0001 §4). |
| A6 | **Naming/placement:** guards end in `Guard`, adapters in `Adapter`/`Client`, config in `..config..`; no cycles between packages (`slices().should().beFreeOfCycles()`). |
| A7 | **Injection style:** constructor injection only; no field injection (`@Autowired` on fields forbidden) — keeps components unit-testable toward the coverage gate. |

### 2.2 Tool surface (closed set)

| Tool | Tier | Input (bounded) | Returns |
|---|---|---|---|
| `listIndices` | read | — (allowlist-scoped) | logical index names + doc counts the caller may see |
| `describeIndex` | read | logical index | mappings + relevant settings (no secrets) |
| `search` | read | logical index, guarded query DSL, page (≤ max) | hits with projected `_source`, total (bounded) |
| `getDocument` | read | logical index, id | single document, projected `_source` |
| `count` | read | logical index, guarded query DSL | match count (bounded) |

## 3. Alternatives Considered

| Option | Pros | Cons | Why not chosen |
|---|---|---|---|
| **Spring AI MCP server (chosen)** | Reuses proven `brightflag-mcp-database-server` pattern; HTTP/SSE built in; annotation-driven tools; same ops model | Couples to Spring AI MCP SDK release cadence | Best fit — operational knowledge transfers; the concurrency-safe SSE path is already validated at SDK 0.17.0 |
| Plain MCP Java SDK (no Spring) | Fewer dependencies; thinner | Re-implements DI, config, HTTP, health, metrics; diverges from the DB server | Rejected — needless divergence and more bespoke security-sensitive code |
| Expose Elasticsearch `_search` as a thin passthrough | Trivial to build | No guard, no allowlist, no projection → violates ADR-0001 §4/§6/§24; injection-prone | Rejected — a passthrough is exactly the blind trust zone ADR-0001 forbids |
| Allow scripting / `_sql` / `_msearch` for flexibility | Powerful querying | Painless scripting = code execution; `_sql`/`_msearch` widen surface and bypass per-index scoping | Rejected — out of a read-only, fail-closed posture; re-evaluate only via a new ADR |
| Coverage as advisory (no gate) | Less friction | Coverage rots; regressions ship | Rejected — user mandate is a hard ≥90% gate |

## 4. Consequences

- **Positive:** A small, auditable, read-only server that slots into the gateway
  with no security logic of its own to get wrong; the `QueryGuard` + index
  allowlist make the dangerous Elasticsearch surface unreachable by construction;
  ArchUnit keeps the datastore encapsulated so the security properties don't erode
  as the code grows; the coverage gate keeps the contract tests honest.
- **Negative / trade-offs:** Java 25 is recent — toolchain/base-image and library
  support must be confirmed (open item). A closed tool set and strict guard mean
  legitimately new query shapes require an ADR/threat-model touch, not just a code
  change. A ≥90% gate adds test-authoring cost, especially around the adapter
  (mitigated with Testcontainers).
- **Follow-on work:** the OPA/Rego `read`-tier rules + index/field obligations for
  these tools (ADR-0001 §5, `policy/`); the gateway route + audience binding for
  this server; the per-environment index allowlist and ES least-privilege role;
  wiring `tests/server-contract` (QA-10) into this server's CI.

## 5. Threat Model

Server-specific surface, layered on the platform threats in ADR-0001 §3 (T1–T10).
Every threat names its primary control(s).

| # | Threat | Vector | Primary control(s) |
|---|---|---|---|
| E1 | **Query-DSL injection / scripting RCE** | Model-supplied query embeds `script`/`script_score`/`runtime_mappings` (Painless) → code execution | `QueryGuard` rejects all scripting and non-search request shapes; ArchUnit A3 forbids mutation/scripting client calls; least-privilege ES role lacks scripting where possible (maps T1) |
| E2 | **Index/scope widening** | Query targets an unlisted index, a `*` wildcard, or `.`-prefixed system index to read beyond grant | Index **allowlist**; wildcards resolved within allowlist; system indices denied; fail-closed on unlisted (maps T7/§24) |
| E3 | **Over-collection / bulk exfiltration** | Huge `size`, deep pagination, or unbounded scroll to vacuum an index | Mandatory `size`/`from` caps, `max_result_window` ceiling, `terminate_after`, request timeout; bulk reads correlated upstream (ADR-0001 §16) (maps T7) |
| E4 | **Field-level over-exposure** | Returning `_source` fields the purpose doesn't need (e.g., PII columns) | Gateway field-projection/redaction obligations applied before response (ADR-0001 §4 C2/C3); `_source` filtering in the adapter (maps T7) |
| E5 | **Confused deputy via ES credential** | Server's broad ES API key reads data the user may not see | Least-privilege role (`read`+`view_index_metadata` on allowlisted indices only); gateway-scoped, audience-bound token required; no god-credential (maps T2/T3) |
| E6 | **Regulated payload in logs/audit** | Document bodies (PHI/PAN/PII) written to logs or traces | Structured audit = hashes/ids/metadata only; SLF4J-only + redaction (ArchUnit A5); contract test C5 (maps T10/§6) |
| E7 | **Unlabeled-data leakage** | Index/doc lacks a classification label and is served anyway | Fail closed: unknown classification → Restricted → denied unless explicitly granted (ADR-0001 §24; contract C6) |
| E8 | **Direct (gateway-bypassing) access** | Caller reaches the server's `/elastic` directly, skipping gateway controls | Network policy: ingress only from gateway (ADR-0001 §4); optional local auth gate is defense-in-depth, not the control |
| E9 | **Token passthrough / wrong audience** | Raw client token or wrong-audience token accepted | Accept only gateway-minted, audience-bound tokens; reject raw/expired/missing (contract C1; maps T3) |

## 6. Compliance Control Mapping

Server-scoped controls mapped to the regimes in `regimes:`. Platform-wide controls
(tokenization service, WORM audit-of-record, residency routing, key custody) are
inherited from ADR-0001 §13 and not re-litigated here; this table covers what *this
server* contributes or enforces locally.

| Control in this server | GDPR/LGPD | HIPAA | PCI-DSS | SOC2 |
|---|---|---|---|---|
| Read-only mandate + `QueryGuard` (no write/script paths) | ✓ (integrity, purpose limit) | ✓ (min necessary) | ✓ (Req. 7 least function) | ✓ (CC6/CC8) |
| Index allowlist + fail-closed scoping | ✓ (data minimization) | ✓ (access control) | ✓ (Req. 7) | ✓ (CC6) |
| Result/field bounding + projection obligations | ✓ (minimization, Art. 5) | ✓ (min necessary §164.514) | ✓ (scope reduction) | ✓ (CC6) |
| Gateway-only, audience-bound token enforcement | ✓ (accountability) | ✓ (access control §164.312(a)) | ✓ (Req. 7/8) | ✓ (CC6) |
| Least-privilege ES role (read + view_index_metadata) | ✓ (Art. 32) | ✓ (access control) | ✓ (Req. 7) | ✓ (CC6) |
| Tokenize/redact obligations honored before response | ✓ (minimization) | ✓ (safeguards) | ✓ (PAN never returned raw → CDE scope) | ✓ (CC6) |
| Structured audit, no raw payloads; OTLP to platform sink | ✓ (Art. 30) | ✓ (audit §164.312(b)) | ✓ (Req. 10) | ✓ (CC7) |
| Fail-closed on missing classification (→ Restricted) | ✓ (Art. 5/9) | ✓ (min necessary) | ✓ (scope) | ✓ (CC3/CC6) |
| TLS in transit to Elasticsearch | ✓ (Art. 32) | ✓ (transmission security) | ✓ (Req. 4) | ✓ (CC6) |

AI-governance overlays (EU AI Act · NIST AI RMF · ISO/IEC 42001) and FedRAMP
Moderate alignment apply via ADR-0001 §26 and are not declared as v1 regimes for
this server.

## 7. Open Items / Next Decisions

- [ ] Confirm **Java 25** base-image + Spring Boot 3.5.x / Spring AI MCP starter
      compatibility on Java 25; pin exact GA versions (build owner).
- [ ] Define the per-environment **index allowlist** (logical name → index/alias)
      and the **classification label** source for each index (data governance).
- [ ] Provision the **least-privilege Elasticsearch role/API key** and decide
      Vault-issued dynamic credentials vs. static API key per environment (security eng).
- [x] Finalize the `read`-tier **OPA/Rego** rules + field/row obligations for these
      five tools in `policy/` (security architecture). **Done:** read-tier `cap_rows`
      (data-driven `read_row_cap`) + `project_source` minimisation obligations added to
      `policy/authz.rego`; ES authorization tests in `policy/authz_elastic_test.rego`
      (`opa test policy/` → 26/26). Example index grants in `policy/data.json`.
- [ ] Decide page-size **defaults and ceilings** and the `search_after` strategy vs.
      `max_result_window` (server owner).
- [x] Confirm the **JaCoCo coverage scope** (exclusions for generated/config code)
      and whether the 90% gate is line, branch, or both (server owner). **Done:** gate is
      **line + branch ≥ 0.90**, excluding entrypoint/config/adapter/`domain.model` (see `pom.xml`).
- [ ] Wire `tests/server-contract` (QA-10) into this server's CI and add the
      release-blocking CI jobs (MCP platform owner). **Partly done:** ArchUnit (A1–A7) and the
      Testcontainers ES adapter IT (`mvn verify`, failsafe) are implemented and green; the
      server-contract suite and the CI workflow itself are still to wire.
- [ ] Assign the accountable **owner** before moving past `draft`.

---

## Appendix — Glossary & References

- **QueryGuard** — read-only validator for Elasticsearch requests; the ES analogue
  of the DB server's `SqlGuard` (`brightflag-mcp-database-server`).
- **Index allowlist** — configured set of logical→concrete indices the server may
  touch; everything else fails closed.
- **Server safety contract** — the minimum local contract every MCP server honors
  (ADR-0001 §4); tested by `tests/server-contract` (QA-10).
- **ArchUnit** — JVM library that asserts architecture rules (layering, dependency
  direction, naming, cycles) as ordinary tests.
- **JaCoCo** — JVM code-coverage tool; used here with a build-failing ≥90% threshold.

**References**
- ADR-0001 — `secure-mcp-service-design` ([0001-secure-mcp-service-design.md](./0001-secure-mcp-service-design.md)) — §4 server contract, §5 authz, §7 tiers, §10 audit, §24 classification, §27 phasing.
- ADR-0002 — `supply-chain-security` ([0002-supply-chain-security.md](./0002-supply-chain-security.md)) — pinned deps, SBOM, signed images.
- Server contract test spec — [`tests/server-contract/README.md`](../../tests/server-contract/README.md) (QA-10).
- [Model Context Protocol — specification](https://modelcontextprotocol.io/specification)
- [Spring AI — MCP server](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [Elasticsearch Java client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html) · *(OpenSearch is a compatible read target via the same guarded surface — an interchangeable example, not a commitment.)*
- [ArchUnit](https://www.archunit.org/) · [JaCoCo](https://www.jacoco.org/jacoco/) · [Testcontainers — Elasticsearch](https://java.testcontainers.org/modules/elasticsearch/)
