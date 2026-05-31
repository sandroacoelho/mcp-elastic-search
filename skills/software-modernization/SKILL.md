---
name: software-modernization
description: >-
  Modernizes legacy software safely and incrementally, language-agnostic. Reads
  the codebase to build context, maps test coverage, scans dependencies for CVEs,
  and upgrades in small, verifiable blocks — keeping the build compiling and ALL
  tests passing at every step, working in branches, and producing one report per
  stage. Use when asked to update/modernize/upgrade a legacy project, bump
  dependency versions, migrate language/framework versions, or remediate
  vulnerable dependencies (CVEs).
---

# Software Modernization

Methodology guide for modernizing any legacy codebase (Python, Java, JS/TS, Go,
Rust, .NET, etc.). The value is in the **process**: small steps, always
verifiable, always reversible, always documented.

## Non-negotiable rules

These rules apply at EVERY stage. If one cannot be met, stop and report to the
user instead of proceeding.

1. **Green build + green tests, always.** After each update (or block), the
   project MUST compile and ALL tests MUST pass, with zero failures. Never move
   to the next step with a broken build or a red test.
2. **Minor/patch with no code change → grouped into blocks.** Compatible updates
   (that don't require touching code) may be applied together.
3. **Major / breaking changes → one at a time, isolated.** Never group them.
   Each gets its own cycle: apply → compile → test → report.
4. **Minors before any major.** Before bumping a major version or breaking
   change, ensure ALL minor/patch updates have already been applied (and that the
   build and tests are green). Do not start any major while a minor/patch is
   still pending.
5. **Everything in branches.** Never work directly on the main branch.
6. **Never commit without asking first.** Always request explicit user
   confirmation before any `git commit`. (Consistent with this repo's git rule in
   `AGENTS.md` — don't init or commit unless explicitly asked.)
7. **Do not mix refactoring with upgrades.** Each block preserves behavior.
   Refactors, features, and style changes stay out — otherwise it's impossible
   to tell what broke.
8. **One report per stage** (see "Reports").

## Workflow

### Phase 0 — Baseline (step zero)

Before ANY update:

- Confirm the project compiles and **all tests pass in the current state**.
- Record current versions (language/runtime, framework, dependencies) and how to
  run the build and tests.
- **If the baseline is already broken** (failing build or red test), that becomes
  the first job: report to the user and do not start modernizing on top of a base
  that already doesn't pass.

### Phase 1 — Context (read the codebase)

- Identify language(s), runtime, framework(s), package manager, and
  manifest/lock files (`package.json`, `pom.xml`, `requirements.txt`, `go.mod`,
  `Cargo.toml`, etc.).
- Map the overall architecture, entry points, and critical modules.
- List direct dependencies and their versions; note which are badly outdated or
  deprecated.

### Phase 2 — Test coverage map

- Find the test framework and how to run it with coverage.
- Map **where there is a safety net and where there is NOT**, focusing on the
  modules that the planned updates will touch.
- **Coverage gap = blocker for a risky change.** The rule "tests always pass"
  only holds where tests exist. Before an update that touches a low/zero-coverage
  area, **add characterization tests** (that pin the current behavior) — then
  update.

### Phase 3 — Security (CVEs)

- Run the scanner: ecosystem-native tool first (`npm audit`, `pip-audit`,
  `cargo audit`, `govulncheck`, `dotnet list package --vulnerable`), with
  `osv-scanner` or Trivy as an agnostic fallback. **Record in the report which
  tool was used.**
- **Prioritize, don't just list:** severity (CVSS), direct vs. transitive
  dependency, and whether the vulnerable path is actually reachable. Fix what
  matters first.
- **Repo alignment:** this project's supply-chain decision
  ([ADR-0002](../../docs/adrs/0002-supply-chain-security.md)) standardizes on
  Trivy for vulnerability scanning and on SBOM generation. When modernizing a
  service that ships from this repo, prefer those tools and pin upgraded
  dependencies/images by digest/version, consistent with ADR-0002 §2–§3.

### Phase 4 — Update plan

Build the order of blocks:

1. **Minor/patch block(s)** first — grouped, no code change.
2. **Majors/breaking changes** next, **one at a time**, ordered by the
   **dependency graph** (a lib that depends on another goes later). Do not order
   arbitrarily. Only start the majors once ALL minors are applied and the
   build/tests are green.

For each major, before applying:

- **Read the official changelog / migration guide** — that's where the real
  instructions on what broke and how to adapt live.
- **Use deprecation warnings as a map:** step up to the **last minor of the
  current major** to surface the deprecation warnings, fix them, and only then
  jump to the next major. This turns the jump into a guided path.

### Phase 5 — Execution (loop)

Create a branch for the work (or one per isolated major). For each block:

1. Apply the update (grouped if minor; isolated if major).
2. Compile. If it fails, fix it (without mixing in unrelated refactors).
3. Run ALL tests. If they fail, fix or revert.
4. Generate the stage report.
5. **Ask the user for approval before committing.** One commit/PR per isolated
   major, for clean reverts and easy review.
6. Only then move on to the next block.

**Rollback / escape hatch:** if an update won't close (too costly, dependency
with no solution, breaking change with no viable path), **revert the branch,
document the update as deferred with the reason in the report, and move on.**
Knowing when to stop prevents the whole modernization from getting stuck on a
single library.

### Phase 6 — Wrap-up

- Final report consolidating what changed, CVEs resolved, what was deferred and
  why, and recommended next steps.

## Reports

One Markdown report per stage in `docs/modernization/`, named in sequence (e.g.,
`00-baseline.md`, `01-context.md`, `02-coverage.md`, `03-security.md`,
`04-plan.md`, `05-minor-block.md`, `06-major-<lib>.md`, ...). This keeps
modernization output under `docs/`, consistent with the repo's documentation
conventions (`docs/README.md`).

Each stage report follows the same template, so they stay comparable:

```markdown
# <Stage> — <date>

## State
Build: ✅/❌   Tests: ✅/❌ (X passing / Y total)   Coverage: Z%

## What changed
- <dependency/version before → after, or action taken>

## CVEs
- <ID> (severity) — resolved / pending / not applicable

## Risks and decisions
- <breaking change handled, deferred item + reason, etc.>

## Next steps
- <what comes in the next stage>
```

## Optional items (depending on the project)

- **CI as a gate:** if a pipeline exists, every branch must pass CI, not just the
  local tests.
- **Performance regression:** passing tests don't catch performance loss — for
  critical paths, compare a before/after benchmark.
