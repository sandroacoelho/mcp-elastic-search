# Architecture Decision Records (ADRs)

Decisions that are hard to reverse, security-relevant, or cross-cutting are
recorded here. See [`../process/adr-process.md`](../process/adr-process.md) for
how ADRs are written, versioned, and retired, and
[`_TEMPLATE.md`](./_TEMPLATE.md) for the starting template.

To create a new ADR, use the **`new-adr`** skill — it scaffolds the file with
correct frontmatter and adds a row to the index below.

## Index

| ADR | Title | Status | Version | Regimes | Owner |
|-----|-------|--------|---------|---------|-------|
| [0001](./0001-secure-mcp-service-design.md) | Secure, Language-Agnostic MCP Service — Architecture & Security Design | draft | 1.8 | GDPR, LGPD, HIPAA, PCI-DSS, SOC2 | _unassigned_ |
| [0002](./0002-supply-chain-security.md) | Software Supply Chain Security | accepted | 1.0 | — | security architecture |
| [0003](./0003-elasticsearch-readonly-mcp-server.md) | Elasticsearch Read-Only MCP Server — Java 25 + Spring Boot | draft | 0.1 | GDPR, LGPD, HIPAA, PCI-DSS, SOC2 | _unassigned_ |

> **Status legend:** draft · proposed · accepted · superseded · deprecated.
> ADR-0001 is the **canonical reference design** for this template — read it before
> writing project-specific ADRs.
