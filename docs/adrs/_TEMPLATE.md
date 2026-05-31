---
title: <Short, specific decision title>
doc_id: <kebab-case-stable-id>            # never changes once assigned
version: 0.1                               # bump on every substantive change (see adr-process.md)
date: <YYYY-MM-DD>                         # date of this version
status: draft                              # draft | proposed | accepted | superseded | deprecated
owner: <name or role>                      # accountable owner; required before status: accepted
audience: [architects]                     # who should read this — e.g. [architects, security, compliance, ai-assistants]
regimes: []                                # compliance regimes in scope — e.g. [GDPR, LGPD, HIPAA, PCI-DSS, SOC2]
tags: []                                   # free-form discovery tags
summary: >
  One paragraph, plain prose. What is being decided and why it matters.
  Keep it to 2–4 sentences — this is what shows up in indexes and search.
---

# <Decision title>

> **Status:** Draft v0.1 · **Date:** <YYYY-MM-DD>
> **Context:** <One line: the system/component and the forcing question.>

---

## 1. Context & Problem

What situation forces a decision? State the constraints, the regulatory/security
drivers, and what "good" looks like. Link related ADRs with their `doc_id`.

## 2. Decision

The decision, stated as a single clear position. Use a decision table for the
confirmed requirements that drive everything downstream:

| Dimension | Decision |
|---|---|
| <e.g. Transport> | <e.g. Remote HTTP (Streamable HTTP / SSE)> |

## 3. Alternatives Considered

| Option | Pros | Cons | Why not chosen |
|---|---|---|---|
| <A> | … | … | … |

## 4. Consequences

- **Positive:** what this unlocks.
- **Negative / trade-offs:** what it costs.
- **Follow-on work:** ADRs or tasks this triggers.

## 5. Threat Model (if security-relevant)

Document attack surface this decision introduces or mitigates. One row per threat;
every threat names its primary control(s).

| # | Threat | Vector | Primary control(s) |
|---|---|---|---|
| T1 | … | … | … |

## 6. Compliance Control Mapping (if regimes declared)

Map each control to the regimes in the `regimes:` frontmatter. The
`compliance-check` skill validates this table against the declared regimes.

| Control in this design | <regime> | <regime> |
|---|---|---|
| … | ✓ | ✓ |

## 7. Open Items / Next Decisions

- [ ] <Unresolved question, with an owner.>

---

## Appendix — Glossary & References

- **<TERM>** — definition.
- [Spec / standard](https://example.com)

<!--
TEMPLATE NOTES (delete this block in real ADRs):
- Sections 5 and 6 are required only when the ADR is security- or compliance-relevant.
  Keep their headings and write "Not applicable — <reason>" rather than deleting them,
  so review tooling can tell "considered and N/A" from "forgotten".
- doc_id is immutable. Filename convention: NNNN-<doc_id>.md (zero-padded sequence).
- See ../process/adr-process.md for the full lifecycle and versioning rules.
-->
