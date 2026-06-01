# CI Workflows

Two workflows run on every PR and on pushes to `main`.

## `ci.yml` — Build & Release Gates

The release-blocking quality gates for the server (ADR-0003) and policy (ADR-0001 §5):

| Job | Gate |
|---|---|
| `server-verify` | `mvn verify` in [`server/`](../../server): unit tests + ArchUnit (A1–A7) + JaCoCo **≥90% line/branch** + server-owned contract checks (QA-10 subset) + Testcontainers ES adapter IT. Uploads the JaCoCo report. |
| `policy-gate` | `opa check` / `opa fmt --fail` / `opa test` over [`policy/`](../../policy) — deny-by-default + read-tier obligations. |

JDK 25 (Temurin) via SHA-pinned `setup-java` with Maven caching; the integration test
uses the runner's Docker daemon (the pom pins `api.version=1.40` for modern engines).

## `supply-chain.yml` — Supply Chain Security

Operationalizes [ADR-0002](../../docs/adrs/0002-supply-chain-security.md) (status: **accepted**):

| Job | Control | ADR-0002 |
|---|---|---|
| `secret-scan` | gitleaks over full history | §4 |
| `vuln-scan` | Trivy filesystem scan — fails on HIGH/CRITICAL | §2, §3 |
| `sbom` | Trivy → CycloneDX SBOM, uploaded as an artifact | §3 |
| `image` | Build the hardened `docker/` image + Trivy image scan | §2 |

Cross-cutting controls baked into the workflow:
- **Every action is pinned by full commit SHA** with a version comment — never a
  mutable tag (§1). This is the exact lesson from the March 2026 Trivy compromise.
- **Least-privilege `GITHUB_TOKEN`**: top-level `permissions: contents: read`;
  jobs widen scope only if they must (§1, §4).

## Local equivalent (pre-commit)

Secret scanning also runs locally via [`../.pre-commit-config.yaml`](../.pre-commit-config.yaml):

```bash
pip install pre-commit && pre-commit install
pre-commit run --all-files
```

## Not yet wired (deliberate next steps)

These are required by ADR-0002 but need project-specific setup (registry, signing
identity), so they're left for the cloning team:
- **Image signing + admission control** (Sigstore/Cosign) — §2.
- **Publishing** the image/SBOM to a registry with provenance (SLSA) — §2, §5.
- **SARIF upload** to GitHub code scanning (needs `security-events: write`) — the
  workflow currently fails the build on findings instead.

## Re-pinning action SHAs

Pins are immutable by design. When intentionally upgrading an action, resolve the
new tag's commit SHA and update both the `uses:` SHA and the `# vX.Y.Z` comment:

```bash
# Resolve a tag to its commit SHA (handles annotated tags):
gh api repos/<owner>/<repo>/git/ref/tags/<tag> \
  --jq 'if .object.type=="tag"
        then .object.url else .object.sha end'
# If it returned a URL (annotated tag), fetch it and read .object.sha.
```

Then re-run the workflow to confirm green before merging.
