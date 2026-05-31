# Risk Register

Required by [ADR-0001 §14](../adrs/0001-secure-mcp-service-design.md). Tracks
identified risks, their treatment, and any **accepted residual risk** (with who
accepted it). Seeded with examples drawn from the §3 threat model — replace with
your real entries.

> Status: **TEMPLATE — example rows below; replace before use.**

Scoring: Likelihood / Impact each **L / M / H**. Treatment: mitigate · transfer ·
avoid · accept.

| ID | Risk | Source | Likelihood | Impact | Treatment | Control / mitigation | Residual | Owner | Accepted by | Review date |
|----|------|--------|------------|--------|-----------|----------------------|----------|-------|-------------|-------------|
| R-001 | Prompt/tool-poisoning steers model to dangerous calls | T1 | M | H | mitigate | Outbound DLP, signed manifests, high-risk gating (§7) | L | _tbd_ | _tbd_ | _tbd_ |
| R-002 | Data exfiltration via external-call tool args | T6 | M | H | mitigate | Egress allowlist + outbound DLP + tokenization (§6,§9) | L | _tbd_ | _tbd_ | _tbd_ |
| R-003 | Vendor/model-provider processes regulated data | §18 | M | H | mitigate/transfer | DPA/BAA, residency pinning, no-training terms (§18) | M | _tbd_ | _tbd_ | _tbd_ |
| R-004 | Audit tampering hides activity | T10 | L | H | mitigate | Hash-chained WORM + RFC 3161 anchoring (§10) | L | _tbd_ | _tbd_ | _tbd_ |

**Rules:** every **accepted** residual risk needs a named accepter and a review
date; re-review at least quarterly and on any architecture change (ties to §22).
