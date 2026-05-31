# Skills — Tool-Neutral Agent Playbooks

This directory is the **single source of truth** for the repo's reusable agent
skills. Each skill is a self-contained Markdown playbook at `<name>/SKILL.md`:
plain prose any AI CLI (or human) can read and follow. There is no Claude-specific
content in the skills themselves — only the *wiring* differs per tool, and the
wiring all points back here so nothing is duplicated.

## The skills

| Skill | Use it when… |
|---|---|
| [`new-adr`](./new-adr/SKILL.md) | Starting a new Architecture Decision Record. |
| [`review-design-doc`](./review-design-doc/SKILL.md) | Checking an ADR/design doc for completeness against repo conventions. |
| [`compliance-check`](./compliance-check/SKILL.md) | Verifying a doc's controls cover every declared compliance regime. |
| [`init-project`](./init-project/SKILL.md) | Filling in this template after cloning (owner, regimes, vendors…). |
| [`software-modernization`](./software-modernization/SKILL.md) | Modernizing a legacy codebase safely, in small verifiable steps. |

## How each AI CLI consumes them

| Tool | Mechanism |
|---|---|
| **Any AGENTS.md-aware CLI** (OpenAI Codex, Cursor, Gemini CLI, Jules, Aider, …) | Reads [`../AGENTS.md`](../AGENTS.md), which indexes these skills and tells the agent to open and follow the relevant `skills/<name>/SKILL.md`. |
| **Claude Code** | Discovers them at `.claude/skills/`, which is a **symlink to this directory** (`../skills`). Each `SKILL.md`'s YAML frontmatter (`name`, `description`) lets Claude Code surface them as invocable skills. |
| **Any other tool / a human** | Point the tool at this folder, or just read the playbook directly — they're plain Markdown. |

> **Windows note:** the `.claude/skills` symlink is committed as a real symlink.
> A Windows clone only materializes it correctly with symlinks enabled — set
> `git config --global core.symlinks true` (Developer Mode or admin may be needed)
> **before** cloning. If `.claude/skills` shows up as a small text file containing
> `../skills`, symlinks were off: delete it and recreate the link with
> `cmd /c "mklink /D .claude\skills ..\skills"` (or `ln -s ../skills .claude/skills`
> in Git Bash/WSL). The canonical skills in `skills/` are unaffected either way —
> only Claude Code's discovery path depends on the link.

## Format

Each `SKILL.md` carries minimal YAML frontmatter:

```yaml
---
name: <kebab-case-id>
description: >-
  One paragraph: what the skill does and when to use it.
---
```

The frontmatter is harmless to tools that ignore it and is what Claude Code uses
for discovery. The body is tool-neutral instructions.

## Adding or changing a skill

1. Edit (or add) `skills/<name>/SKILL.md` here — this is the only copy.
2. Add a row to the table above and to [`../AGENTS.md`](../AGENTS.md).
3. No per-tool copies to update: the `.claude/skills` symlink and `AGENTS.md`
   both reference this directory, so there is nothing to keep in sync.
