# AGENTS.md — Universal Agent Guide

> This is the **tool-neutral** entry point for any AI coding assistant working in
> this repo (OpenAI Codex, Cursor, Gemini CLI, Aider, Jules, Claude Code, …).
> Claude Code reads `CLAUDE.md`, which simply imports this file, so there is one
> source of truth.

This is a **clone-template** repo for standing up a secure, language-agnostic
**MCP (Model Context Protocol)** service in a highly regulated environment
— binding regimes **GDPR, LGPD, HIPAA, PCI-DSS, SOC2**, with **FedRAMP Moderate**
and AI-governance (**EU AI Act, NIST AI RMF, ISO/IEC 42001**) as alignment
overlays (ADR-0001 §13/§26). Teams clone it, then fill in their specifics and add
their own implementation.

**The documentation is the product**, with a small amount of *reference
infrastructure code* that embodies the decisions. There is no application/server
source yet — the cloning team adds that. Optimize work for clear, correct,
well-cross-linked decision documents, and keep any code faithful to the ADRs.

## Where things live

| Path | Purpose |
|---|---|
| `docs/README.md` | Documentation index — start here for navigation. |
| `docs/adrs/` | Architecture Decision Records. ADR-0001 is the canonical reference design. |
| `docs/adrs/_TEMPLATE.md` | The ADR template. New ADRs start from this. |
| `docs/adrs/README.md` | The ADR index table. |
| `docs/process/adr-process.md` | How ADRs are written, versioned, reviewed, retired. **This is the contract.** |
| `docker/` | Hardened, digest-pinned, language-agnostic reference container (builds & runs a placeholder; embodies ADR-0001 §9 + ADR-0002). |
| `policy/` | OPA/Rego authorization policy + tests — the runnable PDP from ADR-0001 §5 (`opa test policy/`). |
| `tests/server-contract/` | Language-neutral test spec every MCP server must pass (ADR-0001 §4 / QA-10). |
| `.github/workflows/` | CI enforcing ADR-0002: SHA-pinned actions, gitleaks, Trivy scan, CycloneDX SBOM, image scan, OPA policy tests. See [`.github/workflows/README.md`](.github/workflows/README.md). |
| `skills/` | Reusable, tool-neutral agent playbooks (the single source of truth — see below). |

## Skills — reusable playbooks (prefer these for common tasks)

The [`skills/`](./skills/) directory holds vendor-neutral playbooks. **When a task
matches one, open and follow its `SKILL.md`.** Claude Code can also invoke them as
skills (it sees `skills/` via the `.claude/skills` symlink); other CLIs simply read
the file. See [`skills/README.md`](./skills/README.md) for how each tool wires in.

- **[`new-adr`](./skills/new-adr/SKILL.md)** — scaffold a new ADR from the template with correct frontmatter and register it in the index.
- **[`review-design-doc`](./skills/review-design-doc/SKILL.md)** — check a design/ADR doc for completeness against repo conventions.
- **[`compliance-check`](./skills/compliance-check/SKILL.md)** — verify a doc's controls cover every regime in its `regimes:` frontmatter.
- **[`init-project`](./skills/init-project/SKILL.md)** — guide a fresh cloner through filling placeholders across the template.
- **[`software-modernization`](./skills/software-modernization/SKILL.md)** — modernize a legacy codebase safely in small, verifiable steps: map coverage, scan CVEs, upgrade in blocks, one report per stage under `docs/modernization/`.

## Conventions you must follow

- **ADR frontmatter schema** is defined in `docs/process/adr-process.md` §3. Every ADR conforms to it.
- **`doc_id` is immutable.** Filenames follow `NNNN-<doc_id>.md` for all ADRs.
- **Regimes ⇒ control mapping.** Any regime listed in `regimes:` frontmatter MUST appear in the ADR's compliance control-mapping table. Don't list a regime you haven't mapped.
- **Required sections that don't apply** (e.g. threat model on a non-security ADR) are marked `Not applicable — <reason>`, never deleted — so review tooling can tell "considered" from "forgotten".
- **Never delete a superseded ADR.** Mark it `superseded`, link forward to its replacement.
- **Match the house style** of ADR-0001: decision tables, threat tables with explicit controls, normative "must/should", vendor-agnostic phrasing (name products only as interchangeable examples).
- **Code must stay faithful to the ADRs.** In `docker/` (and any future code): base images pinned by SHA256 digest (ADR-0002 §2), non-root + read-only rootfs (ADR-0001 §9), no mutable tags. If you change a control, update or cite the ADR.

## Validating your work

"Done" for a **doc** change means:
1. Frontmatter conforms to the schema (`adr-process.md` §3).
2. `review-design-doc` passes (threat-model + compliance sections present or marked N/A, open items tracked, internal links resolve).
3. If `regimes` is non-empty, `compliance-check` passes.
4. The ADR index (`docs/adrs/README.md`) reflects the change.

"Done" for a **`docker/`** change means it still builds and runs hardened:
```bash
docker compose -f docker/docker-compose.yml up --build   # then: curl 127.0.0.1:9090/actuator/health/liveness
```
Verify it stays non-root + read-only-rootfs and the base image is digest-pinned.

"Done" for a **`policy/`** change means the policy compiles, is formatted, and all
tests pass:
```bash
opa check policy/ && opa fmt --fail policy/ && opa test policy/ -v
```

## Out of scope (for the template)

- Application/server **source** code, and CI pipelines — added by the cloning team. `docker/` ships only a hardened reference container with a placeholder service.
- Git operations — don't init or commit unless explicitly asked.
