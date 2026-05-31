# ADR Process — How We Write & Version Decisions

This repo records architecture and security decisions as **ADRs** (Architecture
Decision Records) under `docs/adrs/`. This document defines how they are created,
versioned, reviewed, and retired. It is the contract that the `new-adr`,
`review-design-doc`, and `compliance-check` skills enforce.

## 1. When to write an ADR

Write one when a decision is **hard to reverse, security-relevant, or
cross-cutting** — e.g. transport choice, identity model, policy engine, data
residency, a new compliance regime. Small, local, easily-reversed choices do not
need an ADR.

## 2. File & naming convention

- One ADR per file in `docs/adrs/`.
- Filename: `NNNN-<doc_id>.md` — zero-padded sequence + the immutable `doc_id`
  (e.g. `0001-secure-mcp-service-design.md`).
- `doc_id` is assigned once and **never changes**, even across renames or
  supersession. The sequence number orders ADRs; the `doc_id` identifies them.
- Start from [`_TEMPLATE.md`](../adrs/_TEMPLATE.md). The `new-adr` skill does this for you.

> All ADRs, including the canonical reference design ADR-0001
> ([`0001-secure-mcp-service-design.md`](../adrs/0001-secure-mcp-service-design.md)),
> follow the `NNNN-<doc_id>.md` convention.

## 3. Frontmatter schema (required)

Every ADR carries YAML frontmatter. Fields:

| Field | Rule |
|---|---|
| `title` | Short, specific decision title. |
| `doc_id` | kebab-case, immutable. |
| `version` | Semantic-ish: bump on every substantive change (see §5). |
| `date` | Date of the current version, `YYYY-MM-DD`. |
| `status` | One of: `draft`, `proposed`, `accepted`, `superseded`, `deprecated`. |
| `owner` | Accountable owner. **Required before `status: accepted`.** |
| `audience` | List — who should read it. |
| `regimes` | List of compliance regimes in scope (drives `compliance-check`). |
| `tags` | Free-form discovery tags. |
| `summary` | 2–4 sentence plain-prose abstract. Shown in indexes/search. |

## 4. Lifecycle (status transitions)

```
draft ──▶ proposed ──▶ accepted ──▶ superseded
                          │
                          └────────▶ deprecated
```

- **draft** — being written; not yet circulated.
- **proposed** — circulated for review; owner assigned.
- **accepted** — approved and in force. Requires a named `owner`.
- **superseded** — replaced by a newer ADR. Add a `Superseded by: <doc_id>` line
  at the top and link both ways. Never delete a superseded ADR.
- **deprecated** — no longer in force and not replaced.

## 5. Versioning rules

- **Substantive change** (alters a decision, control, or scope) → bump `version`
  and update `date`. Summarize the change in a short changelog line at the bottom
  if the ADR is `accepted`.
- **Typo / formatting only** → no version bump required.
- Going from `proposed` → `accepted` is itself a version bump.

## 6. Review gates

Before an ADR reaches `accepted`, it must pass:

1. **Completeness** — run `review-design-doc`. Threat-model and compliance
   sections present (or explicitly marked "Not applicable — <reason>"), open
   items tracked, internal links valid.
2. **Compliance mapping** — if `regimes` is non-empty, run `compliance-check` to
   confirm every declared regime appears in the control-mapping table with no
   gaps.
3. **Owner assigned** — `owner` frontmatter is filled.
4. **Separation of duties** for security-critical ADRs — author and approver are
   different people.

## 7. The ADR index

`docs/adrs/README.md` is the index. Add a row when you create an ADR; update its
status when it changes. The `new-adr` skill registers new ADRs automatically.
