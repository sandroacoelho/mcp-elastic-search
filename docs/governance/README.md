# Governance Artifacts

[ADR-0001 §14](../adrs/0001-secure-mcp-service-design.md) requires a set of
corporate governance artifacts — technical controls aren't sufficient for
compliance unless each has a named owner, a review cadence, and an evidence
source. These are **fill-in templates**: structure is pre-seeded from the ADRs;
the cloning team supplies the specifics (owners, dates, decisions).

| Artifact | File | Required by |
|---|---|---|
| RACI matrix (per control family) | [`raci-matrix.md`](./raci-matrix.md) | §14 |
| System Security Plan / control narrative | [`system-security-plan.md`](./system-security-plan.md) | §14 |
| Data flow diagrams (regulated-data boundaries) | [`data-flow-diagrams.md`](./data-flow-diagrams.md) | §14, §17 |
| Risk register (with accepted residual risk) | [`risk-register.md`](./risk-register.md) | §14 |
| Exception register (owners + expiry) | [`exception-register.md`](./exception-register.md) | §14 |
| Evidence calendar (recurring control operation) | [`evidence-calendar.md`](./evidence-calendar.md) | §14, §22 |

## How to use

- Fill these in during `init-project`, or as the program stands up.
- Keep them versioned here; treat changes as governed config (ADR-0001 §15).
- Owners and review cadences map to the [§14 control-ownership table] and the
  [§22 evidence matrix] — keep them in sync with the ADR.

[§14 control-ownership table]: ../adrs/0001-secure-mcp-service-design.md
[§22 evidence matrix]: ../adrs/0001-secure-mcp-service-design.md
