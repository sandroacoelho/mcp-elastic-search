# System Security Plan (SSP) / Control Narrative

Required by [ADR-0001 §14](../adrs/0001-secure-mcp-service-design.md). A single
document that describes the system, its authorization boundary, and how each
control family is implemented — pointing at the ADR for design detail and at the
evidence calendar for proof of operation.

> Status: **TEMPLATE — fill the _tbd_ fields during `init-project`.**

## 1. System description
- **System name:** _tbd_
- **Owner / accountable exec:** _tbd_
- **Purpose:** secure, language-agnostic MCP service for regulated data.
- **Data subjects & regions:** EU / Brazil / US (region-pinned — ADR-0001 §12).
- **Binding compliance regimes:** GDPR, LGPD, HIPAA, PCI-DSS, SOC2 (+ FedRAMP
  Moderate alignment; AI-governance overlays — §13/§26).

## 2. Authorization boundary
- **In boundary:** gateway (PEP), OPA (PDP), MCP servers, audit pipeline, Vault/KMS,
  tokenization service, regional datastores.
- **Out of boundary / inherited:** hosting platform physical + infra controls,
  external IdP(s), model provider(s) — covered by vendor assurance (§18).
- Attach the **segmentation diagram** and **CDE boundary** (PCI, §6) — see
  [`data-flow-diagrams.md`](./data-flow-diagrams.md).

## 3. Control narrative (by family → ADR design → evidence)

| Control family | Implementation summary | ADR | Evidence |
|---|---|---|---|
| Identity & authn | Per-user OIDC, sender-constrained tokens, token exchange | §5 | access certs, token logs |
| Authorization (PDP) | OPA deny-by-default + obligations | §5, `policy/` | policy tests, bundle metadata |
| High-risk gating | Step-up auth + human approval | §7 | approval records |
| Data protection | Tokenization, DLP, minimization, encryption | §6 | DLP/tokenization logs |
| Runtime isolation | Non-root, read-only FS, egress allowlist | §9, `docker/` | image scans, netpol tests |
| Audit & observability | OTel → SIEM → WORM, hash-chain + anchoring | §10 | WORM/anchor verification |
| Secrets & keys | Vault dynamic creds, HSM-backed KMS | §11 | rotation/access logs |
| Residency | Region-pinned processing & keys | §12 | residency config review |
| Supply chain | SHA-pinned CI, SBOM, signed images | ADR-0002, `.github/` | CI runs, SBOM artifacts |
| Server contract | Local safety contract per server | §4 | `tests/server-contract/` |

## 4. Roles & responsibilities
See [`raci-matrix.md`](./raci-matrix.md).

## 5. Continuous monitoring
See [`evidence-calendar.md`](./evidence-calendar.md) and ADR-0001 §22.

## 6. Risk & exceptions
See [`risk-register.md`](./risk-register.md) and [`exception-register.md`](./exception-register.md).
