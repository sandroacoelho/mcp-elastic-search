# Documentation Index

This repo is a **clone-template** for standing up a secure, language-agnostic MCP
service in a regulated environment. The documentation is the product — code is
expected to be added by the team that clones it.

## Map

This index covers `docs/`. The repo also ships the agent harness and reference
code outside `docs/` (linked below for orientation).

| Area | Path | What's there |
|---|---|---|
| **ADRs** | [`adrs/`](./adrs/) | Architecture & security decision records. Start with [ADR-0001](./adrs/0001-secure-mcp-service-design.md), the canonical reference design. |
| **ADR index** | [`adrs/README.md`](./adrs/README.md) | The list of all ADRs with status. |
| **ADR template** | [`adrs/_TEMPLATE.md`](./adrs/_TEMPLATE.md) | Starting point for a new ADR. |
| **Process** | [`process/adr-process.md`](./process/adr-process.md) | How ADRs are written, versioned, reviewed, and retired. |
| **Governance** | [`governance/`](./governance/) | Required §14 corporate artifact templates: RACI, SSP, data-flow diagrams, risk & exception registers, evidence calendar. |
| **Agent guide** | [`../AGENTS.md`](../AGENTS.md) | Tool-neutral guide for any AI CLI (Claude Code reads `../CLAUDE.md`, which imports it). |
| **Skills** | [`../skills/`](../skills/) | Reusable, vendor-neutral agent playbooks (`new-adr`, `review-design-doc`, `compliance-check`, `init-project`, `software-modernization`). |
| **Reference container** | [`../docker/`](../docker/) | Hardened, digest-pinned container embodying ADR-0001 §9 + ADR-0002. |

## Where to start

- **Cloning this template for a new project?** Run the **`init-project`** skill to
  fill in owner, project name, chosen regimes, IdP, and vendors across the docs.
- **Writing a new decision?** Run **`new-adr`**.
- **Reviewing a doc before acceptance?** Run **`review-design-doc`**, then
  **`compliance-check`** if it declares compliance regimes.

## Conventions

- All design content lives in Markdown under `docs/`.
- Every ADR has YAML frontmatter conforming to the schema in
  [`process/adr-process.md`](./process/adr-process.md) §3.
- Cross-link ADRs by their immutable `doc_id`.
- Compliance regimes named in an ADR's `regimes:` frontmatter **must** appear in
  its control-mapping table.
