---
name: compliance-check
description: Cross-check a document's controls against its declared compliance regimes (GDPR, LGPD, HIPAA, PCI-DSS, SOC2, FedRAMP, EU AI Act, NIST AI RMF, ISO 42001) and flag gaps in the control-mapping table. Use when asked to verify compliance coverage of an ADR/design doc, or before marking a regime-bearing ADR accepted.
---

# compliance-check

Verify that every compliance regime an ADR claims to address is actually mapped to
concrete controls — and surface gaps. This is narrower and deeper than
`review-design-doc`: it is specifically about regime ↔ control coverage.

## Inputs

- The target file path. Read its `regimes:` frontmatter — that's the source of
  truth for what must be covered. If `regimes` is empty, report "no regimes
  declared; nothing to check" and stop.

## Procedure

1. **Extract declared regimes** from frontmatter.
2. **Locate the control-mapping table** (the section that maps controls →
   regimes; in ADR-0001 this is §13 "Compliance Control Mapping").
3. **Coverage check — every declared regime must appear** as a mapped column/row
   with at least one control. Flag any regime in frontmatter that is absent from
   the table (a "claimed but unmapped" gap).
4. **Reverse check** — flag any regime appearing in the table but NOT in
   frontmatter (drift; either add it to frontmatter or remove the column).
5. **Substance spot-check** per regime — confirm the mapped controls are
   plausible for that regime's core obligations, e.g.:
   - **GDPR / LGPD** — lawful basis / purpose limitation, data-subject rights
     (DSAR/erasure), records of processing, residency/transfer, Art. 32 security.
   - **HIPAA** — access control, audit controls (§164.312(b)), minimum-necessary,
     encryption, BAAs.
   - **PCI-DSS** — CDE scope/segmentation, tokenization, Req. 10 logging,
     Req. 3 key management, Req. 7/8 access.
   - **SOC2** — CC-series (access, change mgmt, monitoring, incident response).
   - **FedRAMP** — baseline alignment, boundary, continuous monitoring.
   - **EU AI Act / NIST AI RMF / ISO 42001** — risk classification, human
     oversight, transparency, traceability, AI management system.
   Note where a regime is listed but its core obligations have no corresponding
   control row.
6. **Open items** — check that known compliance gaps are tracked in the doc's
   "Open Items" section rather than silently missing.

## Output

A table: `regime → mapped? (✓/✗) → controls found → gaps/notes`, followed by:
- **Blockers** — declared regimes with no mapping (block `accepted`).
- **Drift** — table regimes missing from frontmatter, or vice versa.
- **Thin coverage** — regimes mapped but missing an obligation worth calling out.

Report only; do not edit unless asked. Coverage checks are heuristic — recommend a
human compliance owner confirm before relying on the result.
