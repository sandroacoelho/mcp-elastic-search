---
name: review-design-doc
description: Review a design or ADR document for completeness against this repo's conventions — frontmatter schema, threat model present, controls mapped, open items tracked, internal links valid, house style. Use when asked to review/check an ADR or design doc before it advances toward accepted.
---

# review-design-doc

Check a target doc (an ADR or design doc, usually under `docs/`) for completeness
against repo conventions. This is a **completeness and conventions** review — for
compliance-coverage specifically, defer to the `compliance-check` skill.

## Inputs

- The target file path. If not given, ask or infer from the most recently edited
  doc under `docs/`.

## Checklist

Read `docs/process/adr-process.md` first (it is the contract), then evaluate:

1. **Frontmatter schema (§3 of adr-process):** all required fields present and
   well-formed — `title`, `doc_id` (kebab-case), `version`, `date` (YYYY-MM-DD),
   `status` (valid enum), `audience`, `regimes`, `tags`, `summary` (2–4 sentences).
2. **Owner gate:** if `status: accepted`, `owner` must be filled.
3. **Threat model:** present for security-relevant docs, with one row per threat
   and an explicit primary control for each — or the heading marked
   `Not applicable — <reason>`.
4. **Compliance mapping section:** present if `regimes` is non-empty (deep
   coverage check is the `compliance-check` skill's job — here just confirm the
   section exists and isn't empty).
5. **Open items:** unresolved questions are tracked as a checklist, ideally with
   owners.
6. **Internal links resolve:** every relative link and `doc_id` cross-reference
   points at something real.
7. **House style:** decision tables for confirmed requirements; normative
   "must/should"; vendor-agnostic phrasing (named products framed as
   interchangeable examples, not dependencies); no raw regulated data in examples.
8. **Index sync:** the doc has a matching, accurate row in `docs/adrs/README.md`.

## Output

Produce a concise report grouped as:
- **Blockers** — must fix before the doc advances (missing required field, broken
  link, missing threat/compliance section that isn't marked N/A).
- **Recommendations** — style/clarity improvements.
- **Passed** — checks that are clean.

Do not edit the doc unless the user asks; default to reporting.
