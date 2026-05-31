# RACI Matrix — Control Families

Required by [ADR-0001 §14](../adrs/0001-secure-mcp-service-design.md). One row per
control family (rows seeded from the §14 ownership table). Fill the role columns
with **R** (Responsible), **A** (Accountable — exactly one per row), **C**
(Consulted), **I** (Informed). Replace the example role columns with your org's
roles.

> Status: **TEMPLATE — assignments pending** (tracked in ADR-0001 §28 open items).

| Control family | Platform Security | Security Architecture | Data Governance / Privacy | SOC / SecOps | Third-Party Risk | Business Owner |
|---|---|---|---|---|---|---|
| Gateway enforcement / PEP | A/R | C | I | I | — | I |
| OPA / Rego policy / PDP | R | A | C | I | — | I |
| Tool registry & risk tiers | R | A | I | I | — | C |
| Data classification & purpose mapping | I | C | A/R | I | — | C |
| Tokenization / detokenization | R | C | A | I | C | I |
| SIEM / alert handling | I | C | I | A/R | — | I |
| Vault / KMS / HSM | A/R | C | I | I | — | I |
| Human-approval gate | I | C | C | I | — | A/R |
| Vendor assurance | I | C | C | I | A/R | I |
| Incident response | R | C | C | A/R | C | I |

**Rules:** exactly one **A** per row; **A** owns the control's evidence (see
[`evidence-calendar.md`](./evidence-calendar.md)). Review this matrix at least
annually and on any reorg.
