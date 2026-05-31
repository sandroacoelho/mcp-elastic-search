# CI — Supply Chain Security

[`workflows/supply-chain.yml`](workflows/supply-chain.yml) operationalizes
[ADR-0002](../docs/adrs/0002-supply-chain-security.md) (status: **accepted**). It
runs on every PR and on pushes to `main`.

## What it enforces

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
