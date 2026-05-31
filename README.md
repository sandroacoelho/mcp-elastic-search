# Secure MCP Service — Blueprint Template

A **clone-and-fill template** for designing and governing a secure,
language-agnostic **Model Context Protocol (MCP)** service for highly regulated
corporate environments. The binding compliance regimes are **GDPR, LGPD, HIPAA,
PCI-DSS, and SOC2**, with **FedRAMP Moderate** alignment and AI-governance
overlays (**EU AI Act · NIST AI RMF · ISO/IEC 42001**) layered on top — see
ADR-0001 §13/§26.

The template's core idea, from [ADR-0001](docs/adrs/0001-secure-mcp-service-design.md):

> **Primary security controls do not live inside the MCP servers.** They live in a
> hardened gateway every server sits behind. Servers (Go/Java/Rust) implement
> business logic plus a small mandatory safety contract — so the service is
> simultaneously language-agnostic and high-security.

## What's in here

This is a **documentation/blueprint template** — it ships the decision records,
process, and authoring harness, plus a hardened reference container that embodies
those decisions. The cloning team adds their own gateway, policies, and servers.

```
.
├── AGENTS.md                 # Universal agent guide (any AI CLI) — single source of truth
├── CLAUDE.md                 # Claude Code entry point — imports AGENTS.md
├── skills/                   # Reusable, tool-neutral agent playbooks (canonical)
│   ├── README.md             #   how each AI CLI consumes them
│   ├── new-adr/ · review-design-doc/ · compliance-check/ · init-project/
│   └── software-modernization/
├── docs/
│   ├── README.md             # Documentation index
│   ├── adrs/
│   │   ├── 0001-secure-mcp-service-design.md   # ADR-0001 — canonical reference design
│   │   ├── 0002-supply-chain-security.md       # ADR-0002 — supply-chain security
│   │   ├── _TEMPLATE.md      # ADR template
│   │   └── README.md         # ADR index
│   └── process/
│       └── adr-process.md    # How ADRs are written/versioned/reviewed
├── docker/                   # Hardened, digest-pinned reference container (builds & runs)
│   ├── Dockerfile            #   non-root, read-only rootfs, digest-pinned base
│   ├── docker-compose.yml    #   runtime isolation flags (ADR-0001 §9)
│   ├── .dockerignore
│   └── README.md             #   controls ↔ ADR mapping; how to swap in your server
├── policy/                   # OPA/Rego authorization (PDP) — runnable + tested (ADR-0001 §5)
│   ├── authz.rego · authz_test.rego · data.json
│   └── README.md
├── tests/
│   └── server-contract/      # Language-neutral test spec every MCP server must pass (§4/QA-10)
├── .github/
│   ├── workflows/
│   │   └── supply-chain.yml  # CI: SHA-pinned actions, gitleaks, Trivy, SBOM, image scan (ADR-0002)
│   └── README.md             # what CI enforces + SHA re-pin procedure
├── .pre-commit-config.yaml   # Local secret scanning + hygiene (gitleaks, detect-private-key)
├── .gitleaks.toml            # gitleaks rules + allowlist
├── .gitignore                # Security-first: blocks secrets, keys, regulated data, audit logs
└── .claude/
    └── skills -> ../skills   # symlink so Claude Code discovers the canonical skills
```

The container builds and runs out of the box (a placeholder health service), so
the hardening is verifiable — replace the placeholder with your MCP server. See
[`docker/README.md`](docker/README.md).

## Works with any AI CLI

The agent harness is **tool-neutral**. [`AGENTS.md`](AGENTS.md) is the single
source of truth, read by any AGENTS.md-aware assistant (OpenAI Codex, Cursor,
Gemini CLI, Aider, Jules…). Claude Code reads [`CLAUDE.md`](CLAUDE.md), which just
imports `AGENTS.md`, and discovers the skills through the `.claude/skills`
symlink. One copy of every skill, no per-tool duplication — see
[`skills/README.md`](skills/README.md).

## Skills

Reusable, vendor-neutral playbooks in [`skills/`](skills/). When a task matches
one, the agent opens and follows its `SKILL.md`.

| Skill | Use it when… |
|---|---|
| [`new-adr`](skills/new-adr/SKILL.md) | Starting a new Architecture Decision Record. |
| [`review-design-doc`](skills/review-design-doc/SKILL.md) | Checking an ADR/design doc for completeness against repo conventions. |
| [`compliance-check`](skills/compliance-check/SKILL.md) | Verifying a doc's controls cover every declared compliance regime. |
| [`init-project`](skills/init-project/SKILL.md) | Filling in this template after cloning (owner, regimes, vendors…). |
| [`software-modernization`](skills/software-modernization/SKILL.md) | Modernizing a legacy codebase safely, in small verifiable steps. |

## Decisions (ADRs)

| ADR | Title | Status |
|---|---|---|
| [0001](docs/adrs/0001-secure-mcp-service-design.md) | Secure, Language-Agnostic MCP Service — Architecture & Security Design | draft |
| [0002](docs/adrs/0002-supply-chain-security.md) | Software Supply Chain Security | accepted |

Full list and process: [`docs/adrs/README.md`](docs/adrs/README.md) ·
[`docs/process/adr-process.md`](docs/process/adr-process.md).

## Getting started (after cloning)

1. **Read** [ADR-0001](docs/adrs/0001-secure-mcp-service-design.md) — the reference design.
2. **Run the `init-project` skill** to fill in your project name, owner, chosen
   compliance regimes, IdP, and vendors across the docs.
3. **Write decisions** with the `new-adr` skill as you adapt the design.
4. **Review** with `review-design-doc` and `compliance-check` before marking any
   ADR `accepted`.

See [`docs/README.md`](docs/README.md) for the full documentation map.
