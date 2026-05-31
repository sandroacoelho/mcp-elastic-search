# CLAUDE.md — Claude Code Entry Point

The full agent guide is **tool-neutral** and lives in `AGENTS.md` (shared by every
AI CLI), so there is a single source of truth. It is imported below.

Claude Code specifics:
- **Skills** are discovered at `.claude/skills/`, which is a symlink to the
  canonical [`skills/`](./skills/) directory. Prefer the relevant skill for common
  tasks; the skill list and conventions are in the imported guide.

@AGENTS.md
