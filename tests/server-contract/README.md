# Server Security Contract — Test Skeleton

Every MCP server (Go/Java/Rust) must pass these tests before production, per the
**Minimum MCP server security contract** in
[ADR-0001 §4](../../docs/adrs/0001-secure-mcp-service-design.md) and the **QA-10**
row of the §23/§758 acceptance matrix.

This is a **language-neutral spec**, not a runner: each server implements the
cases in its own test framework (Go `testing`, JUnit, Rust `#[test]`) and wires
them into that server's CI. The cases are black-box — drive the server's HTTP
surface, assert the outcome. **Every ambiguous/negative case must fail closed.**

## Contract test cases

Legend: ✅ = must succeed · ⛔ = must be rejected (fail closed: 4xx + audit event, no data leak).

### C1 — Accept only gateway-issued, audience-bound downstream tokens
| # | Given | Expect |
|---|---|---|
| C1.1 | A valid gateway-minted token with correct audience | ✅ request proceeds |
| C1.2 | A raw client/upstream token (not exchanged by the gateway) | ⛔ |
| C1.3 | A token with the wrong audience | ⛔ |
| C1.4 | An expired token | ⛔ |
| C1.5 | No token | ⛔ |

### C2 — Enforce gateway obligations that shape the query/result
| # | Given | Expect |
|---|---|---|
| C2.1 | Obligation `cap_rows: 1000`, query that would return more | ✅ result capped at 1000 |
| C2.2 | Obligation `tokenize pan` | ✅ response contains surrogate, never raw PAN |
| C2.3 | Field-projection / redaction obligation | ✅ only permitted fields returned |
| C2.4 | Obligation present but server ignores it | ⛔ (contract violation — must apply before responding) |

### C3 — Preserve idempotency for mutating actions
| # | Given | Expect |
|---|---|---|
| C3.1 | Two identical write calls, same idempotency key + same context | ✅ action executes once; second returns the first result |
| C3.2 | Same idempotency key, **changed** request context | ⛔ |
| C3.3 | Mutating call with no idempotency key | ⛔ |

### C4 — Constrain access to the requested resource and purpose
| # | Given | Expect |
|---|---|---|
| C4.1 | Request scoped to subject/resource X | ✅ returns only X |
| C4.2 | Attempt to widen the filter beyond the scoped resource | ⛔ |
| C4.3 | Attempt to read unrequested fields | ⛔ |
| C4.4 | Attempt to query a different data subject | ⛔ |

### C5 — Emit structured audit metadata without raw regulated payloads
| # | Given | Expect |
|---|---|---|
| C5.1 | Any handled request | ✅ audit event has request id, args hash, tool+version, decision id |
| C5.2 | Inspect emitted logs/audit for PAN/PHI/PII/secrets | ⛔ none present (raw regulated payloads never logged) |

### C6 — Fail closed on missing classification or policy context
| # | Given | Expect |
|---|---|---|
| C6.1 | Data with no classification label | ✅ treated as Restricted → ⛔ unless explicitly granted |
| C6.2 | Missing policy/decision context from the gateway | ⛔ |

## Wiring into CI

Add a `server-contract` job to each server's own pipeline that runs these cases
against a locally-started instance. Gate releases on it (QA-10: "Every server
release"). Keep this spec in sync with ADR-0001 §4 if the contract changes.
