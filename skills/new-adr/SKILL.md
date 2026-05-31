---
name: new-adr
description: Scaffold a new Architecture Decision Record (ADR) from the repo template with correct frontmatter, then register it in the ADR index. Use when the user wants to start a new ADR, record an architecture/security decision, or add a design doc to docs/adrs/.
---

# new-adr

Create a new ADR in `docs/adrs/` from the template and register it.

## Steps

1. **Gather inputs** (ask only for what's missing):
   - `title` — short, specific decision title.
   - `doc_id` — kebab-case, immutable. Derive from the title if not given; confirm it's unique (not already used by another file's frontmatter).
   - `regimes` — compliance regimes in scope (may be empty).
   - `owner`, `audience`, `tags` — optional now; `owner` is required before `accepted`.

2. **Determine the sequence number.** Find the highest `NNNN-` prefix in
   `docs/adrs/` and add 1 (currently the highest is `0002-supply-chain-security.md`).
   Zero-pad to 4 digits.

3. **Create the file** `docs/adrs/NNNN-<doc_id>.md` by copying
   `docs/adrs/_TEMPLATE.md` and filling the frontmatter:
   - `version: 0.1`, `status: draft`, `date:` today.
   - Fill `title`, `doc_id`, `regimes`, and any provided optional fields.
   - Delete the `TEMPLATE NOTES` HTML comment block.
   - Write a real 2–4 sentence `summary`.

4. **Register it** in `docs/adrs/README.md`: add a row to the index table
   (ADR number link, title, status `draft`, version `0.1`, regimes, owner).

5. **Report** the created path and remind the user that before `accepted` it must
   pass `review-design-doc` (and `compliance-check` if regimes are non-empty), per
   `docs/process/adr-process.md`.

## Rules

- Conform exactly to the frontmatter schema in `docs/process/adr-process.md` §3.
- `doc_id` must be unique and is immutable once created.
- Keep required sections (threat model, compliance mapping) — if not applicable,
  leave the heading with `Not applicable — <reason>` rather than deleting it.
